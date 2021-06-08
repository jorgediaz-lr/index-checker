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
import com.liferay.portal.kernel.dao.orm.Criterion;
import com.liferay.portal.kernel.dao.orm.Disjunction;
import com.liferay.portal.kernel.dao.orm.Order;
import com.liferay.portal.kernel.dao.orm.Projection;
import com.liferay.portal.kernel.dao.orm.ProjectionFactoryUtil;
import com.liferay.portal.kernel.dao.orm.ProjectionList;
import com.liferay.portal.kernel.dao.orm.Property;
import com.liferay.portal.kernel.dao.orm.PropertyFactoryUtil;
import com.liferay.portal.kernel.dao.orm.RestrictionsFactoryUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.Validator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jorgediazest.util.service.Service;
import jorgediazest.util.table.TableInfo;

/**
 * @author Jorge Díaz
 */
public class ModelImpl implements Model {

	/* Oracle limitation */
	public static final int MAX_NUMBER_OF_CLAUSES = 1000;

	public ModelImpl(
			ModelFactory modelFactory, String className, Service service)
		throws Exception {

		this.modelFactory = modelFactory;
		this.service = service;

		this.className = className;

		this.classSimpleName = className;

		int pos = className.lastIndexOf(".");

		if (pos != -1) {
			this.classSimpleName = className.substring(pos + 1);
		}
	}

	@Override
	public int compareTo(Model o) {
		return this.getClassName(
		).compareTo(
			o.getClassName()
		);
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

			if ((list != null) && !list.isEmpty()) {
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
		return getTableInfo().getAttributeClass(name);
	}

	@Override
	public <T> Criterion getAttributeCriterion(String attribute, List<T> list) {
		if (!this.hasAttribute(attribute) || Validator.isNull(list)) {
			return null;
		}

		Property property = getProperty(attribute);

		if (list.size() == 1) {
			return property.eq(list.get(0));
		}

		int maxNumClauses = MAX_NUMBER_OF_CLAUSES;

		if (list.size() <= maxNumClauses) {
			return property.in(list);
		}

		Disjunction disjunction = RestrictionsFactoryUtil.disjunction();

		int numberOfDisjuntions =
			(list.size() + maxNumClauses - 1) / maxNumClauses;

		for (int i = 0; i < numberOfDisjuntions; i++) {
			int start = i * maxNumClauses;
			int end = Math.min(start + maxNumClauses, list.size());

			List<T> subList = list.subList(start, end);

			disjunction.add(property.in(subList));
		}

		return disjunction;
	}

	@Override
	public <T> Criterion getAttributeCriterion(String attribute, T value) {
		return getAttributeCriterion(
			attribute, Collections.singletonList(value));
	}

	@Override
	public int getAttributePos(String name) {
		return this.getTableInfo(
		).getAttributePos(
			name
		);
	}

	@Override
	public String[] getAttributesName() {
		return getTableInfo().getAttributesName();
	}

	@Override
	public String getClassName() {
		return className;
	}

	@Override
	public long getClassNameId() {
		return PortalUtil.getClassNameId(getClassName());
	}

	@Override
	public String getClassSimpleName() {
		return classSimpleName;
	}

	@Override
	public String getDisplayName(Locale locale) {
		return ModelUtil.getDisplayName(this.getClassName(), locale);
	}

	@Override
	public Model getFilteredModel(Criterion criterion) {
		return getFilteredModel(criterion, null);
	}

	@Override
	public Model getFilteredModel(Criterion criterion, String nameSuffix) {
		if (criterion == null) {
			return this;
		}

		if (count(criterion) == -1) {
			return null;
		}

		ModelWrapper modelWrapper = new ModelWrapper(this);

		modelWrapper.setCriterion(criterion);

		if (Validator.isNotNull(nameSuffix)) {
			modelWrapper.setNameSuffix(nameSuffix);
		}

		return modelWrapper;
	}

	@Override
	public List<String> getKeyAttributes() {
		String primaryKeyAttribute = getPrimaryKeyAttribute();
		String[] primaryKeyMultiAttribute = getPrimaryKeyMultiAttribute();

		if (Validator.isNull(primaryKeyAttribute)) {
			return Arrays.asList(primaryKeyMultiAttribute);
		}

		if (primaryKeyMultiAttribute.length == 0) {
			return Collections.singletonList(primaryKeyAttribute);
		}

		List<String> primaryKeyAttributes = new ArrayList<>();

		primaryKeyAttributes.add(primaryKeyAttribute);

		primaryKeyAttributes.addAll(Arrays.asList(primaryKeyMultiAttribute));

		return primaryKeyAttributes;
	}

	@Override
	public ModelFactory getModelFactory() {
		return modelFactory;
	}

	@Override
	public String getName() {
		return getClassName();
	}

	@Override
	public String getPrimaryKeyAttribute() {
		return getTableInfo().getPrimaryKeyAttribute();
	}

	@Override
	public String[] getPrimaryKeyMultiAttribute() {
		return getTableInfo().getPrimaryKeyMultiAttribute();
	}

	@Override
	public Property getProperty(String attribute) {
		attribute = cleanAttributeName(attribute);

		if (isPartOfPrimaryKeyMultiAttribute(attribute)) {
			attribute = "primaryKey." + attribute;
		}

		return PropertyFactoryUtil.forName(attribute);
	}

	@Override
	public Projection getPropertyProjection(String attribute) {
		String op = null;

		if (attribute.indexOf("(") > 0) {
			op = attribute.substring(0, attribute.indexOf("("));
			attribute = attribute.substring(
				attribute.indexOf("(") + 1, attribute.indexOf(")"));
		}

		return getPropertyProjection(attribute, op);
	}

	@Override
	public ProjectionList getPropertyProjection(String[] attributes) {
		List<String> validAttributes = new ArrayList<>();

		ProjectionList projectionList = getPropertyProjection(
			attributes, validAttributes, null);

		if (attributes.length != validAttributes.size()) {
			throw new IllegalArgumentException(Arrays.toString(attributes));
		}

		return projectionList;
	}

	@Override
	public ProjectionList getPropertyProjection(
		String[] attributes, List<String> validAttributes,
		List<String> notValidAttributes) {

		if ((attributes == null) || (attributes.length == 0)) {
			return null;
		}

		String[] op = new String[attributes.length];
		String[] attributesAux = new String[attributes.length];

		boolean grouping = false;

		for (int i = 0; i < attributes.length; i++) {
			String attribute = attributes[i];

			if (attribute.indexOf("(") > 0) {
				op[i] = attribute.substring(0, attribute.indexOf("("));
				attributesAux[i] = attribute.substring(
					attribute.indexOf("(") + 1, attribute.indexOf(")"));
				grouping = true;
			}
			else {
				op[i] = null;

				if ("pk".equals(attribute)) {
					attribute = this.getPrimaryKeyAttribute();
				}

				attributesAux[i] = attribute;
			}
		}

		if (grouping) {
			for (int i = 0; i < op.length; i++) {
				if (op[i] == null) {
					op[i] = "groupProperty";
				}
			}
		}

		ProjectionList projectionList = ProjectionFactoryUtil.projectionList();

		Projection firstProjection = null;

		int numProjections = 0;

		for (int i = 0; i < attributesAux.length; i++) {
			Projection projection = getPropertyProjection(
				attributesAux[i], op[i]);

			if ((projection == null) && (notValidAttributes != null)) {
				notValidAttributes.add(attributes[i]);
			}

			if (projection == null) {
				continue;
			}

			projectionList.add(projection);

			numProjections++;

			if (firstProjection == null) {
				firstProjection = projection;
			}

			if (validAttributes != null) {
				validAttributes.add(attributes[i]);
			}
		}

		if (numProjections == 0) {
			return null;
		}

		if (numProjections == 1) {
			projectionList.add(firstProjection);
		}

		return projectionList;
	}

	@Override
	public Service getService() {
		return service;
	}

	@Override
	public TableInfo getTableInfo() {
		if (tableInfo == null) {
			tableInfo = service.getTableInfo();
		}

		return tableInfo;
	}

	@Override
	public TableInfo getTableInfo(String attribute) {
		if (hasAttribute(attribute)) {
			return getTableInfo();
		}

		String prefix = null;
		String attrWithoutPrefix = attribute;

		int pos = attribute.indexOf(".");

		if (pos != -1) {
			prefix = attribute.substring(0, pos);
			attrWithoutPrefix = attribute.substring(pos + 1);
		}

		TableInfo tableInfo = getTableInfoMappings().get(attrWithoutPrefix);

		if (tableInfo == null) {
			return null;
		}

		if ((prefix != null) && !prefix.equals(tableInfo.getName())) {
			return null;
		}

		return tableInfo;
	}

	@Override
	public Map<String, TableInfo> getTableInfoMappings() {
		if (tableInfoMappings == null) {
			Map<String, TableInfo> mappings = new ConcurrentHashMap<>();

			List<String> mappingTables = service.getMappingTables();

			for (String mappingTable : mappingTables) {
				TableInfo tableInfo = service.getTableInfo(mappingTable);

				String destinationAttr = tableInfo.getDestinationAttr(
					getPrimaryKeyAttribute());

				mappings.put(destinationAttr, tableInfo);
			}

			this.tableInfoMappings = Collections.unmodifiableMap(mappings);
		}

		return tableInfoMappings;
	}

	@Override
	public boolean hasAttribute(String attribute) {
		attribute = cleanAttributeName(attribute);

		if (getAttributePos(attribute) != -1) {
			return true;
		}

		return false;
	}

	@Override
	public boolean hasAttributes(String[] attributes) {
		for (String attribute : attributes) {
			if (!hasAttribute(attribute)) {
				return false;
			}
		}

		return true;
	}

	@Override
	public boolean isAuditedModel() {
		if (hasAttribute("companyId") && hasAttribute("createDate") &&
			hasAttribute("modifiedDate") && hasAttribute("userId") &&
			hasAttribute("userName")) {

			return true;
		}

		return false;
	}

	@Override
	public boolean isGroupedModel() {
		if (isAuditedModel() && hasAttribute("groupId") &&
			!getPrimaryKeyAttribute().equals("groupId")) {

			return true;
		}

		return false;
	}

	@Override
	public boolean isPartOfPrimaryKeyMultiAttribute(String attribute) {
		for (String primaryKeyAttribute : this.getPrimaryKeyMultiAttribute()) {
			if (primaryKeyAttribute.equals(attribute)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public boolean isResourcedModel() {
		if (hasAttribute("resourcePrimKey") &&
			!getPrimaryKeyAttribute().equals("resourcePrimKey") &&
			!isPartOfPrimaryKeyMultiAttribute("resourcePrimKey")) {

			return true;
		}

		return false;
	}

	@Override
	public boolean isStagedModel() {
		if (hasAttribute("uuid") && hasAttribute("companyId") &&
			hasAttribute("createDate") && hasAttribute("modifiedDate")) {

			return true;
		}

		return false;
	}

	@Override
	public boolean isWorkflowEnabled() {
		if (hasAttribute("status") && hasAttribute("statusByUserId") &&
			hasAttribute("statusByUserName") && hasAttribute("statusDate")) {

			return true;
		}

		return false;
	}

	@Override
	public boolean modelEqualsClass(Class<?> clazz) {
		return this.getClassName(
		).equals(
			clazz.getName()
		);
	}

	@Override
	public String toString() {
		return getName();
	}

	protected String cleanAttributeName(String attribute) {
		if (Validator.isNull(attribute)) {
			return StringPool.BLANK;
		}

		int pos = attribute.indexOf(".");

		if (pos == -1) {
			return attribute;
		}

		String prefix = attribute.substring(0, pos);

		if (prefix.equals(this.getClassName()) ||
			prefix.equals(this.getClassSimpleName())) {

			attribute = attribute.substring(pos + 1);
		}

		return ModelUtil.getCachedAttributeName(attribute);
	}

	protected Projection getPropertyProjection(String attribute, String op) {
		if ("rowCount".equals(op)) {
			return ProjectionFactoryUtil.rowCount();
		}

		attribute = cleanAttributeName(attribute);

		if (!this.hasAttribute(attribute)) {
			return null;
		}

		if (isPartOfPrimaryKeyMultiAttribute(attribute)) {
			attribute = "primaryKey." + attribute;
		}

		Projection property = null;

		if (Validator.isNull(op)) {
			property = ProjectionFactoryUtil.property(attribute);
		}
		else if ("count".equals(op)) {
			property = ProjectionFactoryUtil.count(attribute);
		}
		else if ("countDistinct".equals(op)) {
			property = ProjectionFactoryUtil.countDistinct(attribute);
		}
		else if ("groupProperty".equals(op)) {
			property = ProjectionFactoryUtil.groupProperty(attribute);
		}
		else if ("max".equals(op)) {
			property = ProjectionFactoryUtil.max(attribute);
		}
		else if ("min".equals(op)) {
			property = ProjectionFactoryUtil.min(attribute);
		}
		else if ("sum".equals(op)) {
			property = ProjectionFactoryUtil.sum(attribute);
		}

		return property;
	}

	protected static Log _log = LogFactoryUtil.getLog(ModelImpl.class);

	protected String className = null;
	protected String classSimpleName = null;
	protected ModelFactory modelFactory = null;
	protected Service service = null;
	protected TableInfo tableInfo = null;
	protected Map<String, TableInfo> tableInfoMappings = null;

}