package com.jorgediaz.util.model;

import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.DynamicQueryFactoryUtil;
import com.liferay.portal.kernel.portlet.PortletClassLoaderUtil;
import com.liferay.portal.kernel.util.AggregateClassLoader;
import com.liferay.portal.kernel.util.ClassLoaderPool;
import com.liferay.portal.kernel.util.MethodKey;
import com.liferay.portal.kernel.util.PortalClassLoaderUtil;
import com.liferay.portal.model.ClassName;
import com.liferay.portal.model.ClassedModel;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.service.PortletLocalServiceUtil;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ModelUtil {

	public ClassLoader classLoader = null;
	public Class<? extends BaseModel> defaultModelClass = null;
	public Map<String, Class<? extends BaseModel>> modelClassMap = null;

	public ModelUtil() {
		this.classLoader = getClassLoader();
		this.defaultModelClass = DefaultModel.class;
		this.modelClassMap = null;
	}

	public ModelUtil(Class<? extends BaseModel> defaultModelClass, Map<String, Class<? extends BaseModel>> modelClassMap) {
		this.classLoader = getClassLoader();
		this.defaultModelClass = defaultModelClass;
		this.modelClassMap = modelClassMap;
	}

	public ClassLoader getClassLoader() {

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
	
	public BaseModel getModelObject(ClassName className) throws Exception {
		return getModelObject(className.getValue());
	}

	public Class<? extends BaseModel> getModelClass(String className) throws InstantiationException, IllegalAccessException {

		Class<? extends BaseModel> modelClass = defaultModelClass;

		if(modelClassMap != null && modelClassMap.containsKey(className)) {
			modelClass = modelClassMap.get(className);
		}

		return modelClass;
	}

	public BaseModel getModelObject(String className) throws Exception {

		@SuppressWarnings("unchecked")
		Class<? extends ClassedModel> clazz = (Class<? extends ClassedModel>) this.getJavaClass(className);

		Class<? extends BaseModel> modelClass = this.getModelClass(className);

		if(clazz == null || modelClass == null || !ClassedModel.class.isAssignableFrom(clazz)) {
			return null;
		}

		BaseModel model = null;
		try {
			model = (BaseModel) modelClass.newInstance();
	
			model.init(this, clazz);
		}
		catch(Exception e) {
			System.err.println("getModelObject("+className+") ERROR "+e.getMessage());
			e.printStackTrace();
		}

		return model;
	}

	protected Class<?> getLiferayModelImpl(Class<? extends ClassedModel> clazz) {

		if (clazz == null) {
			return null;
		}

		Package pkg = clazz.getPackage();

		String modelImplClassName = pkg.getName() + ".impl." + clazz.getSimpleName() + "ModelImpl";

		return getJavaClass(modelImplClassName);
	}

	protected Class<?> getLiferayLocalServiceUtil(Class<? extends ClassedModel> clazz) {

		if (clazz == null) {
			return null;
		}

		Package pkg = clazz.getPackage();

		String packageName = pkg.getName();
		int pos = packageName.lastIndexOf(".model");
		if (pos > 0) {
			packageName = packageName.substring(0, pos);
		}

		String localServiceUtil = packageName + ".service." + clazz.getSimpleName() + "LocalServiceUtil";

		return getJavaClass(localServiceUtil);
	}

	protected String getDatabaseAttributes(Class<? extends ClassedModel> clazz) {
		Class<?> liferayModelImpl = getLiferayModelImpl(clazz);

		if (liferayModelImpl == null) {
			return null;
		}

		String tableName;
		String tableSqlCreate;
		try {
			Field fieldTableName = liferayModelImpl.getDeclaredField("TABLE_NAME");
			tableName = (String) fieldTableName.get(null);
	
			Field fieldTableSqlCreate = liferayModelImpl.getDeclaredField("TABLE_SQL_CREATE");
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

	private Map<String, Class<?>> javaClasses = new ConcurrentHashMap<String, Class<?>>();
	private Map<String, MethodKey> executeDynamicQueryMethods = new ConcurrentHashMap<String, MethodKey>();

	@SuppressWarnings("unchecked")
	protected Class<? extends ClassedModel> getJavaClass(ClassName className) {
		return (Class<? extends ClassedModel>) getJavaClass(className.getValue());
	}

	protected Class<?> getJavaClass(String className) {

		Class<?> clazz = javaClasses.get(className);

		if (clazz != null) {
			return clazz;
		}

		try {
			clazz = classLoader.loadClass(className);
			javaClasses.put(className, clazz);
			return clazz;
		}
		catch (Exception e) {
			System.err.println("Class not found: " + className);
			e.printStackTrace();
		}

		return null;
	}

	protected DynamicQuery newDynamicQuery(Class<? extends ClassedModel> clazz) {
		return DynamicQueryFactoryUtil.forClass(clazz, classLoader);
	}

	protected List<?> executeDynamicQuery(Class<? extends ClassedModel> clazz, DynamicQuery dynamicQuery) {
		try {
			Method method = getExecuteDynamicQueryMethod(clazz);
			if (method == null) {
				return null;
			}
			return (List<?>) method.invoke(null, dynamicQuery);
		}
		catch(NoSuchMethodException e) {
			throw new RuntimeException("executeDynamicQuery: dynamicQuery method not found for " + clazz.getCanonicalName(), e);
		}
		catch(IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new RuntimeException("executeDynamicQuery: error invoking dynamicQuery method for " + clazz.getCanonicalName(), e);
		}
	}

	protected Method getExecuteDynamicQueryMethod(Class<? extends ClassedModel> clazz) throws NoSuchMethodException, SecurityException {
		Method method = null;
		if (executeDynamicQueryMethods.containsKey(clazz.getName())) {
			try {
				method = executeDynamicQueryMethods.get(clazz.getName()).getMethod();
			}
			catch (NoSuchMethodException e) {
			}
		}
		if (method == null) {
			Class<?> localServiceUtil = getLiferayLocalServiceUtil(clazz);
			if (localServiceUtil != null) {
				method = localServiceUtil.getMethod("dynamicQuery", DynamicQuery.class);
			}
			if (method == null) {
				executeDynamicQueryMethods.put(clazz.getName(), new MethodKey());
			}
			else {
				executeDynamicQueryMethods.put(clazz.getName(), new MethodKey(method));
			}
		}
		return method;
	}
}
