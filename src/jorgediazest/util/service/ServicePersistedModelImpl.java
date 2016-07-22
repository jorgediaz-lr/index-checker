/**
 * Copyright (c) 2015-present Jorge Díaz All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package jorgediazest.util.service;

import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.MethodKey;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.model.ClassedModel;
import com.liferay.portal.service.BaseLocalService;

import java.lang.reflect.Method;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Jorge Díaz
 */
public class ServicePersistedModelImpl extends ServiceImpl {

	public ServicePersistedModelImpl(
		BaseLocalService modelService, String classPackageName,
		String classSimpleName) {

		if (modelService == null) {
			throw new NullPointerException("modelService cannot be null");
		}

		this.modelService = modelService;

		this.classPackageName = classPackageName;
		this.classSimpleName = classSimpleName;
		this.className = classPackageName + "." + classSimpleName;
	}

	public ServicePersistedModelImpl(ServicePersistedModelImpl service) {
		this.init(service);
	}

	public ClassedModel addObject(ClassedModel object) {
		String methodName = "add" + object.getModelClass().getSimpleName();

		return (ClassedModel)executeServiceMethod(
			methodName, object.getModelClass(), object);
	}

	@Override
	public Service clone() {
		return new ServicePersistedModelImpl(this);
	}

	public ClassedModel createObject(long primaryKey) {
		String methodName = "create" + classSimpleName;

		return (ClassedModel)executeServiceMethod(
			methodName, long.class, (Object)primaryKey);
	}

	public ClassedModel deleteObject(ClassedModel object) {
		String methodName = "delete" + object.getModelClass().getSimpleName();

		return (ClassedModel)executeServiceMethod(
			methodName, object.getModelClass(), object);
	}

	public ClassedModel deleteObject(long primaryKey) {
		String methodName = "delete" + classSimpleName;

		return (ClassedModel)executeServiceMethod(
			methodName, long.class, (Object)primaryKey);
	}

	public List<?> executeDynamicQuery(DynamicQuery dynamicQuery) {
		prepareDynamicQuery(dynamicQuery);

		return (List<?>)executeServiceMethod(
			"dynamicQuery", DynamicQuery.class, dynamicQuery);
	}

	public Object executeServiceMethod(
		String methodName, Class<?> parameterType, Object arg) {

		try {
			Method method = getLocalServiceMethod(methodName, parameterType);

			if (method == null) {
				return null;
			}

			if (arg == null) {
				return method.invoke(modelService);
			}

			return method.invoke(modelService, arg);
		}
		catch (NoSuchMethodException e) {
			throw new RuntimeException(
				"executeMethod: " + methodName + " method not found for " +
				modelService, e);
		}
		catch (Exception e) {
			String cause = StringPool.BLANK;
			Throwable rootException = e.getCause();

			if (rootException != null) {
				cause = " (root cause: " + rootException.getMessage() + ")";
			}

			throw new RuntimeException(
				"executeMethod: " + methodName + " method for " +
				modelService + ": " + cause, e);
		}
	}

	public ClassedModel fetchObject(long primaryKey) {
		String methodName = "fetch" + classSimpleName;

		return (ClassedModel)executeServiceMethod(
			methodName, long.class, (Object)primaryKey);
	}

	public ClassLoader getClassLoader() {
		return modelService.getClass().getClassLoader();
	}

	public void init(Service service) {
		try {
			super.init(service);
			ServicePersistedModelImpl serviceImpl =
				(ServicePersistedModelImpl)service;
			this.localServiceMethods = serviceImpl.localServiceMethods;
			this.modelService = serviceImpl.modelService;
		}
		catch (Exception e) {
			_log.error("Error executing init");
			throw new RuntimeException(e);
		}
	}

	public DynamicQuery newDynamicQuery() {
		return (DynamicQuery)executeServiceMethod("dynamicQuery", null, null);
	}

	public ClassedModel updateObject(ClassedModel object) {
		String methodName = "update" + object.getModelClass().getSimpleName();

		return (ClassedModel)executeServiceMethod(
			methodName, object.getModelClass(), object);
	}

	protected Method getLocalServiceMethod(
		String methodName, Class<?> parameterType)
			throws ClassNotFoundException, NoSuchMethodException {

		String key = methodName;

		if (parameterType != null) {
			key = key + "#" + parameterType.getName();
		}

		Method method = null;

		if (localServiceMethods.containsKey(key)) {
			try {
				method = localServiceMethods.get(key).getMethod();
			}
			catch (NoSuchMethodException e) {
			}
		}

		if (method == null) {
			Class<?> classLocalService = modelService.getClass();

			if (parameterType != null) {
				method = classLocalService.getMethod(methodName, parameterType);
			}
			else {
				method = classLocalService.getMethod(methodName);
			}

			if (method == null) {
				localServiceMethods.put(key, new MethodKey());
			}
			else {
				localServiceMethods.put(key, new MethodKey(method));
			}
		}

		return method;
	}

	protected Map<String, MethodKey> localServiceMethods =
		new ConcurrentHashMap<String, MethodKey>();
	protected BaseLocalService modelService = null;

	private static Log _log = LogFactoryUtil.getLog(
		ServicePersistedModelImpl.class);

}