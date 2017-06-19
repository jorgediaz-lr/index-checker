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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import jorgediazest.util.data.Data;

/**
 * @author Jorge Díaz
 */
public class ModelQueryUtil {

	@SuppressWarnings("unchecked")
	public static void addRelatedModelData(
		Map<Long, Data> dataMap, Map<Long, List<Data>> relatedMap,
		String classNameRelated, String[] attrRelatedOrig,
		String[] attrRelatedDest, String[] mappingsSource,
		String[] mappingsDest, boolean removeUnmatched, boolean rawData) {

		Set<Long> unmatchedDataKeys = new HashSet<Long>();

		for (Entry<Long, Data> entry : dataMap.entrySet()) {
			Data data = entry.getValue();

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

			if (matched.isEmpty()) {
				if (removeUnmatched) {
					unmatchedDataKeys.add(entry.getKey());
				}

				continue;
			}

			if (rawData) {
				if (matched.size() == 1) {
					Object value =
						getRawRelatedData(
							matched.get(0), attrRelatedOrig, attrRelatedDest);

					data.set(classNameRelated, value);

					continue;
				}

				Set<Object> values = new HashSet<Object>(matched.size());

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

					Set<Object> values = new HashSet<Object>(matched.size());

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

	public static void splitSourceDest(
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

	protected static Object getRawRelatedData(
		Data data, String[] attrRelatedOrig, String[] attrRelatedDest) {

		List<Object> list = new ArrayList<Object>();

		for (int k = 0; k<attrRelatedOrig.length; k++) {
			if (Validator.isNotNull(attrRelatedOrig[k])) {
				Object value = data.get(attrRelatedDest[k]);

				list.add(value);
			}
		}

		return list;
	}

	private static Log _log = LogFactoryUtil.getLog(ModelQueryUtil.class);

}