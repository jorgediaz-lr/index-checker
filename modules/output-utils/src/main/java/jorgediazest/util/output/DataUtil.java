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

package jorgediazest.util.output;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import jorgediazest.util.data.Data;

/**
 * @author Jorge Díaz
 */
public class DataUtil {

	public static String[] getListAttr(
		Collection<Data> dataCollection, List<String> attrList) {

		return getListAttr(dataCollection, attrList, dataCollection.size());
	}

	public static String[] getListAttr(
		Collection<Data> dataCollection, List<String> attrList, int size) {

		if (size <= 0) {
			return new String[0];
		}

		if (size > dataCollection.size()) {
			size = dataCollection.size();
		}

		String[] values = new String[size];

		int i = 0;

		for (Data data : dataCollection) {
			String value;

			if (attrList.size() == 1) {
				value = _getString(data, attrList.get(0));
			}
			else {
				String[] auxArr = new String[attrList.size()];

				for (int j = 0; j < attrList.size(); j++) {
					auxArr[j] = _getString(data, attrList.get(j));
				}

				value = Arrays.toString(auxArr);
			}

			values[i++] = value;

			if (i >= size) {
				break;
			}
		}

		return values;
	}

	public static String[] getListAttr(
		Collection<Data> dataCollection, String attr) {

		return getListAttr(dataCollection, attr, dataCollection.size());
	}

	public static String[] getListAttr(
		Collection<Data> dataCollection, String attr, int size) {

		return getListAttr(
			dataCollection, Collections.singletonList(attr), size);
	}

	private static String _getString(Data data, String attr) {
		Object obj = data.get(attr);

		return obj.toString();
	}

}