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

package jorgediazest.indexchecker.data;

import jorgediazest.util.data.Data;
import jorgediazest.util.data.DataModelComparator;
import jorgediazest.util.data.DataUtil;

/**
 * @author Jorge Díaz
 */
public class DataResourceModelComparator extends DataModelComparator {

	public DataResourceModelComparator(String[] exactAttributes) {
		super(exactAttributes);
	}

	@Override
	public int compare(Data data1, Data data2) {
		return DataUtil.compareLongs(
				data1.getResourcePrimKey(), data2.getResourcePrimKey());
	}

	@Override
	public boolean equals(Data data1, Data data2) {
		return (data1.getResourcePrimKey() == data2.getResourcePrimKey());
	}

	@Override
	public Integer hashCode(Data data) {
		if (data.getResourcePrimKey() == -1) {
			return null;
		}

		return -1 * data.getEntryClassName().hashCode() *
			Long.valueOf(data.getResourcePrimKey()).hashCode();
	}

}