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
import com.liferay.portal.kernel.dao.shard.ShardUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.search.BooleanQuery;
import com.liferay.portal.kernel.search.SearchContext;
import com.liferay.portal.security.auth.CompanyThreadLocal;
import com.liferay.portlet.asset.model.AssetCategory;
import com.liferay.portlet.asset.model.AssetEntry;
import com.liferay.portlet.asset.model.AssetTag;
import com.liferay.portlet.documentlibrary.model.DLFileEntry;
import com.liferay.portlet.documentlibrary.model.DLFileVersion;
import com.liferay.portlet.dynamicdatalists.model.DDLRecord;
import com.liferay.portlet.dynamicdatalists.model.DDLRecordVersion;
import com.liferay.portlet.messageboards.model.MBMessage;
import com.liferay.portlet.ratings.model.RatingsStats;

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

		if (model.isResourcedModel()) {
			attributesToCheck.add("resourcePrimKey");
		}

		attributesToCheck.addAll(
			Arrays.asList(mq.getDataComparator().getExactAttributes()));

		if (DLFileEntry.class.getName().equals(model.getClassName())) {
			attributesToCheck.add("version");
		}

		if (MBMessage.class.getName().equals(model.getClassName())) {
			attributesToCheck.add("categoryId");
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

		if (model.getName().equals(DDLRecord.class.getName())) {
			relatedAttributesToCheck.add(
				DDLRecordVersion.class.getName() + ":recordId,version-" +
				": =recordId,version,status");
		}
		else if (model.getName().equals(DLFileEntry.class.getName())) {
			relatedAttributesToCheck.add(
				DLFileVersion.class.getName() + ":fileEntryId,version-" +
				": =fileEntryId, =version,status");
		}

		String attributeClassPK = "pk";

		if (model.isResourcedModel()) {
			attributeClassPK = "resourcePrimKey";
		}

		String mapping = attributeClassPK+"=classPK";

		relatedAttributesToCheck.add(
			AssetEntry.class.getName() + ":" + mapping +
			":AssetEntry.entryId=entryId, =classPK,priority,viewCount,visible");
		relatedAttributesToCheck.add(
			RatingsStats.class.getName() + ":" + mapping +
			":statsId, =classPK,averageScore: ");
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

		if (model.getName().equals(DDLRecord.class.getName())) {
			relatedModels.add(model.getModelFactory().getModelObject(
				DDLRecordVersion.class.getName()));
		}
		else if (model.getName().equals(DLFileEntry.class.getName())) {
			relatedModels.add(model.getModelFactory().getModelObject(
				DLFileVersion.class.getName()));
		}

		relatedModels.add(model.getModelFactory().getModelObject(
			AssetEntry.class.getName()));

		relatedModels.add(model.getModelFactory().getModelObject(
			RatingsStats.class.getName()));

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

			ShardUtil.pushCompanyService(companyId);

			if (_log.isInfoEnabled()) {
				String strGroupIds = null;

				if (groupIds != null) {
					strGroupIds = Arrays.toString(groupIds.toArray());
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

			ShardUtil.popCompanyService();
		}
	}

	private static Log _log = LogFactoryUtil.getLog(
		CallableCheckGroupAndModel.class);

	private long companyId = -1;
	private Set<ExecutionMode> executionMode = null;
	private List<Long> groupIds = null;
	private IndexCheckerModelQuery mq = null;

}