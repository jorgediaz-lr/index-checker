package com.jorgediaz.indexchecker.model;

import com.jorgediaz.util.model.BaseModel;
import com.jorgediaz.util.model.ModelUtil;
import com.liferay.portal.kernel.bean.ClassLoaderBeanHandler;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.search.BaseIndexer;
import com.liferay.portal.kernel.search.Indexer;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.service.PortletLocalServiceUtil;
import com.liferay.portal.util.comparator.PortletLuceneComparator;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModelInfoIndexChecker {

	public static Class<? extends BaseModel> defaultModelClass = DefaultModelIndexChecker.class;

	public static Map<String, Class<? extends BaseModel>> modelClassMap = new HashMap<String, Class<? extends BaseModel>>();
	
	static {
		modelClassMap.put("com.liferay.portlet.asset.model.AssetEntry", NotIndexed.class);
		modelClassMap.put("com.liferay.portlet.calendar.model.CalendarBooking", CalendarBooking.class);
		modelClassMap.put("com.liferay.portal.model.Contact", Contact.class);
		modelClassMap.put("com.liferay.portlet.documentlibrary.model.DLFileEntry", DLFileEntry.class);
		modelClassMap.put("com.liferay.portlet.journal.model.JournalArticle", JournalArticle.class);
		modelClassMap.put("com.liferay.portlet.messageboards.model.MBMessage", MBMessage.class);
		modelClassMap.put("com.liferay.portlet.trash.model.TrashEntry", NotIndexed.class);
		modelClassMap.put("com.liferay.portal.model.User", User.class);
		modelClassMap.put("com.liferay.portlet.wiki.model.WikiNode", WikiNode.class);
		modelClassMap.put("com.liferay.portlet.wiki.model.WikiPage", WikiPage.class);
	}

	

	public ModelInfoIndexChecker(long companyId, String filter) 
			throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException, InstantiationException, SystemException {

		ModelUtil modelUtil = new ModelUtil(defaultModelClass, modelClassMap); 

		this.modelList = new ArrayList<BaseModelIndexChecker>();

		List<Portlet> portlets = PortletLocalServiceUtil.getPortlets(companyId);

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
					BaseIndexer baseindexer = ModelInfoIndexChecker.getBaseIndexer(indexer);

					if(baseindexer != null && !baseindexer.isIndexerEnabled()) {
						continue;
					}

					String[] classNames = indexer.getClassNames();

					for(String fullClassName : classNames) {

						if(fullClassName != null && 
								(filter == null || fullClassName.contains(filter))) {

							BaseModelIndexChecker model = (BaseModelIndexChecker) modelUtil.getModelObject(fullClassName);

							if(model != null) {
								modelList.add(model);
							}
						}
					}
				}
				catch (Exception e) {
					System.err.println("\t" + "EXCEPTION: " + e.getClass() + " - " + e.getMessage());
					e.printStackTrace();
					continue;
				}
			}
			
		}
	}

	protected static BaseIndexer getBaseIndexer(Indexer indexer) {
		BaseIndexer baseindexer = null;
		if(indexer instanceof BaseIndexer) {
			baseindexer = (BaseIndexer)indexer;
		}
		else if(indexer instanceof Proxy) {
			ClassLoaderBeanHandler classLoaderBeanHandler = (ClassLoaderBeanHandler)Proxy.getInvocationHandler(indexer);
			baseindexer = (BaseIndexer)classLoaderBeanHandler.getBean();
		}
		return baseindexer;
	}

	public List<BaseModelIndexChecker> getModelList() {
		return modelList;
	}

	private List<BaseModelIndexChecker> modelList = null;

}
