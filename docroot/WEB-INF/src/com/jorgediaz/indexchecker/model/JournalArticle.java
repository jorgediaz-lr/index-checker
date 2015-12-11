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

package com.jorgediaz.indexchecker.model;

import com.jorgediaz.indexchecker.data.Data;
import com.jorgediaz.util.model.ModelFactory;

import com.liferay.portal.kernel.dao.orm.Criterion;
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.ProjectionFactoryUtil;
import com.liferay.portal.kernel.dao.orm.ProjectionList;
import com.liferay.portal.kernel.dao.orm.RestrictionsFactoryUtil;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.search.SearchException;
import com.liferay.portal.kernel.util.PrefsPropsUtil;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portal.model.ClassedModel;

import java.util.List;
import java.util.Map;

/**
 * @author Jorge Díaz
 */
public class JournalArticle extends IndexCheckerModel {

	public void addMissingJournalArticles(
			Criterion filter, Map<Long, Data> dataMap)
		throws Exception {

		DynamicQuery query = this.newDynamicQuery();

		ProjectionList projectionList =
			this.getPropertyProjection(
				getIndexAttributes().toArray(new String[0]));

		query.setProjection(ProjectionFactoryUtil.distinct(projectionList));

		query.add(filter);

		DynamicQuery articleVersionDynamicQuery = this.newDynamicQuery(
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
		List<Object[]> results = (List<Object[]>)this.executeDynamicQuery(
			query);

		indexAllVersions = indexAllVersionsOld;

		for (Object[] result : results) {
			long pk = (Long)result[0];

			if (!dataMap.containsKey(pk)) {
				Data data = new Data(this);
				data.init(result);
				dataMap.put(data.getPrimaryKey(), data);
			}
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

	@Override
	public void init(
			ModelFactory modelUtil, Class<? extends ClassedModel> clazz)
		throws Exception {

		super.init(modelUtil, clazz);

		try {
			indexAllVersions =
				PrefsPropsUtil.getBoolean(
					"journal.articles.index.all.versions");
		}
		catch (SystemException e) {
			throw new RuntimeException(e);
		}

		if (!indexAllVersions) {
			this.removeIndexedAttribute("id");
			this.setIndexPrimaryKey("resourcePrimKey");
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