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

	public long count(Criterion criterion);

	public List<?> executeDynamicQuery(Criterion criterion) throws Exception;

	public List<?> executeDynamicQuery(Criterion criterion, List<Order> orders)
		throws Exception;

	public List<?> executeDynamicQuery(Criterion criterion, Order order)
		throws Exception;

	public List<?> executeDynamicQuery(
			Criterion criterion, Projection projection)
		throws Exception;

	public List<?> executeDynamicQuery(
			Criterion criterion, Projection projection, List<Order> order)
		throws Exception;

	public Class<?> getAttributeClass(String name);

	public <T> Criterion getAttributeCriterion(String attribute, List<T> list);

	public <T> Criterion getAttributeCriterion(String attribute, T value);

	public String[] getAttributeNames();

	public int getAttributePos(String name);

	public String getClassName();

	public long getClassNameId();

	public String getClassSimpleName();

	public String getDisplayName(Locale locale);

	public Model getFilteredModel(Criterion criterion);

	public Model getFilteredModel(Criterion criterion, String nameSuffix);

	public List<String> getKeyAttributes();

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