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
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.ClassedModel;
import com.liferay.portal.kernel.service.BaseLocalService;
import com.liferay.portal.kernel.service.PersistedModelLocalServiceRegistryUtil;
import com.liferay.portal.kernel.util.PortalClassLoaderUtil;
import com.liferay.portal.kernel.util.Validator;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jorgediazest.util.reflection.ReflectionUtil;

/**
 * @author Jorge Díaz
 */
public class ServiceUtil {

	public static Class<?> getLiferayModelImplClass(
		ClassLoader classloader, String liferayModelImpl) {

		if (Validator.isNull(liferayModelImpl)) {
			return null;
		}

		liferayModelImpl = liferayModelImpl + "ModelImpl";

		liferayModelImpl = liferayModelImpl.replace(
			"ImplModelImpl", "ModelImpl");

		try {
			return ServiceUtil.getJavaClass(classloader, liferayModelImpl);
		}
		catch (ClassNotFoundException cnfe) {
			if (_log.isDebugEnabled()) {
				_log.debug("Class not found: " + liferayModelImpl);
			}

			throw new RuntimeException(cnfe);
		}
	}

	public static String getLiferayModelImplClassName(Service service) {
		DynamicQuery dynamicQuery = service.newDynamicQuery();

		String liferayModelImpl = ReflectionUtil.getWrappedModelImpl(
			dynamicQuery);

		if (liferayModelImpl != null) {
			return liferayModelImpl;
		}

		try {
			dynamicQuery.setLimit(0, 1);

			List<?> list = service.executeDynamicQuery(dynamicQuery);

			Object obj = null;

			if ((list != null) && (list.size() > 0)) {
				obj = list.get(0);
			}

			if (obj instanceof ClassedModel) {
				return obj.getClass(
				).getName();
			}
		}
		catch (Exception e) {
			if (_log.isDebugEnabled()) {
				_log.debug(e, e);
			}
		}

		return null;
	}

	public static Service getService(String className) {
		BaseLocalService modelService =
			(BaseLocalService)
				PersistedModelLocalServiceRegistryUtil.
					getPersistedModelLocalService(className);

		if (modelService != null) {
			return new ServicePersistedModelImpl(modelService, className);
		}

		return getServiceFromPortal(className);
	}

	@SuppressWarnings("unchecked")
	protected static Class<? extends ClassedModel> getClassModelFromPortal(
		String className) {

		try {
			return (Class<? extends ClassedModel>)
				PortalClassLoaderUtil.getClassLoader(
				).loadClass(
					className
				);
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

		if (Validator.isNull(className)) {
			className = StringPool.NULL;
		}

		try {
			clazz = PortalClassLoaderUtil.getClassLoader(
			).loadClass(
				className
			);
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

	protected static String getLiferayLocalServiceClassName(
		String packageName, String simpleName) {

		int pos = packageName.lastIndexOf(".model");

		if (pos > 0) {
			packageName = packageName.substring(0, pos);
		}

		String className =
			packageName + ".service." + simpleName + "LocalService";

		if (_log.isDebugEnabled()) {
			_log.debug(
				"LocalServiceUtil of " + packageName + "." + simpleName + ": " +
					className);
		}

		return className;
	}

	protected static Service getServiceFromPortal(String className) {
		if (cacheNullPortalServices.contains(className)) {
			return null;
		}

		ServiceClassInterfaceImpl service = cachePortalServices.get(className);

		if (service != null) {
			return service;
		}

		Class<? extends ClassedModel> classInterface = getClassModelFromPortal(
			className);

		if (classInterface != null) {
			try {
				service = new ServiceClassInterfaceImpl(classInterface);

				if (service.getLiferayModelImplClass() != null) {
					cachePortalServices.put(className, service);

					return service;
				}
			}
			catch (Exception e) {
				if (_log.isDebugEnabled()) {
					_log.debug(
						"Error creating ServiceClassInterfaceImpl: " +
							e.getMessage());
				}
			}
		}

		cacheNullPortalServices.add(className);

		return null;
	}

	private static Log _log = LogFactoryUtil.getLog(ServiceUtil.class);

	private static Set<String> cacheNullPortalServices =
		Collections.newSetFromMap(new ConcurrentHashMap<>());
	private static Map<String, ServiceClassInterfaceImpl> cachePortalServices =
		new ConcurrentHashMap<>();

}