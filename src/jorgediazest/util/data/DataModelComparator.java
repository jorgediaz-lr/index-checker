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

import java.util.Collections;
import java.util.List;

/**
 * @author Jorge Díaz
 */
public class DataModelComparator extends DataBaseComparator {

	public DataModelComparator(List<String> attributes) {
		this.attributes = attributes;
	}

	@Override
	public int compare(Data data1, Data data2) {

		if ((attributes == null)|| attributes.isEmpty()) {
			throw new IllegalArgumentException();
		}

		int compare = 0;

		for (String attribute : attributes) {
			compare = compareAttributes(data1, data2, attribute, attribute);

			if (compare != 0) {
				break;
			}
		}

		return compare;
	}

	@Override
	public boolean equals(Data data1, Data data2) {
		if (data1.model != data2.model) {
			return false;
		}

		for (String attribute : attributes) {
			if (!equalsAttributes(data1, data2, attribute, attribute)) {
				return false;
			}
		}

		return true;
	}

	public List<String> getAttributes() {
		return Collections.unmodifiableList(attributes);
	}

	@Override
	public Integer hashCode(Data data) {
		int hashCode = 1;

		for (String attribute : attributes) {
			Object o = data.get(attribute);

			if (o == null) {
				return null;
			}

			hashCode *= o.hashCode();
		}

		return data.getEntryClassName().hashCode() * hashCode;
	}

	protected List<String> attributes;

}