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

package jorgediazest.indexchecker.portlet;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.security.auth.CompanyThreadLocal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import jorgediazest.indexchecker.ExecutionMode;
import jorgediazest.indexchecker.data.DataIndexCheckerModelComparator;
import jorgediazest.indexchecker.index.IndexSearchHelper;
import jorgediazest.indexchecker.model.IndexCheckerPermissionsHelper;
import jorgediazest.indexchecker.model.IndexCheckerQueryHelper;
import jorgediazest.indexchecker.util.ConfigurationUtil;

import jorgediazest.util.comparator.DataComparator;
import jorgediazest.util.data.Comparison;
import jorgediazest.util.data.ComparisonUtil;
import jorgediazest.util.data.Data;
import jorgediazest.util.data.DataUtil;
import jorgediazest.util.model.Model;

/**
 * @author Jorge Díaz
 */
public class CallableCheckGroupAndModel implements Callable<Comparison> {

	public CallableCheckGroupAndModel(
		Map<String, Map<Long, List<Data>>> queryCache, long companyId,
		List<Long> groupIds, Date startModifiedDate, Date endModifiedDate,
		Model model, Set<ExecutionMode> executionMode) {

		this.companyId = companyId;
		this.groupIds = groupIds;
		this.startModifiedDate = startModifiedDate;
		this.endModifiedDate = endModifiedDate;
		this.queryCache = queryCache;
		this.model = model;
		this.executionMode = executionMode;
	}

	@Override
	public Comparison call() throws Exception {
		boolean checkRelatedData = executionMode.contains(
			ExecutionMode.CHECK_RELATED_DATA);
		boolean showBothExact = executionMode.contains(
			ExecutionMode.SHOW_BOTH_EXACT);
		boolean showBothNotExact = executionMode.contains(
			ExecutionMode.SHOW_BOTH_NOTEXACT);
		boolean showOnlyLiferay = executionMode.contains(
			ExecutionMode.SHOW_LIFERAY);
		boolean showOnlyIndex = executionMode.contains(
			ExecutionMode.SHOW_INDEX);

		boolean oldIgnoreCase = DataUtil.getIgnoreCase();

		try {
			DataUtil.setIgnoreCase(true);

			CompanyThreadLocal.setCompanyId(companyId);

			if (_log.isInfoEnabled()) {
				String strGroupIds = null;

				if (groupIds != null) {
					strGroupIds = Arrays.toString(groupIds.toArray());

					if (strGroupIds.length() > 100) {
						strGroupIds = strGroupIds.substring(0, 97) + "...";
					}
				}

				_log.info(
					"Model: " + model.getName() + " - CompanyId: " +
						companyId + " - GroupId: " + strGroupIds);
			}

			if (groupIds != null) {
				if (groupIds.isEmpty()) {
					return null;
				}

				if (!model.hasAttribute("groupId") && !groupIds.contains(0L)) {
					return null;
				}
			}

			IndexCheckerQueryHelper queryHelper =
				ConfigurationUtil.getQueryHelper(model);

			IndexCheckerPermissionsHelper permissionsHelper =
				ConfigurationUtil.getPermissionsHelper(model);

			Map<Long, Data> liferayDataMap = queryHelper.getLiferayData(
				model, groupIds);

			for (Data data : liferayDataMap.values()) {
				queryHelper.postProcessData(data);

				if (checkRelatedData) {
					permissionsHelper.addPermissionsClassNameGroupIdFields(
						data);
				}
			}

			queryHelper.addRelatedModelData(
				queryCache, liferayDataMap, model, groupIds, checkRelatedData);

			if (checkRelatedData) {
				for (Data data : liferayDataMap.values()) {
					permissionsHelper.addRolesFields(data);
				}
			}

			Collection<String> exactAttributes =
				ConfigurationUtil.getExactAttributesToCheck(model);

			Set<Data> liferayData = new HashSet<Data>(liferayDataMap.values());

			Set<Data> indexData;

			IndexSearchHelper indexSearchHelper =
				ConfigurationUtil.getIndexSearchHelper(model);

			if ((!showOnlyIndex && liferayData.isEmpty())||
				(indexSearchHelper == null)) {

				indexData = new HashSet<Data>();
			}
			else {
				Set<Model> relatedModels = queryHelper.calculateRelatedModels(
					model, checkRelatedData);

				Set<String> indexAttributesToQuery = new HashSet<String>(
					ConfigurationUtil.getModelAttributesToQuery(model));

				indexAttributesToQuery.addAll(exactAttributes);

				indexData = indexSearchHelper.getIndexData(
					model, relatedModels, indexAttributesToQuery, companyId,
					groupIds, startModifiedDate, endModifiedDate);

				for (Data data : indexData) {
					indexSearchHelper.postProcessData(data);
				}
			}

			List<String> exactAttributesList = new ArrayList<String>(
				model.getKeyAttributes());

			exactAttributesList.addAll(exactAttributes);

			DataComparator exactDataComparator =
				new DataIndexCheckerModelComparator(exactAttributesList);

			exactDataComparator.setIgnoreNulls(true);

			return ComparisonUtil.getComparison(
				model, exactDataComparator, liferayData, indexData,
				showBothExact, showBothNotExact, showOnlyLiferay,
				showOnlyIndex);
		}
		catch (Throwable t) {
			return ComparisonUtil.getError(model, t);
		}
		finally {
			DataUtil.setIgnoreCase(oldIgnoreCase);
		}
	}

	private static Log _log = LogFactoryUtil.getLog(
		CallableCheckGroupAndModel.class);

	private long companyId = -1;
	private Date endModifiedDate = null;
	private Set<ExecutionMode> executionMode = null;
	private List<Long> groupIds = null;
	private Model model = null;
	private Map<String, Map<Long, List<Data>>> queryCache = null;
	private Date startModifiedDate = null;

}