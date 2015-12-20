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

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.PortalClassLoaderUtil;
import com.liferay.portal.service.BaseLocalService;

import java.lang.reflect.Method;

/**
 * @author Jorge Díaz
 */
public class ServiceUtil {

	public static Service getService(
			ClassLoader classLoader, String classPackageName,
			String classSimpleName) {

		BaseLocalService modelService = ServiceUtil.getLocalService(
			classLoader, classPackageName, classSimpleName);

		Service service = new ServiceImpl();
		service.init(modelService, classPackageName, classSimpleName);
		return service;
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
				"loaded class: " + clazz + " from classloader :" + classloader);
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
				_log.debug(e, e);
			}
			else if (_log.isWarnEnabled()) {
				_log.warn(
					"Cannot get service of " + classPackageName + "." +
					classSimpleName + " EXCEPTION: " + e.getClass().getName() +
					": " + e.getMessage());
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
			ModelUtil.getLiferayLocalServiceUtilClassName(
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

	static Log _log = LogFactoryUtil.getLog(ServiceUtil.class);

}