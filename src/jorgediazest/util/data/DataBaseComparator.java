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

package jorgediazest.util.data;

/**
 * @author Jorge Díaz
 */
public class DataBaseComparator implements DataComparator {

	public int compare(Data data1, Data data2) {

		if (data1.equals(data2)) {
			return 0;
		}

		return DataUtil.compareLongs(data1.hashCode(), data2.hashCode());
	}

	public boolean equals(Data data1, Data data2) {

		if (data1.hashCode() != data2.hashCode()) {
			return false;
		}

		return data1.getMap().equals(data2.getMap());
	}

	public boolean equalsAttributes(
		Data data1, Data data2, String attr1, String attr2, Object o1,
		Object o2) {

		if (DataUtil.isNull(o1)) {
			return DataUtil.isNull(o2);
		}

		if (DataUtil.isNull(o2)) {
			return false;
		}

		if (o1.equals(o2)) {
			return true;
		}

		int type1 = data1.getAttributeType(attr1);
		int type2 = data2.getAttributeType(attr2);

		if ((type1 != type2) || (type1 == 0) || (type2 == 0)) {
			o1 = DataUtil.castString(o1);
			o2 = DataUtil.castString(o2);

			return o1.equals(o2);
		}

		return false;
	}

	public boolean exact(Data data1, Data data2) {
		return equals(data1, data2);
	}

	@Override
	public String[] getExactAttributes() {
		return new String[0];
	}

	public Integer hashCode(Data data) {

		return data.getMap().hashCode();
	}

}