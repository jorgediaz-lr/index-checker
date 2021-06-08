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

import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.dao.orm.Conjunction;
import com.liferay.portal.kernel.dao.orm.Criterion;
import com.liferay.portal.kernel.dao.orm.Disjunction;
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.Order;
import com.liferay.portal.kernel.dao.orm.Projection;
import com.liferay.portal.kernel.dao.orm.RestrictionsFactoryUtil;
import com.liferay.portal.kernel.model.ClassName;
import com.liferay.portal.kernel.security.permission.ResourceActionsUtil;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.Validator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jorgediazest.util.service.Service;

/**
 * @author Jorge Díaz
 */
public class ModelUtil {

	public static List<?> executeDynamicQuery(
			Service service, Criterion criterion, Projection projection,
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

		if (criterion != null) {
			query.add(criterion);
		}

		return service.executeDynamicQuery(query);
	}

	public static Criterion generateConjunctionCriterion(
		Criterion... criterion) {

		List<Criterion> criterionList = new ArrayList<>();

		for (Criterion criterionAux : criterion) {
			if (criterionAux != null) {
				criterionList.add(criterionAux);
			}
		}

		if (criterionList.isEmpty()) {
			return null;
		}

		if (criterionList.size() == 1) {
			return criterionList.get(0);
		}

		Conjunction conjuntion = RestrictionsFactoryUtil.conjunction();

		for (Criterion criterionAux : criterionList) {
			conjuntion.add(criterionAux);
		}

		return conjuntion;
	}

	public static Criterion generateDisjunctionCriterion(
		Criterion... criterion) {

		List<Criterion> criterionList = new ArrayList<>();

		for (Criterion criterionAux : criterion) {
			if (criterionAux != null) {
				criterionList.add(criterionAux);
			}
		}

		if (criterionList.isEmpty()) {
			return null;
		}

		if (criterionList.size() == 1) {
			return criterionList.get(0);
		}

		Disjunction disjunction = RestrictionsFactoryUtil.disjunction();

		for (Criterion criterionAux : criterionList) {
			disjunction.add(criterionAux);
		}

		return disjunction;
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

	public static List<String> getClassNameValues(
		Collection<ClassName> classNames) {

		List<String> classNameStr = new ArrayList<>();

		for (ClassName className : classNames) {
			String value = className.getValue();

			if (Validator.isNotNull(value) && value.contains(".model.")) {
				classNameStr.add(value);
			}
		}

		return classNameStr;
	}

	public static String getDisplayName(String className, Locale locale) {
		String displayName = ResourceActionsUtil.getModelResource(
			locale, className);

		if (displayName.startsWith(
				ResourceActionsUtil.getModelResourceNamePrefix())) {

			return StringPool.BLANK;
		}

		return displayName;
	}

	protected static Map<String, String> cachedAttributeNames =
		new ConcurrentHashMap<>();

}