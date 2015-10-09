package com.jorgediaz.indexchecker;

import com.jorgediaz.indexchecker.data.Data;
import com.jorgediaz.indexchecker.index.IndexWrapper;
import com.jorgediaz.indexchecker.model.IndexCheckerModel;
import com.jorgediaz.indexchecker.model.IndexCheckerModelFactory;
import com.jorgediaz.util.model.Model;
import com.jorgediaz.util.model.ModelFactory;

import com.liferay.portal.kernel.dao.orm.Criterion;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.Group;
import com.liferay.portal.service.GroupLocalServiceUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
public class IndexChecker {

	public static Map<Long, List<IndexCheckerResult>>
		executeScript(
			Class<? extends IndexWrapper> indexWrapperClass, Company company,
			List<Group> groups, List<String> classNames,
			Set<ExecutionMode> executionMode)
		throws SystemException {

		IndexWrapper indexWrapper = getIndexWrapper(indexWrapperClass, company);

		ModelFactory modelFactory = new IndexCheckerModelFactory();

		Map<String, Model> modelMap = modelFactory.getModelMap(classNames);

		Map<Long, List<IndexCheckerResult>> resultDataMap =
			new LinkedHashMap<>();

		for (Model model : modelMap.values()) {
			try {
				IndexCheckerModel icModel;
				try {
					icModel = (IndexCheckerModel)model;
				}
				catch (Exception e) {
					/* TODO DEBUG */
					System.err.println(
						"Model: " + model.getName() + " EXCEPTION: " +
							e.getClass() + " - " + e.getMessage());

					e.printStackTrace();
					icModel = null;
				}

				if (icModel == null) {
					continue;
				}

				Map<Long, IndexCheckerResult> modelDataMap =
					getData(
						indexWrapper, company.getCompanyId(), groups, icModel,
						executionMode);

				for (
					Entry<Long, IndexCheckerResult> entry :
						modelDataMap.entrySet()) {

					if (!resultDataMap.containsKey(entry.getKey())) {
						resultDataMap.put(
							entry.getKey(),
							new ArrayList<IndexCheckerResult>());
					}

					resultDataMap.get(entry.getKey()).add(entry.getValue());
				}
			}
			catch (Exception e) {
				System.err.println(
					"Model: " + model.getName() + " EXCEPTION: " +
						e.getClass() + " - " + e.getMessage());
				e.printStackTrace();
			}
		}

		return resultDataMap;
	}

	public static List<String> executeScriptGetIndexMissingClassNames(
		Class<? extends IndexWrapper> indexWrapperClass, Company company,
		List<String> classNames)
	throws SystemException {

		List<String> out = new ArrayList<String>();

		IndexWrapper indexWrapper = getIndexWrapper(indexWrapperClass, company);

		ModelFactory modelFactory = new IndexCheckerModelFactory();

		Map<String, Model> modelMap = modelFactory.getModelMap(classNames);

		Set<String> classNamesNotAvailable =
			indexWrapper.getMissingClassNamesAtLiferay(modelMap);

		if (classNamesNotAvailable.size() == 0) {
			out.add("");
			out.add("All classNames at Index also exists at Liferay :-)");

			return out;
		}

		out.add("");
		out.add("---------------");
		out.add("The following classNames exists at Index but not at Liferay!");
		out.add("---------------");

		for (String className : classNamesNotAvailable) {
			out.add(className);
		}

		return out;
	}

	public static List<String> generateOutput(
			int maxLength, Set<ExecutionMode> executionMode,
			Map<Long, List<IndexCheckerResult>> resultDataMap)
		throws SystemException {

		List<String> out = new ArrayList<String>();

		for (
			Entry<Long, List<IndexCheckerResult>> entry :
				resultDataMap.entrySet()) {

			String groupTitle = null;
			Group group = GroupLocalServiceUtil.fetchGroup(entry.getKey());

			if ((group == null) &&
				executionMode.contains(ExecutionMode.GROUP_BY_SITE)) {

				groupTitle = "N/A";
			}
			else if (group != null) {
				groupTitle = group.getGroupId() + " - " + group.getName();
			}

			if (groupTitle != null) {
				out.add("\n---------------");
				out.add("GROUP: " + groupTitle);
				out.add("---------------");

				if (executionMode.contains(
						ExecutionMode.DUMP_ALL_OBJECTS_TO_LOG)) {

					System.out.println("\n---------------");
					System.out.println("GROUP: " + groupTitle);
					System.out.println("---------------");
				}
			}

			List<IndexCheckerResult> resultList = entry.getValue();

			int i = 0;

			for (IndexCheckerResult result : resultList) {
				IndexCheckerModel model = result.getModel();
				out.add("*** ClassName["+ i +"]: "+ model.getName());

				if (executionMode.contains(
						ExecutionMode.DUMP_ALL_OBJECTS_TO_LOG)) {

					System.out.println(
						"*** ClassName["+ i +"]: "+ model.getName());
				}

				out.addAll(dumpData(result, maxLength, executionMode));

				i++;
			}
		}

		return out;
	}

	protected static List<String> dumpData(
		IndexCheckerResult data, int maxLength,
		Set<ExecutionMode> executionMode) {

		List<String> out = new ArrayList<String>();

		IndexCheckerModel model = data.getModel();
		Set<Data> exactDataSetIndex = data.getIndexExactData();
		Set<Data> exactDataSetLiferay = data.getLiferayExactData();
		Set<Data> notExactDataSetIndex = data.getIndexNotExactData();
		Set<Data> notExactDataSetLiferay = data.getLiferayNotExactData();
		Set<Data> liferayOnlyData = data.getLiferayOnlyData();
		Set<Data> indexOnlyData = data.getIndexOnlyData();

		if ((exactDataSetIndex != null) && !exactDataSetIndex.isEmpty()) {
			out.add("==both-exact==");
			out.addAll(
				dumpData(model.getName(), exactDataSetLiferay, maxLength));

			if (executionMode.contains(ExecutionMode.DUMP_ALL_OBJECTS_TO_LOG)) {
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

		if ((notExactDataSetIndex != null) && !notExactDataSetIndex.isEmpty()) {
			out.add("==both-notexact==");
			out.addAll(
				dumpData(model.getName(), notExactDataSetIndex, maxLength));

			if (executionMode.contains(ExecutionMode.DUMP_ALL_OBJECTS_TO_LOG)) {
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

		if ((liferayOnlyData != null) && !liferayOnlyData.isEmpty()) {
			out.add("==only liferay==");
			out.addAll(dumpData(model.getName(), liferayOnlyData, maxLength));

			if (executionMode.contains(ExecutionMode.DUMP_ALL_OBJECTS_TO_LOG)) {
				System.out.println("==only liferay==");

				for (Data d : liferayOnlyData) {
					System.out.println(d.getAllData(","));
				}
			}
		}

		if ((indexOnlyData != null) && !indexOnlyData.isEmpty()) {
			out.add("==only index==");
			out.addAll(dumpData(model.getName(), indexOnlyData, maxLength));

			if (executionMode.contains(ExecutionMode.DUMP_ALL_OBJECTS_TO_LOG)) {
				System.out.println("==only index==");

				for (Data d : indexOnlyData) {
					System.out.println(d.getAllData(","));
				}
			}
		}

		return out;
	}

	protected static List<String> dumpData(
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

		String listPK = IndexCheckerUtil.getListValues(valuesPK, maxLength);
		out.add(
			"\tnumber of primary keys: "+valuesPK.size()+
			"\n\tprimary keys values: ["+listPK+"]");

		Set<Long> valuesRPKset = new HashSet<Long>(valuesRPK);

		if (valuesRPKset.size()>0) {
			String listRPK = IndexCheckerUtil.getListValues(
				valuesRPKset, maxLength);
			out.add(
				"\tnumber of resource primary keys: "+ valuesRPKset.size()+
				"\n\tresource primary keys values: ["+listRPK+"]");
		}

		return out;
	}

	protected static Map<Long, IndexCheckerResult> getData(
			IndexWrapper indexWrapper, long companyId, List<Group> groups,
			IndexCheckerModel icModel, Set<ExecutionMode> executionMode)
		throws Exception {

		Map<Long, IndexCheckerResult> dataMap =
			new LinkedHashMap<Long, IndexCheckerResult>();

		if (icModel.hasGroupId() &&
			executionMode.contains(ExecutionMode.GROUP_BY_SITE)) {

			Map<Long, Set<Data>> indexDataMap =
				indexWrapper.getClassNameDataByGroupId(icModel);

			for (Group group : groups) {
				Set<Data> indexData = indexDataMap.get(group.getGroupId());

				if (indexData == null) {
					indexData = new HashSet<Data>();
				}

				List<Group> listGroupAux = new ArrayList<>();
				listGroupAux.add(group);

				IndexCheckerResult data =
					getIndexCheckResult(
						companyId, listGroupAux, icModel, indexData,
						executionMode);

				if (data != null) {
					dataMap.put(group.getGroupId(), data);
				}
			}
		}
		else {
			Set<Data> indexData = indexWrapper.getClassNameData(icModel);

			IndexCheckerResult data = getIndexCheckResult(
				companyId, groups, icModel, indexData, executionMode);

			if (data != null) {
				dataMap.put(0L, data);
			}
		}

		return dataMap;
	}

	protected static IndexCheckerResult getIndexCheckResult(
			long companyId, List<Group> groups, IndexCheckerModel icModel,
			Set<Data> indexData, Set<ExecutionMode> executionMode)
		throws Exception {

		List<Long> listGroupId = new ArrayList<>();

		for (Group group : groups) {
			listGroupId.add(group.getGroupId());
		}

		Criterion filter = icModel.getCompanyGroupFilter(
			companyId, listGroupId);

		Set<Data> liferayData = new HashSet<Data>(
			icModel.getLiferayData(filter).values());

		IndexCheckerResult data = IndexCheckerResult.getIndexCheckResult(
			icModel, liferayData, indexData, executionMode);

		return data;
	}

	protected static IndexWrapper getIndexWrapper(
			Class<? extends IndexWrapper> indexWrapperClass, Company company)
		throws SystemException {

		IndexWrapper indexWrapper;

		try {
			indexWrapper =
				indexWrapperClass.getDeclaredConstructor(
					long.class).newInstance(company.getCompanyId());
		}
		catch (Exception e) {
			System.err.println(
				"\t" + "EXCEPTION: " + e.getClass() + " - " +
					e.getMessage());
			e.printStackTrace();
			throw new SystemException(e);
		}

		return indexWrapper;
	}

}