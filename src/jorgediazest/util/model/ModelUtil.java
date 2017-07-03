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

package jorgediazest.util.model;

import com.liferay.portal.kernel.dao.orm.Conjunction;
import com.liferay.portal.kernel.dao.orm.Criterion;
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.Order;
import com.liferay.portal.kernel.dao.orm.Projection;
import com.liferay.portal.kernel.dao.orm.RestrictionsFactoryUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.servlet.PluginContextListener;
import com.liferay.portal.kernel.servlet.ServletContextPool;
import com.liferay.portal.kernel.util.AggregateClassLoader;
import com.liferay.portal.kernel.util.PortalClassLoaderUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.model.ClassName;
import com.liferay.portal.util.PortalUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletContext;

import jorgediazest.util.service.Service;

/**
 * @author Jorge Díaz
 */
public class ModelUtil {

	public static List<?> executeDynamicQuery(
			Service service, Criterion filter, Projection projection,
			List<Order> orders)
		throws Exception {

		DynamicQuery query = service.newDynamicQuery();

		if (projection != null) {
			query.setProjection(projection);
		}

		if (orders != null) {
			for (Order order : orders) {
				query.addOrder(order);
			}
		}

		if (filter != null) {
			query.add(filter);
		}

		return service.executeDynamicQuery(query);
	}

	public static Criterion generateConjunctionQueryFilter(
		Criterion criterion1, Criterion criterion2) {

		if (criterion1 == null) {
			return criterion2;
		}
		else if (criterion2 == null) {
			return criterion1;
		}

		Conjunction conjuntion = RestrictionsFactoryUtil.conjunction();
		conjuntion.add(criterion1);
		conjuntion.add(criterion2);
		return conjuntion;
	}

	@Deprecated
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

	public static Criterion generateSQLCriterion(String sql) {
		if (Validator.isNull(sql)) {
			return null;
		}

		sql = PortalUtil.transformSQL(sql);

		return RestrictionsFactoryUtil.sqlRestriction(sql);
	}

	public static String getCachedAttributeName(String attribute) {
		if (!cachedAttributeNames.containsKey(attribute)) {
			cachedAttributeNames.put(attribute, attribute);
		}

		return cachedAttributeNames.get(attribute);
	}

	@Deprecated
	public static ClassLoader getClassLoaderAggregate() {

		ClassLoader portalClassLoader = PortalClassLoaderUtil.getClassLoader();

		AggregateClassLoader aggregateClassLoader = new AggregateClassLoader(
			portalClassLoader);

		if (_log.isDebugEnabled()) {
			_log.debug("Adding " + portalClassLoader);
		}

		aggregateClassLoader.addClassLoader(portalClassLoader);

		aggregateClassLoader.addClassLoader(getClassLoaders());

		return aggregateClassLoader;
	}

	@Deprecated
	public static List<ClassLoader> getClassLoaders() {
		List<ClassLoader> classLoaders = new ArrayList<ClassLoader>();

		for (String servletContextName : ServletContextPool.keySet()) {
			try {
				ServletContext servletContext = ServletContextPool.get(
					servletContextName);

				ClassLoader classLoader =
					(ClassLoader)servletContext.getAttribute(
						PluginContextListener.PLUGIN_CLASS_LOADER);

				if (classLoader == null) {
					continue;
				}

				if (_log.isDebugEnabled()) {
					_log.debug(
						"Adding " + classLoader + " for " + servletContextName);
				}

				classLoaders.add(classLoader);
			}
			catch (Exception e) {
				if (_log.isWarnEnabled()) {
					_log.warn(
						"Error adding classLoader for " + servletContextName +
						": " + e.getMessage(), e);
				}
			}
		}

		return classLoaders;
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

	protected static Map<String, String> cachedAttributeNames =
		new ConcurrentHashMap<String, String>();

	private static Log _log = LogFactoryUtil.getLog(ModelUtil.class);

}