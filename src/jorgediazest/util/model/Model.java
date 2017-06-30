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
import com.liferay.portal.kernel.dao.orm.ProjectionList;
import com.liferay.portal.kernel.dao.orm.Property;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import jorgediazest.util.service.Service;
import jorgediazest.util.table.TableInfo;

/**
 * @author Jorge Díaz
 */
public interface Model extends Comparable<Model> {

	public long count();

	public long count(Criterion condition);

	public List<?> executeDynamicQuery(Criterion filter) throws Exception;

	public List<?> executeDynamicQuery(Criterion filter, List<Order> orders)
		throws Exception;

	public List<?> executeDynamicQuery(Criterion filter, Order order)
		throws Exception;

	public List<?> executeDynamicQuery(Criterion filter, Projection projection)
		throws Exception;

	public List<?> executeDynamicQuery(
		Criterion filter, Projection projection, List<Order> order)
	throws Exception;

	public Criterion generateCriterionFilter(String stringFilter);

	public Criterion generateInCriteria(String property, List<Long> list);

	public Criterion generateSingleCriterion(String filter);

	public Criterion generateSingleCriterion(
		String attrName, String attrValue, String op);

	public int getAttributePos(String name);

	public Object[][] getAttributes();

	public String[] getAttributesName();

	public int[] getAttributesType();

	public int getAttributeType(String name);

	public Class<?> getAttributeTypeClass(String name);

	public String getClassName();

	public long getClassNameId();

	public String getClassSimpleName();

	public Criterion getCompanyFilter(long companyId);

	public Criterion getCompanyGroupFilter(long companyId, List<Long> groupIds);

	public Criterion getCompanyGroupFilter(long companyId, long groupId);

	public String getDisplayName(Locale locale);

	public Model getFilteredModel(Criterion filters);

	public Model getFilteredModel(Criterion filters, String nameSufix);

	public Model getFilteredModel(String filters);

	public Model getFilteredModel(String filters, String nameSufix);

	public ModelFactory getModelFactory();

	public String getName();

	public String getPrimaryKeyAttribute();

	public String[] getPrimaryKeyMultiAttribute();

	public Property getProperty(String attribute);

	public Projection getPropertyProjection(String attribute);

	public ProjectionList getPropertyProjection(String[] attributes);

	public ProjectionList getPropertyProjection(
		String[] attributes, List<String> validAttributes,
		List<String> notValidAttributes);

	public Service getService();

	public TableInfo getTableInfo();

	public TableInfo getTableInfo(String attribute);

	public Map<String, TableInfo> getTableInfoMappings();

	public boolean hasAttribute(String attribute);

	public boolean hasAttributes(String[] attributes);

	public boolean isAuditedModel();

	public boolean isGroupedModel();

	public boolean isPartOfPrimaryKeyMultiAttribute(String attribute);

	public boolean isResourcedModel();

	public boolean isStagedModel();

	public boolean isWorkflowEnabled();

	public boolean modelEqualsClass(Class<?> clazz);

}