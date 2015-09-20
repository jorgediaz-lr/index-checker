package com.jorgediaz.util.model;

import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.DynamicQueryFactoryUtil;
import com.liferay.portal.kernel.util.MethodKey;
import com.liferay.portal.model.ClassedModel;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ModelFactory {

	public ClassLoader classLoader = null;
	public Class<? extends Model> defaultModelClass = null;
	public Map<String, Class<? extends Model>> modelClassMap = null;

	private Map<String, Class<?>> javaClasses = new ConcurrentHashMap<String, Class<?>>();
	private Map<String, MethodKey> executeDynamicQueryMethods = new ConcurrentHashMap<String, MethodKey>();


	public ModelFactory() {
		this.classLoader = ModelUtil.getClassLoader();
		this.defaultModelClass = DefaultModel.class;
		this.modelClassMap = null;
	}

	public ModelFactory(Class<? extends Model> defaultModelClass, Map<String, Class<? extends Model>> modelClassMap) {
		this.classLoader = ModelUtil.getClassLoader();
		this.defaultModelClass = defaultModelClass;
		this.modelClassMap = modelClassMap;
	}

	public Class<? extends Model> getModelClass(String className) throws InstantiationException, IllegalAccessException {

		Class<? extends Model> modelClass = defaultModelClass;

		if(modelClassMap != null && modelClassMap.containsKey(className)) {
			modelClass = modelClassMap.get(className);
		}

		return modelClass;
	}

	public final Model getModelObject(String className) throws Exception {

		@SuppressWarnings("unchecked")
		Class<? extends ClassedModel> clazz = (Class<? extends ClassedModel>) ModelUtil.getJavaClass(javaClasses, classLoader, className);
	
		return getModelObject(clazz);
	}

	public Model getModelObject(Class<? extends ClassedModel> clazz) throws Exception {

		Class<? extends Model> modelClass = this.getModelClass(clazz.getCanonicalName());

		if(clazz == null || modelClass == null || !ClassedModel.class.isAssignableFrom(clazz)) {
			return null;
		}

		Model model = null;
		try {
			model = (Model) modelClass.newInstance();
	
			model.init(this, clazz);
		}
		catch(Exception e) {
			System.err.println("getModelObject("+clazz.getCanonicalName()+") ERROR "+e.getClass().getCanonicalName()+": "+e.getMessage());
			throw new RuntimeException(e);
		}

		return model;
	}

	protected String getDatabaseAttributes(Class<? extends ClassedModel> clazz) {
		String liferayModelImpl = ModelUtil.getLiferayModelImpl(clazz);
		Class<?> classLiferayModelImpl = ModelUtil.getJavaClass(javaClasses, classLoader, liferayModelImpl);

		if(classLiferayModelImpl == null) {
			return null;
		}
		return ModelUtil.getDatabaseAttributes(classLiferayModelImpl);
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
			String localServiceUtil = ModelUtil.getLiferayLocalServiceUtil(clazz);
			Class<?> classLocalServiceUtil = ModelUtil.getJavaClass(javaClasses, classLoader, localServiceUtil);
			if (localServiceUtil != null) {
				method = classLocalServiceUtil.getMethod("dynamicQuery", DynamicQuery.class);
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
