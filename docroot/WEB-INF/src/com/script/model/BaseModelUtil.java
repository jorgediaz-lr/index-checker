package com.script.model;

import com.liferay.portal.kernel.bean.ClassLoaderBeanHandler;
import com.liferay.portal.kernel.search.BaseIndexer;
import com.liferay.portal.kernel.search.Indexer;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;

public class BaseModelUtil {

	public static BaseIndexer getBaseIndexer(Indexer indexer) {
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

	public static BaseModel getModel(String fullClassName, Indexer indexer, ClassLoader classLoader)
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
		type.init(fullClassName,tableName,attributes.split(","));
		type.setIndexer(indexer);
		return type;
	}

	protected static BaseModel getModelJavaClass(String className) throws InstantiationException, IllegalAccessException {
		BaseModel type;

		try {
			Class<?> typeClass = Class.forName("com.script.model."+className);
			System.out.println(typeClass);
			type = (BaseModel) typeClass.newInstance();
		}
		catch (ClassNotFoundException e) {
			System.out.println(e);
			type = new DefaultModel();
		}/*
		if("WikiPage".equals(className)) {
			type = new WikiPage();
		}
		else if("DLFileEntry".equals(className)) {
			type = new DLFileEntry();
		}
		else if("MBMessage".equals(className)) {
			type = new MBMessage();
		}
		else if("AssetEntry".equals(className)) {
			type = new AssetEntry();
		}
		else if("TrashEntry".equals(className)) {
			type = new TrashEntry();
		}
		else {
			type = new DefaultModel();
		}*/
		return type;
	}
}
