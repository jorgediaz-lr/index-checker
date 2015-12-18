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
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.servlet.ServletContextPool;
import com.liferay.portal.kernel.util.AggregateClassLoader;
import com.liferay.portal.kernel.util.PortalClassLoaderUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.model.ClassName;

import java.sql.Types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;

/**
 * @author Jorge Díaz
 */
public class ModelUtil {

	public static Object castStringToJdbcTypeObject(int type, String value) {
		value = StringUtil.unquote(value);

		Object result = null;

		switch (type) {
			case Types.CHAR:
			case Types.VARCHAR:
			case Types.LONGVARCHAR:
			case Types.CLOB:
				result = value;
				break;

			case Types.NUMERIC:
			case Types.DECIMAL:
				result = new java.math.BigDecimal(value);
				break;

			case Types.BIT:
			case Types.BOOLEAN:
				result = Boolean.parseBoolean(value);
				break;

			case Types.TINYINT:
				result = Byte.parseByte(value);
				break;

			case Types.SMALLINT:
				result = Short.parseShort(value);
				break;

			case Types.INTEGER:
				result = Integer.parseInt(value);
				break;

			case Types.BIGINT:
				result = Long.parseLong(value);
				break;

			case Types.REAL:
			case Types.FLOAT:
				result = Float.parseFloat(value);
				break;

			case Types.DOUBLE:
				result = Double.parseDouble(value);
				break;

			case Types.BINARY:
			case Types.VARBINARY:
			case Types.LONGVARBINARY:
				result = value.getBytes();
				break;

			case Types.DATE:
				result = java.sql.Date.valueOf(value);
				break;

			case Types.TIME:
				result = java.sql.Time.valueOf(value);
				break;

			case Types.TIMESTAMP:
				result = java.sql.Timestamp.valueOf(value);
				break;

			default:
				throw new RuntimeException(
					"Unsupported conversion for " +
						ModelUtil.getJdbcTypeNames().get(type));
		}

		return result;
	}

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

	public static Class<?> getJdbcTypeClass(int type) {
		Class<?> result = Object.class;

		switch (type) {
			case Types.CHAR:
			case Types.VARCHAR:
			case Types.LONGVARCHAR:
			case Types.CLOB:
				result = String.class;
				break;

			case Types.NUMERIC:
			case Types.DECIMAL:
				result = java.math.BigDecimal.class;
				break;

			case Types.BIT:
			case Types.BOOLEAN:
				result = Boolean.class;
				break;

			case Types.TINYINT:
				result = Byte.class;
				break;

			case Types.SMALLINT:
				result = Short.class;
				break;

			case Types.INTEGER:
				result = Integer.class;
				break;

			case Types.BIGINT:
				result = Long.class;
				break;

			case Types.REAL:
			case Types.FLOAT:
				result = Float.class;
				break;

			case Types.DOUBLE:
				result = Double.class;
				break;

			case Types.BINARY:
			case Types.VARBINARY:
			case Types.LONGVARBINARY:
				result = Byte[].class;
				break;

			case Types.DATE:
				result = java.sql.Date.class;
				break;

			case Types.TIME:
				result = java.sql.Time.class;
				break;

			case Types.TIMESTAMP:
				result = java.sql.Timestamp.class;
				break;
		}

		return result;
	}

	public static Map<Integer, String> getJdbcTypeNames() {
		return ReflectionUtil.getJdbcTypeNames();
	}

	public static String getLiferayLocalServiceUtilClassName(
		String packageName, String simpleName) {

		int pos = packageName.lastIndexOf(".model");

		if (pos > 0) {
			packageName = packageName.substring(0, pos);
		}

		String className =
			packageName + ".service." + simpleName +
				"LocalServiceUtil";

		if (_log.isDebugEnabled()) {
			_log.debug(
				"LocalServiceUtil of " + packageName + "." + simpleName + ": " +
				className);
		}

		return className;
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

	private static Log _log = LogFactoryUtil.getLog(ModelUtil.class);

}