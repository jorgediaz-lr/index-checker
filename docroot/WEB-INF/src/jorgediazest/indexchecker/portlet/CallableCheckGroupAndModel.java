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
import com.liferay.portlet.asset.AssetRendererFactoryRegistryUtil;
import com.liferay.portlet.asset.model.AssetRendererFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import jorgediazest.indexchecker.ExecutionMode;
import jorgediazest.indexchecker.model.IndexCheckerModelQuery;
import jorgediazest.indexchecker.util.ConfigurationUtil;

import jorgediazest.util.data.Comparison;
import jorgediazest.util.data.ComparisonUtil;
import jorgediazest.util.data.Data;
import jorgediazest.util.data.DataUtil;
import jorgediazest.util.model.Model;
import jorgediazest.util.model.ModelFactory;
import jorgediazest.util.modelquery.ModelQuery;

/**
 * @author Jorge Díaz
 */
public class CallableCheckGroupAndModel implements Callable<Comparison> {

	public static Set<String> calculateAttributesToQuery(ModelQuery mq) {
		Model model = mq.getModel();

		Set<String> attributesToCheck = new LinkedHashSet<String>(
			ConfigurationUtil.getModelAttributesToQuery(model));

		attributesToCheck.addAll(
			Arrays.asList(mq.getDataComparator().getExactAttributes()));

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

	public Collection<String> calculateRelatedAttributesToCheck(Model model) {

		Collection<String> relatedAttributesToCheck =
			ConfigurationUtil.getRelatedAttributesToCheck(model);

		if (checkAssetEntryRelations(model)) {
			return relatedAttributesToCheck;
		}

		Collection<String> relatedAttributesToCheckFiltered =
			new LinkedHashSet<String>();

		for (String relatedAttributeToCheck : relatedAttributesToCheck) {
			if (!relatedAttributeToCheck.startsWith(
					"com.liferay.portlet.asset.model.Asset")) {

				relatedAttributesToCheckFiltered.add(relatedAttributeToCheck);
			}
		}

		return relatedAttributesToCheckFiltered;
	}

	public Set<Model> calculateRelatedModels(
		ModelFactory modelFactory, String[] relatedAttributesToCheck) {

		Set<String> relatedClassNames = new LinkedHashSet<String>();

		for (String relatedAttributeToCheck : relatedAttributesToCheck) {
			relatedClassNames.add(relatedAttributeToCheck.split(":")[0]);
		}

		Set<Model> relatedModels = new LinkedHashSet<Model>();

		for (String relatedClassName : relatedClassNames) {
			Model relatedModel = modelFactory.getModelObject(relatedClassName);

			relatedModels.add(relatedModel);
		}

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

			Criterion filter = model.getCompanyGroupFilter(companyId, groupIds);

			String[] attributesToQuery = calculateAttributesToQuery(
				mq).toArray(new String[0]);

			Map<Long, Data> liferayDataMap;

			liferayDataMap = mq.getData(attributesToQuery, filter);

			for (Data data : liferayDataMap.values()) {
				mq.addPermissionsClassNameGroupIdFields(data);
			}

			String[] relatedAttrToCheck = calculateRelatedAttributesToCheck(
				model).toArray(new String[0]);

			mq.addRelatedModelData(liferayDataMap, relatedAttrToCheck, filter);

			for (Data data : liferayDataMap.values()) {
				mq.addRolesFields(data);
			}

			Set<Data> liferayData = new HashSet<Data>(liferayDataMap.values());

			Set<Data> indexData;

			if (executionMode.contains(ExecutionMode.SHOW_INDEX) ||
				!liferayData.isEmpty()) {

				SearchContext searchContext = mq.getIndexSearchContext(
					companyId);
				BooleanQuery contextQuery = mq.getIndexQuery(
					groupIds, searchContext);

				Set<Model> relatedModels = calculateRelatedModels(
					model.getModelFactory(), relatedAttrToCheck);

				indexData = mq.getIndexData(
					relatedModels, attributesToQuery, searchContext,
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

	protected boolean checkAssetEntryRelations(Model model) {
		boolean assetEntryRelations = true;

		AssetRendererFactory assetRendererFactory =
			AssetRendererFactoryRegistryUtil.getAssetRendererFactoryByClassName(
				model.getClassName());

		if ((assetRendererFactory == null) ||
			!assetRendererFactory.isSelectable()) {

			assetEntryRelations = false;
		}

		return assetEntryRelations;
	}

	private static Log _log = LogFactoryUtil.getLog(
		CallableCheckGroupAndModel.class);

	private long companyId = -1;
	private Set<ExecutionMode> executionMode = null;
	private List<Long> groupIds = null;
	private IndexCheckerModelQuery mq = null;

}