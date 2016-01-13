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

package jorgediazest.indexchecker.model;

import com.liferay.portal.kernel.dao.orm.Criterion;

import jorgediazest.indexchecker.data.Data;
import jorgediazest.indexchecker.data.DataUtil;

/**
 * @author Jorge Díaz
 */
public class WikiPage extends IndexCheckerModel {

	@Override
	public int compareTo(Data data1, Data data2) {
		if ((data1.getResourcePrimKey() != -1) &&
			(data2.getResourcePrimKey() != -1)) {

			return DataUtil.compareLongs(
				data1.getResourcePrimKey(), data2.getResourcePrimKey());
		}
		else {
			return 0;
		}
	}

	@Override
	public boolean equals(Data data1, Data data2) {
		if ((data1.getResourcePrimKey() != -1) &&
			(data2.getResourcePrimKey() != -1)) {

			return (data1.getResourcePrimKey() == data2.getResourcePrimKey());
		}
		else {
			return false;
		}
	}

	@Override
	public Criterion generateQueryFilter() {
		return this.generateCriterionFilter("head=true,redirectTitle=");
	}

	@Override
	public Integer hashCode(Data data) {
		if (data.getResourcePrimKey() != -1) {
			return -1 * data.getEntryClassName().hashCode() *
				Long.valueOf(data.getResourcePrimKey()).hashCode();
		}

		return null;
	}

}