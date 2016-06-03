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

import com.liferay.portal.kernel.util.LocalizationUtil;
import com.liferay.portal.kernel.util.Validator;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import jorgediazest.util.model.Model;
import jorgediazest.util.model.TableInfo;

/**
 * @author Jorge Díaz
 */
public class Data implements Comparable<Data> {

	public Data(Model model, DataComparator comparator) {
		this.comparator = comparator;
		this.model = model;
		addModelTableInfo(model);
	}

	public void addModelTableInfo(Model model) {
		this.tableInfoSet.add(model.getTableInfo());

		for (TableInfo tableInfo : model.getTableInfoMappings().values()) {
			this.tableInfoSet.add(tableInfo);
		}
	}

	@Override
	public int compareTo(Data data) {
		return comparator.compare(this, data);
	}

	public boolean equals(Object obj) {
		if (!(obj instanceof Data)) {
			return false;
		}

		Data data = ((Data)obj);

		if (this == obj) {
			return true;
		}

		return comparator.equals(this, data);
	}

	public boolean equalsAttributes(Data data, String attr) {
		return this.equalsAttributes(
			data, attr, attr, get(attr), data.get(attr));
	}

	public boolean equalsAttributes(Data data, String attr1, String attr2) {
		return this.equalsAttributes(
			data, attr1, attr2, get(attr1), data.get(attr2));
	}

	public boolean equalsAttributes(
		Data data, String attr1, String attr2, Object o1, Object o2) {

		if (DataUtil.isNull(o1)) {
			return DataUtil.isNull(o2);
		}

		if (o1.equals(o2)) {
			return true;
		}

		int type1 = this.getAttributeType(attr1);
		int type2 = data.getAttributeType(attr2);

		if ((type1 != type2) || (type1 == 0) || (type2 == 0)) {
			o1 = o1.toString();
			o2 = o2.toString();

			return o1.equals(o2);
		}

		return false;
	}

	public boolean exact(Data data) {

		return comparator.exact(this, data);
	}

	public Object get(String attribute) {
		if (Validator.isNull(attribute)) {
			return null;
		}

		if ("pk".equals(attribute)) {
			return get(model.getPrimaryKeyAttribute());
		}

		return map.get(attribute);
	}

	public <T> T get(String attribute, T defaultValue) {
		@SuppressWarnings("unchecked")
		T value = (T)this.get(attribute);

		if (value != null) {
			return value;
		}

		return defaultValue;
	}

	public Set<String> getAttributes() {
		return map.keySet();
	}

	public String[] getAttributesArr() {
		return map.keySet().toArray(new String[map.keySet().size()]);
	}

	public int getAttributeType(String attribute) {
		for (TableInfo tableInfo : tableInfoSet) {
			int type = tableInfo.getAttributeType(attribute);

			if (type != 0) {
				return type;
			}
		}

		return 0;
	}

	public Long getCompanyId() {
		return (Long)get("companyId");
	}

	public String getEntryClassName() {
		return model.getClassName();
	}

	public Long getGroupId() {
		return (Long)get("groupId");
	}

	public Map<String, Object> getMap() {
		return map;
	}

	public Model getModel() {
		return model;
	}

	public long getPrimaryKey() {
		return get("pk", -1L);
	}

	public long getResourcePrimKey() {
		return get("resourcePrimKey", -1L);
	}

	public String getUuid() {
		return (String) get("uuid");
	}

	public int hashCode() {
		if (hashCode != null) {
			return hashCode;
		}

		hashCode = comparator.hashCode(this);

		if (hashCode == null) {
			hashCode = super.hashCode();
		}

		return hashCode;
	}

	public boolean includesValueOfAttribute(
			Data data, String attr1, String attr2) {

		boolean eq = equalsAttributes(data, attr1, attr2);

		Object value1 = get(attr1);

		if (!eq && (value1 instanceof Set)) {
			@SuppressWarnings("unchecked")
			Set<Object> value1Set = (Set<Object>)value1;

			for (Object value1Aux : value1Set) {
				eq = this.equalsAttributes(
					data, attr1, attr2, value1Aux, data.get(attr2));

				if (eq) {
					break;
				}
			}
		}

		return eq;
	}

	public void set(String attribute, Object value) {
		if (value == null) {
			return;
		}

		Object convertedObject;
		int type = getAttributeType(attribute);

		if (type == 0) {
			convertedObject = value;
		}
		else {
			convertedObject = DataUtil.castObjectToJdbcTypeObject(type, value);
		}

		if (convertedObject == null) {
			return;
		}

		if ((type == 0) && ("companyId".equals(attribute) ||
			 "groupId".equals(attribute) ||
			 "resourcePrimKey".equals(attribute))) {

			return;
		}

		if (convertedObject instanceof String) {
			String aux = (String)convertedObject;

			if (Validator.isXml(aux)) {
				convertedObject = LocalizationUtil.getLocalizationMap(aux);
			}
		}

		map.put(attribute, convertedObject);
	}

	public void set(String attribute, Object[] values) {
		if ((values == null)||(values.length == 0)) {
			return;
		}

		if (values.length == 1) {
			set(attribute, values[0]);

			return;
		}

		Set<Object> convertedObjects = new HashSet<Object>(values.length);
		int type = getAttributeType(attribute);

		for (Object o : values) {
			if (type == 0) {
				convertedObjects.add(o.toString());

				continue;
			}

			Object c = DataUtil.castObjectToJdbcTypeObject(type, o);

			if (c == null) {
				return;
			}

			convertedObjects.add(c);
		}

		if ((type == 0) && ("companyId".equals(attribute) ||
			 "groupId".equals(attribute) ||
			 "resourcePrimKey".equals(attribute))) {

			return;
		}

		map.put(attribute, convertedObjects);
	}

	public void set(String attribute, Set<Object> values) {
		if (values == null) {
			return;
		}

		set(attribute, values.toArray());
	}

	public void setPrimaryKey(long primaryKey) {
		set(model.getPrimaryKeyAttribute(), primaryKey);
	}

	public void setResourcePrimKey(long resourcePrimKey) {
		set("resourcePrimKey", resourcePrimKey);
	}

	public String toString() {
		long pk = this.getPrimaryKey();
		long rpk = this.getResourcePrimKey();

		if ((pk == -1) && (rpk == -1)) {
			return this.getEntryClassName() + " " + map.toString();
		}

		return this.getEntryClassName() + " " + pk + " " + rpk + " " +
			this.getUuid();
	}

	protected DataComparator comparator = null;
	protected Integer hashCode = null;
	protected Map<String, Object> map = new LinkedHashMap<String, Object>();
	protected Model model = null;
	protected Set<TableInfo> tableInfoSet = new LinkedHashSet<TableInfo>();

}