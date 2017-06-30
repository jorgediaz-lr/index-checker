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
import java.util.Map.Entry;
import java.util.Set;

import jorgediazest.util.data.Data;
import jorgediazest.util.model.Model;

/**
 * @author Jorge Díaz
 */
public class ModelQueryUtil {

	public static void addRelatedModelData(
		Map<Long, ?> dataMap, Map<Long, List<Data>> matchedMap,
		String[] attributes) {

		if (dataMap == null) {
			return;
		}

		for (Entry<Long, ?> entry : dataMap.entrySet()) {
			List<Data> matched = matchedMap.get(entry.getKey());

			if ((matched == null) || matched.isEmpty()) {
				continue;
			}

			Collection<Data> dataCollection = castToDataCollection(
				entry.getValue());

			for (Data data : dataCollection) {
				Model model = matched.get(0).getModel();

				data.addModelTableInfo(model);

				addMatchedData(data, matched, attributes);
			}
		}
	}

	public static void addRelatedModelDataRaw(
		Map<Long, ?> dataMap, Map<Long, List<Data>> matchedMap,
		String[] attributes) {

		for (Entry<Long, ?> entry : dataMap.entrySet()) {
			List<Data> matched = matchedMap.get(entry.getKey());

			if (matched.isEmpty()) {
				continue;
			}

			Collection<Data> dataCollection = castToDataCollection(
				entry.getValue());

			String classNameRelated = matched.get(0).getEntryClassName();

			Object rawDataObject = getRawRelatedData(matched, attributes);

			for (Data data : dataCollection) {
				data.set(classNameRelated, rawDataObject);
			}
		}
	}

	protected static void addMatchedData(
		Data data, List<Data> matched, String[] attributes) {

		for (int k = 0; k<attributes.length; k++) {
			String attribute = attributes[k];

			if (Validator.isNull(attribute)) {
				continue;
			}

			Set<Object> values = new HashSet<Object>(matched.size());

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

	@SuppressWarnings("unchecked")
	protected static List<Data> getMatchingEntries(
		Data data, Map<Long, List<Data>> relatedMap, String[] mappingsSource,
		String[] mappingsDest) {

		List<Data> relatedList = new ArrayList<Data>();

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

		List<Data> matched = new ArrayList<Data>();

		for (Data related : relatedList) {
			boolean equalsAttributes = true;

			for (int j = 1; j<mappingsSource.length; j++) {
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

		return matched;
	}

	protected static Map<Long, List<Data>> getMatchingEntriesMap(
		Map<Long, Data> dataMap, Map<Long, List<Data>> relatedMap,
		String[] mappingsSource, String[] mappingsDest) {

		Map<Long, List<Data>> matchedMap = new HashMap<Long, List<Data>>();

		for (Entry<Long, Data> entry : dataMap.entrySet()) {
			Data data = entry.getValue();

			List<Data> matched = getMatchingEntries(
					data, relatedMap, mappingsSource, mappingsDest);

			matchedMap.put(entry.getKey(), matched);
		}

		return matchedMap;
	}

	protected static Object getRawRelatedData(
		List<Data> matched, String[] attributes) {

		Set<Object> values = new HashSet<Object>(matched.size());

		for (Data m : matched) {
			List<Object> list = new ArrayList<Object>();

			for (int k = 0; k<attributes.length; k++) {
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

	protected static void splitSourceDest(
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

	private static Log _log = LogFactoryUtil.getLog(ModelQueryUtil.class);

}