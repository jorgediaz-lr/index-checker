package com.jorgediaz.indexchecker.model;

import com.liferay.portal.kernel.bean.ClassLoaderBeanHandler;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.search.BaseIndexer;
import com.liferay.portal.kernel.search.Indexer;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.service.PortletLocalServiceUtil;
import com.liferay.portal.util.comparator.PortletLuceneComparator;

import java.lang.reflect.Field;
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

							BaseModel model = ModelInfo.getModel(fullClassName, companyId, indexer, baseindexer.getClass().getClassLoader());

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

	protected static BaseModel getModel(String fullClassName, long companyId, Indexer indexer, ClassLoader classLoader)
			throws ClassNotFoundException, NoSuchFieldException,
			IllegalAccessException, InstantiationException {

		int lastDot = fullClassName.lastIndexOf(".");

		String packageName = fullClassName.substring(0,lastDot);
		String className = 
			fullClassName.substring(lastDot+1,fullClassName.length());
		String modelImplClassName = packageName + ".impl." + className + "ModelImpl"; /* PROBLEMATICO DESDE PORTLET */

		Class<?> modelImplClass = classLoader.loadClass(modelImplClassName);

		Field fieldTableName = 
				modelImplClass.getDeclaredField("TABLE_NAME");
		String tableName = (String) fieldTableName.get(null);
		System.out.println("\tTableName: "+tableName);

		Field fieldTableSqlCreate = 
				modelImplClass.getDeclaredField("TABLE_SQL_CREATE");
		String tableSqlCreate = (String) fieldTableSqlCreate.get(null);
		System.out.println("\tTableSqlCreate: "+tableSqlCreate);

		int posTableName = tableSqlCreate.indexOf(tableName);
		if(posTableName<=0) {
			System.out.println("Error, TABLE_NAME not found at TABLE_SQL_CREATE");
		}
		posTableName = posTableName + tableName.length()+2;

		String attributes = tableSqlCreate.substring(posTableName,tableSqlCreate.length()-1);
		System.out.println("\tAttributes: "+attributes);

		BaseModel type = getModelJavaClass(className);
		type.init(fullClassName, tableName, attributes.split(","));
		type.setIndexer(indexer);
		type.setCompanyId(companyId);

		return type;
	}

	protected static BaseModel getModelJavaClass(String className) throws InstantiationException, IllegalAccessException {
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
