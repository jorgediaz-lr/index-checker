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

import com.liferay.portal.kernel.language.LanguageUtil;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import jorgediazest.util.data.Data;
import jorgediazest.util.data.DataModelComparator;
import jorgediazest.util.data.DataUtil;

/**
 * @author Jorge Díaz
 */
public class DataIndexCheckerModelComparator extends DataModelComparator {

	public DataIndexCheckerModelComparator(
		String primaryKeyAttribute, String[] exactAttributes) {

		super(exactAttributes);

		this.primaryKeyAttribute = primaryKeyAttribute;
	}

	@Override
	public int compare(Data data1, Data data2) {
		return DataUtil.compareLongs(
				data1.get(primaryKeyAttribute, -1L),
				data2.get(primaryKeyAttribute, -1L));
	}

	@Override
	public boolean equals(Data data1, Data data2) {
		long primaryKey1 = data1.get(primaryKeyAttribute, -1L);
		long primaryKey2 = data2.get(primaryKeyAttribute, -1L);

		return (primaryKey1 == primaryKey2);
	}

	public boolean equalsAttributes(
		Data data1, Data data2, String attr1, String attr2, Object o1,
		Object o2) {

		if (super.equalsAttributes(data1, data2, attr1, attr2, o1, o2)) {
			return true;
		}

		if (o1 instanceof Set && o2 instanceof Set) {
			for (Locale key : LanguageUtil.getAvailableLocales()) {
				Set<String> values1 = new HashSet<String>();
				Set<String> values2 = new HashSet<String>();

				if (!fillMapValues(o1, key, values1)) {
					return false;
				}

				if (!fillMapValues(o2, key, values2)) {
					return false;
				}

				if (!values1.equals(values2)) {
					return false;
				}
			}

			return true;
		}

		return false;
	}

	public boolean fillMapValues(Object obj, Locale key, Set<String> values) {
		try {
			Set<?> set = (Set<?>)obj;

			for (Iterator<?> it = set.iterator(); it.hasNext();) {
				Object element = it.next();

				if (!(element instanceof Map)) {
					return false;
				}

				@SuppressWarnings("unchecked")
				Map<Locale, String> map = (Map<Locale, String>)element;
				String aux = map.get(key);

				if (aux != null) {
					values.add(aux);
				}
			}
		}
		catch (Exception e) {
			return false;
		}

		return true;
	}

	@Override
	public Integer hashCode(Data data) {
		long primaryKey = data.get(primaryKeyAttribute, -1L);

		if (primaryKey == -1L) {
			return null;
		}

		return -1 * data.getEntryClassName().hashCode() *
			Long.valueOf(primaryKey).hashCode();
	}

	protected String primaryKeyAttribute = null;

}