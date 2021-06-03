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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import jorgediazest.util.data.Data;

/**
 * @author Jorge Díaz
 */
public class DataModelComparator extends DataBaseComparator {

	public DataModelComparator(List<String> attributes) {
		this.attributes = new String[attributes.size()];
		this.operations = new String[attributes.size()];

		int i=0;
		for (String attribute : attributes) {
			String operation = attribute.substring(0,2);
			if (">=".equals(operation)||"<=".equals(operation)) {
				attribute=attribute.substring(2);
			}
			else {
				operation=null;
			}
			this.attributes[i]=attribute;
			this.operations[i]=operation;
			i++;
		}
	}

	@Override
	public int compare(Data data1, Data data2) {

		if ((attributes == null)|| (attributes.length==0)) {
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
		if (data1.getModel() != data2.getModel()) {
			return false;
		}

		for (int i=0;i<attributes.length;i++) {
			String attribute=attributes[i];
			String operation=operations[i];
			if (operation == null) {
				if (!equalsAttributes(data1, data2, attribute, attribute)) {
					return false;
				}

				continue;
			}

			int compare = compareAttributes(data1, data2, attribute, attribute);

			if(!equalAttributeWithOperation(compare, operation)) {
				return false;
			}
		}

		return true;
	}

	private boolean equalAttributeWithOperation(int compare, String operation) {
		if (">=".equals(operation)) {
			if (compare < 0) {
				return false;
			}
		}
		else if ("<=".equals(operation)) {
			if (compare > 0) {
				return false;
			}
		}
		else {
			throw new IllegalArgumentException();
		}
		return true;
	}

	public List<String> getAttributes() {
		return Collections.unmodifiableList(Arrays.asList(attributes));
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

	protected String[] attributes;
	protected String[] operations;

}