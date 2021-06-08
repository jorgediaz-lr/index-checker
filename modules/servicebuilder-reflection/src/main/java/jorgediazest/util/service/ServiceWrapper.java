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

import com.liferay.portal.kernel.dao.orm.Criterion;
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.ClassedModel;

import java.util.List;

import jorgediazest.util.model.ModelUtil;
import jorgediazest.util.reflection.ReflectionUtil;
import jorgediazest.util.table.TableInfo;

/**
 * @author Jorge Díaz
 */
public class ServiceWrapper implements Cloneable, Service {

	public ServiceWrapper(Service service) {
		this.service = service;
	}

	public void addCriterion(Criterion criterion) {
		this.criterion = ModelUtil.generateConjunctionCriterion(
			this.criterion, criterion);
	}

	@Override
	public ClassedModel addObject(ClassedModel object) {
		return service.addObject(object);
	}

	@Override
	public ServiceWrapper clone() {
		ServiceWrapper serviceWrapper = new ServiceWrapper(service);

		serviceWrapper.setCriterion(criterion);

		return serviceWrapper;
	}

	@Override
	public ClassedModel createObject(long primaryKey) {
		return service.createObject(primaryKey);
	}

	@Override
	public ClassedModel deleteObject(ClassedModel object) {
		return service.deleteObject(object);
	}

	@Override
	public ClassedModel deleteObject(long primaryKey) {
		return service.deleteObject(primaryKey);
	}

	@Override
	public List<?> executeDynamicQuery(DynamicQuery dynamicQuery)
		throws Exception {

		prepareDynamicQuery(dynamicQuery);

		return service.executeDynamicQuery(dynamicQuery);
	}

	@Override
	public ClassedModel fetchObject(long primaryKey) {
		return service.fetchObject(primaryKey);
	}

	@Override
	public ClassLoader getClassLoader() {
		return service.getClassLoader();
	}

	@Override
	public String getClassName() {
		return service.getClassName();
	}

	@Override
	public String getClassSimpleName() {
		return service.getClassSimpleName();
	}

	public Criterion getCriterion() {
		return criterion;
	}

	@Override
	public List<String> getMappingTables() {
		return service.getMappingTables();
	}

	@Override
	public TableInfo getTableInfo() {
		return service.getTableInfo();
	}

	@Override
	public TableInfo getTableInfo(String fieldPrefix) {
		return service.getTableInfo(fieldPrefix);
	}

	@Override
	public DynamicQuery newDynamicQuery() {
		return service.newDynamicQuery();
	}

	@Override
	public DynamicQuery newDynamicQuery(String alias) {
		return service.newDynamicQuery(alias);
	}

	public void prepareDynamicQuery(DynamicQuery dynamicQuery) {
		if (criterion != null) {
			dynamicQuery.add(criterion);
		}

		if (_log.isDebugEnabled()) {
			if (criterion != null) {
				String filterString = ReflectionUtil.getWrappedString(
					criterion, "getWrappedCriterion");

				_log.debug("added filter: " + filterString);
			}

			String filterDynamicQuery = ReflectionUtil.getWrappedString(
				dynamicQuery, "getDetachedCriteria");

			_log.debug("executing dynamicQuery: " + filterDynamicQuery);
		}
	}

	public final void setCriterion(Criterion criterion) {
		this.criterion = criterion;
	}

	@Override
	public ClassedModel updateObject(ClassedModel object) {
		return service.updateObject(object);
	}

	protected Criterion criterion = null;
	protected Service service;

	private static Log _log = LogFactoryUtil.getLog(ServiceWrapper.class);

}