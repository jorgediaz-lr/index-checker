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

import com.liferay.portal.kernel.dao.orm.Criterion;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.search.BooleanQuery;
import com.liferay.portal.kernel.search.SearchContext;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Callable;

import jorgediazest.indexchecker.ExecutionMode;
import jorgediazest.indexchecker.model.IndexCheckerModel;

import jorgediazest.util.data.Comparison;
import jorgediazest.util.data.ComparisonUtil;
import jorgediazest.util.data.Data;
import jorgediazest.util.model.Model;

/**
 * @author Jorge Díaz
 */
public class CallableCheckGroupAndModel implements Callable<Comparison> {

	public static Set<String> calculateAttributesToCheck(Model model) {
		Set<String> attributesToCheck = new LinkedHashSet<String>();

		attributesToCheck.add(model.getPrimaryKeyAttribute());
		attributesToCheck.add("companyId");
		attributesToCheck.add("groupId");

		if (model.isResourcedModel()) {
			attributesToCheck.add("resourcePrimKey");
		}

		attributesToCheck.addAll(
			Arrays.asList(model.getDataComparator().getExactAttributes()));

		return attributesToCheck;
	}

	public CallableCheckGroupAndModel(
		long companyId, long groupId, IndexCheckerModel model,
		Set<ExecutionMode> executionMode) {

		this.companyId = companyId;
		this.groupId = groupId;
		this.model = model;
		this.executionMode = executionMode;
	}

	public Set<String> calculateRelatedAttributesToCheck(Model model) {
		Set<String> relatedAttributesToCheck = new LinkedHashSet<String>();

		String attributeClassPK = "pk";

		if (model.isResourcedModel()) {
			attributeClassPK = "resourcePrimKey";
		}

		String mapping = attributeClassPK+"=classPK";

		relatedAttributesToCheck.add(
			"com.liferay.portlet.asset.model.AssetEntry:" + mapping +
			":entryId, =classPK,priority,viewCount,visible");
		relatedAttributesToCheck.add(
			"com.liferay.portlet.ratings.model.RatingsStats:" + mapping +
			":statsId, =classPK,ratings=averageScore: ");
		relatedAttributesToCheck.add(
			"com.liferay.portlet.asset.model.AssetEntry:" +
			"entryId=MappingTable:assetCategoryIds=categoryId");
		relatedAttributesToCheck.add(
			"com.liferay.portlet.asset.model.AssetEntry:" +
			"entryId=MappingTable:assetTagIds=tagId");

		return relatedAttributesToCheck;
	}

	@Override
	public Comparison call() throws Exception {

		try {
			if (_log.isInfoEnabled()) {
				_log.info(
					"Model: " + model.getName() + " - CompanyId: " +
						companyId + " - GroupId: " + groupId);
			}

			if ((groupId == 0L) &&
				model.hasAttribute("groupId") &&
				executionMode.contains(ExecutionMode.GROUP_BY_SITE)) {

				return null;
			}

			if ((groupId != 0L) &&
				!model.hasAttribute("groupId") &&
				executionMode.contains(ExecutionMode.GROUP_BY_SITE)) {

				return null;
			}

			Criterion filter = model.getCompanyGroupFilter(companyId, groupId);

			String[] attributesToCheck = calculateAttributesToCheck(
				model).toArray(new String[0]);

			String[] relatedAttrToCheck = calculateRelatedAttributesToCheck(
				model).toArray(new String[0]);

			Set<Data> liferayData = new HashSet<Data>(
				model.getData(
					attributesToCheck, relatedAttrToCheck, filter).values());

			Set<Data> indexData;

			if (executionMode.contains(ExecutionMode.SHOW_INDEX) ||
				!liferayData.isEmpty()) {

				SearchContext searchContext = model.getIndexSearchContext(
					companyId);
				BooleanQuery contextQuery = model.getIndexQuery(
					groupId, searchContext);

				indexData = model.getIndexData(
					attributesToCheck, searchContext, contextQuery);
			}
			else {
				indexData = new HashSet<Data>();
			}

			boolean showBothExact = executionMode.contains(
				ExecutionMode.SHOW_BOTH_EXACT);
			boolean showBothNotExact = executionMode.contains(
				ExecutionMode.SHOW_BOTH_NOTEXACT);
			boolean showOnlyLiferay = executionMode.contains(
				ExecutionMode.SHOW_LIFERAY);
			boolean showOnlyIndex = executionMode.contains(
				ExecutionMode.SHOW_INDEX);

			return ComparisonUtil.getComparison(
				model, liferayData, indexData, showBothExact, showBothNotExact,
				showOnlyLiferay, showOnlyIndex);
		}
		catch (Throwable t) {
			return ComparisonUtil.getError(model, t);
		}
	}

	private static Log _log = LogFactoryUtil.getLog(
		CallableCheckGroupAndModel.class);

	private long companyId = -1;
	private Set<ExecutionMode> executionMode = null;
	private long groupId = -1;
	private IndexCheckerModel model = null;

}