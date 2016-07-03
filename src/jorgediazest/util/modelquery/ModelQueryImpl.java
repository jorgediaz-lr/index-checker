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

import com.liferay.portal.kernel.dao.orm.Conjunction;
import com.liferay.portal.kernel.dao.orm.Criterion;
import com.liferay.portal.kernel.dao.orm.Disjunction;
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.ProjectionFactoryUtil;
import com.liferay.portal.kernel.dao.orm.ProjectionList;
import com.liferay.portal.kernel.dao.orm.RestrictionsFactoryUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.Validator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jorgediazest.util.data.Data;
import jorgediazest.util.data.DataComparator;
import jorgediazest.util.data.DataUtil;
import jorgediazest.util.model.Model;
import jorgediazest.util.model.ModelUtil;
import jorgediazest.util.model.TableInfo;
import jorgediazest.util.modelquery.ModelQueryFactory.DataComparatorFactory;
import jorgediazest.util.service.Service;

/**
 * @author Jorge Díaz
 */
public abstract class ModelQueryImpl implements ModelQuery {

	public void addRelatedModelData(
			Map<Long, Data> dataMap, String classNameRelated,
			String[] attrRelated, String[] mappings, boolean removeUnmatched,
			boolean rawData)
		throws Exception {

		addRelatedModelData(
			dataMap, classNameRelated, attrRelated, mappings, removeUnmatched,
			rawData, null);
	}

	@SuppressWarnings("unchecked")
	public void addRelatedModelData(
			Map<Long, Data> dataMap, String classNameRelated,
			String[] attrRelated, String[] mappings, boolean removeUnmatched,
			boolean rawData, Criterion filter)
		throws Exception {

		String[] attrRelatedOrig = new String[attrRelated.length];
		String[] attrRelatedDest = new String[attrRelated.length];
		String[] mappingsSource = new String[mappings.length];
		String[] mappingsDest = new String[mappings.length];

		splitSourceDest(attrRelated, attrRelatedOrig, attrRelatedDest);
		splitSourceDest(mappings, mappingsSource, mappingsDest);

		Map<Long, List<Data>> relatedMap = getRelatedModelData(
			classNameRelated, attrRelatedDest, mappingsDest[0], filter);

		Set<Long> unmatchedDataKeys = new HashSet<>();

		for (Entry<Long, Data> entry : dataMap.entrySet()) {
			Data data = entry.getValue();

			List<Data> relatedList = new ArrayList<>();

			Object key = data.get(mappingsSource[0]);

			if (key instanceof Set) {
				for (Object k : (Set<Object>)key) {
					List<Data> list = relatedMap.get(k);

					if (list != null) {
						relatedList.addAll(list);
					}
				}
			}
			else {
				List<Data> list = relatedMap.get(key);

				if (list != null) {
					relatedList.addAll(list);
				}
			}

			List<Data> matched = new ArrayList<>();

			for (Data related : relatedList) {
				boolean equalsAttributes = true;

				for (int j = 1; j<mappings.length; j++) {
					equalsAttributes = data.includesValueOfAttribute(
						related, mappingsSource[j], mappingsDest[j]);

					if (!equalsAttributes) {
						break;
					}
				}

				if (equalsAttributes) {
					matched.add(related);
				}
			}

			if (matched.isEmpty()) {
				if (removeUnmatched) {
					unmatchedDataKeys.add(entry.getKey());
				}

				continue;
			}

			if (rawData) {
				if (matched.size() == 1) {
					Object value = getRawRelatedData(
						matched.get(0), attrRelatedOrig, attrRelatedDest);

					data.set(classNameRelated, value);

					continue;
				}

				Set<Object> values = new HashSet<>(matched.size());

				for (Data m : matched) {
					Object value = getRawRelatedData(
						m, attrRelatedOrig, attrRelatedDest);
					values.add(value);
				}

				data.set(classNameRelated, values);

				continue;
			}

			data.addModelTableInfo(matched.get(0).getModel());

			for (int k = 0; k<attrRelatedOrig.length; k++) {
				if (Validator.isNotNull(attrRelatedOrig[k])) {
					if (matched.size() == 1) {
						Object value = matched.get(0).get(attrRelatedDest[k]);

						data.set(attrRelatedOrig[k], value);

						continue;
					}

					Set<Object> values = new HashSet<>(matched.size());

					for (Data m : matched) {
						values.add(m.get(attrRelatedDest[k]));
					}

					data.set(attrRelatedOrig[k], values);
				}
			}
		}

		for (Long key : unmatchedDataKeys) {
			dataMap.remove(key);
		}
	}

	public void clearCache() {
		cachedDifferentAttributeValues = new HashMap<>();
		cachedDifferentAttributeValuesDup = new HashMap<>();
	}

	public int compareTo(ModelQuery o) {
		return getModel().compareTo(o.getModel());
	}

	public Data createDataObject(String[] attributes, Object[] result) {
		Model model = this.model;
		DataComparator dataComparator = this.dataComparator;

		return DataUtil.createDataObject(
			model, dataComparator, attributes, result);
	}

	public Criterion getCompanyFilter(long companyId) {
		return getCompanyGroupFilter(companyId, null);
	}

	public Criterion getCompanyGroupFilter(
			long companyId, List<Long> groupIds) {

		Conjunction conjunction = RestrictionsFactoryUtil.conjunction();

		if (getModel().hasAttribute("companyId")) {
			conjunction.add(getModel().getProperty("companyId").eq(companyId));
		}

		if (getModel().hasAttribute("groupId") &&
			Validator.isNotNull(groupIds)) {

			Disjunction disjunction = RestrictionsFactoryUtil.disjunction();

			for (Long groupId : groupIds) {
				disjunction.add(getModel().getProperty("groupId").eq(groupId));
			}

			conjunction.add(disjunction);
		}

		return conjunction;
	}

	public Criterion getCompanyGroupFilter(long companyId, long groupId) {

		List<Long> groupIds = null;

		if (groupId != 0) {
			groupIds = new ArrayList<>();

			groupIds.add(groupId);
		}

		return getCompanyGroupFilter(companyId, groupIds);
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

		Map<Long, List<Data>> dataMapWithDuplicates = getDataWithDuplicates(
			attributes, mapKeyAttribute, filter);

		Map<Long, Data> dataMap = new HashMap<>();

		for (Entry<Long, List<Data>> e : dataMapWithDuplicates.entrySet()) {
			Data data = e.getValue().get(0);
			dataMap.put(e.getKey(), data);
		}

		return dataMap;
	}

	public Map<Long, Data> getData(
			String[] attributesModel, String[] attributesRelated,
			Criterion filter)
		throws Exception {

		return getData(attributesModel, attributesRelated, "pk", filter);
	}

	public Map<Long, Data> getData(
			String[] attributesModel, String[] attributesRelated,
			String mapKeyAttribute, Criterion filter)
		throws Exception {

			Map<Long, Data> dataMap = getData(
				attributesModel, mapKeyAttribute, filter);

			for (String attributeRelated : attributesRelated) {
				String[] relatedDataArr = attributeRelated.split(":");

				if (relatedDataArr.length > 3) {
					if (Validator.isNull(relatedDataArr[3])) {
						filter = null;
					}
					else {
						ModelQuery relatedModel = mqFactory.getModelQueryObject(
							relatedDataArr[0]);

						filter =
							relatedModel.getModel().generateCriterionFilter(
								relatedDataArr[3]);
					}
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

					attrRelated = attrRelated.substring(
						1, attrRelated.length()-1);
				}

				addRelatedModelData(
					dataMap, relatedDataArr[0], attrRelated.split(","),
					mappings.split(","), removeUnmatched, rawData, filter);
			}

			return dataMap;
		}

	public DataComparator getDataComparator() {
		return dataComparator;
	}

	public Map<Long, Data> getDataWithCache() throws Exception {
		return getDataWithCache(null);
	}

	public Map<Long, Data> getDataWithCache(String[] attr) throws Exception {
		return getDataWithCache(attr, "pk");
	}

	public Map<Long, Data> getDataWithCache(
			String[] attr, String mapKeyAttribute)
		throws Exception {

		Map<Long, Data> values = cachedDifferentAttributeValues.get(
			Arrays.toString(attr) + "key: " + mapKeyAttribute);

		if (values == null) {
			values = this.getData(attr, mapKeyAttribute);

			cachedDifferentAttributeValues.put(
				Arrays.toString(attr) + "key: " + mapKeyAttribute, values);
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

		Map<Long, List<Data>> dataMap = new HashMap<>();

		DynamicQuery query = model.getService().newDynamicQuery();

		if (attributes == null) {
			attributes = getModel().getAttributesName();
		}

		List<String> validAttributes = new ArrayList<>();
		ProjectionList projectionList = getModel().getPropertyProjection(
			attributes, validAttributes);

		query.setProjection(ProjectionFactoryUtil.distinct(projectionList));

		if (filter != null) {
			query.add(filter);
		}

		Service service = model.getService();

		@SuppressWarnings("unchecked")
		List<Object[]> results = (List<Object[]>)service.executeDynamicQuery(
			query);

		String[] validAttributesArr = validAttributes.toArray(
			new String[validAttributes.size()]);

		long i = -1;

		for (Object[] result : results) {
			Data data = createDataObject(validAttributesArr, result);

			Long mappingAttributeValue = DataUtil.castLong(
				data.get(mapKeyAttribute));

			if (Validator.isNull(mappingAttributeValue)) {
				mappingAttributeValue = i--;
			}

			if (!dataMap.containsKey(mappingAttributeValue)) {
				List<Data> list = new ArrayList<>();
				list.add(data);
				dataMap.put(mappingAttributeValue, list);
			}
			else {
				dataMap.get(mappingAttributeValue).add(data);
			}
		}

		return dataMap;
	}

	public Map<Long, List<Data>> getDataWithDuplicatesCache(
			String[] attr, String mapKeyAttribute)
		throws Exception {

		Map<Long, List<Data>> values = cachedDifferentAttributeValuesDup.get(
			Arrays.toString(attr) + "key: " + mapKeyAttribute);

		if (values == null) {
			values = this.getDataWithDuplicates(attr, mapKeyAttribute);

			cachedDifferentAttributeValuesDup.put(
				Arrays.toString(attr) + "key: " + mapKeyAttribute, values);
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

		Map <Long, Data> relatedData = new HashMap<>();

		for (Data data : dataMap.values()) {
			long mappingAttributeValue = data.get(mappingAttributeName, -1L);

			if (mappingAttributeValue > 0) {
				relatedData.put(mappingAttributeValue, data);
			}
		}

		return relatedData;
	}

	public Map<Long, List<Data>> getRelatedModelData(
			String classNameRelated, String[] attributes, String mappingAttr,
			Criterion filter)
		throws Exception {

		ModelQuery mq = mqFactory.getModelQueryObject(classNameRelated);

		if ((mq == null) || (mq.getModel() == null)) {
			return new HashMap<>();
		}

		if ("MappingTable".equals(mappingAttr)) {
			TableInfo tableInfo = mq.getModel().getTableInfo(attributes[0]);

			return queryTable(
				tableInfo, mq.getModel().getPrimaryKeyAttribute());
		}

		if ("classPK".equals(mappingAttr)) {
			Criterion classNameIdFilter = mq.getModel().getProperty(
				"classNameId").eq(getModel().getClassNameId());
			filter = ModelUtil.generateConjunctionQueryFilter(
				classNameIdFilter, filter);
		}

		if (getModel().getClassName().equals(mq.getModel().getClassName())) {
			return new HashMap<>();
		}

		if (filter == null) {
			return mq.getDataWithDuplicatesCache(attributes, mappingAttr);
		}

		return mq.getDataWithDuplicates(attributes, mappingAttr, filter);
	}

	public void init(Model model, DataComparatorFactory dataComparatorFactory)
		throws Exception {

		this.model = model;

		this.dataComparator = dataComparatorFactory.getDataComparator(this);
	}

	public Set<Data> queryTable(TableInfo tableInfo) throws Exception {
		if (Validator.isNull(tableInfo)) {
			return new HashSet<Data>();
		}

		return queryTable(tableInfo, tableInfo.getAttributesName());
	}

	public Map<Long, List<Data>> queryTable(
			TableInfo tableInfo, String mappingAttr)
		throws Exception {

		Set<Data> dataSet = queryTable(tableInfo);
		Map<Long, List<Data>> dataMap = new HashMap<>();

		for (Data data : dataSet) {
			Long key = (Long)data.get(mappingAttr);

			if (key == null) {
				continue;
			}

			if (!dataMap.containsKey(key)) {
				List<Data> list = new ArrayList<>();
				list.add(data);
				dataMap.put(key, list);
			}
			else {
				dataMap.get(key).add(data);
			}
		}

		return dataMap;
	}

	public Set<Data> queryTable(TableInfo tableInfo, String[] attributesName)
		throws Exception {

		return DatabaseUtil.queryTableWithCache(
			dataSetCacheMap, model, tableInfo, attributesName);
	}

	public void setModelQueryFactory(ModelQueryFactory mqFactory) {
		this.mqFactory = mqFactory;
	}

	public String toString() {
		return getModel().toString();
	}

	protected Object getRawRelatedData(
		Data data, String[] attrRelatedOrig, String[] attrRelatedDest) {

		List<Object> list = new ArrayList<>();

		for (int k = 0; k<attrRelatedOrig.length; k++) {
			if (Validator.isNotNull(attrRelatedOrig[k])) {
				Object value = data.get(attrRelatedDest[k]);

				list.add(value);
			}
		}

		return list;
	}

	protected void splitSourceDest(
		String[] dataArray, String[] dataArrayOrigin, String[] dataArrayDest) {

		int i = 0;

		for (String data : dataArray) {
			String[] aux = data.split("=");
			dataArrayOrigin[i] = aux[0];
			int posDest = 0;

			if (aux.length > 1) {
				posDest = 1;
			}

			dataArrayDest[i] = aux[posDest];
			i++;
		}
	}

	protected static Log _log = LogFactoryUtil.getLog(ModelQueryImpl.class);

	protected Map<String, Map<Long, Data>> cachedDifferentAttributeValues =
		new ConcurrentHashMap<>();
	protected Map<String, Map<Long, List<Data>>>
		cachedDifferentAttributeValuesDup = new ConcurrentHashMap<>();
	protected DataComparator dataComparator;
	protected Model model = null;
	protected ModelQueryFactory mqFactory = null;

	private Map<String, Set<Data>> dataSetCacheMap = new ConcurrentHashMap<>();

}