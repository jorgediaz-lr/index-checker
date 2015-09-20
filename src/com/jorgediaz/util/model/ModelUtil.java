package com.jorgediaz.util.model;

import com.liferay.portal.kernel.portlet.PortletClassLoaderUtil;
import com.liferay.portal.kernel.util.AggregateClassLoader;
import com.liferay.portal.kernel.util.ClassLoaderPool;
import com.liferay.portal.kernel.util.PortalClassLoaderUtil;
import com.liferay.portal.model.ClassedModel;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.service.PortletLocalServiceUtil;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

public class ModelUtil {

	public static ClassLoader getClassLoader() {

		ClassLoader portalClassLoader = PortalClassLoaderUtil.getClassLoader();

		AggregateClassLoader aggregateClassLoader = new AggregateClassLoader(portalClassLoader);

		aggregateClassLoader.addClassLoader(portalClassLoader);

		List<Portlet> portlets = PortletLocalServiceUtil.getPortlets();

		for (Portlet portlet : portlets) {

			String portletId = portlet.getRootPortletId();

			ClassLoader classLoader = PortletClassLoaderUtil.getClassLoader(portletId);

			if (classLoader == null) {
				classLoader = ClassLoaderPool.getClassLoader(portletId);
			}

			aggregateClassLoader.addClassLoader(classLoader);
		}
		return aggregateClassLoader;
	}

	public static Class<?> getJavaClass(Map<String, Class<?>> javaClassesCache, ClassLoader classloader, String className) {

		Class<?> clazz = javaClassesCache.get(className);

		if (clazz != null) {
			return clazz;
		}

		try {
			clazz = classloader.loadClass(className);
			javaClassesCache.put(className, clazz);
			return clazz;
		}
		catch (Exception e) {
			System.err.println("Class not found: " + className);
			throw new RuntimeException(e);
		}
	}

	public static String getDatabaseAttributes(Class<?> classLiferayModelImpl) {

		String tableName;
		String tableSqlCreate;
		try {
			Field fieldTableName = classLiferayModelImpl.getDeclaredField("TABLE_NAME");
			tableName = (String) fieldTableName.get(null);
	
			Field fieldTableSqlCreate = classLiferayModelImpl.getDeclaredField("TABLE_SQL_CREATE");
			tableSqlCreate = (String) fieldTableSqlCreate.get(null);
		}
		catch(Exception e) {
			throw new RuntimeException("Error accessing to TABLE_NAME or TABLE_SQL_CREATE", e);
		}

		int posTableName = tableSqlCreate.indexOf(tableName);
		if (posTableName <= 0) {
			System.err.println("Error, TABLE_NAME not found at TABLE_SQL_CREATE");
			return null;
		}
		posTableName = posTableName + tableName.length() + 2;

		return tableSqlCreate.substring(posTableName, tableSqlCreate.length() - 1);
	}

	public static String getLiferayModelImpl(Class<? extends ClassedModel> clazz) {
		Package pkg = clazz.getPackage();
	
		return pkg.getName() + ".impl." + clazz.getSimpleName() + "ModelImpl";
	}

	public static String getLiferayLocalServiceUtil(Class<? extends ClassedModel> clazz) {
		Package pkg = clazz.getPackage();
	
		String packageName = pkg.getName();
		int pos = packageName.lastIndexOf(".model");
		if (pos > 0) {
			packageName = packageName.substring(0, pos);
		}
	
		return packageName + ".service." + clazz.getSimpleName() + "LocalServiceUtil";
	}
}
