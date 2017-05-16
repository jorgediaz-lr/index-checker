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
import com.liferay.portal.kernel.patcher.PatcherUtil;
import com.liferay.portal.kernel.search.Document;
import com.liferay.portal.kernel.search.Field;
import com.liferay.portal.kernel.search.SearchException;
import com.liferay.portal.kernel.security.auth.CompanyThreadLocal;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.ReleaseInfo;
import com.liferay.portal.kernel.workflow.WorkflowConstants;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jorgediazest.indexchecker.util.IndexCheckerUtil;

import jorgediazest.util.data.Data;
import jorgediazest.util.data.DataUtil;
import jorgediazest.util.model.Model;
import jorgediazest.util.modelquery.ModelQueryFactory.DataComparatorFactory;
import jorgediazest.util.service.Service;

/**
 * @author Jorge Díaz
 */
public class JournalArticleQuery extends IndexCheckerModelQuery {

	public void addMissingJournalArticles(
			String[] attributes, Criterion filter, Criterion filterStatus,
			Map<Long, Data> dataMap)
		throws Exception {

		Service service = getModel().getService();
		DynamicQuery query = service.newDynamicQuery();

		List<String> validAttributes = new ArrayList<String>();

		ProjectionList projectionList = getModel().getPropertyProjection(
			attributes, validAttributes);

		query.setProjection(ProjectionFactoryUtil.distinct(projectionList));

		query.add(filter);

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

		query.add(
			getModel().getProperty("version").eq(articleVersionDynamicQuery));

		query.add(filterStatus);

		@SuppressWarnings("unchecked")
		List<Object[]> results = (List<Object[]>)service.executeDynamicQuery(
			query);

		String[] validAttributesArr = validAttributes.toArray(
			new String[validAttributes.size()]);

		for (Object[] result : results) {
			Data data = createDataObject(validAttributesArr, result);

			if (!dataMap.containsKey(data.getResourcePrimKey())) {
				dataMap.put(data.getResourcePrimKey(), data);
			}
		}
	}

	@Override
	public void fillDataObject(Data data, String[] attributes, Document doc) {
		super.fillDataObject(data, attributes, doc);

		if (indexAllVersions) {
			long id = DataUtil.getIdFromUID(doc.get(Field.UID));
			data.setPrimaryKey(id);
		}
	}

	@Override
	public Map<Long, Data> getData(
			String[] attributes, String mapKeyAttribute, Criterion filter)
		throws Exception {

		if (indexAllVersions) {
			return super.getData(attributes, mapKeyAttribute, filter);
		}

		Map<Long, Data> dataMap = new HashMap<Long, Data>();

		Criterion filterStatusApproved = getModel().generateCriterionFilter(
			"status=" + WorkflowConstants.STATUS_APPROVED + "+status=" +
				WorkflowConstants.STATUS_IN_TRASH);

		addMissingJournalArticles(
			attributes, filter, filterStatusApproved, dataMap);

		Criterion filterStatusNotApproved = getModel().generateCriterionFilter(
			"status<>" + WorkflowConstants.STATUS_APPROVED + ",status<>" +
				WorkflowConstants.STATUS_IN_TRASH);

		addMissingJournalArticles(
			attributes, filter, filterStatusNotApproved, dataMap);

		Map<Long, Data> dataMap2 = new HashMap<Long, Data>();

		for (Data data : dataMap.values()) {
			dataMap2.put((Long)data.get(mapKeyAttribute), data);
		}

		return dataMap2;
	}

	@Override
	public void init(Model model, DataComparatorFactory dataComparatorFactory)
		throws Exception {

		super.init(model, dataComparatorFactory);

		if (JournalArticleQuery.isOldConfiguration()) {
			String configurationValue =
				IndexCheckerUtil.getPortletPropertiesKey(
					model.getService().getClassLoader(),
					"com.liferay.journal.configuration.JournalServiceConfigurationValues",
					"JOURNAL_ARTICLE_INDEX_ALL_VERSIONS");
			indexAllVersions = GetterUtil.getBoolean(configurationValue);
		}
		else {
			indexAllVersions =
				(boolean) IndexCheckerUtil.getCompanyConfigurationKey(
					CompanyThreadLocal.getCompanyId(),
					model.getService().getClassLoader(),
					"com.liferay.journal.configuration.JournalServiceConfiguration",
					"indexAllArticleVersionsEnabled");
		}
	}

	public static boolean isOldConfiguration() {

		for (String installedPatch : PatcherUtil.getInstalledPatches()) {
			if (installedPatch.startsWith("de-")) {
				String[] fixpackNumber = installedPatch.split("\\-");
				try {
					long fixpackNum = Long.parseLong(fixpackNumber[1]);

					if (fixpackNum>=13) {
						return false;
					}

					return true;
				}
				catch (Exception e) {
				}
			}
		}

		try {
			String releaseVersion = ReleaseInfo.getVersion();
	
			long minorVersion = Long.parseLong(releaseVersion.split("\\.")[2]);

			if ((minorVersion>=3) && (minorVersion != 10)) {
				return false;
			}

			return true;
		}
		catch (Exception e) {
		}

		return true;
	}

	@Override
	public Map<Data, String> reindex(Collection<Data> dataCollection) {

		Map<Long, Data> articles = new HashMap<Long, Data>();

		for (Data data : dataCollection) {
			articles.put(data.getResourcePrimKey(), data);
		}

		return super.reindex(articles.values());
	}

	@Override
	public void reindex(Data value) throws SearchException {
		getModel().getIndexerNullSafe().reindex(
			getModel().getClassName(), value.getResourcePrimKey());
	}

	protected boolean indexAllVersions;

}