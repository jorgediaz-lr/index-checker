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

package com.jorgediaz.util.service;

import com.jorgediaz.util.model.ModelUtil;

import com.liferay.portal.kernel.dao.orm.Criterion;
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.DynamicQueryFactoryUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.MethodKey;
import com.liferay.portal.kernel.util.PortalClassLoaderUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.model.ClassedModel;
import com.liferay.portal.model.Group;
import com.liferay.portal.service.BaseLocalService;

import java.lang.reflect.Method;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Jorge Díaz
 */
public class ServiceImpl implements Service {

	public static Class<?> getLiferayModelImplClass(
		ClassLoader classloader, String packageName, String simpleName) {

		String liferayModelImpl = ModelUtil.getLiferayModelImplClassName(
			packageName, simpleName);

		Class<?> classLiferayModelImpl;
		try {
			classLiferayModelImpl = ServiceUtil.getJavaClass(
				classloader, liferayModelImpl);
		}
		catch (ClassNotFoundException e) {
			_log.error("Class not found: " + liferayModelImpl);
			throw new RuntimeException(e);
		}

		if (classLiferayModelImpl == null) {
			_log.error("Class not found: " + liferayModelImpl);
			throw new RuntimeException("Class not found: " + liferayModelImpl);
		}

		return classLiferayModelImpl;
	}

	public ClassedModel addObject(ClassedModel object) {
		String methodName = "add" + object.getModelClass().getSimpleName();

		return (ClassedModel)executeServiceMethod(
			methodName, object.getModelClass(), object);
	}

	public ServiceImpl clone() {
		ServiceImpl service;
		try {
			service = this.getClass().newInstance();
			service.className = this.className;
			service.classPackageName = this.classPackageName;
			service.classSimpleName = this.classSimpleName;
			service.modelService = this.modelService;
			service.filter = this.filter;
			service.classInterface = this.classInterface;
		}
		catch (Exception e) {
			_log.error("Error executing clone");
			throw new RuntimeException(e);
		}

		return service;
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

	public List<?> executeDynamicQuery(DynamicQuery dynamicQuery)
		throws Exception {

		if (filter != null) {
			dynamicQuery.add(filter);

			if (_log.isDebugEnabled()) {
				_log.debug("adding custom filter: " + filter);
			}
		}

		return (List<?>)executeServiceMethod(
			"dynamicQuery", DynamicQuery.class, dynamicQuery);
	}

	public Object executeServiceMethod(
		String methodName, Class<?> parameterType, Object arg) {
			if (modelService == null) {
				throw new IllegalArgumentException("service must be not null");
			}

			try {
				Method method = getLocalServiceMethod(
					methodName, parameterType);

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
		if (modelService != null) {
			return modelService.getClass().getClassLoader();
		}

		return null;
	}

	public Criterion getFilter() {
		return filter;
	}

	public Class<?> getLiferayModelImplClass() {
		return ServiceImpl.getLiferayModelImplClass(
			getClassLoader(), classPackageName, classSimpleName);
	}

	public void init(
		BaseLocalService modelService, String classPackageName,
		String classSimpleName) {

		this.modelService = modelService;

		this.className = classPackageName + "." + classSimpleName;
		this.classPackageName = classPackageName;
		this.classSimpleName = classSimpleName;

		if (modelService == null) {
			Class<?> clazz = Group.class;
			this.modelService = ServiceUtil.getLocalService(
				null, clazz.getPackage().getName(), clazz.getSimpleName());

			this.classInterface = getClassModel(className);
		}
	}

	public DynamicQuery newDynamicQuery() {
		if (classInterface != null) {
			return DynamicQueryFactoryUtil.forClass(
				classInterface, null, classInterface.getClassLoader());
		}

		return (DynamicQuery)executeServiceMethod("dynamicQuery", null, null);
	}

	public void setFilter(Criterion filter) {
		this.filter = filter;
	}

	public ClassedModel updateObject(ClassedModel object) {
		String methodName = "update" + object.getModelClass().getSimpleName();

		return (ClassedModel)executeServiceMethod(
			methodName, object.getModelClass(), object);
	}

	@SuppressWarnings("unchecked")
	protected Class<? extends ClassedModel> getClassModel(String className) {
		try {
			return (Class<? extends ClassedModel>)
				PortalClassLoaderUtil.getClassLoader().loadClass(className);
		}
		catch (ClassNotFoundException e) {
			_log.error("Class not found: " + className);
			throw new RuntimeException(e);
		}
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

			for (; classLocalService != null;
				classLocalService = classLocalService.getSuperclass()) {

				try {
					if (parameterType != null) {
						method = classLocalService.getDeclaredMethod(
							methodName, parameterType);
					}
					else {
						method = classLocalService.getDeclaredMethod(
							methodName);
					}

					break;
				}
				catch (Exception e) {
				}
			}

			if (method == null) {
				localServiceMethods.put(key, new MethodKey());
			}
			else {
				if (!method.isAccessible()) {
					method.setAccessible(true);
				}

				localServiceMethods.put(key, new MethodKey(method));
			}
		}

		return method;
	}

	protected Class<? extends ClassedModel> classInterface = null;
	protected String className = null;
	protected String classPackageName = null;
	protected String classSimpleName = null;
	protected Criterion filter = null;

	protected Map<String, MethodKey> localServiceMethods =
		new ConcurrentHashMap<String, MethodKey>();

	static Log _log = LogFactoryUtil.getLog(ServiceImpl.class);

	protected BaseLocalService modelService = null;

}