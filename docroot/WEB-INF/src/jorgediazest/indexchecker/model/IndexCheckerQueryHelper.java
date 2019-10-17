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
import com.liferay.portal.kernel.util.CharPool;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jorgediazest.indexchecker.util.ConfigurationUtil;

import jorgediazest.util.data.Data;
import jorgediazest.util.model.Model;
import jorgediazest.util.model.ModelFactory;
import jorgediazest.util.model.ModelUtil;
import jorgediazest.util.query.Query;
import jorgediazest.util.query.QueryUtil;
public class IndexCheckerQueryHelper {

	public static Object processTreePath(Object treePath) {
		if (treePath == null) {
			return null;
		}

		if (treePath instanceof Set) {
			Set<?> treePathSet = (Set<?>)treePath;

			Set<Object> treePathFiltered = new HashSet<Object>();

			for (Object obj : treePathSet) {
				if (Validator.isNotNull(obj)) {
					treePathFiltered.add(obj);
				}
			}

			return treePathFiltered;
		}

		if (!(treePath instanceof String)) {
			return treePath;
		}

		String treePathStr = (String)treePath;

		if ((treePathStr.length() > 0) &&
			(treePathStr.charAt(0) == CharPool.SLASH)) {

			treePathStr = treePathStr.substring(1);
		}

		if (Validator.isNull(treePathStr)) {
			return StringPool.BLANK;
		}

		return StringUtil.split(treePathStr, CharPool.SLASH);
	}

	@SuppressWarnings("unchecked")
	public void addRelatedModelData(
			Map<String, Map<Long, List<Data>>> queryCache,
			Map<Long, Data> liferayDataMap, Model model, List<Long> groupIds,
			boolean addOptionalRelatedData)
		throws Exception {

		if (liferayDataMap.isEmpty()) {
			return;
		}

		Criterion groupCriterion = model.getAttributeCriterion(
			"groupId", groupIds);

		ModelFactory modelFactory = model.getModelFactory();

		List<Map<String, Object>> relatedDataToQueryList =
			ConfigurationUtil.getRelatedDataToQuery(model);

		for (Map<String, Object> rdtq : relatedDataToQueryList) {
			boolean addRelatedData = GetterUtil.getBoolean(
				(Boolean)rdtq.get("forceAdd"), addOptionalRelatedData);

			if (!addRelatedData) {
				continue;
			}

			String relatedClassName = (String) rdtq.get("model");
			List<String> mappingsSource = (List<String>)rdtq.get(
				"mappingsSource");
			List<String> mappingsRelated = (List<String>)rdtq.get(
				"mappingsRelated");
			List<String> attributesToQuery = (List<String>)rdtq.get(
				"attributesToQuery");
			List<String> attributesAlias = (List<String>)rdtq.get(
				"attributesAlias");
			boolean rawData = GetterUtil.getBoolean(
				(Boolean)rdtq.get("raw"),false);
			String relatedFilterString = (String)rdtq.get("filter");

			Model relatedModel = modelFactory.getModelObject(relatedClassName);

			if (relatedModel == null) {
				continue;
			}

			addRelatedModelData(
				queryCache, liferayDataMap, model, relatedModel, mappingsSource,
				mappingsRelated, attributesToQuery, attributesAlias, rawData,
				groupCriterion, relatedFilterString);
		}
	}

	public void addRelatedModelData(
			Map<String, Map<Long, List<Data>>> queryCache,
			Map<Long, Data> liferayDataMap, Model model, Model relatedModel,
			List<String> mappingsSource, List<String> mappingsRelated,
			List<String> attributesToQuery, List<String> attributesAlias,
			boolean rawData, Criterion groupCriterion,
			String relatedFilterString)
		throws Exception {

		Criterion relatedCriterion = null;

		if (relatedFilterString != null) {
			relatedCriterion = ModelUtil.generateSQLCriterion(
				relatedFilterString);
		}
		else if (relatedModel.isGroupedModel()) {
			relatedCriterion = groupCriterion;
		}

		if ("classPK".equals(mappingsRelated.get(0))) {
			Criterion classNameIdCriterion = relatedModel.getProperty(
				"classNameId").eq(model.getClassNameId());

			relatedCriterion = ModelUtil.generateConjunctionCriterion(
				classNameIdCriterion, relatedCriterion);
		}

		List<String> relatedAttributes = new ArrayList<String>();
		relatedAttributes.addAll(attributesToQuery);
		relatedAttributes.addAll(mappingsRelated);

		Map<Long, List<Data>> relatedMap;

		if (relatedCriterion == null) {
			relatedMap = getDataWithDuplicatesWithCache(
				queryCache, relatedModel, relatedAttributes, mappingsRelated,
				relatedCriterion);
		}
		else {
			relatedMap = Query.getDataWithDuplicates(
				relatedModel, relatedAttributes.toArray(new String[0]),
				mappingsRelated.get(0), relatedCriterion);
		}

		Map<Long, List<Data>> matchedMap =
			QueryUtil.getMatchingEntriesMap(
				liferayDataMap, relatedMap,
				mappingsSource.toArray(new String[0]),
				mappingsRelated.toArray(new String[0]));

		if ((attributesAlias == null) || attributesAlias.isEmpty()) {
			attributesAlias = attributesToQuery;
		}
		else {
			Set<Data> matchedList = new HashSet<Data>();
			for (List<Data> sublist : matchedMap.values()) {
				matchedList.addAll(sublist);
			}

			for (Data data : matchedList) {
				_copyAttributesToAlias(
					data, attributesToQuery, attributesAlias);
			}
		}

		if (rawData) {
			QueryUtil.addRelatedModelDataRaw(
				liferayDataMap, matchedMap,
				attributesAlias.toArray(new String[0]));
		}
		else {
			QueryUtil.addRelatedModelData(
				liferayDataMap, matchedMap,
				attributesAlias.toArray(new String[0]));
		}
	}

	public Set<Model> calculateRelatedModels(Model model) {

		List<Map<String, Object>> relatedDataToQueryList =
			ConfigurationUtil.getRelatedDataToQuery(model);

		ModelFactory modelFactory = model.getModelFactory();

		Set<Model> relatedModels = new LinkedHashSet<Model>();

		for (Map<String, Object> relatedDataToQuery : relatedDataToQueryList) {
			String relatedClassName = (String) relatedDataToQuery.get("model");

			Model relatedModel = modelFactory.getModelObject(relatedClassName);

			if (relatedModel != null) {
				relatedModels.add(relatedModel);
			}
		}

		return relatedModels;
	}

	public Map<Long, List<Data>> getDataWithDuplicatesWithCache(
			Map<String, Map<Long, List<Data>>> queryCache, Model relatedModel,
			List<String> relatedAttributes, List<String> mappingsRelated,
			Criterion relatedCriterion)
		throws Exception {

		Map<Long, List<Data>> relatedMap;

		String attributes = Arrays.toString(relatedAttributes.toArray());

		String cacheKey =
			relatedModel.getName() + "_" + attributes + "_key_" +
				mappingsRelated.get(0);

		relatedMap = queryCache.get(cacheKey);

		if (relatedMap == null) {
			synchronized(relatedModel) {
				relatedMap = queryCache.get(cacheKey);

				if (relatedMap == null) {
					relatedMap = Query.getDataWithDuplicates(
						relatedModel, relatedAttributes.toArray(new String[0]),
						mappingsRelated.get(0), relatedCriterion);

					queryCache.put(cacheKey, relatedMap);
				}
			}
		}

		return relatedMap;
	}

	public Map<Long, Data> getLiferayData(Model model, List<Long> groupIds)
		throws Exception {

		Criterion criterion = model.getAttributeCriterion("groupId", groupIds);

		Collection<String> attributesToQuery =
			ConfigurationUtil.getModelAttributesToQuery(model);

		String[] attributesToQueryArr = attributesToQuery.toArray(
			new String[0]);

		return Query.getData(model, attributesToQueryArr, criterion);
	}

	public void postProcessData(Data data) {
		Object treePath = data.get("treePath");

		treePath = IndexCheckerQueryHelper.processTreePath(treePath);

		data.set("treePath", treePath);
	}

	private void _copyAttributesToAlias(
		Data data, List<String> attributes, List<String> aliases) {

		for (int i=0;i<attributes.size();i++) {
			String attribute = attributes.get(i);
			String alias = aliases.get(i);

			Set<Object> existing = _castToSet(data.get(alias));
			Set<Object> added = _castToSet(data.get(attribute));

			Set<Object> newSet = new HashSet<Object>();

			newSet.addAll(existing);
			newSet.addAll(added);

			data.set(alias,newSet);
		}
	}

	private Set<Object> _castToSet(Object object) {
		if (object == null) {
			return Collections.emptySet();
		}

		if (object instanceof Set) {
			return (Set)object;
		}

		return Collections.singleton(object);
	}

	private static Log _log = LogFactoryUtil.getLog(
		IndexCheckerQueryHelper.class);

}