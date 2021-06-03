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

import java.util.Comparator;

import jorgediazest.util.data.Data;

/**
 * @author Jorge Díaz
 */
public interface DataComparator extends Comparator<Data> {

	public int compare(Data data1, Data data2);

	public boolean equals(Data data1, Data data2);

	public boolean equalsAttributes(
		Class<?> type1, Class<?> type2, Object o1, Object o2);

	public boolean equalsAttributes(
		Data data1, Data data2, String attr1, String attr2);

	public boolean getIgnoreNulls();

	public Integer hashCode(Data data);

	public void setIgnoreNulls(boolean ignoreNulls);

}