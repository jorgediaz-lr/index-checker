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

import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.dao.orm.Criterion;
import com.liferay.portal.kernel.dao.orm.RestrictionsFactoryUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.BaseModel;
import com.liferay.portal.kernel.util.Validator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import jorgediazest.util.comparator.DataComparator;
import jorgediazest.util.model.Model;
import jorgediazest.util.model.ModelUtil;
import jorgediazest.util.service.Service;
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
			relatedModelsSet.add(model);
		}

		tableInfoSet.add(model.getTableInfo());

		Map<String, TableInfo> map = model.getTableInfoMappings();

		addTableInfo(map.values());
	}

	public void addTableInfo(Collection<TableInfo> tableInfoCol) {
		tableInfoSet.addAll(tableInfoCol);
	}

	public void addTableInfo(TableInfo tableInfo) {
		tableInfoSet.add(tableInfo);
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

		if (this == obj) {
			return true;
		}

		DataComparator comparator = getComparator();

		Data data = (Data)obj;

		return comparator.equals(this, data);
	}

	public Object get(String attribute) {
		if (Validator.isNull(attribute)) {
			return null;
		}

		String primaryKeyAttribute = getPrimaryKeyAttribute();

		if (Objects.equals("pk", attribute) &&
			!Objects.equals("pk", primaryKeyAttribute)) {

			return get(primaryKeyAttribute);
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
		T value = (T)get(attribute);

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
			attribute = attribute.substring(pos + 1);
		}

		Class<?> attributeClass = Object.class;

		for (TableInfo tableInfo : tableInfoSet) {
			if ((prefix != null) &&
				!Objects.equals(tableInfo.getName(), prefix)) {

				continue;
			}

			attributeClass = tableInfo.getAttributeClass(attribute);

			if (!Object.class.equals(attributeClass)) {
				break;
			}
		}

		if (String.class.equals(attributeClass) &&
			(attribute.equals("uuid") || attribute.equals("uuid_") ||
			 attribute.endsWith("Uuid"))) {

			attributeClass = UUID.class;
		}

		return attributeClass;
	}

	public Set<String> getAttributes() {
		return map.keySet();
	}

	public String[] getAttributesArray() {
		return getAttributes().toArray(new String[getAttributes().size()]);
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

		List<Data> dataList = new ArrayList<>();

		Object key = get(mappingsSource[0]);

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
		List<Data> matched = new ArrayList<>();

		for (Data data : dataList) {
			boolean equalsAttributes = true;

			for (int j = 1; j < mappingsSource.length; j++) {
				String attr1 = mappingsSource[j];
				String attr2 = mappingsDest[j];

				equalsAttributes = dataComparator.equalsAttributes(
					this, data, attr1, attr2);

				if (equalsAttributes) {
					continue;
				}

				Object value = get(attr1);

				if (value instanceof Set) {
					Set<Object> valueSet = (Set<Object>)value;

					Class<?> type1 = getAttributeClass(attr1);
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

	public BaseModel<?> getObject() {
		if (object != null) {
			return object;
		}

		long primaryKey = getPrimaryKey();

		try {
			Service service = model.getService();

			return (BaseModel<?>)service.fetchObject(primaryKey);
		}
		catch (UnsupportedOperationException uoe) {
			if (_log.isDebugEnabled()) {
				_log.debug("error calling fetchObject: " + uoe.getMessage());
			}
		}

		String primaryKeyAttribute = getPrimaryKeyAttribute();

		if (Validator.isNull(primaryKeyAttribute) ||
			primaryKeyAttribute.equals("pk")) {

			return null;
		}

		Criterion criterion = RestrictionsFactoryUtil.eq(
			primaryKeyAttribute, primaryKey);

		List<?> list = null;

		try {
			list = model.executeDynamicQuery(criterion);
		}
		catch (Exception e) {
			if (_log.isDebugEnabled()) {
				_log.debug(
					"error calling executeDynamicQuery: " + e.getMessage());
			}
		}

		if ((list != null) && (list.size() == 1)) {
			return (BaseModel<?>)list.get(0);
		}

		return null;
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
		if (Objects.equals("pk", attribute)) {
			attribute = getPrimaryKeyAttribute();
		}

		Class<?> type = getAttributeClass(attribute);

		if (!isValid(attribute, type, value)) {
			return;
		}

		Object transformedObject;

		Class<?> clazz = value.getClass();

		if (value instanceof Set) {
			Set set = (Set)value;

			transformedObject = DataUtil.transformArray(type, set.toArray());
		}
		else if ((value instanceof Object[]) || clazz.isArray()) {
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
		if (Objects.equals("pk", attribute)) {
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
		if (Objects.equals("pk", attribute)) {
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
		if ((attributes == null) || (attributes.length == 0)) {
			if ((data.length == 1) && (data[0] instanceof BaseModel<?>)) {
				setObject((BaseModel<?>)data[0]);
			}

			return;
		}

		int i = 0;

		for (String attrib : attributes) {
			set(attrib, data[i++]);
		}
	}

	public void setObject(BaseModel<?> object) {
		this.object = object;

		Map<String, Object> map = object.getModelAttributes();

		for (Map.Entry<String, Object> entry : map.entrySet()) {
			set(entry.getKey(), entry.getValue());
		}
	}

	public void setPrimaryKey(long primaryKey) {
		set(getPrimaryKeyAttribute(), primaryKey);
	}

	public String toString() {
		long pk = getPrimaryKey();

		long rpk = getResourcePrimKey();

		String name = getEntryClassName();

		if (Validator.isNull(name)) {
			Iterator<TableInfo> iterator = tableInfoSet.iterator();

			if (iterator.hasNext()) {
				TableInfo tableInfo = iterator.next();

				name = tableInfo.getName();
			}
		}

		if ((pk == -1) && (rpk == -1)) {
			return name + " " + map.toString();
		}

		return name + " " + pk + " " + rpk + " " + get("uuid");
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

		if (!Objects.equals("companyId", attribute) &&
			!Objects.equals("groupId", attribute) &&
			!Objects.equals("resourcePrimKey", attribute) &&
			!Objects.equals("uuid_", attribute) &&
			!Objects.equals("uuid", attribute)) {

			return true;
		}

		return false;
	}

	protected Integer hashCode = null;
	protected Map<String, Object> map = new LinkedHashMap<>();
	protected Model model = null;
	protected BaseModel<?> object = null;
	protected Set<Model> relatedModelsSet = new LinkedHashSet<>();
	protected Set<TableInfo> tableInfoSet = new LinkedHashSet<>();

	private static Log _log = LogFactoryUtil.getLog(Data.class);

}