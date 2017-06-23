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

package jorgediazest.util.modelquery;

import com.liferay.portal.kernel.dao.orm.Criterion;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jorgediazest.util.data.Data;
import jorgediazest.util.data.DataComparator;
import jorgediazest.util.data.DataUtil;
import jorgediazest.util.model.Model;
import jorgediazest.util.model.ModelFactory;
import jorgediazest.util.model.ModelUtil;
import jorgediazest.util.modelquery.ModelQueryFactory.DataComparatorFactory;
import jorgediazest.util.table.TableInfo;

/**
 * @author Jorge Díaz
 */
public abstract class ModelQueryImpl implements ModelQuery {

	public void addRelatedModelData(
			Map<Long, Data> dataMap, ModelQuery relatedModelQuery,
			String[] attrRelatedOrig, String[] attrRelatedDest,
			String[] mappingsSource, String[] mappingsDest,
			boolean removeUnmatched, boolean rawData, Criterion filter)
		throws Exception {

		Model model = getModel();
		Model relatedModel = relatedModelQuery.getModel();

		String mappingAttr = mappingsDest[0];

		if (model.getClassName().equals(relatedModel.getClassName()) &&
			!"MappingTable".equals(mappingAttr)) {

			return;
		}

		Map<Long, List<Data>> relatedMap;

		if ((filter == null)||"MappingTable".equals(mappingAttr)) {
			relatedMap = relatedModelQuery.getDataWithDuplicatesCache(
				attrRelatedDest, mappingAttr);
		}
		else {
			relatedMap = relatedModelQuery.getDataWithDuplicates(
				attrRelatedDest, mappingAttr, filter);
		}

		Map<Long, List<Data>> matchedMap = ModelQueryUtil.getMatchingEntriesMap(
			dataMap, relatedMap, mappingsSource, mappingsDest);

		if (rawData) {
			ModelQueryUtil.addRelatedModelDataRaw(
				dataMap, matchedMap, attrRelatedOrig, attrRelatedDest);
		}
		else {
			ModelQueryUtil.addRelatedModelData(
				dataMap, matchedMap, attrRelatedOrig, attrRelatedDest);
		}

		if (removeUnmatched) {
			for (Entry<Long, List<Data>> entry : matchedMap.entrySet()) {
				if (entry.getValue().isEmpty()) {
					dataMap.remove(entry.getKey());
				}
			}
		}
	}

	public void addRelatedModelData(
			Map<Long, Data> dataMap, String relatedClassName,
			String[] attrRelated, String[] mappings, boolean removeUnmatched,
			boolean rawData, Criterion filter)
		throws Exception {

		ModelQuery relatedModelQuery = mqFactory.getModelQueryObject(
			relatedClassName);

		if (relatedModelQuery == null) {
			return;
		}

		String[] attrRelatedOrig = new String[attrRelated.length];
		String[] attrRelatedDest = new String[attrRelated.length];
		String[] mappingsSource = new String[mappings.length];
		String[] mappingsDest = new String[mappings.length];

		ModelQueryUtil.splitSourceDest(
			attrRelated, attrRelatedOrig, attrRelatedDest);
		ModelQueryUtil.splitSourceDest(mappings, mappingsSource, mappingsDest);

		Model relatedModel = relatedModelQuery.getModel();

		if ("classPK".equals(mappingsDest[0])) {
			Criterion classNameIdFilter = relatedModel.getProperty(
				"classNameId").eq(model.getClassNameId());
			filter = ModelUtil.generateConjunctionQueryFilter(
				classNameIdFilter, filter);
		}

		addRelatedModelData(
			dataMap, relatedModelQuery, attrRelatedOrig, attrRelatedDest,
			mappingsSource, mappingsDest, removeUnmatched, rawData, filter);
	}

	public void addRelatedModelData(
			Map<Long, Data> dataMap, String[] attributesRelated,
			Criterion filter)
		throws Exception {

		ModelFactory modelFactory = mqFactory.getModelFactory();

		for (String attributeRelated : attributesRelated) {
			String[] relatedDataArr = attributeRelated.split(":");

			String className = relatedDataArr[0];

			Model relatedModel = modelFactory.getModelObject(className);

			if (relatedModel == null) {
				continue;
			}

			if (relatedDataArr.length > 3) {
				filter = relatedModel.generateCriterionFilter(
					relatedDataArr[3]);
			}

			boolean removeUnmatched = false;
			String mappings = relatedDataArr[1];

			if (mappings.endsWith("-")) {
				removeUnmatched = true;

				mappings = mappings.substring(0, mappings.length()-1);
			}

			boolean rawData = false;
			String attrRelated = relatedDataArr[2];

			if (attrRelated.startsWith("[") && attrRelated.endsWith("]")) {
				rawData = true;

				attrRelated = attrRelated.substring(1, attrRelated.length()-1);
			}

			addRelatedModelData(
				dataMap, className, attrRelated.split(","), mappings.split(","),
				removeUnmatched, rawData, filter);
		}
	}

	public void clearCache() {
		cachedDifferentAttributeValues = new HashMap<String, Map<Long, Data>>();
		cachedDifferentAttributeValuesDup =
				new HashMap<String, Map<Long, List<Data>>>();
	}

	public int compareTo(ModelQuery o) {
		return getModel().compareTo(o.getModel());
	}

	public final Map<Long, Data> getData() throws Exception {
		return getData(null, "pk", null);
	}

	public final Map<Long, Data> getData(Criterion filter) throws Exception {
		return getData(null, filter);
	}

	public final Map<Long, Data> getData(String[] attributes) throws Exception {
		return getData(attributes, "pk", null);
	}

	public final Map<Long, Data> getData(String[] attributes, Criterion filter)
		throws Exception {

		return getData(attributes, "pk", filter);
	}

	public final Map<Long, Data> getData(
			String[] attributes, String mapKeyAttribute)
		throws Exception {

		return getData(attributes, mapKeyAttribute, null);
	}

	public Map<Long, Data> getData(
			String[] attributes, String mapKeyAttribute, Criterion filter)
		throws Exception {

		return DataUtil.getData(
			model, dataComparator, attributes, mapKeyAttribute, filter);
	}

	public DataComparator getDataComparator() {
		return dataComparator;
	}

	@Deprecated
	public Map<Long, Data> getDataWithCache(String[] attr) throws Exception {
		return getDataWithCache(attr, "pk");
	}

	@Deprecated
	public Map<Long, Data> getDataWithCache(
			String[] attr, String mapKeyAttribute)
		throws Exception {

		Map<Long, Data> values = cachedDifferentAttributeValues.get(
			Arrays.toString(attr) + "key: " + mapKeyAttribute);

		if (values == null) {
			synchronized(this) {
				values = cachedDifferentAttributeValues.get(
					Arrays.toString(attr) + "key: " + mapKeyAttribute);

				if (values == null) {
					values = this.getData(attr, mapKeyAttribute);

					cachedDifferentAttributeValues.put(
						Arrays.toString(attr) + "key: " + mapKeyAttribute,
						values);
				}
			}
		}

		return values;
	}

	public final Map<Long, List<Data>> getDataWithDuplicates(
			String[] attributes, String mapKeyAttribute)
		throws Exception {

		return getDataWithDuplicates(attributes, mapKeyAttribute, null);
	}

	public Map<Long, List<Data>> getDataWithDuplicates(
			String[] attributes, String mapKeyAttribute, Criterion filter)
		throws Exception {

		if ("MappingTable".equals(mapKeyAttribute) &&
			(attributes.length == 1)) {

			TableInfo tableInfo = model.getTableInfo(attributes[0]);

			Set<Data> dataSet = DatabaseUtil.queryTable(model, tableInfo);

			return DataUtil.getMapFromSetData(
				dataSet, model.getPrimaryKeyAttribute());
		}

		return DataUtil.getDataWithDuplicates(
			model, dataComparator, attributes, mapKeyAttribute, filter);
	}

	@Deprecated
	public Map<Long, List<Data>> getDataWithDuplicatesCache(
			String[] attr, String mapKeyAttribute)
		throws Exception {

		Map<Long, List<Data>> values = cachedDifferentAttributeValuesDup.get(
			Arrays.toString(attr) + "key: " + mapKeyAttribute);

		if (values == null) {
			synchronized(this) {
				values = cachedDifferentAttributeValuesDup.get(
					Arrays.toString(attr) + "key: " + mapKeyAttribute);

				if (values == null) {
					values = this.getDataWithDuplicates(attr, mapKeyAttribute);

					cachedDifferentAttributeValuesDup.put(
						Arrays.toString(attr) + "key: " + mapKeyAttribute,
						values);
				}
			}
		}

		return values;
	}

	public Model getModel() {
		return model;
	}

	public ModelQueryFactory getModelQueryFactory() {
		return mqFactory;
	}

	public Map<Long, Data> getRelatedData(
		String[] attributes, Criterion filter,
		String mappingAttributeName) throws Exception {

		Map<Long, Data> dataMap = this.getData(attributes, filter);

		Map <Long, Data> relatedData = new HashMap<Long, Data>();

		for (Data data : dataMap.values()) {
			long mappingAttributeValue = data.get(mappingAttributeName, -1L);

			if (mappingAttributeValue > 0) {
				relatedData.put(mappingAttributeValue, data);
			}
		}

		return relatedData;
	}

	public void init(Model model, DataComparatorFactory dataComparatorFactory)
		throws Exception {

		this.model = model;

		this.dataComparator = dataComparatorFactory.getDataComparator(this);
	}

	public void setModelQueryFactory(ModelQueryFactory mqFactory) {
		this.mqFactory = mqFactory;
	}

	public String toString() {
		return getModel().toString();
	}

	protected static Log _log = LogFactoryUtil.getLog(ModelQueryImpl.class);

	protected Map<String, Map<Long, Data>> cachedDifferentAttributeValues =
		new ConcurrentHashMap<String, Map<Long, Data>>();
	protected Map<String, Map<Long, List<Data>>>
		cachedDifferentAttributeValuesDup =
			new ConcurrentHashMap<String, Map<Long, List<Data>>>();
	protected DataComparator dataComparator;
	protected Model model = null;
	protected ModelQueryFactory mqFactory = null;

}