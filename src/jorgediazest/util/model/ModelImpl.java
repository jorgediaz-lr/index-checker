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
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.security.permission.ResourceActionsUtil;
import com.liferay.portal.util.PortalUtil;

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

	public int compareTo(Model o) {
		return this.getClassName().compareTo(o.getClassName());
	}

	public long count() {
		return count(null);
	}

	public long count(Criterion condition) {
		try {
			List<?> list = executeDynamicQuery(
				condition, ProjectionFactoryUtil.rowCount());

			if (list != null) {
				return (Long)list.get(0);
			}
		}
		catch (Exception e) {
			_log.error("Error executing count");
			throw new RuntimeException(e);
		}

		return -1;
	}

	public List<?> executeDynamicQuery(Criterion filter) throws Exception {

		return executeDynamicQuery(filter, null, null);
	}

	@Override
	public List<?> executeDynamicQuery(Criterion filter, List<Order> orders)
		throws Exception {

		return executeDynamicQuery(filter, null, orders);
	}

	@Override
	public List<?> executeDynamicQuery(Criterion filter, Order order)
		throws Exception {

		List<Order> orders = Collections.singletonList(order);

		return executeDynamicQuery(filter, null, orders);
	}

	@Override
	public List<?> executeDynamicQuery(Criterion filter, Projection projection)
		throws Exception {

		return executeDynamicQuery(filter, projection, null);
	}

	@Override
	public List<?> executeDynamicQuery(
			Criterion filter, Projection projection, List<Order> orders)
		throws Exception {

		return ModelUtil.executeDynamicQuery(
			getService(), filter, projection, orders);
	}

	public Criterion generateInCriterion(String property, List<Long> list) {
		int size = MAX_NUMBER_OF_CLAUSES;

		if (list.size() <= size) {
			return getProperty(property).in(list);
		}

		Disjunction disjunction = RestrictionsFactoryUtil.disjunction();

		for (int i = 0; i<((list.size() + size - 1) / size); i++) {
			int start = i * size;
			int end = Math.min(start + size, list.size());
			List<Long> listAux = list.subList(start, end);
			disjunction.add(this.getProperty(property).in(listAux));
		}

		return disjunction;
	}

	public int getAttributePos(String name) {
		return this.getTableInfo().getAttributePos(name);
	}

	public Object[][] getAttributes() {
		return getTableInfo().getAttributes();
	}

	public String[] getAttributesName() {
		return getTableInfo().getAttributesName();
	}

	public int[] getAttributesType() {
		return getTableInfo().getAttributesType();
	}

	public int getAttributeType(String name) {
		return getTableInfo().getAttributeType(name);
	}

	public Class<?> getAttributeTypeClass(String name) {
		return getTableInfo().getAttributeTypeClass(name);
	}

	public String getClassName() {
		return className;
	}

	public long getClassNameId() {
		return PortalUtil.getClassNameId(getClassName());
	}

	public String getClassSimpleName() {
		return classSimpleName;
	}

	public Criterion getCompanyCriterion(long companyId) {
		if (!this.hasAttribute("companyId")) {
			return null;
		}

		return getProperty("companyId").eq(companyId);
	}

	public String getDisplayName(Locale locale) {
		String displayName = ResourceActionsUtil.getModelResource(
			locale, getClassName());

		if (displayName.startsWith(
				ResourceActionsUtil.getModelResourceNamePrefix())) {

			return StringPool.BLANK;
		}

		return displayName;
	}

	public Model getFilteredModel(Criterion filters) {
		return getFilteredModel(filters, null);
	}

	public Model getFilteredModel(Criterion filters, String nameSufix) {
		if (filters == null) {
			return this;
		}

		ModelWrapper modelWrapper = new ModelWrapper(this);
		modelWrapper.setFilter(filters);

		if (Validator.isNotNull(nameSufix)) {
			modelWrapper.setNameSuffix(nameSufix);
		}

		return modelWrapper;
	}

	public Model getFilteredModel(String sqlFilter) {
		return getFilteredModel(sqlFilter, sqlFilter);
	}

	public Model getFilteredModel(String sqlFilter, String nameSufix) {

		Criterion filter = ModelUtil.generateSQLCriterion(sqlFilter);

		if (filter == null) {
			return null;
		}

		return getFilteredModel(filter, nameSufix);
	}

	public Criterion getGroupCriterion(List<Long> groupIds) {

		if (!this.hasAttribute("groupId") || Validator.isNull(groupIds)) {
			return null;
		}

		Property groupIdProperty = getProperty("groupId");

		Disjunction disjunction = RestrictionsFactoryUtil.disjunction();

		for (Long groupId : groupIds) {
			disjunction.add(groupIdProperty.eq(groupId));
		}

		return disjunction;
	}

	public Criterion getGroupCriterion(long groupId) {
		return getGroupCriterion(Collections.singletonList(groupId));
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

		List<String> primaryKeyAttributes = new ArrayList<String>();

		primaryKeyAttributes.add(primaryKeyAttribute);

		primaryKeyAttributes.addAll(Arrays.asList(primaryKeyMultiAttribute));

		return primaryKeyAttributes;
	}

	public ModelFactory getModelFactory() {
		return modelFactory;
	}

	public String getName() {
		return getClassName();
	}

	public String getPrimaryKeyAttribute() {
		return getTableInfo().getPrimaryKeyAttribute();
	}

	public String[] getPrimaryKeyMultiAttribute() {
		return getTableInfo().getPrimaryKeyMultiAttribute();
	}

	public Property getProperty(String attribute) {
		attribute = cleanAttributeName(attribute);

		if (isPartOfPrimaryKeyMultiAttribute(attribute)) {
			attribute = "primaryKey." + attribute;
		}

		return PropertyFactoryUtil.forName(attribute);
	}

	public Projection getPropertyProjection(String attribute) {

		String op = null;

		if (attribute.indexOf("(") > 0) {
			op = attribute.substring(0, attribute.indexOf("("));
			attribute = attribute.substring(
				attribute.indexOf("(") + 1, attribute.indexOf(")"));
		}

		return getPropertyProjection(attribute, op);
	}

	public ProjectionList getPropertyProjection(String[] attributes) {

		List<String> validAttributes = new ArrayList<String>();
		ProjectionList projectionList = getPropertyProjection(
			attributes, validAttributes, null);

		if (attributes.length != validAttributes.size()) {
			throw new IllegalArgumentException(Arrays.toString(attributes));
		}

		return projectionList;
	}

	public ProjectionList getPropertyProjection(
		String[] attributes, List<String> validAttributes,
		List<String> notValidAttributes) {

		String[] op = new String[attributes.length];
		String[] attributesAux = new String[attributes.length];

		boolean grouping = false;

		for (int i = 0; i<attributes.length; i++) {
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
			for (int i = 0; i<op.length; i++) {
				if (op[i] == null) {
					op[i] = "groupProperty";
				}
			}
		}

		ProjectionList projectionList = ProjectionFactoryUtil.projectionList();

		Projection firstProjection = null;

		int numProjections = 0;

		for (int i = 0; i<attributesAux.length; i++) {
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

		if (numProjections == 1) {
			projectionList.add(firstProjection);
		}

		return projectionList;
	}

	public Service getService() {
		return service;
	}

	public TableInfo getTableInfo() {
		if (tableInfo == null) {
			tableInfo = service.getTableInfo();
		}

		return tableInfo;
	}

	public TableInfo getTableInfo(String attribute) {
		if (hasAttribute(attribute)) {
			return getTableInfo();
		}

		String prefix = null;
		String attrWithoutPrefix = attribute;

		int pos = attribute.indexOf(".");

		if (pos != -1) {
			prefix = attribute.substring(0, pos);
			attrWithoutPrefix = attribute.substring(pos+1);
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

	public Map<String, TableInfo> getTableInfoMappings() {
		if (tableInfoMappings == null) {
			tableInfoMappings = new ConcurrentHashMap<String, TableInfo>();

			List<String> mappingTables = service.getMappingTables();

			for (String mappingTable : mappingTables) {
				TableInfo tableInfo = service.getTableInfo(mappingTable);

				String destinationAttr = tableInfo.getDestinationAttr(
					getPrimaryKeyAttribute());
				tableInfoMappings.put(destinationAttr, tableInfo);
			}
		}

		return tableInfoMappings;
	}

	public boolean hasAttribute(String attribute) {

		attribute = cleanAttributeName(attribute);

		return (getAttributePos(attribute) != -1);
	}

	public boolean hasAttributes(String[] attributes) {
		for (String attribute : attributes) {
			if (!hasAttribute(attribute)) {
				return false;
			}
		}

		return true;
	}

	public boolean isAuditedModel() {
		if (hasAttribute("companyId") && hasAttribute("createDate") &&
			hasAttribute("modifiedDate") && hasAttribute("userId") &&
			hasAttribute("userName")) {

			return true;
		}

		return false;
	}

	public boolean isGroupedModel() {
		if (isAuditedModel() && hasAttribute("groupId") &&
			!getPrimaryKeyAttribute().equals("groupId")) {

			return true;
		}
		else {
			return false;
		}
	}

	public boolean isPartOfPrimaryKeyMultiAttribute(String attribute) {

		for (String primaryKeyAttribute : this.getPrimaryKeyMultiAttribute()) {
			if (primaryKeyAttribute.equals(attribute)) {
				return true;
			}
		}

		return false;
	}

	public boolean isResourcedModel() {
		if (hasAttribute("resourcePrimKey") &&
			!getPrimaryKeyAttribute().equals("resourcePrimKey") &&
			!isPartOfPrimaryKeyMultiAttribute("resourcePrimKey")) {

			return true;
		}

		return false;
	}

	public boolean isStagedModel() {
		if (hasAttribute("uuid") && hasAttribute("companyId") &&
			hasAttribute("createDate") &&
			hasAttribute("modifiedDate")) {

			return true;
		}

		return false;
	}

	public boolean isWorkflowEnabled() {
		if (hasAttribute("status") && hasAttribute("statusByUserId") &&
			hasAttribute("statusByUserName") && hasAttribute("statusDate")) {

			return true;
		}

		return false;
	}

	public boolean modelEqualsClass(Class<?> clazz) {
		return this.getClassName().equals(clazz.getName());
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