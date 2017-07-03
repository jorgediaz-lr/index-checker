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
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portlet.asset.AssetRendererFactoryRegistryUtil;
import com.liferay.portlet.asset.model.AssetRendererFactory;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jorgediazest.indexchecker.util.ConfigurationUtil;

import jorgediazest.util.data.Data;
import jorgediazest.util.data.DataComparator;
import jorgediazest.util.data.DataUtil;
import jorgediazest.util.model.Model;
import jorgediazest.util.model.ModelFactory;
import jorgediazest.util.modelquery.ModelQuery;
import jorgediazest.util.modelquery.ModelQueryFactory;
public class IndexCheckerQueryHelper {

	public void addRelatedModelData(
			Map<Long, Data> liferayDataMap, ModelQueryFactory mqFactory,
			Model model, long companyId, List<Long> groupIds)
		throws Exception {

		Criterion filter = model.getCompanyGroupFilter(companyId, groupIds);

		String[] relatedAttrToCheck = calculateRelatedAttributesToCheck(
			model).toArray(new String[0]);

		ModelQuery mq = mqFactory.getModelQueryObject(model);

		mq.addRelatedModelData(liferayDataMap, relatedAttrToCheck, filter);
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

	public Set<Model> calculateRelatedModels(Model model) {

		Collection<String> relatedAttributesToCheck =
			calculateRelatedAttributesToCheck(model);

		Set<String> relatedClassNames = new LinkedHashSet<String>();

		for (String relatedAttributeToCheck : relatedAttributesToCheck) {
			relatedClassNames.add(relatedAttributeToCheck.split(":")[0]);
		}

		ModelFactory modelFactory = model.getModelFactory();

		Set<Model> relatedModels = new LinkedHashSet<Model>();

		for (String relatedClassName : relatedClassNames) {
			Model relatedModel = modelFactory.getModelObject(relatedClassName);

			relatedModels.add(relatedModel);
		}

		return relatedModels;
	}

	public Map<Long, Data> getLiferayData(
			Model model, long companyId, List<Long> groupIds)
		throws Exception {

		Criterion filter = model.getCompanyGroupFilter(companyId, groupIds);

		Collection<String> attributesToQuery =
			ConfigurationUtil.getModelAttributesToQuery(model);

		String[] attributesToQueryArr = attributesToQuery.toArray(
			new String[0]);

		DataComparator dataComparator = ConfigurationUtil.getDataComparator(
			model);

		return DataUtil.getData(
			model, dataComparator, attributesToQueryArr, filter);
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
		IndexCheckerQueryHelper.class);

}