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

import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.DynamicQueryFactoryUtil;
import com.liferay.portal.kernel.model.ClassedModel;
import com.liferay.portal.kernel.service.BaseLocalService;
import com.liferay.portal.kernel.util.MethodKey;

import java.lang.reflect.Method;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Jorge Díaz
 */
public class ServicePersistedModelImpl extends ServiceImpl {

	public ServicePersistedModelImpl(
		BaseLocalService modelService, String className) {

		if (modelService == null) {
			throw new NullPointerException("modelService cannot be null");
		}

		this.modelService = modelService;
		this.className = className;

		classSimpleName = className;

		int pos = className.lastIndexOf(".");

		if (pos > 0) {
			classSimpleName = className.substring(pos + 1, className.length());
		}
	}

	public ClassedModel addObject(ClassedModel object) {
		String methodName = "add" + _getSimpleName(object);

		return (ClassedModel)executeServiceMethod(
			methodName, object.getModelClass(), object);
	}

	public ClassedModel createObject(long primaryKey) {
		String methodName = "create" + classSimpleName;

		return (ClassedModel)executeServiceMethod(
			methodName, long.class, (Object)primaryKey);
	}

	public ClassedModel deleteObject(ClassedModel object) {
		String methodName = "delete" + _getSimpleName(object);

		return (ClassedModel)executeServiceMethod(
			methodName, object.getModelClass(), object);
	}

	public ClassedModel deleteObject(long primaryKey) {
		String methodName = "delete" + classSimpleName;

		return (ClassedModel)executeServiceMethod(
			methodName, long.class, (Object)primaryKey);
	}

	public List<?> executeDynamicQuery(DynamicQuery dynamicQuery) {
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
					modelService,
				e);
		}
		catch (Exception e) {
			String cause = StringPool.BLANK;
			Throwable rootException = e.getCause();

			if (rootException != null) {
				cause = " (root cause: " + rootException.getMessage() + ")";
			}

			throw new RuntimeException(
				"executeMethod: " + methodName + " method for " + modelService +
					": " + cause,
				e);
		}
	}

	public ClassedModel fetchObject(long primaryKey) {
		String methodName = "fetch" + classSimpleName;

		return (ClassedModel)executeServiceMethod(
			methodName, long.class, (Object)primaryKey);
	}

	public ClassLoader getClassLoader() {
		Class<?> clazz = modelService.getClass();

		return clazz.getClassLoader();
	}

	public DynamicQuery newDynamicQuery() {
		return (DynamicQuery)executeServiceMethod("dynamicQuery", null, null);
	}

	public DynamicQuery newDynamicQuery(String alias) {
		try {
			Class<?> modelImplClass = getLiferayModelImplClass();

			String className = modelImplClass.getName();

			className = className.replace("ModelImpl", "Impl");

			Class<?> clazz = getClassLoader().loadClass(className);

			return DynamicQueryFactoryUtil.forClass(clazz, alias, getClassLoader());
		}
		catch (ClassNotFoundException cnfe) {
			throw new RuntimeException(cnfe);
		}
	}

	public ClassedModel updateObject(ClassedModel object) {
		String methodName = "update" + _getSimpleName(object);

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
				MethodKey methodKey = localServiceMethods.get(key);

				method = methodKey.getMethod();
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
		new ConcurrentHashMap<>();
	protected BaseLocalService modelService = null;

	private static String _getSimpleName(ClassedModel classedModel) {
		Class<?> clazz = classedModel.getModelClass();

		return clazz.getSimpleName();
	}

}