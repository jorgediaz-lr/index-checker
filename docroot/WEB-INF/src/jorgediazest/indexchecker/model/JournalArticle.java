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

package jorgediazest.indexchecker.model;

import com.liferay.portal.kernel.dao.orm.Criterion;
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.ProjectionFactoryUtil;
import com.liferay.portal.kernel.dao.orm.ProjectionList;
import com.liferay.portal.kernel.dao.orm.RestrictionsFactoryUtil;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.search.Field;
import com.liferay.portal.kernel.search.SearchException;
import com.liferay.portal.kernel.util.PrefsPropsUtil;
import com.liferay.portal.kernel.workflow.WorkflowConstants;

import java.util.List;
import java.util.Map;

import jorgediazest.indexchecker.data.Data;
import jorgediazest.indexchecker.data.DataUtil;
import jorgediazest.indexchecker.index.DocumentWrapper;

import jorgediazest.util.model.ModelUtil;
import jorgediazest.util.service.Service;

/**
 * @author Jorge Díaz
 */
public class JournalArticle extends IndexCheckerModel {

	public void addMissingJournalArticles(
			Criterion filter, Map<Long, Data> dataMap)
		throws Exception {

		DynamicQuery query = service.newDynamicQuery();

		ProjectionList projectionList =
			this.getPropertyProjection(
				getLiferayIndexedAttributes().toArray(new String[0]));

		query.setProjection(ProjectionFactoryUtil.distinct(projectionList));

		query.add(filter);

		DynamicQuery articleVersionDynamicQuery = ModelUtil.newDynamicQuery(
			com.liferay.portlet.journal.model.JournalArticle.class,
			"articleVersion");

		articleVersionDynamicQuery.setProjection(
			ProjectionFactoryUtil.alias(
				ProjectionFactoryUtil.max(
					"articleVersion.version"), "articleVersion.version"));

		// We need to use the "this" default alias to make sure the database
		// engine handles this subquery as a correlated subquery

		articleVersionDynamicQuery.add(
			RestrictionsFactoryUtil.eqProperty(
				"this.resourcePrimKey", "articleVersion.resourcePrimKey"));

		query.add(getProperty("version").eq(articleVersionDynamicQuery));

		boolean indexAllVersionsOld = indexAllVersions;
		indexAllVersions = true;

		@SuppressWarnings("unchecked")
		List<Object[]> results = (List<Object[]>)service.executeDynamicQuery(
			query);

		indexAllVersions = indexAllVersionsOld;

		for (Object[] result : results) {
			long pk = (Long)result[0];

			if (!dataMap.containsKey(pk)) {
				Data data = createDataObject(result);
				dataMap.put(data.getPrimaryKey(), data);
			}
		}
	}

	@Override
	public int compareTo(Data data1, Data data2) {
		if ((data1.getPrimaryKey() != -1) && (data2.getPrimaryKey() != -1) &&
			indexAllVersions) {

			return DataUtil.compareLongs(
				data1.getPrimaryKey(), data2.getPrimaryKey());
		}
		else if ((data1.getResourcePrimKey() != -1) &&
				 (data2.getResourcePrimKey() != -1)) {

			return DataUtil.compareLongs(
				data1.getResourcePrimKey(), data2.getResourcePrimKey());
		}
		else {
			return 0;
		}
	}

	@Override
	public Data createDataObject(DocumentWrapper doc) {
		Data data = super.createDataObject(doc);

		if (indexAllVersions) {
			long id = DataUtil.getIdFromUID(doc.get(Field.UID));
			data.setPrimaryKey(id);
		}

		return data;
	}

	@Override
	public boolean equals(Data data1, Data data2) {
		if ((data1.getPrimaryKey() != -1) && (data2.getPrimaryKey() != -1) &&
			indexAllVersions) {

			return (data1.getPrimaryKey() == data2.getPrimaryKey());
		}
		else if ((data1.getResourcePrimKey() != -1) &&
				 (data2.getResourcePrimKey() != -1)) {

			return (data1.getResourcePrimKey() == data2.getResourcePrimKey());
		}
		else {
			return false;
		}
	}

	@Override
	public Criterion generateQueryFilter() {

		String filter = "classNameId=0,indexable=true";

		if (!indexAllVersions) {
			filter =
				filter + ",status=" + WorkflowConstants.STATUS_APPROVED +
				"+status=" + WorkflowConstants.STATUS_IN_TRASH;
		}

		return this.generateCriterionFilter(filter);
	}

	public Map<Long, Data> getLiferayData(Criterion filter) throws Exception {
		Map<Long, Data> dataMap = super.getLiferayData(filter);

		if (!indexAllVersions) {
			addMissingJournalArticles(filter, dataMap);
		}

		return dataMap;
	}

	public Integer hashCode(Data data) {
		if ((data.getPrimaryKey() != -1) && indexAllVersions) {
			return data.getEntryClassName().hashCode() *
				Long.valueOf(data.getPrimaryKey()).hashCode();
		}
		else if (data.getResourcePrimKey() != -1) {
			return -1 * data.getEntryClassName().hashCode() *
				Long.valueOf(data.getResourcePrimKey()).hashCode();
		}

		return null;
	}

	@Override
	public void init(
			String classPackageName, String classSimpleName, Service service)
		throws Exception {

		super.init(classPackageName, classSimpleName, service);

		try {
			indexAllVersions =
				PrefsPropsUtil.getBoolean(
					"journal.articles.index.all.versions");
		}
		catch (SystemException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void reindex(Data value) throws SearchException {
		if (indexAllVersions) {
			super.reindex(value);
		}
		else {
			getIndexer().reindex(
				this.getClassName(), value.getResourcePrimKey());
		}
	}

	protected boolean indexAllVersions;

}