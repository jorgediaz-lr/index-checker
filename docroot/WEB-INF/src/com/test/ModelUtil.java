package com.test;

import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.portlet.PortletClassLoaderUtil;
import com.liferay.portal.kernel.util.ClassLoaderPool;
import com.liferay.portal.kernel.util.MethodKey;
import com.liferay.portal.kernel.util.PortalClassLoaderUtil;
import com.liferay.portal.model.ClassName;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.service.PortletLocalServiceUtil;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ModelUtil {

	public Set<ClassLoader> classLoaderSet = null;

	public ModelUtil() {
		this.classLoaderSet = getClassLoaders();
	}

	public Set<ClassLoader> getClassLoaders() {
		Set<ClassLoader> classLoaderSet = new HashSet<ClassLoader>();

		List<Portlet> portlets = PortletLocalServiceUtil.getPortlets();

		for(Portlet portlet : portlets) {

			String portletId = portlet.getRootPortletId();

			ClassLoader classLoader = PortletClassLoaderUtil.getClassLoader(portletId);

			if (classLoader == null) {
				classLoader = ClassLoaderPool.getClassLoader(portletId);
			}

			classLoaderSet.add(classLoader);
		}
		return classLoaderSet;
	}

	public Class<?> getJavaModelImpl(Class<?> clazz) {
	
		if(clazz == null) {
			return null;
		}

		Package pkg = clazz.getPackage();

		String modelImplClassName = pkg.getName() + ".impl." + clazz.getSimpleName() + "ModelImpl";

		return getJavaClass(modelImplClassName);
	}

	public Class<?> getJavaLocalServiceUtil(Class<?> clazz) {
	
		if(clazz == null) {
			return null;
		}

		Package pkg = clazz.getPackage();

		String packageName = pkg.getName();
		int pos = packageName.lastIndexOf(".model");
		if (pos > 0) {
			packageName = packageName.substring(0, pos);
		}

		String localServiceUtil = packageName  + ".service." + clazz.getSimpleName() + "LocalServiceUtil";

		return getJavaClass(localServiceUtil);
	}

	public String getTableName(Class<?> clazz) throws Exception {
		Class<?> modelImplClass = getJavaModelImpl(clazz);

		if(modelImplClass == null) {
			return null;
		}

		Field fieldTableName = 
				modelImplClass.getDeclaredField("TABLE_NAME");
		return (String) fieldTableName.get(null);
	}

	public String[] getAttributes(Class<?> clazz) throws Exception {
		Class<?> modelImplClass = getJavaModelImpl(clazz);

		if(modelImplClass == null) {
			return null;
		}

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

		return attributes.split(",");
	}

	private Map<String, Class<?>> javaClasses = new ConcurrentHashMap<String, Class<?>>();
	private Map<String, MethodKey> newDynamicQueryMethods = new ConcurrentHashMap<String, MethodKey>();
	private Map<String, MethodKey> executeDynamicQueryMethods = new ConcurrentHashMap<String, MethodKey>();

	public Class<?> getJavaClass(ClassName className) {
		return getJavaClass(className.getValue());
	}

	public Class<?> getJavaClass(String className) {

		 Class<?> clazz = javaClasses.get(className);

		if (clazz != null) {
			return clazz;
		}

		/* Search first at portal classloader */
		try {
			clazz = PortalClassLoaderUtil.getClassLoader().loadClass(className);
			javaClasses.put(className, clazz);
			return clazz;
		}
		catch(Exception e) {
		}

		/* Search at portlet classloaders */
		for (ClassLoader classLoader : classLoaderSet) {
			try {
				clazz = classLoader.loadClass(className);
				javaClasses.put(className, clazz);
				return clazz;
			}
			catch(Exception e) {
				/* TODO log debug */
			}
		}
		return null;
	}

	
	public DynamicQuery newDynamicQuery(Class<?> clazz) {
		Method method = null;
		if(newDynamicQueryMethods.containsKey(clazz.getName())) {
			try {
				method = newDynamicQueryMethods.get(clazz.getName()).getMethod();
			} catch (NoSuchMethodException e) {
				/* TODO log error or warn */
				e.printStackTrace();
			}
		}
		else {
			Class<?> localServiceUtil = getJavaLocalServiceUtil(clazz);
			if(localServiceUtil != null) {
				try {
					method = localServiceUtil.getMethod("dynamicQuery");
				} catch (NoSuchMethodException | SecurityException e) {
					/* TODO log error or warn */
					e.printStackTrace();
				}
			}
			if(method == null) {
				newDynamicQueryMethods.put(clazz.getName(), new MethodKey());
			}
			else {
				newDynamicQueryMethods.put(clazz.getName(), new MethodKey(method));
			}
		}
		if(method == null) {
			return null;
		}
		try {
			return (DynamicQuery) method.invoke(null);
		} catch (IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			/* TODO log error or warn */
			e.printStackTrace();
			return null;
		}
	}

	public List<?> executeDynamicQuery(Class<?> clazz, DynamicQuery dynamicQuery) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Method method = null;
		if(executeDynamicQueryMethods.containsKey(clazz.getName())) {
			try {
				method = executeDynamicQueryMethods.get(clazz.getName()).getMethod();
			} catch (NoSuchMethodException e) {
				/* TODO log error or warn */
				e.printStackTrace();
			}
		}
		else {
			Class<?> localServiceUtil = getJavaLocalServiceUtil(clazz);
			if(localServiceUtil != null) {
				try {
					method = localServiceUtil.getMethod("dynamicQuery", DynamicQuery.class);
				} catch (NoSuchMethodException | SecurityException e) {
					/* TODO log error or warn */
					e.printStackTrace();
				}
			}
			if(method == null) {
				executeDynamicQueryMethods.put(clazz.getName(), new MethodKey());
			}
			else {
				executeDynamicQueryMethods.put(clazz.getName(), new MethodKey(method));
			}
		}
		if(method == null) {
			return null;
		}
		try {
			return (List<?>) method.invoke(null, dynamicQuery);
		} catch (IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			/* TODO log error or warn */
			e.printStackTrace();
			return null;
		}
	}
}
