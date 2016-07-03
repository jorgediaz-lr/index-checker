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

import com.liferay.portal.kernel.util.Validator;

import java.util.Collection;
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

	public void addModelTableInfo(Collection<Model> modelList) {
		for (Model model : modelList) {
			addModelTableInfo(model);
		}
	}

	public void addModelTableInfo(Model model) {
		if (!this.model.equals(model)) {
			this.relatedModelsSet.add(model);
		}

		this.tableInfoSet.add(model.getTableInfo());

		this.addTableInfo(model.getTableInfoMappings().values());
	}

	public void addTableInfo(Collection<TableInfo> tableInfoCol) {
		this.tableInfoSet.addAll(tableInfoCol);
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

		return comparator.equalsAttributes(this, data, attr1, attr2, o1, o2);
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
		String prefix = null;
		int pos = attribute.indexOf(".");

		if (pos != -1) {
			prefix = attribute.substring(0, pos);
			attribute = attribute.substring(pos+1);
		}

		for (TableInfo tableInfo : tableInfoSet) {
			if ((prefix != null) && !tableInfo.getName().equals(prefix)) {
				continue;
			}

			int type = tableInfo.getAttributeType(attribute);

			if (type != 0) {
				return type;
			}
		}

		return 0;
	}

	public Class<?> getAttributeTypeClass(String attribute) {
		String prefix = null;
		int pos = attribute.indexOf(".");

		if (pos != -1) {
			prefix = attribute.substring(0, pos);
			attribute = attribute.substring(pos+1);
		}

		for (TableInfo tableInfo : tableInfoSet) {
			if ((prefix != null) && !tableInfo.getName().equals(prefix)) {
				continue;
			}

			return tableInfo.getAttributeTypeClass(attribute);
		}

		return Object.class;
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

	public Set<Model> getRelatedModels() {
		return relatedModelsSet;
	}

	public long getResourcePrimKey() {
		return get("resourcePrimKey", -1L);
	}

	public String getUuid() {
		return (String)get("uuid");
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

	public boolean isValid(String attribute, int type, Object value) {
		if (value == null) {
			return false;
		}

		if (type != 0) {
			return true;
		}

		if (!("companyId".equals(attribute) || "groupId".equals(attribute) ||
			 "resourcePrimKey".equals(attribute))) {

			return true;
		}

		return false;
	}

	@SuppressWarnings("rawtypes")
	public void set(String attribute, Object value) {
		if ("pk".equals(attribute)) {
			attribute = model.getPrimaryKeyAttribute();
		}

		int type = getAttributeType(attribute);

		if (!isValid(attribute, type, value)) {
			return;
		}

		Object transformObject;

		Class valueClass = value.getClass();

		if (value instanceof Set) {
			transformObject = DataUtil.transformArray(
				type, ((Set)value).toArray());
		}
		else if (value instanceof Object[] || valueClass.isArray()) {
			transformObject = DataUtil.transformArray(type, (Object[])value);
		}
		else {
			transformObject = DataUtil.transformObject(type, value);
		}

		if (transformObject != null) {
			map.put(attribute, transformObject);
		}
	}

	public void set(String attribute, Object[] values) {
		if ("pk".equals(attribute)) {
			attribute = model.getPrimaryKeyAttribute();
		}

		int type = getAttributeType(attribute);

		if (!isValid(attribute, type, values)) {
			return;
		}

		Object transformObject = DataUtil.transformArray(type, values);

		if (transformObject != null) {
			map.put(attribute, transformObject);
		}
	}

	public void set(String attribute, Set<Object> values) {
		if ("pk".equals(attribute)) {
			attribute = model.getPrimaryKeyAttribute();
		}

		int type = getAttributeType(attribute);

		if (!isValid(attribute, type, values)) {
			return;
		}

		Object transformObject = DataUtil.transformArray(
			type, values.toArray());

		if (transformObject != null) {
			map.put(attribute, transformObject);
		}
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
	protected Map<String, Object> map = new LinkedHashMap<>();
	protected Model model = null;
	protected Set<Model> relatedModelsSet = new LinkedHashSet<>();
	protected Set<TableInfo> tableInfoSet = new LinkedHashSet<>();

}