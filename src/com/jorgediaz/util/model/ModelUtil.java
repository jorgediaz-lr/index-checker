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

import com.jorgediaz.util.reflection.ReflectionUtil;

import com.liferay.portal.kernel.dao.orm.Criterion;
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.DynamicQueryFactoryUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.servlet.ServletContextPool;
import com.liferay.portal.kernel.util.AggregateClassLoader;
import com.liferay.portal.kernel.util.PortalClassLoaderUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.model.ClassName;
import com.liferay.portal.model.ClassedModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.servlet.ServletContext;

/**
 * @author Jorge Díaz
 */
public class ModelUtil {

	public static Criterion generateSingleCriterion(
		Model model, String filter) {

		String[] ops = {"=", "<>", " like ", ">", "<" ,"<=", ">="};

		Criterion criterion = null;

		for (String op : ops) {
			boolean dummyValue = false;

			String filterAux = filter;

			if (filterAux.endsWith(op)) {
				filterAux = filterAux + "DUMMY_TEXT";
				dummyValue = true;
			}

			String[] filterArr = filterAux.split(op);

			if ((filterArr != null) && (filterArr.length == 2)) {
				String attrName = filterArr[0];
				String attrValue = filterArr[1];

				if (dummyValue) {
					attrValue = attrValue.replaceAll(
						"DUMMY_TEXT", StringPool.BLANK);
				}

				if (model.hasAttribute(attrName)) {
					criterion = model.generateSingleCriterion(
						attrName, attrValue, op);
				}

				break;
			}
		}

		return criterion;
	}

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
			String value = className.getValue();

			if (Validator.isNotNull(value) && value.contains(".model.")) {
				classNameStr.add(value);
			}
		}

		return classNameStr;
	}

	public static Object[][] getDatabaseAttributesArr(
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

	public static String getDatabaseAttributesStr(
		Class<?> classLiferayModelImpl) {

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

	public static String getLiferayModelImplClassName(
		String packageName, String simpleName) {

		String className = packageName + ".impl." + simpleName + "ModelImpl";

		if (_log.isDebugEnabled()) {
			_log.debug(
				"ModelImpl of " + packageName + "." + simpleName + ": " +
			className);
		}

		return className;
	}

	public static DynamicQuery newDynamicQuery(
		Class<? extends ClassedModel> clazz, String alias) {

		return DynamicQueryFactoryUtil.forClass(
			clazz, alias, clazz.getClassLoader());
	}

	private static Log _log = LogFactoryUtil.getLog(ModelUtil.class);

}