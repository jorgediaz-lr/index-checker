package com.jorgediaz.util.model;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.servlet.ServletContextPool;
import com.liferay.portal.kernel.util.AggregateClassLoader;
import com.liferay.portal.kernel.util.PortalClassLoaderUtil;
import com.liferay.portal.model.ClassName;
import com.liferay.portal.model.ClassedModel;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import java.sql.Types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
public class ModelUtil {

	public static ClassLoader getClassLoader() {

		ClassLoader portalClassLoader = PortalClassLoaderUtil.getClassLoader();

		AggregateClassLoader aggregateClassLoader = new AggregateClassLoader(
			portalClassLoader);

		if (_log.isDebugEnabled()) {
			_log.debug("Adding " + portalClassLoader);
		}

		aggregateClassLoader.addClassLoader(portalClassLoader);

		for (String servletContextName : ServletContextPool.keySet()) {
			try {
				ServletContext servletContext = ServletContextPool.get(
					servletContextName);

				ClassLoader classLoader = servletContext.getClassLoader();

				_log.debug(
					"Adding " + classLoader + " for " + servletContextName);

				aggregateClassLoader.addClassLoader(classLoader);
			}
			catch (Exception e) {
				if (_log.isWarnEnabled()) {
					_log.warn(
						"Error adding classLoader for " + servletContextName +
						": " + e.getMessage(), e);
				}
			}
		}

		return aggregateClassLoader;
	}

	public static List<String> getClassNameValues(
			Collection<ClassName> classNames) {

		List<String> classNameStr = new ArrayList<String>();

		for (ClassName className : classNames) {
			classNameStr.add(className.getValue());
		}

		return classNameStr;
	}

	public static Class<?> getJavaClass(
		Map<String, Class<?>> javaClassesCache, ClassLoader classloader,
		String className) throws ClassNotFoundException {

		Class<?> clazz = javaClassesCache.get(className);

		if (clazz != null) {
			return clazz;
		}

		try {
			clazz = PortalClassLoaderUtil.getClassLoader().loadClass(className);
		}
		catch (ClassNotFoundException e) {
		}

		if (clazz == null) {
			clazz = classloader.loadClass(className);
		}

		if (_log.isDebugEnabled()) {
			_log.debug(
				"loaded class: " + clazz + " from classloader :" + classloader);
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
				catch (IllegalArgumentException e) {
				}
				catch (IllegalAccessException e) {
				}
			}

			jdbcTypeNames = aux;
		}

		return jdbcTypeNames;
	}

	public static String getLiferayLocalServiceUtilClassName(
		Class<? extends ClassedModel> clazz) {

		Package pkg = clazz.getPackage();

		String packageName = pkg.getName();
		int pos = packageName.lastIndexOf(".model");

		if (pos > 0) {
			packageName = packageName.substring(0, pos);
		}

		String className =
			packageName + ".service." + clazz.getSimpleName() +
				"LocalServiceUtil";

		if (_log.isDebugEnabled()) {
			_log.debug(
				"LocalServiceUtil of " + clazz.getName() + ": " + className);
		}

		return className;
	}

	public static String getLiferayModelImplClassName(
		Class<? extends ClassedModel> clazz) {

		Package pkg = clazz.getPackage();

		String className =
			pkg.getName() + ".impl." + clazz.getSimpleName() + "ModelImpl";

		if (_log.isDebugEnabled()) {
			_log.debug("ModelImpl of " + clazz.getName() + ": " + className);
		}

		return className;
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
				classLiferayModelImpl.getName() + "#" +
				liferayModelImplField, e);
		}

		return data;
	}

	public static void makeFieldModifiable(Field nameField)
		throws NoSuchFieldException, SecurityException,
			IllegalArgumentException, IllegalAccessException {

		nameField.setAccessible(true);
		int modifiers = nameField.getModifiers();
		Field modifierField =
			nameField.getClass().getDeclaredField("modifiers");
		modifiers = modifiers & ~Modifier.FINAL;
		modifierField.setAccessible(true);
		modifierField.setInt(nameField, modifiers);
	}

	public static void setFieldValue(Object owner, Field field, Object value)
		throws IllegalArgumentException, IllegalAccessException,
			NoSuchFieldException, SecurityException {

		makeFieldModifiable(field);
		field.set(owner, value);
	}

	private static Log _log = LogFactoryUtil.getLog(ModelUtil.class);

	private static Map<Integer, String> jdbcTypeNames = null;

}