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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import jorgediazest.util.model.Model;

/**
 * @author Jorge Díaz
 */
public class Data implements Comparable<Data> {

	public Data(Model model, DataComparator comparator) {
		this.comparator = comparator;
		this.model = model;
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

	public boolean equalsAttributes(Data data, String attr1, String attr2) {
		return DataUtil.equalsAttributes(
			getModel(), data.getModel(), attr1, attr2, get(attr1),
			data.get(attr2));
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

	public void set(String attribute, Object value) {
		if (value == null) {
			return;
		}

		Object convertedObject;
		int type = model.getAttributeType(attribute);

		if (type == 0) {
			convertedObject = value.toString();
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

		map.put(attribute, convertedObject);
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

}