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

import java.util.HashMap;
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

		if (this.model != data.model) {
			return false;
		}

		return comparator.equals(this, data);
	}

	public boolean exact(Data data) {

		return comparator.exact(this, data);
	}

	public Object get(String attribute) {
		if (attribute.equals("pk") ||
			attribute.equals(model.getPrimaryKeyAttribute())) {

			return getPrimaryKey();
		}

		if (attribute.equals("resourcePrimKey")) {
			return getResourcePrimKey();
		}

		return data.get(attribute);
	}

	public String getAllData(String sep) {

		return this.getEntryClassName() + sep + primaryKey + sep +
			resourcePrimKey + sep + data.toString();
	}

	public Set<String> getAttributes() {
		return data.keySet();
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

	public Model getModel() {
		return model;
	}

	public long getPrimaryKey() {
		return primaryKey;
	}

	public long getResourcePrimKey() {
		return resourcePrimKey;
	}

	public String getUuid() {
		return (String) get("uuid");
	}

	public int hashCode() {
		Integer hashCode = comparator.hashCode(this);

		if (hashCode != null) {
			return hashCode;
		}
		else {
			return super.hashCode();
		}
	}

	public void set(String attribute, Object value) {
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

		if ("resourcePrimKey".equals(attribute)) {
			setResourcePrimKey((Long)convertedObject);
		}
		else if (model.getPrimaryKeyAttribute().equals(attribute)) {
			setPrimaryKey((Long)convertedObject);
		}
		else {
			data.put(attribute, convertedObject);
		}
	}

	public void setPrimaryKey(long primaryKey) {
		this.primaryKey = primaryKey;
	}

	public void setResourcePrimKey(long resourcePrimKey) {
		this.resourcePrimKey = resourcePrimKey;
	}

	public String toString() {
		return this.getEntryClassName() + " " +
				primaryKey + " " + resourcePrimKey + " " + this.getUuid();
	}

	protected DataComparator comparator = null;
	protected Map<String, Object> data = new HashMap<String, Object>();
	protected Model model = null;
	protected long primaryKey = -1;
	protected long resourcePrimKey = -1;

}