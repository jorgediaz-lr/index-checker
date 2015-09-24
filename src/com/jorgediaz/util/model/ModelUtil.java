package com.jorgediaz.util.model;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.portlet.PortletClassLoaderUtil;
import com.liferay.portal.kernel.util.AggregateClassLoader;
import com.liferay.portal.kernel.util.ClassLoaderPool;
import com.liferay.portal.kernel.util.PortalClassLoaderUtil;
import com.liferay.portal.model.ClassedModel;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.service.PortletLocalServiceUtil;

import java.lang.reflect.Field;

import java.sql.Types;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
public class ModelUtil {

	public static ClassLoader getClassLoader() {

		ClassLoader portalClassLoader = PortalClassLoaderUtil.getClassLoader();

		AggregateClassLoader aggregateClassLoader = new AggregateClassLoader(
			portalClassLoader);

		if (_log.isDebugEnabled()) {
			_log.debug("Adding " + portalClassLoader);
		}

		aggregateClassLoader.addClassLoader(portalClassLoader);

		List<Portlet> portlets = PortletLocalServiceUtil.getPortlets();

		for (Portlet portlet : portlets) {
			String portletId = portlet.getRootPortletId();

			ClassLoader classLoader = PortletClassLoaderUtil.getClassLoader(
				portletId);

			if (classLoader == null) {
				classLoader = ClassLoaderPool.getClassLoader(portletId);
			}

			_log.debug("Adding " + classLoader);
			aggregateClassLoader.addClassLoader(classLoader);
		}

		return aggregateClassLoader;
	}

	public static Class<?> getJavaClass(
		Map<String, Class<?>> javaClassesCache, ClassLoader classloader,
		String className) throws ClassNotFoundException {

		Class<?> clazz = javaClassesCache.get(className);

		if (clazz != null) {
			return clazz;
		}

		clazz = classloader.loadClass(className);

		if (_log.isDebugEnabled()) {
			_log.debug("loaded class: " + clazz);
		}

		javaClassesCache.put(className, clazz);
		return clazz;
	}

	public static Map<Integer, String> getJdbcTypeNames() {

		if (jdbcTypeNames == null) {
			Map<Integer, String> aux = new HashMap<Integer, String>();

			for (Field field : Types.class.getFields()) {
				try {
					aux.put((Integer)field.get(null), field.getName());
				}
				catch (IllegalArgumentException | IllegalAccessException e) {
				}
			}

			jdbcTypeNames = aux;
		}

		return jdbcTypeNames;
	}

	public static String getLiferayLocalServiceUtil(
		Class<? extends ClassedModel> clazz) {

		Package pkg = clazz.getPackage();

		String packageName = pkg.getName();
		int pos = packageName.lastIndexOf(".model");

		if (pos > 0) {
			packageName = packageName.substring(0, pos);
		}

		String name =
			packageName + ".service." + clazz.getSimpleName() +
				"LocalServiceUtil";

		if (_log.isDebugEnabled()) {
			_log.debug(
				"LocalServiceUtil of " + clazz.getCanonicalName() + ": " +
				name);
		}

		return name;
	}

	public static String getLiferayModelImpl(
		Class<? extends ClassedModel> clazz) {

		Package pkg = clazz.getPackage();

		String name =
			pkg.getName() + ".impl." + clazz.getSimpleName() + "ModelImpl";

		if (_log.isDebugEnabled()) {
			_log.debug(
				"ModelImpl of " + clazz.getCanonicalName() + ": " + name);
		}

		return name;
	}

	public static Object getLiferayModelImplField(
		Class<?> classLiferayModelImpl, String liferayModelImplField) {

		Object data = null;
		try {
			Field field = classLiferayModelImpl.getDeclaredField(
				liferayModelImplField);
			data = field.get(null);
		}
		catch (Exception e) {
			throw new RuntimeException(
				"Error accessing to " +
				classLiferayModelImpl.getCanonicalName() + "#" +
				liferayModelImplField, e);
		}

		return data;
	}

	private static Log _log = LogFactoryUtil.getLog(ModelUtil.class);

	private static Map<Integer, String> jdbcTypeNames = null;

}