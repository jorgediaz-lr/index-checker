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

import java.util.Objects;

/**
 * @author Jorge Díaz
 */
public class DataModelComparator extends DataBaseComparator {

	public DataModelComparator(String[] exactAttributes) {
		this.exactAttributes = exactAttributes;
	}

	public int compare(Data data1, Data data2) {
		return DataUtil.compareLongs(
			data1.getPrimaryKey(), data2.getPrimaryKey());
	}

	public boolean equals(Data data1, Data data2) {
		if (data1.model != data2.model) {
			return false;
		}

		if (data1.getPrimaryKey() == data2.getPrimaryKey()) {
			return true;
		}

		return false;
	}

	public boolean exact(Data data1, Data data2) {
		if (!data1.equals(data2)) {
			return false;
		}

		if (!Objects.equals(data1.getCompanyId(), data2.getCompanyId())) {
			return false;
		}

		if (data1.getModel().hasAttribute("groupId") &&
			!Objects.equals(data1.getGroupId(), data2.getGroupId())) {

			return false;
		}

		for (String attr : exactAttributes) {
			if (!data1.equalsAttributes(data2, attr)) {
				return false;
			}
		}

		return true;
	}

	public String[] getExactAttributes() {
		return exactAttributes;
	}

	public Integer hashCode(Data data) {
		if (data.getPrimaryKey() == -1) {
			return null;
		}

		return data.getEntryClassName().hashCode() *
			Long.valueOf(data.getPrimaryKey()).hashCode();
	}

	protected String[] exactAttributes = null;

}