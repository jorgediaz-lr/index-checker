package com.jorgediaz.indexchecker;

import com.jorgediaz.indexchecker.data.Data;
import com.jorgediaz.indexchecker.index.IndexWrapper;
import com.jorgediaz.indexchecker.model.IndexCheckerModel;
import com.jorgediaz.indexchecker.model.IndexCheckerModelFactory;
import com.jorgediaz.util.model.ModelFactory;

import com.liferay.portal.kernel.dao.orm.Conjunction;
import com.liferay.portal.kernel.dao.orm.PropertyFactoryUtil;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.dao.orm.RestrictionsFactoryUtil;
import com.liferay.portal.kernel.dao.shard.ShardUtil;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.model.ClassName;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.Group;
import com.liferay.portal.service.ClassNameLocalServiceUtil;
import com.liferay.portal.service.CompanyLocalServiceUtil;
import com.liferay.portal.service.GroupLocalServiceUtil;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
public class IndexChecker {

	public static String listStringToString(List<String> out) {
		if (Validator.isNull(out)) {
			return null;
		}

		StringBundler stringBundler = new StringBundler(out.size()*2);

		for (String s : out) {
			stringBundler.append(s);
			stringBundler.append(StringPool.NEW_LINE);
		}

		return stringBundler.toString();
	}

	public List<String> executeScript(
			int maxLength, String filter, Set<ExecutionMode> executionMode,
			Class<? extends IndexWrapper> indexWrapperClass)
		throws IOException, SystemException {

		List<String> out = new ArrayList<String>();

		List<Company> companies = CompanyLocalServiceUtil.getCompanies();

		for (Company company : companies) {
			long startTime = System.currentTimeMillis();
			out.add("COMPANY: "+company);

			if (executionMode.contains(ExecutionMode.DUMP_ALL_OBJECTS_TO_LOG)) {
				System.out.println("COMPANY: "+company);
			}

			try {
				ShardUtil.pushCompanyService(company.getCompanyId());

				IndexWrapper indexWrapper = null;
				ModelFactory modelFactory = null;
				List<String> classNames = new ArrayList<String>();

				try {
					indexWrapper =
						indexWrapperClass.getDeclaredConstructor(
							long.class).newInstance(company.getCompanyId());

					out.add("IndexWrapper: "+indexWrapper);
					out.add("num documents: "+indexWrapper.numDocs());

					modelFactory = new IndexCheckerModelFactory();

					List<ClassName> classNamesAux =
						ClassNameLocalServiceUtil.getClassNames(
							QueryUtil.ALL_POS, QueryUtil.ALL_POS);

					for (ClassName className : classNamesAux) {
						if ((className.getValue() != null) &&
							((filter == null) ||
							 className.getValue().contains(filter))) {

							classNames.add(className.getValue());
						}
					}
				}
				catch (Exception e) {
					out.add(
						"\t" + "EXCEPTION: " + e.getClass() + " - " +
							e.getMessage());
					e.printStackTrace();
					return out;
				}

				List<Group> groups =
					GroupLocalServiceUtil.getCompanyGroups(
						company.getCompanyId(), QueryUtil.ALL_POS,
						QueryUtil.ALL_POS);

				out.addAll(dumpUncheckedClassNames(modelFactory, indexWrapper));

				out.addAll(
					dumpData(
						modelFactory, indexWrapper, company.getCompanyId(),
						groups, classNames, executionMode, maxLength));
			}
			finally {
				ShardUtil.popCompanyService();
			}

			long endTime = System.currentTimeMillis();
			out.add(
				"\nProcessed company "+company.getCompanyId()+" in "+
					(endTime-startTime)+" ms");
			out.add(StringPool.BLANK);
		}

		return out;
	}

	protected static Data[] getBothDataArray(Set<Data> set1, Set<Data> set2) {
		Set<Data> both = new TreeSet<Data>(set1);
		both.retainAll(set2);
		return both.toArray(new Data[0]);
	}

	protected static String getListValues(
		Collection<Long> values, int maxLength) {

		String list = "";

		for (Long value : values) {
			if ("".equals(list)) {
				list = "" + value;
			}
			else {
				list = list + "," + value;
			}
		}

		if (list.length()>maxLength && (maxLength > 3)) {
			list = list.substring(0, maxLength-3) + "...";
		}

		return list;
	}

	protected List<String> dumpData(
		IndexCheckerModel modelClass, Set<Data> liferayData,
		Set<Data> indexData, int maxLength, Set<ExecutionMode> executionMode) {

		List<String> out = new ArrayList<String>();

		boolean reindex = executionMode.contains(ExecutionMode.REINDEX);
		boolean removeOrphan = executionMode.contains(
			ExecutionMode.REMOVE_ORPHAN);
		Data[] bothArrSetLiferay = getBothDataArray(liferayData, indexData);
		Data[] bothArrSetIndex = getBothDataArray(indexData, liferayData);

		if (executionMode.contains(ExecutionMode.SHOW_BOTH_EXACT) ||
			executionMode.contains(ExecutionMode.SHOW_BOTH_NOTEXACT) ||
			reindex) {

			if ((bothArrSetIndex.length > 0) &&
				(bothArrSetLiferay.length > 0)) {

				Set<Data> exactDataSetIndex = new HashSet<Data>();
				Set<Data> exactDataSetLiferay = new HashSet<Data>();
				Set<Data> notExactDataSetIndex = new HashSet<Data>();
				Set<Data> notExactDataSetLiferay = new HashSet<Data>();

				for (int i = 0; i<bothArrSetIndex.length; i++) {
					Data dataIndex = bothArrSetIndex[i];
					Data dataLiferay = bothArrSetLiferay[i];

					if (!dataIndex.equals(dataLiferay)) {
						throw new RuntimeException("Inconsistent data");
					}
					else if (dataIndex.exact(dataLiferay)) {
						if (executionMode.contains(
								ExecutionMode.SHOW_BOTH_EXACT)) {

							exactDataSetIndex.add(dataIndex);
							exactDataSetLiferay.add(dataLiferay);
						}
					}
					else if (executionMode.contains(
								ExecutionMode.SHOW_BOTH_NOTEXACT)) {

						notExactDataSetIndex.add(dataIndex);
						notExactDataSetLiferay.add(dataLiferay);
					}
				}

				if ((exactDataSetIndex.size() > 0) &&
					executionMode.contains(ExecutionMode.SHOW_BOTH_EXACT)) {

					out.add("==both-exact==");
					out.addAll(
						dumpData(
							modelClass.getName(), exactDataSetLiferay,
							maxLength));

					if (executionMode.contains(
							ExecutionMode.DUMP_ALL_OBJECTS_TO_LOG)) {

						System.out.println("==both-exact(index)==");

						for (Data d : exactDataSetIndex) {
							System.out.println(d.getAllData(","));
						}

						System.out.println("==both-exact(liferay)==");

						for (Data d : exactDataSetLiferay) {
							System.out.println(d.getAllData(","));
						}
					}
				}

				if ((notExactDataSetIndex.size() > 0) &&
					executionMode.contains(ExecutionMode.SHOW_BOTH_NOTEXACT)) {

					out.add("==both-notexact==");
					out.addAll(
						dumpData(
							modelClass.getClassName(), notExactDataSetIndex,
							maxLength));

					if (executionMode.contains(
							ExecutionMode.DUMP_ALL_OBJECTS_TO_LOG)) {

						System.out.println("==both-notexact(index)==");

						for (Data d : notExactDataSetIndex) {
							System.out.println(d.getAllData(","));
						}

						System.out.println("==both-notexact(liferay)==");

						for (Data d : notExactDataSetLiferay) {
							System.out.println(d.getAllData(","));
						}
					}
				}

				if (reindex) {
					Map<Data, String> errors = modelClass.reindex(
						notExactDataSetIndex);

					for (Entry<Data, String> error : errors.entrySet()) {
						out.add(
							"\t" + "ERROR reindexing " + error.getKey() +
							"EXCEPTION" + error.getValue());
					}
				}
			}
		}

		Set<Data> bothDataSet = new HashSet<Data>(indexData);
		bothDataSet.retainAll(liferayData);

		if (executionMode.contains(ExecutionMode.SHOW_LIFERAY) || reindex) {
			liferayData.removeAll(bothDataSet);

			if (liferayData.size() > 0) {
				if (executionMode.contains(ExecutionMode.SHOW_LIFERAY)) {
					out.add("==only liferay==");
					out.addAll(
						dumpData(modelClass.getName(), liferayData, maxLength));

					if (executionMode.contains(
							ExecutionMode.DUMP_ALL_OBJECTS_TO_LOG)) {

						System.out.println("==only liferay==");

						for (Data d : liferayData) {
							System.out.println(d.getAllData(","));
						}
					}
				}

				if (reindex) {
					Map<Data, String> errors = modelClass.reindex(liferayData);

					for (Entry<Data, String> error : errors.entrySet()) {
						out.add(
							"\t" + "ERROR reindexing " + error.getKey() +
							"EXCEPTION" + error.getValue());
					}
				}
			}
		}

		if (executionMode.contains(ExecutionMode.SHOW_INDEX) || removeOrphan) {
			indexData.removeAll(bothDataSet);

			if (indexData.size() > 0) {
				if (executionMode.contains(ExecutionMode.SHOW_INDEX)) {
					out.add("==only index==");
					out.addAll(
						dumpData(modelClass.getName(), indexData, maxLength));

					if (executionMode.contains(
							ExecutionMode.DUMP_ALL_OBJECTS_TO_LOG)) {

						System.out.println("==only index==");

						for (Data d : indexData) {
							System.out.println(d.getAllData(","));
						}
					}
				}

				if (removeOrphan) {
					Map<Data, String> errors = modelClass.deleteAndCheck(
						indexData);

					for (Entry<Data, String> error : errors.entrySet()) {
						out.add(
							"\tERROR deleting from index " + error.getKey() +
							"EXCEPTION" + error.getValue());
					}
				}
			}
		}

		return out;
	}

	protected List<String> dumpData(
		ModelFactory modelFactory, IndexWrapper indexWrapper, Long companyId,
		List<Group> groups, List<String> classNames,
		Set<ExecutionMode> executionMode, int maxLength) {

		List<String> out = new ArrayList<String>();

		int i = 0;

		for (String className : classNames) {
			try {
				IndexCheckerModel model;
				try {
					model =
						(IndexCheckerModel)modelFactory.getModelObject(
							className);
				}
				catch (Exception e) {
					/* TODO DEBUG */
					System.err.println(
						"\t" + "EXCEPTION: " + e.getClass() + " - " +
							e.getMessage());

					e.printStackTrace();
					model = null;
				}

				if (model == null) {
					continue;
				}

				out.add("\n---------------");
				out.add("ClassName["+ i +"]: "+ model.getName());
				out.add("---------------");

				if (executionMode.contains(
						ExecutionMode.DUMP_ALL_OBJECTS_TO_LOG)) {

					System.out.println("\n---------------");
					System.out.println("ClassName["+ i +"]: "+ model.getName());
					System.out.println("---------------");
				}

				i++;

				if (model.hasGroupId()) {
					Map<Long, Set<Data>> indexDataMap =
						indexWrapper.getClassNameDataByGroupId(model);

					if (executionMode.contains(ExecutionMode.GROUP_BY_SITE)) {
						for (Group group : groups) {
							List<Long> listGroupId = new ArrayList<>();
							listGroupId.add(group.getGroupId());
							Conjunction conjunction =
								RestrictionsFactoryUtil.conjunction();

							if (model.hasAttribute("companyId")) {
								conjunction.add(
									PropertyFactoryUtil.forName(
										"companyId").eq(companyId));
							}

							if (model.hasGroupId()) {
								conjunction.add(
									PropertyFactoryUtil.forName(
										"groupId").in(listGroupId));
							}

							Set<Data> liferayData =
								new HashSet<Data>(model.getLiferayData(
									conjunction).values());
							Set<Data> indexData = indexDataMap.get(
								group.getGroupId());

							if (indexData == null) {
								indexData = new HashSet<Data>();
							}

							if ((indexData.size() > 0) ||
								(liferayData.size() > 0)) {

								out.add(
									"***GROUP: "+group.getGroupId() + " - " +
										group.getName());

								if (executionMode.contains(
										ExecutionMode.DUMP_ALL_OBJECTS_TO_LOG)) {

									System.out.println(
										"***GROUP: "+group.getGroupId() +
										" - " + group.getName());
								}

								out.addAll(
									dumpData(
										model, liferayData, indexData,
										maxLength, executionMode));
							}
						}
					}
					else {
						List<Long> listGroupId = new ArrayList<>();

						for (Group group : groups) {
							listGroupId.add(group.getGroupId());
						}

						Conjunction conjunction =
							RestrictionsFactoryUtil.conjunction();

						if (model.hasAttribute("companyId")) {
							conjunction.add(
								PropertyFactoryUtil.forName(
									"companyId").eq(companyId));
						}

						if (model.hasGroupId()) {
							conjunction.add(
								PropertyFactoryUtil.forName(
									"groupId").in(listGroupId));
						}

						Set<Data> liferayData =
							new HashSet<Data>(model.getLiferayData(
								conjunction).values());
						Set<Data> indexData = indexWrapper.getClassNameData(
							model);

						if ((indexData.size() > 0) ||
							(liferayData.size() > 0)) {

							out.addAll(
								dumpData(
									model, liferayData, indexData, maxLength,
									executionMode));
						}
					}
				}
				else {
					Conjunction conjunction =
						RestrictionsFactoryUtil.conjunction();

					if (model.hasAttribute("companyId")) {
						conjunction.add(
							PropertyFactoryUtil.forName(
								"companyId").eq(companyId));
					}

					Set<Data> liferayData =
						new HashSet<Data>(model.getLiferayData(
							conjunction).values());
					Set<Data> indexData = indexWrapper.getClassNameData(model);

					if ((indexData.size() > 0) || (liferayData.size() > 0)) {
						if (executionMode.contains(
								ExecutionMode.GROUP_BY_SITE)) {

							out.add("***GROUP: N/A");

							if (executionMode.contains(
									ExecutionMode.DUMP_ALL_OBJECTS_TO_LOG)) {

								System.out.println("***GROUP: N/A");
							}
						}

						out.addAll(
							dumpData(
								model, liferayData, indexData, maxLength,
								executionMode));
					}
				}
			}
			catch (Exception e) {
				out.add(
					"\t" + "EXCEPTION: " + e.getClass() + " - " +
						e.getMessage());
				System.err.println(
					"\t" + "EXCEPTION: " + e.getClass() + " - " +
						e.getMessage());
				e.printStackTrace();
			}
		}

		return out;
	}

	protected List<String> dumpData(
		String entryClassName, Collection<Data> liferayData, int maxLength) {

		List<String> out = new ArrayList<String>();

		List<Long> valuesPK = new ArrayList<Long>();
		List<Long> valuesRPK = new ArrayList<Long>();

		for (Data value : liferayData) {
			valuesPK.add(value.getPrimaryKey());

			if (value.getResourcePrimKey() != -1) {
				valuesRPK.add(value.getResourcePrimKey());
			}
		}

		String listPK = getListValues(valuesPK, maxLength);
		out.add(
			entryClassName+"\n\tnumber of primary keys: "+valuesPK.size()+
			"\n\tprimary keys values: ["+listPK+"]");

		Set<Long> valuesRPKset = new HashSet<Long>(valuesRPK);

		if (valuesRPKset.size()>0) {
			String listRPK = getListValues(valuesRPKset, maxLength);
			out.add(
				entryClassName+"\n\tnumber of resource primary keys: "+
				valuesRPKset.size()+"\n\tresource primary keys values: ["+
				listRPK+"]");
		}

		return out;
	}

	protected List<String> dumpUncheckedClassNames(
		ModelFactory modelFactory, IndexWrapper indexWrapper) {

		List<String> out = new ArrayList<String>();

		Set<String> classNamesNotAvailable = new HashSet<String>();

		Set<String> indexClassNameSet = indexWrapper.getTermValues(
			"entryClassName");

		for (String indexClassName : indexClassNameSet) {
			IndexCheckerModel model;
			try {
				model =
					(IndexCheckerModel)modelFactory.getModelObject(
						indexClassName);
			}
			catch (Exception e) {
				model = null;
			}

			if (model == null) {
				classNamesNotAvailable.add(indexClassName);
			}
		}

		if (classNamesNotAvailable.size() > 0) {
			out.add("");
			out.add("---------------");
			out.add("classNames at Index, that we are not going to check!!");
			out.add("---------------");

			for (String className : classNamesNotAvailable) {
				out.add(className);
			}
		}

		return out;
	}

}