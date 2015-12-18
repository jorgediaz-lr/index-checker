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

package com.jorgediaz.util.model;

import com.liferay.portal.kernel.dao.orm.Criterion;
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.MethodKey;
import com.liferay.portal.kernel.util.PortalClassLoaderUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.model.ClassedModel;
import com.liferay.portal.model.Group;
import com.liferay.portal.service.BaseLocalService;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import java.sql.Types;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Jorge Díaz
 */
public class ReflectionUtil {

	public static Criterion generateSingleCriterion(
		Model model, String attrName, String attrValue, String op) {

		Criterion criterion = null;

		if (model.isPartOfPrimaryKeyMultiAttribute(attrName)) {
			attrName = "primaryKey." + attrName;
		}

		try {
			if (model.hasAttribute(attrValue)) {
				criterion =
					(Criterion)contructorCriterionImpl.newInstance(
						contructorPropertyExpression.newInstance(
					new Object[] { attrName, attrValue, op}));
			}
			else {
				Object value =
					ModelUtil.castStringToJdbcTypeObject(
						model.getAttributeType(attrName), attrValue);

				criterion =
					(Criterion)contructorCriterionImpl.newInstance(
						contructorSimpleExpression.newInstance(
					new Object[] { attrName, value, op}));
			}
		}
		catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}

		return criterion;
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

	public String getDatabaseAttributesStr(Class<?> classLiferayModelImpl) {
		String tableName =
			(String)ReflectionUtil.getLiferayModelImplField(
				classLiferayModelImpl, "TABLE_NAME");
		String tableSqlCreate =
			(String)ReflectionUtil.getLiferayModelImplField(
				classLiferayModelImpl, "TABLE_SQL_CREATE");

		int posTableName = tableSqlCreate.indexOf(tableName);

		if (posTableName <= 0) {
			_log.error("Error, TABLE_NAME not found at TABLE_SQL_CREATE");
			return null;
		}

		posTableName = posTableName + tableName.length() + 2;

		String tableAttributes = tableSqlCreate.substring(
			posTableName, tableSqlCreate.length() - 1);

		int posPrimaryKeyMultiAttr = tableAttributes.indexOf(",primary key (");

		if (posPrimaryKeyMultiAttr > 0) {
			tableAttributes = tableAttributes.replaceAll(
				",primary key \\(", "#");
			tableAttributes = tableAttributes.substring(
				0, tableAttributes.length() - 1);
		}

		if (_log.isDebugEnabled()) {
			_log.debug(
				"Database attributes of " + classLiferayModelImpl.getClass() +
				": " + tableAttributes);
		}

		return tableAttributes;
	}

	public Class<?> getJavaClass(ClassLoader classloader, String className)
		throws ClassNotFoundException {

		Class<?> clazz = javaClasses.get(className);

		if (clazz != null) {
			return clazz;
		}

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

		if (clazz != null) {
			javaClasses.put(className, clazz);
		}

		return clazz;
	}

	public Class<?> getJavaClass(String className)
		throws ClassNotFoundException {

		return getJavaClass(null, className);
	}

	public Class<?> getLiferayModelImplClass(
		ClassLoader classloader, String packageName, String simpleName) {

		String liferayModelImpl = ModelUtil.getLiferayModelImplClassName(
			packageName, simpleName);

		Class<?> classLiferayModelImpl;
		try {
			classLiferayModelImpl = getJavaClass(classloader, liferayModelImpl);
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

	public BaseLocalService getService(
		ClassLoader classLoader, String classPackageName,
		String classSimpleName) {

		try {
			return (BaseLocalService)executeMethod(
				classLoader, classPackageName, classSimpleName, "getService",
				null, null);
		}
		catch (Exception e) {
			if (_log.isDebugEnabled()) {
				_log.debug(e, e);
			}
			else if (_log.isInfoEnabled()) {
				_log.info(
					"Cannot get service of " + classPackageName + "." +
					classSimpleName + " EXCEPTION: " + e.getClass().getName() +
					": " + e.getMessage());
			}

			return null;
		}
	}

	protected List<?> executeDynamicQuery(
		Class<? extends ClassedModel> clazz, DynamicQuery dynamicQuery) {

		String classPackageName = clazz.getPackage().getName();
		String classSimpleName = clazz.getSimpleName();

		ClassLoader classLoader = clazz.getClassLoader();

		String classFullName = classPackageName + "." + classSimpleName;

		try {
			Method method = getLocalServiceUtilMethod(
				classLoader, classPackageName, classSimpleName, "dynamicQuery",
				DynamicQuery.class);

			if (method == null) {
				return null;
			}

			return (List<?>)method.invoke(null, dynamicQuery);
		}
		catch (Exception e) {
			if (!classFullName.equals(Group.class.getName()) &&
				(e instanceof ClassNotFoundException ||
				 e instanceof NoSuchMethodException)) {

				if (_log.isInfoEnabled()) {
					_log.info(
						"executeDynamicQuery: dynamicQuery method not found " +
						"for " + classFullName + " - " + e.getMessage() +
						" trying with GroupLocalServiceUtil.dynamicQuery");
				}

				return executeDynamicQuery(Group.class, dynamicQuery);
			}

			String cause = StringPool.BLANK;
			Throwable rootException = e.getCause();

			if (rootException != null) {
				cause = " (root cause: " + rootException.getMessage() + ")";
			}

			throw new RuntimeException(
				"executeDynamicQuery: error invoking dynamicQuery method for " +
					classFullName + ": " + cause, e);
		}
	}

	protected Object executeMethod(
		ClassLoader classLoader, String classPackageName,
		String classSimpleName, String methodName, Class<?> parameterType,
		Object arg) {

		String classFullName = classPackageName + "." + classSimpleName;

		try {
			Method method = getLocalServiceUtilMethod(
				classLoader, classPackageName, classSimpleName, methodName,
				parameterType);

			if (method == null) {
				return null;
			}

			if (arg == null) {
				return method.invoke(null);
			}

			return method.invoke(null, arg);
		}
		catch (ClassNotFoundException e) {
			throw new RuntimeException(
				"executeMethod: class not found exception calling " +
				methodName + " for " + classFullName, e);
		}
		catch (NoSuchMethodException e) {
			throw new RuntimeException(
				"executeMethod: " + methodName + " method not found for " +
				classFullName, e);
		}
		catch (Exception e) {
			String cause = StringPool.BLANK;
			Throwable rootException = e.getCause();

			if (rootException != null) {
				cause = " (root cause: " + rootException.getMessage() + ")";
			}

			throw new RuntimeException(
				"executeMethod: " + methodName + " method for " +
				classFullName + ": " + cause, e);
		}
	}

	protected Object executeServiceMethod(
		BaseLocalService service, String methodName, Class<?> parameterType,
		Object arg) {

		if (service == null) {
			throw new IllegalArgumentException("service must be not null");
		}

		try {
			Method method = getLocalServiceMethod(
				service, methodName, parameterType);

			if (method == null) {
				return null;
			}

			if (arg == null) {
				return method.invoke(service);
			}

			return method.invoke(service, arg);
		}
		catch (NoSuchMethodException e) {
			throw new RuntimeException(
				"executeMethod: " + methodName + " method not found for " +
				service, e);
		}
		catch (Exception e) {
			String cause = StringPool.BLANK;
			Throwable rootException = e.getCause();

			if (rootException != null) {
				cause = " (root cause: " + rootException.getMessage() + ")";
			}

			throw new RuntimeException(
				"executeMethod: " + methodName + " method for " +
				service + ": " + cause, e);
		}
	}

	protected Object[][] getDatabaseAttributesArr(
		Class<?> classLiferayModelImpl) {

		Object[][] tableColumns =
			(Object[][])ReflectionUtil.getLiferayModelImplField(
				classLiferayModelImpl, "TABLE_COLUMNS");

		if (_log.isDebugEnabled()) {
			_log.debug(
				"Database attributes array of " +
				classLiferayModelImpl.getName() + ": " +
				Arrays.toString(tableColumns));
		}

		return tableColumns;
	}

	protected Method getLocalServiceMethod(
		BaseLocalService service, String methodName, Class<?> parameterType)
			throws ClassNotFoundException, NoSuchMethodException {

		String key = service.getClass().getName() + "#" + methodName;

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
			Class<?> classLocalService = service.getClass();

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

	protected Method getLocalServiceUtilMethod(
		ClassLoader classLoader, String packageName, String simpleName,
		String methodName, Class<?> parameterType)
			throws ClassNotFoundException, NoSuchMethodException {

		String key = packageName + "." + simpleName + "#" + methodName;

		if (parameterType != null) {
			key = key + "#" + parameterType.getName();
		}

		Method method = null;

		if (localServiceUtilMethods.containsKey(key)) {
			try {
				method = localServiceUtilMethods.get(key).getMethod();
			}
			catch (NoSuchMethodException e) {
			}
		}

		if (method == null) {
			String localServiceUtil =
				ModelUtil.getLiferayLocalServiceUtilClassName(
					packageName, simpleName);

			Class<?> classLocalServiceUtil = getJavaClass(
				classLoader, localServiceUtil);

			if ((localServiceUtil != null) && (parameterType != null)) {
				method = classLocalServiceUtil.getMethod(
					methodName, parameterType);
			}
			else if (localServiceUtil != null) {
				method = classLocalServiceUtil.getMethod(methodName);
			}

			if (method == null) {
				localServiceUtilMethods.put(key, new MethodKey());
			}
			else {
				localServiceUtilMethods.put(key, new MethodKey(method));
			}
		}

		return method;
	}

	protected Map<String, Class<?>> javaClasses =
		new ConcurrentHashMap<String, Class<?>>();

	private static Log _log = LogFactoryUtil.getLog(ReflectionUtil.class);

	private static Constructor<?> contructorCriterionImpl;
	private static Constructor<?> contructorPropertyExpression;
	private static Constructor<?> contructorSimpleExpression;
	private static Map<Integer, String> jdbcTypeNames = null;

	static {
		try {
			Class<?> criterion =
				PortalClassLoaderUtil.getClassLoader().loadClass(
				"org.hibernate.criterion.Criterion");

			Class<?> simpleExpression =
				PortalClassLoaderUtil.getClassLoader().loadClass(
				"org.hibernate.criterion.SimpleExpression");
			contructorSimpleExpression =
				simpleExpression.getDeclaredConstructor(
					String.class, Object.class, String.class);
			contructorSimpleExpression.setAccessible(true);

			Class<?> propertyExpression =
				PortalClassLoaderUtil.getClassLoader().loadClass(
				"org.hibernate.criterion.PropertyExpression");
			contructorPropertyExpression =
				propertyExpression.getDeclaredConstructor(
					String.class, String.class, String.class);
			contructorPropertyExpression.setAccessible(true);

			Class<?> criterionImpl =
				PortalClassLoaderUtil.getClassLoader().loadClass(
				"com.liferay.portal.dao.orm.hibernate.CriterionImpl");
			contructorCriterionImpl = criterionImpl.getDeclaredConstructor(
				criterion);
		}
		catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	private Map<String, MethodKey> localServiceMethods =
		new ConcurrentHashMap<String, MethodKey>();
	private Map<String, MethodKey> localServiceUtilMethods =
		new ConcurrentHashMap<String, MethodKey>();

}