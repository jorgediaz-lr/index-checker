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

package jorgediazest.util.comparator;

import java.util.Iterator;
import java.util.Set;

import jorgediazest.util.data.Data;

/**
 * @author Jorge Díaz
 */
public class DataComparatorMap extends DataBaseComparator {

	@Override
	public int compare(Data data1, Data data2) {
		Set<String> k1 = data1.getMap().keySet();
		Set<String> k2 = data2.getMap().keySet();

		if (k1.size() != k2.size()) {
			return k1.size() - k2.size();
		}

		int compare = 0;

		Iterator<String> it1 = k1.iterator();
		Iterator<String> it2 = k2.iterator();

		while (it1.hasNext() && it2.hasNext()) {
			String s1 = it1.next();
			String s2 = it2.next();

			compare = s1.compareTo(s2);

			if (compare != 0) {
				break;
			}

			compare = compareAttributes(data1, data2, s1, s2);

			if (compare != 0) {
				break;
			}
		}

		return compare;
	}

	@Override
	public boolean equals(Data data1, Data data2) {
		Set<String> k1 = data1.getMap().keySet();
		Set<String> k2 = data2.getMap().keySet();

		if (k1.size() != k2.size()) {
			return false;
		}

		if (data1.hashCode() != data2.hashCode()) {
			return false;
		}

		Iterator<String> it1 = k1.iterator();
		Iterator<String> it2 = k2.iterator();

		while (it1.hasNext() && it2.hasNext()) {
			if (!equalsAttributes(data1, data2, it1.next(), it2.next())) {
				return false;
			}
		}

		return true;
	}

	@Override
	public Integer hashCode(Data data) {
		int hashCode = 1;

		for (Object o : data.getMap().values()) {
			hashCode *= o.toString().hashCode();
		}

		return hashCode;
	}

}