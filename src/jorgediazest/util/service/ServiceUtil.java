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

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.PortalClassLoaderUtil;
import com.liferay.portal.model.ClassedModel;
import com.liferay.portal.service.BaseLocalService;

import java.lang.reflect.Method;

import java.util.HashMap;
import java.util.Map;

import jorgediazest.util.model.ModelUtil;

/**
 * @author Jorge Díaz
 */
public class ServiceUtil {

	public static String getLiferayLocalServiceClassName(
		String packageName, String simpleName) {

		int pos = packageName.lastIndexOf(".model");

		if (pos > 0) {
			packageName = packageName.substring(0, pos);
		}

		String className =
			packageName + ".service." + simpleName +
				"LocalService";

		if (_log.isDebugEnabled()) {
			_log.debug(
				"LocalServiceUtil of " + packageName + "." + simpleName + ": " +
				className);
		}

		return className;
	}

	public static String getLiferayLocalServiceUtilClassName(
		String packageName, String simpleName) {

		return getLiferayLocalServiceClassName(packageName, simpleName) +
			"Util";
	}

	public static Class<?> getLiferayModelImplClass(
			ClassLoader classloader, String packageName, String simpleName) {

		String liferayModelImpl = ModelUtil.getLiferayModelImplClassName(
			packageName, simpleName);

		Exception exception = null;

		Class<?> classLiferayModelImpl = null;
		try {
			classLiferayModelImpl = getJavaClass(classloader, liferayModelImpl);
		}
		catch (ClassNotFoundException e) {
			exception = e;
		}

		if (classLiferayModelImpl == null) {
			if (_log.isDebugEnabled()) {
				_log.debug("Class not found: " + liferayModelImpl);
			}

			if (exception != null) {
				throw new RuntimeException(exception);
			}

			throw new RuntimeException("Class not found: " + liferayModelImpl);
		}

		return classLiferayModelImpl;
	}

	public static Service getService(
		ClassLoader classLoader, String classPackageName,
		String classSimpleName) {

		BaseLocalService modelService = ServiceUtil.getLocalService(
			classLoader, classPackageName, classSimpleName);

		if (modelService != null) {
			Service service = new ServiceImpl();

			service.init(modelService, classPackageName, classSimpleName);

			return service;
		}

		return null;
	}

	public static Service getServiceFromPortal(
			String classPackageName, String classSimpleName) {

		String className = classPackageName + "." + classSimpleName;

		Service service = portalServices.get(className);

		if (service == null) {
			service = getService(null, classPackageName, classSimpleName);
		}

		if (service == null) {
			service = getServiceFromPortalClassInterface(className);
		}

		if (service != null) {
			portalServices.put(className, service);
		}

		return service;
	}

	public static Service getServiceFromPortalClassInterface(String classname) {

		Class<? extends ClassedModel> classInterface = getClassModelFromPortal(
			classname);

		if (classInterface == null) {
			return null;
		}

		Service service = new ServiceImpl();

		service.init(classInterface);

		return service;
	}

	@SuppressWarnings("unchecked")
	protected static Class<? extends ClassedModel> getClassModelFromPortal(
		String className) {

		try {
			return (Class<? extends ClassedModel>)
				PortalClassLoaderUtil.getClassLoader().loadClass(className);
		}
		catch (ClassNotFoundException e) {
			if (_log.isDebugEnabled()) {
				_log.debug("ClassModel not found: " + className);
			}

			return null;
		}
	}

	protected static Class<?> getJavaClass(
			ClassLoader classloader, String className)
		throws ClassNotFoundException {

		Class<?> clazz = null;

		try {
			clazz = PortalClassLoaderUtil.getClassLoader().loadClass(className);
		}
		catch (ClassNotFoundException e) {
		}

		if ((clazz == null) && (classloader != null)) {
			clazz = classloader.loadClass(className);
		}

		if (_log.isDebugEnabled()) {
			_log.debug(
				"loaded class: " + clazz + " from classloader: " + classloader);
		}

		return clazz;
	}

	protected static BaseLocalService getLocalService(
		ClassLoader classLoader, String classPackageName,
		String classSimpleName) {

		try {
			Method method = getLocalServiceUtilMethod(
				classLoader, classPackageName, classSimpleName, "getService",
				null);

			if (method == null) {
				return null;
			}

			return (BaseLocalService)method.invoke(null);
		}
		catch (Exception e) {
			if (_log.isDebugEnabled()) {
				_log.debug(
					"Cannot get service of " + classPackageName + "." +
					classSimpleName + " in classloader: " + classLoader +
					" - EXCEPTION: " + e.getClass().getName() +
					": " + e.getMessage());
			}

			if (_log.isTraceEnabled()) {
				_log.trace(e, e);
			}

			return null;
		}
	}

	protected static Method getLocalServiceUtilMethod(
		ClassLoader classLoader, String packageName, String simpleName,
		String methodName, Class<?> parameterType)
			throws ClassNotFoundException, NoSuchMethodException {

		Method method = null;

		String localServiceUtil =
			ServiceUtil.getLiferayLocalServiceUtilClassName(
				packageName, simpleName);

		Class<?> classLocalServiceUtil = getJavaClass(
			classLoader, localServiceUtil);

		if ((localServiceUtil != null) && (parameterType != null)) {
			method = classLocalServiceUtil.getMethod(methodName, parameterType);
		}
		else if (localServiceUtil != null) {
			method = classLocalServiceUtil.getMethod(methodName);
		}

		return method;
	}

	private static Log _log = LogFactoryUtil.getLog(ServiceUtil.class);

	private static Map<String, Service> portalServices =
		new HashMap<String, Service>();

}