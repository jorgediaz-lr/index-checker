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

import com.liferay.portal.kernel.dao.orm.Criterion;
import com.liferay.portal.kernel.dao.orm.Order;
import com.liferay.portal.kernel.dao.orm.Projection;
import com.liferay.portal.kernel.dao.orm.ProjectionFactoryUtil;
import com.liferay.portal.kernel.dao.orm.ProjectionList;
import com.liferay.portal.kernel.dao.orm.Property;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.Validator;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jorgediazest.util.service.Service;
import jorgediazest.util.service.ServiceWrapper;
import jorgediazest.util.table.TableInfo;

/**
 * @author Jorge Díaz
 */
public class ModelWrapper implements Model, Cloneable {

	public ModelWrapper(Model model) {
		this.model = model;
	}

	public void addCriterion(Criterion criterion) {
		if (serviceWrapper == null) {
			serviceWrapper = new ServiceWrapper(model.getService());
		}

		serviceWrapper.addCriterion(criterion);
	}

	@Override
	public Model clone() {
		ModelWrapper modelWrapper = new ModelWrapper(model);

		if (serviceWrapper != null) {
			modelWrapper.serviceWrapper = serviceWrapper.clone();
		}

		modelWrapper.setNameSuffix(suffix);

		return modelWrapper;
	}

	@Override
	public int compareTo(Model o) {
		return model.compareTo(o);
	}

	@Override
	public long count() {
		return count(null);
	}

	@Override
	public long count(Criterion criterion) {
		try {
			List<?> list = executeDynamicQuery(
				criterion, ProjectionFactoryUtil.rowCount());

			if (list != null) {
				return (Long)list.get(0);
			}
		}
		catch (Exception e) {
			_log.error(
				"Error executing count for " + getName() + ": " +
					e.getMessage());
		}

		return -1;
	}

	@Override
	public List<?> executeDynamicQuery(Criterion criterion) throws Exception {

		return executeDynamicQuery(criterion, null, null);
	}

	@Override
	public List<?> executeDynamicQuery(Criterion criterion, List<Order> orders)
		throws Exception {

		return executeDynamicQuery(criterion, null, orders);
	}

	@Override
	public List<?> executeDynamicQuery(Criterion criterion, Order order)
		throws Exception {

		List<Order> orders = Collections.singletonList(order);

		return executeDynamicQuery(criterion, null, orders);
	}

	@Override
	public List<?> executeDynamicQuery(
			Criterion criterion, Projection projection)
		throws Exception {

		return executeDynamicQuery(criterion, projection, null);
	}

	@Override
	public List<?> executeDynamicQuery(
			Criterion criterion, Projection projection, List<Order> orders)
		throws Exception {

		return ModelUtil.executeDynamicQuery(
			getService(), criterion, projection, orders);
	}

	@Override
	public Class<?> getAttributeClass(String name) {
		return model.getAttributeClass(name);
	}

	@Override
	public <T> Criterion getAttributeCriterion(String attribute, List<T> list) {
		return model.getAttributeCriterion(attribute, list);
	}

	@Override
	public <T> Criterion getAttributeCriterion(String attribute, T value) {
		return model.getAttributeCriterion(attribute, value);
	}

	@Override
	public int getAttributePos(String name) {
		return model.getAttributePos(name);
	}

	@Override
	public String[] getAttributesName() {
		return model.getAttributesName();
	}

	@Override
	public String getClassName() {
		return model.getClassName();
	}

	@Override
	public long getClassNameId() {
		return model.getClassNameId();
	}

	@Override
	public String getClassSimpleName() {
		return model.getClassSimpleName();
	}

	public String getDisplayName(Locale locale) {
		String displayName = model.getDisplayName(locale);

		if (Validator.isNotNull(displayName) && Validator.isNotNull(suffix)) {
			return displayName + " (" + suffix + ")";
		}

		return displayName;
	}

	@Override
	public Model getFilteredModel(Criterion criterion) {
		return getFilteredModel(criterion, null);
	}

	@Override
	public Model getFilteredModel(Criterion criterion, String nameSuffix) {
		if (serviceWrapper != null) {
			criterion = ModelUtil.generateConjunctionCriterion(
				criterion, serviceWrapper.getCriterion());
		}

		return model.getFilteredModel(criterion, nameSuffix);
	}

	@Override
	public List<String> getKeyAttributes() {
		if (keyAttributes != null) {
			return keyAttributes;
		}

		return model.getKeyAttributes();
	}

	@Override
	public ModelFactory getModelFactory() {
		return model.getModelFactory();
	}

	@Override
	public String getName() {
		if (name == null) {
			return model.getName();
		}

		return name;
	}

	@Override
	public String getPrimaryKeyAttribute() {
		return model.getPrimaryKeyAttribute();
	}

	@Override
	public String[] getPrimaryKeyMultiAttribute() {
		return model.getPrimaryKeyMultiAttribute();
	}

	@Override
	public Property getProperty(String attribute) {
		return model.getProperty(attribute);
	}

	@Override
	public Projection getPropertyProjection(String attribute) {
		return model.getPropertyProjection(attribute);
	}

	@Override
	public ProjectionList getPropertyProjection(String[] attributes) {
		return model.getPropertyProjection(attributes);
	}

	@Override
	public ProjectionList getPropertyProjection(
		String[] attributes, List<String> validAttributes,
		List<String> notValidAttributes) {

		return model.getPropertyProjection(
			attributes, validAttributes, notValidAttributes);
	}

	@Override
	public Service getService() {
		if (serviceWrapper == null) {
			return model.getService();
		}

		return serviceWrapper;
	}

	@Override
	public TableInfo getTableInfo() {
		return model.getTableInfo();
	}

	@Override
	public TableInfo getTableInfo(String attribute) {
		return model.getTableInfo(attribute);
	}

	@Override
	public Map<String, TableInfo> getTableInfoMappings() {
		return model.getTableInfoMappings();
	}

	@Override
	public boolean hasAttribute(String attribute) {
		return model.hasAttribute(attribute);
	}

	@Override
	public boolean hasAttributes(String[] attributes) {
		return model.hasAttributes(attributes);
	}

	@Override
	public boolean isAuditedModel() {
		return model.isAuditedModel();
	}

	@Override
	public boolean isGroupedModel() {
		return model.isGroupedModel();
	}

	@Override
	public boolean isPartOfPrimaryKeyMultiAttribute(String attribute) {
		return model.isPartOfPrimaryKeyMultiAttribute(attribute);
	}

	@Override
	public boolean isResourcedModel() {
		return model.isResourcedModel();
	}

	@Override
	public boolean isStagedModel() {
		return model.isStagedModel();
	}

	@Override
	public boolean isWorkflowEnabled() {
		return model.isWorkflowEnabled();
	}

	@Override
	public boolean modelEqualsClass(Class<?> clazz) {
		return model.modelEqualsClass(clazz);
	}

	public void setCriterion(Criterion criterion) {
		if (count(criterion)==-1) {
			throw new IllegalArgumentException();
		}

		if (serviceWrapper == null) {
			serviceWrapper = new ServiceWrapper(model.getService());
		}

		serviceWrapper.setCriterion(criterion);
	}

	public void setKeyAttributes(List<String> keyAttributes) {
		this.keyAttributes = keyAttributes;
	}

	public void setNameSuffix(String suffix) {
		if (Validator.isNull(suffix)) {
			return;
		}

		this.suffix = suffix;

		this.name = getClassName() + "_" + this.suffix;
	}

	@Override
	public String toString() {
		return getName();
	}

	protected static Log _log = LogFactoryUtil.getLog(ModelWrapper.class);

	protected List<String> keyAttributes = null;
	protected Model model;
	protected String name = null;
	protected ServiceWrapper serviceWrapper = null;
	protected String suffix = null;

}