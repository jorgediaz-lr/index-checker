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

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;

import jorgediazest.util.data.Data;
import jorgediazest.util.data.DataUtil;

/**
 * @author Jorge Díaz
 */
public abstract class DataBaseComparator implements DataComparator {

	public int compareAttributes(
		Data data1, Data data2, String attr1, String attr2) {

		int type1 = data1.getAttributeType(attr1);
		int type2 = data2.getAttributeType(attr2);

		Object o1 = data1.get(attr1);
		Object o2 = data2.get(attr2);

		return compareAttributes(type1, type2, o1, o2);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public int compareAttributes(int type1, int type2, Object o1, Object o2) {
		if (o1 == o2) {
			return 0;
		}

		if ((o1 == null) && (o2 != null)) {
			return -1;
		}

		if (o2 == null) {
			return 1;
		}

		if ((o1 instanceof Comparable) && (o2 instanceof Comparable) &&
			(type1 == type2) && (type1 != 0) && (type2 != 0)) {

			Comparable c1 = (Comparable)o1;
			Comparable c2 = (Comparable)o2;

			return c1.compareTo(c2);
		}

		String s1 = DataUtil.castString(o1);
		String s2 = DataUtil.castString(o2);

		return s1.compareTo(s2);
	}

	public boolean equalsAttributes(
		Data data1, Data data2, String attr1, String attr2) {

		int type1 = data1.getAttributeType(attr1);
		int type2 = data2.getAttributeType(attr2);

		Object o1 = data1.get(attr1);
		Object o2 = data2.get(attr2);

		boolean equalsAttribute = equalsAttributes(type1, type2, o1, o2);

		if (_log.isDebugEnabled() && !(equalsAttribute)) {
			_log.debug(
				"data1=" + data1 + " data2=" + data2 + " attr1=" + attr1 +
					" attr2=" + attr2 + " o1=" + o1 + " o2=" + o2 +
						" are not equal");
		}

		return equalsAttribute;
	}

	public boolean equalsAttributes(
			int type1, int type2, Object o1, Object o2) {

		boolean isNull1 = DataUtil.isNull(o1);
		boolean isNull2 = DataUtil.isNull(o2);

		if (isNull1 || isNull2) {
			return (isNull1 && isNull2);
		}

		boolean equalsAttribute = o1.equals(o2);

		if (equalsAttribute) {
			return true;
		}

		if ((type1 != type2) || (type1 == 0) || (type2 == 0)) {
			String str1 = DataUtil.castString(o1);
			String str2 = DataUtil.castString(o2);

			return str1.equals(str2);
		}

		return false;
	}

	public Integer hashCode(Data data) {

		return data.getMap().hashCode();
	}

	private static Log _log = LogFactoryUtil.getLog(DataBaseComparator.class);

}