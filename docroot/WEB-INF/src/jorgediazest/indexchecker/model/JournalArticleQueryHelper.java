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
import com.liferay.portal.kernel.workflow.WorkflowConstants;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jorgediazest.indexchecker.util.ConfigurationUtil;

import jorgediazest.util.data.Data;
import jorgediazest.util.data.DataUtil;
import jorgediazest.util.model.Model;
import jorgediazest.util.model.ModelUtil;
import jorgediazest.util.service.Service;

/**
 * @author Jorge Díaz
 */
public class JournalArticleQueryHelper extends IndexCheckerQueryHelper {

	public JournalArticleQueryHelper() throws Exception {
		indexAllVersions =
			ConfigurationUtil.getJournalArticleIndexAllVersions();
	}

	public void addMissingJournalArticles(
			Model model, String[] attributes, Criterion filter,
			Criterion filterStatus, Map<Long, Data> dataMap)
		throws Exception {

		Service service = model.getService();
		DynamicQuery query = service.newDynamicQuery();

		List<String> validAttributes = new ArrayList<String>();

		ProjectionList projectionList = model.getPropertyProjection(
			attributes, validAttributes, null);

		query.setProjection(ProjectionFactoryUtil.distinct(projectionList));

		if (filter != null) {
			query.add(filter);
		}

		DynamicQuery articleVersionDynamicQuery = service.newDynamicQuery(
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

		articleVersionDynamicQuery.add(filterStatus);

		query.add(model.getProperty("version").eq(articleVersionDynamicQuery));

		query.add(filterStatus);

		@SuppressWarnings("unchecked")
		List<Object[]> results = (List<Object[]>)service.executeDynamicQuery(
			query);

		String[] validAttributesArr = validAttributes.toArray(
			new String[validAttributes.size()]);

		for (Object[] result : results) {
			Data data = DataUtil.createDataObject(
				model, validAttributesArr, result);

			if (!dataMap.containsKey(data.getResourcePrimKey())) {
				dataMap.put(data.getResourcePrimKey(), data);
			}
		}
	}

	@Override
	public Map<Long, Data> getLiferayData(Model model, List<Long> groupIds)
		throws Exception {

		if (indexAllVersions) {
			return super.getLiferayData(model, groupIds);
		}

		Criterion criterion = model.getAttributeCriterion("groupId", groupIds);

		Collection<String> attributesToQuery =
			ConfigurationUtil.getModelAttributesToQuery(model);

		String[] attributesToQueryArr = attributesToQuery.toArray(
			new String[0]);

		Map<Long, Data> dataMap = new HashMap<Long, Data>();

		Criterion criterionStatusApproved = ModelUtil.generateSQLCriterion(
			"status=" + WorkflowConstants.STATUS_APPROVED + " or status=" +
				WorkflowConstants.STATUS_IN_TRASH);

		addMissingJournalArticles(
			model, attributesToQueryArr, criterion, criterionStatusApproved,
			dataMap);

		Criterion criterionStatusNotApproved = ModelUtil.generateSQLCriterion(
			"status<>" + WorkflowConstants.STATUS_APPROVED + " or status<>" +
				WorkflowConstants.STATUS_IN_TRASH);

		addMissingJournalArticles(
			model, attributesToQueryArr, criterion, criterionStatusNotApproved,
			dataMap);

		return dataMap;
	}

	protected boolean indexAllVersions;

}