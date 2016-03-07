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
import com.liferay.portal.kernel.dao.orm.Projection;
import com.liferay.portal.kernel.dao.orm.ProjectionList;
import com.liferay.portal.kernel.dao.orm.Property;
import com.liferay.portal.kernel.lar.StagedModelDataHandler;
import com.liferay.portal.kernel.search.Indexer;
import com.liferay.portal.kernel.trash.TrashHandler;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import jorgediazest.util.data.Data;
import jorgediazest.util.service.Service;

/**
 * @author Jorge Díaz
 */
public interface Model extends Cloneable, Comparable<Model> {

	public void addFilter(Criterion filter);

	public Model clone();

	public int compareTo(Data data, Data data2);

	public long count();

	public long count(Criterion condition);

	public boolean equals(Data data1, Data data2);

	public boolean exact(Data data1, Data data2);

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

	public String getClassName();

	public long getClassNameId();

	public Criterion getCompanyFilter(long companyId);

	public Criterion getCompanyGroupFilter(long companyId, long groupId);

	public Map<Long, Data> getData() throws Exception;

	public Map<Long, Data> getData(Criterion filter) throws Exception;

	public Map<Long, Data> getData(String[] attributes) throws Exception;

	public Map<Long, Data> getData(String[] attributes, Criterion filter)
		throws Exception;

	public String getDisplayName(Locale locale);

	public Criterion getFilter();

	public Model getFilteredModel(String filters);

	public Model getFilteredModel(String filters, String nameSufix);

	public Indexer getIndexer();

	public ModelFactory getModelFactory();

	public String getName();

	public String getPrimaryKeyAttribute();

	public String[] getPrimaryKeyMultiAttribute();

	public Property getProperty(String attribute);

	public Projection getPropertyProjection(String attribute);

	public ProjectionList getPropertyProjection(String[] attributes);

	public Service getService();

	public StagedModelDataHandler<?> getStagedModelDataHandler();

	public TrashHandler getTrashHandler();

	public boolean hasAttribute(String attribute);

	public boolean hasAttributes(String[] attributes);

	public Integer hashCode(Data data);

	public boolean hasIndexer();

	public void init(
			String classPackageName, String classSimpleName, Service service)
		throws Exception;

	public boolean isAuditedModel();

	public boolean isGroupedModel();

	public boolean isPartOfPrimaryKeyMultiAttribute(String attribute);

	public boolean isResourcedModel();

	public boolean isStagedModel();

	public boolean isTrashEnabled();

	public boolean isWorkflowEnabled();

	public boolean modelEqualsClass(Class<?> clazz);

	public void setFilter(Criterion filter);

	public void setModelFactory(ModelFactory modelFactory);

	public void setNameSuffix(String suffix);

}