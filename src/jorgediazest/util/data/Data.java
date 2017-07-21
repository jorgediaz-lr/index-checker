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

import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import jorgediazest.util.comparator.DataComparator;
import jorgediazest.util.model.Model;
import jorgediazest.util.model.ModelUtil;
import jorgediazest.util.table.TableInfo;

/**
 * @author Jorge Díaz
 */
public class Data implements Comparable<Data> {

	public Data() {
	}

	public Data(Model model) {
		this.model = model;
		addModelTableInfo(model);
	}

	public Data(TableInfo tableInfo) {
		addTableInfo(tableInfo);
	}

	public void addModelTableInfo(Collection<Model> modelList) {
		for (Model model : modelList) {
			addModelTableInfo(model);
		}
	}

	public void addModelTableInfo(Model model) {
		if (model == null) {
			return;
		}

		if ((this.model == null) || !this.model.equals(model)) {
			this.relatedModelsSet.add(model);
		}

		this.tableInfoSet.add(model.getTableInfo());

		this.addTableInfo(model.getTableInfoMappings().values());
	}

	public void addTableInfo(Collection<TableInfo> tableInfoCol) {
		this.tableInfoSet.addAll(tableInfoCol);
	}

	public void addTableInfo(TableInfo tableInfo) {
		this.tableInfoSet.add(tableInfo);
	}

	@Override
	public int compareTo(Data data) {
		DataComparator comparator = getComparator();

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

		DataComparator comparator = getComparator();

		return comparator.equals(this, data);
	}

	public Object get(String attribute) {
		if (Validator.isNull(attribute)) {
			return null;
		}

		if ("pk".equals(attribute)) {
			return get(getPrimaryKeyAttribute());
		}

		if ((model != null) && !map.containsKey(attribute)) {
			String newAttribute = model.getClassSimpleName() + "." + attribute;

			if (!map.containsKey(newAttribute)) {
				newAttribute = model.getClassName() + "." + attribute;
			}

			if (!map.containsKey(newAttribute)) {
				return null;
			}

			attribute = newAttribute;
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

	public Class<?> getAttributeClass(String attribute) {
		String prefix = null;
		int pos = attribute.indexOf(".");

		if (pos != -1) {
			prefix = attribute.substring(0, pos);
			attribute = attribute.substring(pos+1);
		}

		Class<?> attributeClass = Object.class;

		for (TableInfo tableInfo : tableInfoSet) {
			if ((prefix != null) && !tableInfo.getName().equals(prefix)) {
				continue;
			}

			attributeClass = tableInfo.getAttributeClass(attribute);

			if (!Object.class.equals(attributeClass)) {
				break;
			}
		}

		if (String.class.equals(attributeClass) &&
			("uuid".equals(attribute) || "uuid_".equals(attribute) ||
			 attribute.endsWith("Uuid"))) {

			attributeClass = UUID.class;
		}

		return attributeClass;
	}

	public Set<String> getAttributes() {
		return map.keySet();
	}

	public String[] getAttributesArray() {
		return map.keySet().toArray(new String[map.keySet().size()]);
	}

	public Long getCompanyId() {
		return (Long)get("companyId");
	}

	public String getEntryClassName() {
		if (model == null) {
			return StringPool.BLANK;
		}

		return model.getClassName();
	}

	public Long getGroupId() {
		return (Long)get("groupId");
	}

	public Map<String, Object> getMap() {
		return Collections.unmodifiableMap(map);
	}

	@SuppressWarnings("unchecked")
	public List<Data> getMatchingEntries(
		Map<Long, List<Data>> dataMap, String[] mappingsSource,
		String[] mappingsDest) {

		List<Data> dataList = new ArrayList<Data>();

		Object key = this.get(mappingsSource[0]);

		if (key instanceof Set) {
			for (Object k : (Set<Object>)key) {
				List<Data> list = dataMap.get(k);

				if (list != null) {
					dataList.addAll(list);
				}
			}
		}
		else {
			List<Data> list = dataMap.get(key);

			if (list != null) {
				dataList.addAll(list);
			}
		}

		DataComparator dataComparator = getComparator();
		List<Data> matched = new ArrayList<Data>();

		for (Data data : dataList) {
			boolean equalsAttributes = true;

			for (int j = 1; j<mappingsSource.length; j++) {
				String attr1 = mappingsSource[j];
				String attr2 = mappingsDest[j];

				equalsAttributes = dataComparator.equalsAttributes(
					this, data, attr1, attr2);

				if (equalsAttributes) {
					continue;
				}

				Object value = this.get(attr1);

				if (value instanceof Set) {
					Set<Object> valueSet = (Set<Object>)value;

					Class<?> type1 = this.getAttributeClass(attr1);
					Class<?> type2 = data.getAttributeClass(attr2);

					for (Object setElement : valueSet) {
						equalsAttributes = dataComparator.equalsAttributes(
							type1, type2, setElement, data.get(attr2));

						if (equalsAttributes) {
							break;
						}
					}
				}

				if (!equalsAttributes) {
					break;
				}
			}

			if (equalsAttributes) {
				matched.add(data);
			}
		}

		return matched;
	}

	public Model getModel() {
		return model;
	}

	public long getPrimaryKey() {
		return get("pk", -1L);
	}

	public Set<Model> getRelatedModels() {
		return Collections.unmodifiableSet(relatedModelsSet);
	}

	public long getResourcePrimKey() {
		return get("resourcePrimKey", -1L);
	}

	public Set<TableInfo> getTableInfoSet() {
		return Collections.unmodifiableSet(tableInfoSet);
	}

	public UUID getUuid() {
		Object uuid = get("uuid");

		if (uuid instanceof UUID) {
			return (UUID)uuid;
		}

		return UUID.fromString(uuid.toString());
	}

	public int hashCode() {
		if (hashCode != null) {
			return hashCode;
		}

		DataComparator comparator = getComparator();

		hashCode = comparator.hashCode(this);

		if (hashCode == null) {
			hashCode = super.hashCode();
		}

		return hashCode;
	}

	@SuppressWarnings("rawtypes")
	public void set(String attribute, Object value) {
		if ("pk".equals(attribute)) {
			attribute = getPrimaryKeyAttribute();
		}

		Class<?> type = getAttributeClass(attribute);

		if (!isValid(attribute, type, value)) {
			return;
		}

		Object transformedObject;

		if (value instanceof Set) {
			transformedObject = DataUtil.transformArray(
				type, ((Set)value).toArray());
		}
		else if (value instanceof Object[] || value.getClass().isArray()) {
			transformedObject = DataUtil.transformArray(type, (Object[])value);
		}
		else {
			transformedObject = DataUtil.transformObject(type, value);
		}

		if (transformedObject != null) {
			attribute = ModelUtil.getCachedAttributeName(attribute);

			map.put(attribute, transformedObject);
		}
	}

	public void set(String attribute, Object[] values) {
		if ("pk".equals(attribute)) {
			attribute = getPrimaryKeyAttribute();
		}

		Class<?> type = getAttributeClass(attribute);

		if (!isValid(attribute, type, values)) {
			return;
		}

		Object transformedObject = DataUtil.transformArray(type, values);

		if (transformedObject != null) {
			attribute = ModelUtil.getCachedAttributeName(attribute);

			map.put(attribute, transformedObject);
		}
	}

	public void set(String attribute, Set<Object> values) {
		if ("pk".equals(attribute)) {
			attribute = getPrimaryKeyAttribute();
		}

		Class<?> type = getAttributeClass(attribute);

		if (!isValid(attribute, type, values)) {
			return;
		}

		Object transformedObject = DataUtil.transformArray(
			type, values.toArray());

		if (transformedObject != null) {
			attribute = ModelUtil.getCachedAttributeName(attribute);

			map.put(attribute, transformedObject);
		}
	}

	public <T> void setArray(String[] attributes, T[] data) {
		int i = 0;

		for (String attrib : attributes) {
			this.set(attrib, data[i++]);
		}
	}

	public void setPrimaryKey(long primaryKey) {
		set(getPrimaryKeyAttribute(), primaryKey);
	}

	public String toString() {
		long pk = this.getPrimaryKey();
		long rpk = this.getResourcePrimKey();
		String name = this.getEntryClassName();

		if (Validator.isNull(name)) {
			Iterator<TableInfo> iterator = tableInfoSet.iterator();

			if (iterator.hasNext()) {
				name = iterator.next().getName();
			}
		}

		if ((pk == -1) && (rpk == -1)) {
			return name + " " + map.toString();
		}

		return name + " " + pk + " " + rpk + " " + this.getUuid();
	}

	protected DataComparator getComparator() {
		return DataUtil.getDataComparator(model);
	}

	protected String getPrimaryKeyAttribute() {
		String pkAttribute = null;

		if (model != null) {
			pkAttribute = model.getPrimaryKeyAttribute();
		}

		if (Validator.isNull(pkAttribute)) {
			return "pk";
		}

		return pkAttribute;
	}

	protected boolean isValid(String attribute, Class<?> type, Object value) {
		if ((type == null) || (value == null)) {
			return false;
		}

		if (!Object.class.equals(type)) {
			return true;
		}

		return !("companyId".equals(attribute) || "groupId".equals(attribute) ||
			 "resourcePrimKey".equals(attribute) || "uuid_".equals(attribute) ||
			 "uuid".equals(attribute));
	}

	protected Integer hashCode = null;
	protected Map<String, Object> map = new LinkedHashMap<String, Object>();
	protected Model model = null;
	protected Set<Model> relatedModelsSet = new LinkedHashSet<Model>();
	protected Set<TableInfo> tableInfoSet = new LinkedHashSet<TableInfo>();

}