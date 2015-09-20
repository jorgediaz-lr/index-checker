package com.jorgediaz.indexchecker;

import com.jorgediaz.indexchecker.data.Data;
import com.jorgediaz.indexchecker.index.IndexWrapper;
import com.jorgediaz.indexchecker.index.IndexWrapperLuceneReflection;
import com.jorgediaz.indexchecker.model.BaseModelIndexChecker;
import com.jorgediaz.indexchecker.model.ModelInfoIndexChecker;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.dao.shard.ShardUtil;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.search.SearchException;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.Group;
import com.liferay.portal.service.CompanyLocalServiceUtil;
import com.liferay.portal.service.GroupLocalServiceUtil;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class IndexChecker {

	public PrintWriter out = null;
	
	public IndexChecker(PrintWriter out) {
		this.out = out;
	}

	public void dumpData(int maxLength, String filter, Set<ExecutionMode> executionMode, Class<? extends IndexWrapper> indexWrapperClass) throws IOException, SystemException{
		
		List<Company> companies = CompanyLocalServiceUtil.getCompanies();

		for (Company company : companies) {
			long startTime = System.currentTimeMillis();
			out.println("COMPANY: "+company);
			if(executionMode.contains(ExecutionMode.DUMP_ALL_OBJECTS_TO_LOG)) {
				System.out.println("COMPANY: "+company);
			}

			try {
				ShardUtil.pushCompanyService(company.getCompanyId());

				IndexWrapper indexWrapper = null;
				ModelInfoIndexChecker modelInfo = null;
	
				try {
					indexWrapper = indexWrapperClass.getDeclaredConstructor(long.class).newInstance(company.getCompanyId());
					out.println("IndexWrapper: "+indexWrapper);
					out.println("num documents: "+indexWrapper.numDocs());
					modelInfo = new ModelInfoIndexChecker(company.getCompanyId(), filter);
					out.println("ModelInfo: "+modelInfo);
				}
				catch (Exception e) {
					out.println("\t" + "EXCEPTION: " + e.getClass() + " - " + e.getMessage());
					e.printStackTrace();
					return;
				}
				List<Group> groups = GroupLocalServiceUtil.getCompanyGroups(company.getCompanyId(), QueryUtil.ALL_POS, QueryUtil.ALL_POS);
				
				dumpUncheckedClassNames(modelInfo, indexWrapper);
				
				dumpData(modelInfo, indexWrapper, company.getCompanyId(), groups, executionMode, maxLength);
			}
			finally {
				ShardUtil.popCompanyService();
			}
			long endTime = System.currentTimeMillis();
			out.println("\nProcessed company "+company.getCompanyId()+" in "+(endTime-startTime)+" ms");
			out.println();
		}
	}

	protected void dumpUncheckedClassNames(ModelInfoIndexChecker modelUtil,
			IndexWrapper indexWrapper) {
		Set<String> indexClassNameSet = indexWrapper.getTermValues("entryClassName");
		
		for(BaseModelIndexChecker modelClass : modelUtil.getModelList()) {
			indexClassNameSet.remove(modelClass.getFullClassName());
		}
		if(indexClassNameSet.size() > 0) {
			out.println("");
			out.println("---------------");
			out.println("classNames at Index, that we are not going to check!!");
			out.println("---------------");
			for(String className : indexClassNameSet) {
				out.println(className);
			}
		}
	}

	protected void dumpData(ModelInfoIndexChecker modelInfo, IndexWrapper indexWrapper, Long companyId,
			List<Group> groups, Set<ExecutionMode> executionMode, int maxLength) {

		int i = 0;
		for(BaseModelIndexChecker modelClass : modelInfo.getModelList()) {
			try {
				out.println("\n---------------\nClassName["+(i++)+"]: "+ modelClass.getFullClassName() +"\n---------------");
				if(executionMode.contains(ExecutionMode.DUMP_ALL_OBJECTS_TO_LOG)) {
					System.out.println("\n---------------\nClassName["+(i++)+"]: "+ modelClass.getFullClassName() +"\n---------------");
				}

				if(modelClass.hasGroupId()) {
					Map<Long, Set<Data>> indexDataMap = indexWrapper.getClassNameDataByGroupId(modelClass);

					if(executionMode.contains(ExecutionMode.GROUP_BY_SITE)) {
						for(Group group : groups) {
							List<Long> listGroupId = new ArrayList<>();
							listGroupId.add(group.getGroupId());
							Set<Data> liferayData = new HashSet<Data>(modelClass.getLiferayData(companyId, listGroupId).values());
							Set<Data> indexData = indexDataMap.get(group.getGroupId());
							if(indexData == null) {
								indexData = new HashSet<Data>();
							}
							if((indexData.size() > 0) || liferayData.size() > 0) {
								out.println("***GROUP: "+group.getGroupId() + " - " + group.getName());
								if(executionMode.contains(ExecutionMode.DUMP_ALL_OBJECTS_TO_LOG)) {
									System.out.println("***GROUP: "+group.getGroupId() + " - " + group.getName());
								}
								dumpData(modelClass, liferayData, indexData, maxLength, executionMode);
							}
						}
					}
					else {
						List<Long> listGroupId = new ArrayList<>();
						for(Group group : groups) {
							listGroupId.add(group.getGroupId());
						}
						Set<Data> liferayData = new HashSet<Data>(modelClass.getLiferayData(companyId, listGroupId).values());
						Set<Data> indexData = indexWrapper.getClassNameData(modelClass);

						if(indexData.size() > 0 || liferayData.size() > 0) {
							dumpData(modelClass, liferayData, indexData, maxLength, executionMode);
						}
					}
				}
				else {
					Set<Data> liferayData = new HashSet<Data>(modelClass.getLiferayData(companyId).values());
					Set<Data> indexData = indexWrapper.getClassNameData(modelClass);

					if(indexData.size() > 0 || liferayData.size() > 0) {
						if(executionMode.contains(ExecutionMode.GROUP_BY_SITE)) {
							out.println("***GROUP: N/A");
							if(executionMode.contains(ExecutionMode.DUMP_ALL_OBJECTS_TO_LOG)) {
								System.out.println("***GROUP: N/A");
							}
						}
						dumpData(modelClass, liferayData, indexData, maxLength, executionMode);
					}
				}
			}
			catch (Exception e) {
				out.println("\t" + "EXCEPTION: " + e.getClass() + " - " + e.getMessage());
				e.printStackTrace();
			}
		}
	}

	protected void dumpData(BaseModelIndexChecker modelClass, Set<Data> liferayData, Set<Data> indexData, int maxLength, Set<ExecutionMode> executionMode) {
		boolean reindex = executionMode.contains(ExecutionMode.REINDEX);
		boolean removeOrphan = executionMode.contains(ExecutionMode.REMOVE_ORPHAN);
		Data[] bothArrSetLiferay = getBothDataArray(liferayData, indexData);
		Data[] bothArrSetIndex = getBothDataArray(indexData, liferayData);
		if(executionMode.contains(ExecutionMode.SHOW_BOTH_EXACT) || executionMode.contains(ExecutionMode.SHOW_BOTH_NOTEXACT) || reindex) {
			if(bothArrSetIndex.length > 0 && bothArrSetLiferay.length > 0) {
				Set<Data> exactDataSetIndex = new HashSet<Data>();
				Set<Data> exactDataSetLiferay = new HashSet<Data>();
				Set<Data> notExactDataSetIndex = new HashSet<Data>();
				Set<Data> notExactDataSetLiferay = new HashSet<Data>();
				for(int i = 0; i<bothArrSetIndex.length; i++) {
					Data dataIndex = bothArrSetIndex[i];
					Data dataLiferay = bothArrSetLiferay[i];
					if(!dataIndex.equals(dataLiferay)) {
						throw new RuntimeException("Inconsistent data");
					}
					else if(dataIndex.exact(dataLiferay)) {
						if(executionMode.contains(ExecutionMode.SHOW_BOTH_EXACT)) {
							exactDataSetIndex.add(dataIndex);
							exactDataSetLiferay.add(dataLiferay);
						}
					}
					else if(executionMode.contains(ExecutionMode.SHOW_BOTH_NOTEXACT)) {
						notExactDataSetIndex.add(dataIndex);
						notExactDataSetLiferay.add(dataLiferay);
					}
				}
				if(exactDataSetIndex.size() > 0 && executionMode.contains(ExecutionMode.SHOW_BOTH_EXACT)) {
					out.println("==both-exact==");
					dumpData(modelClass.getFullClassName(), exactDataSetLiferay, maxLength);
					if(executionMode.contains(ExecutionMode.DUMP_ALL_OBJECTS_TO_LOG)) {
						System.out.println("==both-exact(index)==");
						for(Data d : exactDataSetIndex) {
							System.out.println(d.getAllData(","));
						}
						System.out.println("==both-exact(liferay)==");
						for(Data d : exactDataSetLiferay) {
							System.out.println(d.getAllData(","));
						}
					}
				}
				if(notExactDataSetIndex.size() > 0 && executionMode.contains(ExecutionMode.SHOW_BOTH_NOTEXACT)) {
					out.println("==both-notexact==");
					dumpData(modelClass.getFullClassName(), notExactDataSetIndex, maxLength);
					if(executionMode.contains(ExecutionMode.DUMP_ALL_OBJECTS_TO_LOG)) {
						System.out.println("==both-notexact(index)==");
						for(Data d : notExactDataSetIndex) {
							System.out.println(d.getAllData(","));
						}
						System.out.println("==both-notexact(liferay)==");
						for(Data d : notExactDataSetLiferay) {
							System.out.println(d.getAllData(","));
						}
					}
				}
				if(reindex) {
					reindexData(modelClass, notExactDataSetIndex);
				}
			}
		}
		Set<Data> bothDataSet = new HashSet<Data>(indexData);
		bothDataSet.retainAll(liferayData);
		if(executionMode.contains(ExecutionMode.SHOW_LIFERAY) || reindex) {
			liferayData.removeAll(bothDataSet);
			if(liferayData.size() > 0) {
				if(executionMode.contains(ExecutionMode.SHOW_LIFERAY)) {
					out.println("==only liferay==");
					dumpData(modelClass.getFullClassName(), liferayData, maxLength);
					if(executionMode.contains(ExecutionMode.DUMP_ALL_OBJECTS_TO_LOG)) {
						System.out.println("==only liferay==");
						for(Data d : liferayData) {
							System.out.println(d.getAllData(","));
						}
					}
				}
				if(reindex) {
					reindexData(modelClass, liferayData);
				}
			}
		}
		if(executionMode.contains(ExecutionMode.SHOW_INDEX) || removeOrphan) {
			indexData.removeAll(bothDataSet);
			if(indexData.size() > 0) {
				if(executionMode.contains(ExecutionMode.SHOW_INDEX)) {
					out.println("==only index==");
					dumpData(modelClass.getFullClassName(), indexData, maxLength);
					if(executionMode.contains(ExecutionMode.DUMP_ALL_OBJECTS_TO_LOG)) {
						System.out.println("==only index==");
						for(Data d : indexData) {
							System.out.println(d.getAllData(","));
						}
					}
				}
				if(removeOrphan) {
					deleteDataFromIndex(modelClass, indexData);
				}
			}
		}
	}

	protected void dumpData(String entryClassName, Collection<Data> liferayData, int maxLength) {

		List<Long> valuesPK = new ArrayList<Long>();
		List<Long> valuesRPK = new ArrayList<Long>();

		for(Data value : liferayData) {
			valuesPK.add(value.getPrimaryKey());
			if(value.getResourcePrimKey() != -1) {
				valuesRPK.add(value.getResourcePrimKey());
			}
		}
		
		String listPK = getListValues(valuesPK,maxLength);
		out.println(entryClassName+"\n\tnumber of primary keys: "+valuesPK.size()+"\n\tprimary keys values: ["+listPK+"]");

		Set<Long> valuesRPKset = new HashSet<Long>(valuesRPK);
		if(valuesRPKset.size()>0) {
			String listRPK = getListValues(valuesRPKset,maxLength);
			out.println(entryClassName+"\n\tnumber of resource primary keys: "+valuesRPKset.size()+"\n\tresource primary keys values: ["+listRPK+"]");
		}
	}

	protected void reindexData(BaseModelIndexChecker modelClass, Set<Data> liferayData) {
		for(Data value : liferayData) {
			try {
				modelClass.reindex(value);
			} catch (SearchException e) {
				out.println("\t" + "EXCEPTION: " + e.getClass() + " - " + e.getMessage());
				e.printStackTrace();
			}
		}
	}

	protected void deleteDataFromIndex(BaseModelIndexChecker modelClass, Set<Data> liferayData) {
		for(Data value : liferayData) {
			/* Delete object from index */
			try {
				modelClass.delete(value);
			} catch (SearchException e) {
				out.println("\t" + "EXCEPTION: " + e.getClass() + " - " + e.getMessage());
				e.printStackTrace();
			}
			/* Reindex object, perhaps we deleted it by error */
			try {
				modelClass.reindex(value);
			} catch (Exception e) {
			}
		}
	}

	protected static Data[] getBothDataArray(Set<Data> set1, Set<Data> set2) {
		Set<Data> both = new TreeSet<Data>(set1);
		both.retainAll(set2);
		return both.toArray(new Data[0]);
	}

	protected static String getListValues(Collection<Long> values, int maxLength) {
		String list = "";
		for (Long value : values) {
			if("".equals(list)) {
				list = "" + value;
			}
			else {
				list = list + "," + value;
			}
		}
		if(list.length()>maxLength && maxLength > 3) {
			list = list.substring(0, maxLength-3) + "...";
		}
		return list;
	}

	public static void dumpData(PrintWriter out) throws IOException, SystemException {
		int maxLength = 160;
		//maxLength = Integer.MAX_VALUE;
		String filterClassName = null;
		EnumSet<ExecutionMode> executionMode = EnumSet.of( ExecutionMode.GROUP_BY_SITE, ExecutionMode.SHOW_BOTH_EXACT, ExecutionMode.SHOW_BOTH_NOTEXACT, ExecutionMode.SHOW_LIFERAY, ExecutionMode.SHOW_INDEX);

		Class<? extends IndexWrapper> indexWrapperClass = IndexWrapperLuceneReflection.class;

		IndexChecker ic = new IndexChecker(out);
		ic.dumpData(maxLength, filterClassName, executionMode, indexWrapperClass);
	}
}

