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

import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.DynamicQueryFactoryUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.MethodKey;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.model.ClassedModel;
import com.liferay.portal.model.Group;
import com.liferay.portal.service.BaseLocalService;

import java.lang.reflect.Method;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Jorge Díaz
 */
public class ModelFactory {

	public ModelFactory() {
		this(null, null);
	}

	public ModelFactory(Class<? extends Model> defaultModelClass) {
		this(defaultModelClass, null);
	}

	public ModelFactory(
		Class<? extends Model> defaultModelClass,
		Map<String, Class<? extends Model>> modelClassMap) {

		if (defaultModelClass == null) {
			this.defaultModelClass = DefaultModel.class;
		}
		else {
			this.defaultModelClass = defaultModelClass;
		}

		this.modelClassMap = modelClassMap;
	}

	public Map<String, Model> getModelMap(Collection<String> classNames) {

		ClassLoader classLoader = ModelUtil.getClassLoader();

		Map<String, Model> modelMap = new LinkedHashMap<String, Model>();

		for (String classname : classNames) {
			Model model = null;
			String[] attributes = null;
			try {
				model = this.getModelObject(classLoader, classname);

				if (model != null) {
					attributes = model.getAttributesName();
				}
			}
			catch (Exception e) {
				if (_log.isDebugEnabled()) {
					_log.debug(e, e);
				}
				else if (_log.isInfoEnabled()) {
					_log.info(
						"Cannot get model object of " + classname +
						" EXCEPTION: " + e.getClass().getName() + ": " +
						e.getMessage());
				}
			}

			if ((model != null) && (attributes != null)) {
				modelMap.put(model.getName(), model);
			}
		}

		return modelMap;
	}

	public Model getModelObject(Class<? extends ClassedModel> clazz) {

		if ((clazz == null) || !ClassedModel.class.isAssignableFrom(clazz)) {
			if (_log.isDebugEnabled()) {
				_log.debug(
					"Class: " + clazz.getName() + "is null or does not "+
					"implements ClassedModel, returning null");
			}

			return null;
		}

		String className = clazz.getName();
		Class<? extends Model> modelClass = this.defaultModelClass;

		if ((this.modelClassMap != null) &&
			this.modelClassMap.containsKey(className)) {

			modelClass = this.modelClassMap.get(className);
		}

		Model model = null;
		try {
			model = (Model)modelClass.newInstance();

			model.init(this, clazz);
		}
		catch (Exception e) {
			_log.error(
				"getModelObject(" + clazz.getName() + ") ERROR " +
				e.getClass().getName() + ": " + e.getMessage());
			throw new RuntimeException(e);
		}

		return model;
	}

	/**
	 * primaries keys can be at following ways:
	 *
	 * - single => create table
	 * UserGroupGroupRole (userGroupId LONG not null,groupId LONG not
	 * null,roleId LONG not null,primary key (userGroupId, groupId, roleId))";
	 *
	 * - multi => create table JournalArticle (uuid_ VARCHAR(75) null,id_ LONG
	 * not null primary key,resourcePrimKey LONG,groupId LONG,companyId LONG,
	 * userId LONG,userName VARCHAR(75) null,createDate DATE null,modifiedDate
	 * DATE  null,folderId LONG,classNameId LONG,classPK LONG,treePath STRING
	 * null,articleId VARCHAR(75) null,version DOUBLE,title STRING null,urlTitle
	 * VARCHAR(150) null,description TEXT null,content TEXT null,type_
	 * VARCHAR(75) null,structureId VARCHAR(75) null,templateId VARCHAR(75)
	 * null,layoutUuid VARCHAR(75) null,displayDate DATE null,expirationDate
	 * DATE null,reviewDate DATE null,indexable BOOLEAN,smallImage
	 * BOOLEAN,smallImageId LONG,smallImageURL STRING null,status
	 * INTEGER,statusByUserId LONG,statusByUserName VARCHAR(75) null,statusDate
	 * DATE null)
	 */

	@SuppressWarnings("unchecked")
	public final Model getModelObject(
		ClassLoader classLoader, String className) {

		Class<? extends ClassedModel> clazz;
		try {
			clazz =
				(Class<? extends ClassedModel>)ModelUtil.getJavaClass(
					javaClasses, classLoader, className);
		}
		catch (ClassNotFoundException e) {
			_log.error("Class not found: " + className);
			throw new RuntimeException(e);
		}

		return getModelObject(clazz);
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
		ClassLoader classloader, String packageName, String simpleName) {

		String liferayModelImpl = ModelUtil.getLiferayModelImplClassName(
			packageName, simpleName);

		Class<?> classLiferayModelImpl;
		try {
			classLiferayModelImpl =
				ModelUtil.getJavaClass(
					javaClasses, classloader, liferayModelImpl);
		}
		catch (ClassNotFoundException e) {
			_log.error("Class not found: " + liferayModelImpl);

			throw new RuntimeException(e);
		}

		if (classLiferayModelImpl == null) {
			_log.error("Class not found: " + liferayModelImpl);

			throw new RuntimeException("Class not found: " + liferayModelImpl);
		}

		Object[][] tableColumns =
			(Object[][])ModelUtil.getLiferayModelImplField(
				classLiferayModelImpl, "TABLE_COLUMNS");

		if (_log.isDebugEnabled()) {
			_log.debug(
				"Database attributes array of " + packageName + "." +
				simpleName + ": " + Arrays.toString(tableColumns));
		}

		return tableColumns;
	}

	protected String getDatabaseAttributesStr(
		ClassLoader classloader, String packageName, String simpleName) {

		String liferayModelImpl = ModelUtil.getLiferayModelImplClassName(
			packageName, simpleName);

		Class<?> classLiferayModelImpl;
		try {
			classLiferayModelImpl =
				ModelUtil.getJavaClass(
					javaClasses, classloader, liferayModelImpl);
		}
		catch (ClassNotFoundException e) {
			_log.error("Class not found: " + liferayModelImpl);
			throw new RuntimeException(e);
		}

		if (classLiferayModelImpl == null) {
			_log.error("Class not found: " + liferayModelImpl);
			throw new RuntimeException("Class not found: " + liferayModelImpl);
		}

		String tableName =
			(String)ModelUtil.getLiferayModelImplField(
				classLiferayModelImpl, "TABLE_NAME");
		String tableSqlCreate =
			(String)ModelUtil.getLiferayModelImplField(
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
				"Database attributes of " + packageName + "." + simpleName +
				": " + tableAttributes);
		}

		return tableAttributes;
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

			Class<?> classLocalServiceUtil =
				ModelUtil.getJavaClass(
					javaClasses, classLoader, localServiceUtil);

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

	protected DynamicQuery newDynamicQuery(
		Class<? extends ClassedModel> clazz, String alias) {

		return DynamicQueryFactoryUtil.forClass(
			clazz, alias, clazz.getClassLoader());
	}

	protected Class<? extends Model> defaultModelClass = null;
	protected Map<String, Class<? extends Model>> modelClassMap = null;

	private static Log _log = LogFactoryUtil.getLog(ModelFactory.class);

	private Map<String, Class<?>> javaClasses =
		new ConcurrentHashMap<String, Class<?>>();
	private Map<String, MethodKey> localServiceMethods =
		new ConcurrentHashMap<String, MethodKey>();
	private Map<String, MethodKey> localServiceUtilMethods =
		new ConcurrentHashMap<String, MethodKey>();

}