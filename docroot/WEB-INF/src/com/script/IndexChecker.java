package com.script;

import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.dao.shard.ShardUtil;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.search.BaseIndexer;
import com.liferay.portal.kernel.search.Indexer;
import com.liferay.portal.kernel.search.SearchException;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.Group;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.service.CompanyLocalServiceUtil;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.PortletLocalServiceUtil;
import com.liferay.portal.util.comparator.PortletLuceneComparator;
import com.script.data.Data;
import com.script.index.IndexWrapper;
import com.script.index.IndexWrapperLuceneReflection;
import com.script.model.BaseModel;
import com.script.model.BaseModelUtil;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class IndexChecker {

	public PrintWriter out = null;
	
	public IndexChecker(PrintWriter out) {
		this.out = out;
	}

	public void dumpData(int maxLength, String filter, Set<OutputMode> outputMode, boolean reindex, boolean removeOrphan) throws IOException, SystemException{
		
		List<Company> companies = CompanyLocalServiceUtil.getCompanies();

		for (Company company : companies) {
			out.println("COMPANY: "+company);

			try {
				ShardUtil.pushCompanyService(company.getCompanyId());

				IndexWrapper indexWrapper = null;
				List<BaseModel> modelClassList = null;
	
				try {
					//indexWrapper = new IndexWrapperLucene(out, company);
					indexWrapper = new IndexWrapperLuceneReflection(out, company);
					out.println("IndexWrapper: "+indexWrapper);
					out.println("num documents: "+indexWrapper.numDocs());
					out.println("max documents: "+indexWrapper.maxDoc());
					modelClassList = getLRCompanyModelClasses(company, filter);
				}
				catch (Exception e) {
					out.println("\t" + "EXCEPTION: " + e.getClass() + " - " + e.getMessage());
					e.printStackTrace();
					return;
				}
				Map<String,Long> indexClassNameNum = indexWrapper.getClassNameNum(filter);

				List<Group> groups = GroupLocalServiceUtil.getCompanyGroups(company.getCompanyId(), QueryUtil.ALL_POS, QueryUtil.ALL_POS);
				int i = 0;
				for(BaseModel modelClass : modelClassList) {
					try {
						indexClassNameNum.remove(modelClass.getFullClassName());
						out.println("\n---------------\nClassName["+(i++)+"]: "+ modelClass.getFullClassName() +"\n---------------");

						if(outputMode.contains(OutputMode.GROUP_BY_SITE) && modelClass.hasGroupId()) {
							Map<Long, Set<Data>> indexDataMap = indexWrapper.getClassNameDataByGroupId(modelClass.getFullClassName());
							for(Group group : groups) {

								Set<Data> liferayData = modelClass.getLiferayData(company.getCompanyId(),group.getGroupId());
								Set<Data> indexData = indexDataMap.get(group.getGroupId());
								if((indexData != null && indexData.size() > 0) || liferayData.size() > 0) {
									out.println("***GROUP: "+group.getGroupId() + " - " + group.getName());
									dumpData(modelClass, liferayData, indexData, maxLength, outputMode, reindex, removeOrphan);
								}
							}
						}
						else {
							Set<Data> liferayData = modelClass.getLiferayData(company.getCompanyId());
							Set<Data> indexData = indexWrapper.getClassNameData(modelClass.getFullClassName());

							if(indexData.size() > 0 || liferayData.size() > 0) {
								if(outputMode.contains(OutputMode.GROUP_BY_SITE)) {
									out.println("***GROUP: N/A");
								}
								dumpData(modelClass, liferayData, indexData, maxLength, outputMode, reindex, removeOrphan);
							}
						}
					}
					catch (Exception e) {
						out.println("\t" + "EXCEPTION: " + e.getClass() + " - " + e.getMessage());
						e.printStackTrace();
					}
				}
	
				if(indexClassNameNum.keySet().size() > 0) {
					out.println("\n---------------\nclassNames at Index, that we didn't check!!\n---------------");
				}
				for(String className : indexClassNameNum.keySet()) {
					out.println(className + " - " + indexClassNameNum.get(className));
				}
			}
			finally {
				ShardUtil.popCompanyService();
			}
		}
	}

	public Set<Data> filterByGroup(Group group, Set<Data> originalSet) {
		Set<Data> filteredSet = new HashSet<Data>();
		for(Data data : originalSet) {
			if((group == null && data.groupId == -1) || (group != null && data.groupId == group.getGroupId())) {
				filteredSet.add(data);
			}
		}
		return filteredSet;
	}

	protected void dumpData(BaseModel modelClass, Set<Data> liferayData, Set<Data> indexData, int maxLength, Set<OutputMode> outputMode, boolean reindex, boolean removeOrphan) {
		Set<Data> bothDataSet = new HashSet<Data>(indexData);
		bothDataSet.retainAll(liferayData);
		if(outputMode.contains(OutputMode.BOTH)) {
			if(bothDataSet.size() > 0) {
				out.println("==both==");
				dumpData(modelClass.getFullClassName(), bothDataSet, maxLength);
			}
		}
		if(outputMode.contains(OutputMode.LIFERAY) || reindex) {
			liferayData.removeAll(bothDataSet);
			if(liferayData.size() > 0) {
				if(outputMode.contains(OutputMode.LIFERAY)) {
					out.println("==only liferay==");
					dumpData(modelClass.getFullClassName(), liferayData, maxLength);
				}
				if(reindex) {
					reindexData(modelClass, liferayData);
				}
			}
		}
		if(outputMode.contains(OutputMode.INDEX) || removeOrphan) {
			indexData.removeAll(bothDataSet);
			if(indexData.size() > 0) {
				if(outputMode.contains(OutputMode.INDEX)) {
					out.println("==only index==");
					dumpData(modelClass.getFullClassName(), indexData, maxLength);
				}
				if(removeOrphan) {
					deleteDataFromIndex(modelClass, indexData);
				}
			}
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

	protected void dumpData(String entryClassName, Collection<Data> liferayData, int maxLength) {

		List<Long> valuesPK = new ArrayList<Long>();
		List<Long> valuesRPK = new ArrayList<Long>();

		for(Data value : liferayData) {
			valuesPK.add(value.primaryKey);
			if(value.resourcePrimaryKey != -1) {
				valuesRPK.add(value.resourcePrimaryKey);
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

	protected String getListValues(Collection<Long> values, int maxLength) {
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

	public List<BaseModel> getLRCompanyModelClasses(Company company, String filter) 
			throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException, InstantiationException, SystemException {

		List<BaseModel> liferayCompanyModelClasses = new ArrayList<BaseModel>();

		List<Portlet> portlets = PortletLocalServiceUtil.getPortlets(
				company.getCompanyId());

		portlets = ListUtil.sort(portlets, new PortletLuceneComparator());


		for (Portlet portlet : portlets) {
			System.out.println("Portlet: "+portlet);

			if (!portlet.isActive()) {
				continue;
			}

			List<Indexer> indexers = portlet.getIndexerInstances();

			if (indexers == null) {
				continue;
			}

			for(Indexer indexer : indexers) {
				System.out.println("Indexer: "+indexer);
				try {
					BaseIndexer baseindexer = BaseModelUtil.getBaseIndexer(indexer);

					if(baseindexer != null && !baseindexer.isIndexerEnabled()) {
						continue;
					}

					String[] classNames = indexer.getClassNames();

					for(String fullClassName : classNames) {

						if(fullClassName != null && 
								(filter == null || fullClassName.contains(filter))) {

							BaseModel model = BaseModelUtil.getModel(fullClassName, indexer, baseindexer.getClass().getClassLoader());

							if(model != null && model.isIndexedModel()) {
								liferayCompanyModelClasses.add(model);
							}
						}
					}
				}
				catch (Exception e) {
					out.println("\t" + "EXCEPTION: " + e.getClass() + " - " + e.getMessage());
					e.printStackTrace();
					continue;
				}
			}
			
		}
		return liferayCompanyModelClasses;
	}

	public static void dumpData(PrintWriter out) throws IOException, SystemException {
		int maxLength = 120;
		boolean reindex = false;
		boolean removeOrphan = false;
		//maxLength = Integer.MAX_VALUE;
		String filterClassName = null;
		EnumSet<OutputMode> outputMode = EnumSet.of( OutputMode.GROUP_BY_SITE, OutputMode.BOTH, OutputMode.LIFERAY, OutputMode.INDEX);
		//EnumSet<OutputMode> outputMode = EnumSet.of( OutputMode.LIFERAY, OutputMode.INDEX);

		IndexChecker ic = new IndexChecker(out);
		ic.dumpData(maxLength, filterClassName, outputMode, reindex, removeOrphan);
	}
}

//IndexChecker.dumpData(out);
