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

package jorgediazest.util.query;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.Validator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jorgediazest.util.data.Data;
import jorgediazest.util.model.Model;

/**
 * @author Jorge Díaz
 */
public class QueryUtil {

	public static void addRelatedModelData(
		Map<Long, ?> dataMap, Map<Long, List<Data>> matchedMap,
		String[] attributes) {

		addRelatedModelData(dataMap, matchedMap, attributes, false);
	}

	public static void addRelatedModelData(
		Map<Long, ?> dataMap, Map<Long, List<Data>> matchedMap,
		String[] attributes, boolean appendMode) {

		if (dataMap == null) {
			return;
		}

		for (Map.Entry<Long, ?> entry : dataMap.entrySet()) {
			List<Data> matched = matchedMap.get(entry.getKey());

			if ((matched == null) || matched.isEmpty()) {
				continue;
			}

			Collection<Data> dataCollection = castToDataCollection(
				entry.getValue());

			for (Data data : dataCollection) {
				Model model = matched.get(
					0
				).getModel();

				data.addModelTableInfo(model);

				addMatchedData(data, matched, attributes, appendMode);
			}
		}
	}

	public static void addRelatedModelDataRaw(
		Map<Long, ?> dataMap, Map<Long, List<Data>> matchedMap,
		String[] attributes) {

		if (matchedMap.isEmpty()) {
			return;
		}

		for (Map.Entry<Long, ?> entry : dataMap.entrySet()) {
			List<Data> matched = matchedMap.get(entry.getKey());

			if ((matched == null) || matched.isEmpty()) {
				continue;
			}

			Collection<Data> dataCollection = castToDataCollection(
				entry.getValue());

			String classNameRelated = matched.get(
				0
			).getEntryClassName();

			Object rawDataObject = getRawRelatedData(matched, attributes);

			for (Data data : dataCollection) {
				data.set(classNameRelated, rawDataObject);
			}
		}
	}

	public static Map<Long, List<Data>> getMatchingEntriesMap(
		Map<Long, Data> dataMap, Map<Long, List<Data>> relatedMap,
		String[] mappingsSource, String[] mappingsDest) {

		if (dataMap.isEmpty() || relatedMap.isEmpty()) {
			return Collections.emptyMap();
		}

		Map<Long, List<Data>> matchedMap = new HashMap<>();

		for (Map.Entry<Long, Data> entry : dataMap.entrySet()) {
			Data data = entry.getValue();

			List<Data> matched = data.getMatchingEntries(
				relatedMap, mappingsSource, mappingsDest);

			if (!matched.isEmpty()) {
				matchedMap.put(entry.getKey(), matched);
			}
		}

		return matchedMap;
	}

	protected static void addMatchedData(
		Data data, List<Data> matched, String[] attributes,
		boolean appendMode) {

		if (matched.isEmpty()) {
			return;
		}

		for (String attribute : attributes) {
			if (Validator.isNull(attribute)) {
				continue;
			}

			Set<Object> values = new HashSet<>(matched.size());

			if (appendMode) {
				Object existingValue = data.get(attribute);

				if (existingValue instanceof Set) {
					values.addAll((Set)existingValue);
				}
				else if (existingValue != null) {
					values.add(existingValue);
				}
			}

			for (Data m : matched) {
				values.add(m.get(attribute));
			}

			if (values.size() == 1) {
				data.set(attribute, values.toArray()[0]);
			}
			else {
				data.set(attribute, values);
			}
		}
	}

	protected static Object getRawRelatedData(
		List<Data> matched, String[] attributes) {

		Set<Object> values = new HashSet<>(matched.size());

		for (Data m : matched) {
			List<Object> list = new ArrayList<>();

			for (int k = 0; k < attributes.length; k++) {
				if (Validator.isNotNull(attributes[k])) {
					Object value = m.get(attributes[k]);

					list.add(value);
				}
			}

			values.add(list);
		}

		if (values.size() == 1) {
			return values.toArray()[0];
		}

		return values;
	}

	@SuppressWarnings("unchecked")
	private static Collection<Data> castToDataCollection(Object object) {
		if (object instanceof Data) {
			return Collections.singletonList((Data)object);
		}

		if (Collection.class.isAssignableFrom(object.getClass())) {
			return (Collection<Data>)object;
		}

		if (Map.class.isAssignableFrom(object.getClass())) {
			return ((Map<?, Data>)object).values();
		}

		return Collections.emptyList();
	}

	private static Log _log = LogFactoryUtil.getLog(QueryUtil.class);

}