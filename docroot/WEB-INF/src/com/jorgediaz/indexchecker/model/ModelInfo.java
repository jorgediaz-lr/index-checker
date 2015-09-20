package com.jorgediaz.indexchecker.model;

import com.liferay.portal.kernel.bean.ClassLoaderBeanHandler;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.search.BaseIndexer;
import com.liferay.portal.kernel.search.Indexer;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.service.PortletLocalServiceUtil;
import com.liferay.portal.util.comparator.PortletLuceneComparator;
import com.test.ModelUtil;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModelInfo {

	public ModelInfo(long companyId, String filter) 
			throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException, InstantiationException, SystemException {

		this.modelHash = new HashMap<String, BaseModel>();
		this.modelList = new ArrayList<BaseModel>();

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

			ModelUtil modelUtil = new ModelUtil(); 

			for(Indexer indexer : indexers) {
				System.out.println("Indexer: "+indexer);
				try {
					BaseIndexer baseindexer = ModelInfo.getBaseIndexer(indexer);

					if(baseindexer != null && !baseindexer.isIndexerEnabled()) {
						continue;
					}

					String[] classNames = indexer.getClassNames();

					for(String fullClassName : classNames) {

						if(fullClassName != null && 
								(filter == null || fullClassName.contains(filter))) {

							BaseModel model = ModelInfo.createModel(modelUtil, fullClassName);

							if(model != null && model.isIndexedModel()) {
								modelHash.put(fullClassName, model);
								modelList.add(model);
							}
						}
					}
				}
				catch (Exception e) {
					System.out.println("\t" + "EXCEPTION: " + e.getClass() + " - " + e.getMessage());
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

	protected static BaseModel createModel(ModelUtil modelUtil, String fullClassName)
			throws Exception {

		BaseModel type = getModelJavaClass(fullClassName);

		type.init(modelUtil, fullClassName);

		return type;
	}

	public static BaseModel getModelJavaClass(String fullClassName)
			throws InstantiationException, IllegalAccessException {

		int lastDot = fullClassName.lastIndexOf(".");

		String className = 
			fullClassName.substring(lastDot+1,fullClassName.length());
		BaseModel type;
		
		try {
			Class<?> typeClass = Class.forName("com.jorgediaz.indexchecker.model."+className);
			System.out.println(typeClass);
			type = (BaseModel) typeClass.newInstance();
		}
		catch (ClassNotFoundException e) {
			System.out.println(e);
			type = new DefaultModel();
		}

		return type;
	}

	public BaseModel getModel(String fullClassName) {
		return modelHash.get(fullClassName);
	}

	public boolean hasModel(String fullClassName) {
		return modelHash.containsKey(fullClassName);
	}

	public List<BaseModel> getModelList() {
		return modelList;
	}

	private Map<String, BaseModel> modelHash = null;
	private List<BaseModel> modelList = null;

}
