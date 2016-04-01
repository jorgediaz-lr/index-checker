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