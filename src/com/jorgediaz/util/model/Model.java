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
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.Projection;
import com.liferay.portal.kernel.dao.orm.ProjectionList;
import com.liferay.portal.kernel.dao.orm.Property;
import com.liferay.portal.kernel.search.Indexer;
import com.liferay.portal.model.ClassedModel;
import com.liferay.portal.service.BaseLocalService;

import java.util.List;
import java.util.Locale;

/**
 * @author Jorge Díaz
 */
public interface Model extends Cloneable {

	public ClassedModel addObject(ClassedModel object);

	public Model clone();

	public ClassedModel createObject(long primaryKey);

	public ClassedModel deleteObject(ClassedModel object);

	public ClassedModel deleteObject(long primaryKey);

	public List<?> executeDynamicQuery(DynamicQuery dynamicQuery)
		throws Exception;

	public ClassedModel fetchObject(long primaryKey);

	public Criterion generateCriterionFilter(String stringFilter);

	public Criterion generateDisjunctionCriterion(String[] filters);

	public Criterion generateSingleCriterion(String filter);

	public Criterion generateSingleCriterion(
		String attrName, String attrValue, String op);

	public int getAttributePos(String name);

	public Object[][] getAttributes();

	public String[] getAttributesName();

	public int[] getAttributesType();

	public int getAttributeType(String name);

	public String getClassName();

	public long getClassNameId();

	public String getDisplayName(Locale locale);

	public Criterion getFilter();

	public Model getFilteredModel(String filters);

	public Model getFilteredModel(String filters, String nameSufix);

	public Indexer getIndexer();

	public void setModelFactory(ModelFactory modelFactory);

	public ModelFactory getModelFactory();

	public String getName();

	public String getPrimaryKeyAttribute();

	public String[] getPrimaryKeyMultiAttribute();

	public Property getProperty(String attribute);

	public Projection getPropertyProjection(String attribute);

	public ProjectionList getPropertyProjection(String[] attributes);

	public boolean hasAttribute(String attribute);

	public boolean hasAttributes(String[] attributes);

	public boolean hasIndexer();

	public void init(
			ReflectionUtil reflectionUtil, String classPackageName,
			String classSimpleName, BaseLocalService service)
		throws Exception;

	public boolean isAuditedModel();

	public boolean isGroupedModel();

	public boolean isPartOfPrimaryKeyMultiAttribute(String attribute);

	public boolean isResourcedModel();

	public boolean isStagedModel();

	public boolean isWorkflowEnabled();

	public boolean modelEqualsClass(Class<?> clazz);

	public DynamicQuery newDynamicQuery();

	public DynamicQuery newDynamicQuery(
		Class<? extends ClassedModel> clazz, String alias);

	public void setFilter(Criterion filter);

	public void setNameSuffix(String suffix);

	public ClassedModel updateObject(ClassedModel object);

}