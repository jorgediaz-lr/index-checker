package com.jorgediaz.indexchecker;

import com.jorgediaz.indexchecker.data.Data;
import com.jorgediaz.indexchecker.index.IndexWrapper;
import com.jorgediaz.indexchecker.index.IndexWrapperLuceneReflection;
import com.jorgediaz.indexchecker.model.BaseModel;
import com.jorgediaz.indexchecker.model.ModelInfo;
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

	public void dumpData(int maxLength, String filter, Set<ExecutionMode> executionMode) throws IOException, SystemException{
		
		List<Company> companies = CompanyLocalServiceUtil.getCompanies();

		for (Company company : companies) {
			out.println("COMPANY: "+company);

			try {
				ShardUtil.pushCompanyService(company.getCompanyId());

				IndexWrapper indexWrapper = null;
				ModelInfo modelInfo = null;
	
				try {
					//indexWrapper = new IndexWrapperLucene(company.getCompanyId());
					indexWrapper = new IndexWrapperLuceneReflection(company.getCompanyId());
					out.println("IndexWrapper: "+indexWrapper);
					out.println("num documents: "+indexWrapper.numDocs());
					out.println("max documents: "+indexWrapper.maxDoc());
					modelInfo = new ModelInfo(company.getCompanyId(), filter);
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
		}
	}

	protected void dumpUncheckedClassNames(ModelInfo modelUtil,
			IndexWrapper indexWrapper) {
		Set<String> indexClassNameSet = indexWrapper.getTermValues("entryClassName");
		
		for(BaseModel modelClass : modelUtil.getModelList()) {
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

	protected void dumpData(ModelInfo modelUtil, IndexWrapper indexWrapper, Long companyId,
			List<Group> groups, Set<ExecutionMode> executionMode, int maxLength) {

		int i = 0;
		for(BaseModel modelClass : modelUtil.getModelList()) {
			try {
				out.println("\n---------------\nClassName["+(i++)+"]: "+ modelClass.getFullClassName() +"\n---------------");

				if(executionMode.contains(ExecutionMode.GROUP_BY_SITE) && modelClass.hasGroupId()) {
					Map<Long, Set<Data>> indexDataMap = indexWrapper.getClassNameDataByGroupId(modelClass);
					for(Group group : groups) {

						Set<Data> liferayData = modelClass.getLiferayData(companyId, group.getGroupId());
						Set<Data> indexData = indexDataMap.get(group.getGroupId());
						if(indexData == null) {
							indexData = new HashSet<Data>();
						}
						if((indexData.size() > 0) || liferayData.size() > 0) {
							out.println("***GROUP: "+group.getGroupId() + " - " + group.getName());
							dumpData(modelClass, liferayData, indexData, maxLength, executionMode);
						}
					}
				}
				else {
					Set<Data> liferayData = modelClass.getLiferayData(companyId);
					Set<Data> indexData = indexWrapper.getClassNameData(modelClass);

					if(indexData.size() > 0 || liferayData.size() > 0) {
						if(executionMode.contains(ExecutionMode.GROUP_BY_SITE)) {
							out.println("***GROUP: N/A");
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

	protected void dumpData(BaseModel modelClass, Set<Data> liferayData, Set<Data> indexData, int maxLength, Set<ExecutionMode> executionMode) {
		boolean reindex = executionMode.contains(ExecutionMode.REINDEX);
		boolean removeOrphan = executionMode.contains(ExecutionMode.REMOVE_ORPHAN);
		Data[] bothArrSetLiferay = getBothDataArray(liferayData, indexData);
		Data[] bothArrSetIndex = getBothDataArray(indexData, liferayData);
		if(executionMode.contains(ExecutionMode.SHOW_BOTH_EXACT) || executionMode.contains(ExecutionMode.SHOW_BOTH_NOTEXACT) || reindex) {
			if(bothArrSetIndex.length > 0 && bothArrSetLiferay.length > 0) {
				Set<Data> exactDataSet = new HashSet<Data>();
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
							exactDataSet.add(dataIndex);
						}
					}
					else if(executionMode.contains(ExecutionMode.SHOW_BOTH_NOTEXACT)) {
						notExactDataSetIndex.add(dataIndex);
						notExactDataSetLiferay.add(dataLiferay);
					}
				}
				if(exactDataSet.size() > 0 && executionMode.contains(ExecutionMode.SHOW_BOTH_EXACT)) {
					out.println("==both-exact==");
					dumpData(modelClass.getFullClassName(), exactDataSet, maxLength);
				}
				if(notExactDataSetIndex.size() > 0 && executionMode.contains(ExecutionMode.SHOW_BOTH_NOTEXACT)) {
					out.println("==both-notexact==");
					dumpData(modelClass.getFullClassName(), notExactDataSetIndex, maxLength);
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
		out.println(entryClassName+" PKsize: "+valuesPK.size()+" PKvalues: "+listPK);

		Set<Long> valuesRPKset = new HashSet<Long>(valuesRPK);
		if(valuesRPKset.size()>0) {
			String listRPK = getListValues(valuesRPKset,maxLength);
			out.println(entryClassName+" RPKsize: "+valuesRPKset.size()+" RPKvalues: "+listRPK);
		}
	}

	protected void reindexData(BaseModel modelClass, Set<Data> liferayData) {
		for(Data value : liferayData) {
			try {
				modelClass.reindex(value);
			} catch (SearchException e) {
				out.println("\t" + "EXCEPTION: " + e.getClass() + " - " + e.getMessage());
				e.printStackTrace();
			}
		}
	}

	protected void deleteDataFromIndex(BaseModel modelClass, Set<Data> liferayData) {
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
		int maxLength = 120;
		//maxLength = Integer.MAX_VALUE;
		String filterClassName = null;
		EnumSet<ExecutionMode> executionMode = EnumSet.of( ExecutionMode.GROUP_BY_SITE, ExecutionMode.SHOW_BOTH_EXACT, ExecutionMode.SHOW_BOTH_NOTEXACT, ExecutionMode.SHOW_LIFERAY, ExecutionMode.SHOW_INDEX);

		IndexChecker ic = new IndexChecker(out);
		ic.dumpData(maxLength, filterClassName, executionMode);
	}
}

