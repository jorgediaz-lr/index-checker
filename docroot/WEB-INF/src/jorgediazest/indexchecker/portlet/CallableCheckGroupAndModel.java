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
import com.liferay.portal.kernel.security.auth.CompanyThreadLocal;
import com.liferay.asset.kernel.AssetRendererFactoryRegistryUtil;
import com.liferay.asset.kernel.model.AssetCategory;
import com.liferay.asset.kernel.model.AssetEntry;
import com.liferay.asset.kernel.model.AssetRendererFactory;
import com.liferay.asset.kernel.model.AssetTag;
import com.liferay.document.library.kernel.model.DLFileEntry;
import com.liferay.document.library.kernel.model.DLFileVersion;
import com.liferay.message.boards.kernel.model.MBMessage;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import jorgediazest.indexchecker.ExecutionMode;
import jorgediazest.indexchecker.model.IndexCheckerModelQuery;

import jorgediazest.util.data.Comparison;
import jorgediazest.util.data.ComparisonUtil;
import jorgediazest.util.data.Data;
import jorgediazest.util.data.DataUtil;
import jorgediazest.util.model.Model;
import jorgediazest.util.modelquery.ModelQuery;

/**
 * @author Jorge Díaz
 */
public class CallableCheckGroupAndModel implements Callable<Comparison> {

	public static Set<String> calculateAttributesToCheck(ModelQuery mq) {
		Model model = mq.getModel();

		Set<String> attributesToCheck = new LinkedHashSet<String>();

		attributesToCheck.add(model.getPrimaryKeyAttribute());
		attributesToCheck.add("companyId");
		attributesToCheck.add("groupId");
		attributesToCheck.add("classPK");
		attributesToCheck.add("classNameId");

		if (model.isResourcedModel()) {
			attributesToCheck.add("resourcePrimKey");
		}

		attributesToCheck.addAll(
			Arrays.asList(mq.getDataComparator().getExactAttributes()));

		if (DLFileEntry.class.getName().equals(model.getClassName())) {
			attributesToCheck.add("repositoryId");
			attributesToCheck.add("version");
		}

		if (MBMessage.class.getName().equals(model.getClassName())) {
			attributesToCheck.add("categoryId");
		}

		if ("com.liferay.dynamic.data.lists.model.DDLRecord".equals(
			model.getClassName())) {

			attributesToCheck.add("recordSetId");
		}

		if ("com.liferay.calendar.model.CalendarBooking".equals(
			model.getClassName())) {

			attributesToCheck.add("calendarId");
		}

		return attributesToCheck;
	}

	public CallableCheckGroupAndModel(
		long companyId, List<Long> groupIds, IndexCheckerModelQuery query,
		Set<ExecutionMode> executionMode) {

		this.companyId = companyId;
		this.groupIds = groupIds;
		this.mq = query;
		this.executionMode = executionMode;
	}

	public Set<String> calculateRelatedAttributesToCheck(Model model) {
		Set<String> relatedAttributesToCheck = new LinkedHashSet<String>();

		if (model.getName().equals("com.liferay.dynamic.data.lists.model.DDLRecord")) {
			relatedAttributesToCheck.add(
				"com.liferay.dynamic.data.lists.model.DDLRecordVersion:recordId,version-" +
				": =recordId,version,status");
		}
		else if (model.getName().equals(DLFileEntry.class.getName())) {
			relatedAttributesToCheck.add(
				DLFileVersion.class.getName() + ":fileEntryId,version-" +
				": =fileEntryId, =version,status");
		}

		AssetRendererFactory<?> assetRendererFactory =
			AssetRendererFactoryRegistryUtil.getAssetRendererFactoryByClassName(
				model.getName());

		if ((assetRendererFactory == null) ||
			!assetRendererFactory.isSelectable()) {

			return relatedAttributesToCheck;
		}

		//AssetEntry attributes

		if (model.isResourcedModel()) {
			relatedAttributesToCheck.add(
					AssetEntry.class.getName() + ":resourcePrimKey=classPK" +
				":AssetEntry.entryId=entryId, =classPK,priority,visible");
		}
		else {
			relatedAttributesToCheck.add(
					AssetEntry.class.getName() + ":pk=classPK" +
				":AssetEntry.entryId=entryId, =classPK,priority,visible");
		}

		relatedAttributesToCheck.add(
			AssetEntry.class.getName() + ":" +
			"AssetEntry.entryId=MappingTable:" +
			"AssetEntries_AssetCategories.categoryId=categoryId");
		relatedAttributesToCheck.add(
			AssetCategory.class.getName() + ":" +
			"AssetEntries_AssetCategories.categoryId=categoryId:" +
			" =categoryId,AssetCategory.title=title");
		relatedAttributesToCheck.add(
			AssetEntry.class.getName() + ":" +
			"AssetEntry.entryId=MappingTable:" +
			"AssetEntries_AssetTags.tagId=tagId");
		relatedAttributesToCheck.add(
			AssetTag.class.getName() + ":" +
			"AssetEntries_AssetTags.tagId=tagId: =tagId,AssetTag.name=name");

		return relatedAttributesToCheck;
	}

	public Set<Model> calculateRelatedModels(Model model) {

		Set<Model> relatedModels = new LinkedHashSet<Model>();

		if (model.getName().equals("com.liferay.dynamic.data.lists.model.DDLRecord")) {
			relatedModels.add(model.getModelFactory().getModelObject(
				"com.liferay.dynamic.data.lists.model.DDLRecordVersion"));
		}
		else if (model.getName().equals(DLFileEntry.class.getName())) {
			relatedModels.add(model.getModelFactory().getModelObject(
				DLFileVersion.class.getName()));
		}

		AssetRendererFactory<?> assetRendererFactory =
			AssetRendererFactoryRegistryUtil.getAssetRendererFactoryByClassName(
				model.getName());

		if ((assetRendererFactory == null) ||
			!assetRendererFactory.isSelectable()) {

			return relatedModels;
		}

		//AssetEntry and related models

		relatedModels.add(model.getModelFactory().getModelObject(
			AssetEntry.class.getName()));

		relatedModels.add(model.getModelFactory().getModelObject(
			AssetCategory.class.getName()));

		relatedModels.add(model.getModelFactory().getModelObject(
			AssetTag.class.getName()));

		return relatedModels;
	}

	@Override
	public Comparison call() throws Exception {

		Model model = mq.getModel();

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

			if ((groupIds != null) && (groupIds.size() == 0)) {
				return null;
			}

			if ((groupIds != null) && (!groupIds.contains(0L) &&
				 !model.hasAttribute("groupId"))) {

				return null;
			}

			Criterion filter = mq.getCompanyGroupFilter(companyId, groupIds);

			String[] attributesToCheck = calculateAttributesToCheck(
				mq).toArray(new String[0]);

			String[] relatedAttrToCheck = calculateRelatedAttributesToCheck(
				model).toArray(new String[0]);

			Set<Model> relatedModels = calculateRelatedModels(model);

			Set<Data> liferayData = new HashSet<Data>(
				mq.getData(
					attributesToCheck, relatedAttrToCheck, filter).values());

			Set<Data> indexData;

			if (executionMode.contains(ExecutionMode.SHOW_INDEX) ||
				!liferayData.isEmpty()) {

				SearchContext searchContext = mq.getIndexSearchContext(
					companyId);
				BooleanQuery contextQuery = mq.getIndexQuery(
					groupIds, searchContext);

				indexData = mq.getIndexData(
					relatedModels, attributesToCheck, searchContext,
					contextQuery);
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
		finally {
			DataUtil.setIgnoreCase(oldIgnoreCase);
		}
	}

	private static Log _log = LogFactoryUtil.getLog(
		CallableCheckGroupAndModel.class);

	private long companyId = -1;
	private Set<ExecutionMode> executionMode = null;
	private List<Long> groupIds = null;
	private IndexCheckerModelQuery mq = null;

}