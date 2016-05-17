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

package jorgediazest.util.model;

import com.liferay.portal.kernel.util.StringPool;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jorgediazest.util.data.Data;
import jorgediazest.util.reflection.ReflectionUtil;

/**
 * @author Jorge Díaz
 */
public class TableInfo {

	public TableInfo(Model model) {
		this(model, "TABLE");
	}

	public TableInfo(Model model, String fieldPrefix) {

		this.model = model;

		Class<?> classLiferayModelImpl =
			model.getService().getLiferayModelImplClass();

		attributesArr = (Object[][])ReflectionUtil.getLiferayModelImplField(
				classLiferayModelImpl, fieldPrefix + "_COLUMNS");
		name = (String)ReflectionUtil.getLiferayModelImplField(
				classLiferayModelImpl, fieldPrefix + "_NAME");
		sqlCreate = (String)ReflectionUtil.getLiferayModelImplField(
				classLiferayModelImpl, fieldPrefix + "_SQL_CREATE");
		attributesStr = ModelUtil.getDatabaseAttributesStr(name, sqlCreate);

		if ((attributesStr != null) && (attributesStr.indexOf('#') > 0)) {
			String aux = attributesStr.split("#")[1];
			primaryKeyMultiAttribute = aux.split(",");

			for (int i = 0; i < primaryKeyMultiAttribute.length; i++) {
				primaryKeyMultiAttribute[i] =
					primaryKeyMultiAttribute[i].trim();
			}
		}
		else {
			primaryKeyMultiAttribute = new String[0];
		}

		String[] arrDatabaseAttributes = getCreateTableAttributes().split(",");

		for (String attr : arrDatabaseAttributes) {
			String[] aux = attr.split(" ");

			if (aux.length < 2) {
				continue;
			}

			String col = aux[0];

			if (col.endsWith("_")) {
				col = col.substring(0, col.length() - 1);
			}

			if (attr.endsWith("not null primary key")) {
				primaryKeyAttribute = col;
			}
		}

		if (primaryKeyAttribute == null) {
			primaryKeyAttribute = StringPool.BLANK;
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof TableInfo)) {
			return false;
		}

		TableInfo tableInfo = ((TableInfo)obj);
		return getName().equals(tableInfo.getName());
	}

	public int getAttributePos(String name) {
		if (mapAttributePosition.containsKey(name)) {
			return mapAttributePosition.get(name);
		}

		Object[][] values = this.getAttributes();

		if (name.endsWith(StringPool.UNDERLINE)) {
			name = name.substring(0, name.length() - 1);
		}

		String nameWithUnderline = name + StringPool.UNDERLINE;

		int pos = -1;

		for (int i = 0; i < values.length; i++) {
			if (((String)values[i][0]).endsWith(StringPool.UNDERLINE) &&
				((String)values[i][0]).equals(nameWithUnderline)) {

				pos = i;
			}
			else if (((String)values[i][0]).equals(name)) {
				pos = i;
			}
		}

		mapAttributePosition.put(name, pos);

		return pos;
	}

	public Object[][] getAttributes() {
		return attributesArr;
	}

	public String[] getAttributesName() {
		Object[][] values = this.getAttributes();

		String[] names = new String[values.length];

		for (int i = 0; i < values.length; i++) {
			names[i] = (String)values[i][0];
		}

		return names;
	}

	public int[] getAttributesType() {
		Object[][] values = this.getAttributes();

		int[] types = new int[values.length];

		for (int i = 0; i < values.length; i++) {
			types[i] = (Integer)values[i][1];
		}

		return types;
	}

	public int getAttributeType(String name) {
		int pos = this.getAttributePos(name);

		if (pos == -1) {
			return 0;
		}

		return (Integer)this.getAttributes()[pos][1];
	}

	public String getDestinationAttr(String primaryKey) {
		String[] attrNames = getAttributesName();
		String destinationAttr = null;

		for (int i = 0; i<attrNames.length; i++) {
			if ((primaryKey != null) && !primaryKey.equals(attrNames[i])) {
				destinationAttr = attrNames[i];
				break;
			}
		}

		if (destinationAttr == null) {
			destinationAttr = Arrays.toString(attrNames);
		}

		return destinationAttr;
	}

	public String getName() {
		return name;
	}

	public String getPrimaryKeyAttribute() {
		return primaryKeyAttribute;
	}

	public String[] getPrimaryKeyMultiAttribute() {
		return primaryKeyMultiAttribute;
	}

	public String getSqlCreate() {
		return sqlCreate;
	}

	@Override
	public int hashCode() {
		return getName().hashCode();
	}

	public Set<Data> queryTable() throws Exception {
		if (dataSetCache == null) {
			dataSetCache = DatabaseUtil.queryTable(
				model, name, getAttributesName());
		}

		return dataSetCache;
	}

	public Map<Long, List<Data>> queryTable(String mappingAttr)
		throws Exception {

		Set<Data> dataSet = queryTable();
		Map<Long, List<Data>> dataMap = new HashMap<Long, List<Data>>();

		for (Data data : dataSet) {
			long key = (Long)data.get(mappingAttr);

			if (!dataMap.containsKey(key)) {
				List<Data> list = new ArrayList<Data>();
				list.add(data);
				dataMap.put(key, list);
			}
			else {
				dataMap.get(key).add(data);
			}
		}

		return dataMap;
	}

	protected String getCreateTableAttributes() {
		String aux = attributesStr;

		if (aux.indexOf('#') > 0) {
			aux = aux.split("#")[0];
		}

		return aux;
	}

	protected Map<String, Integer> mapAttributePosition =
		new ConcurrentHashMap<String, Integer>();

	private Object[][] attributesArr = null;
	private String attributesStr = null;
	private Set<Data> dataSetCache = null;
	private Model model = null;
	private String name = null;

	/**
	 * primaries keys can be at following ways:
	 *
	 * - single => create table UserGroupGroupRole (userGroupId LONG not
	 * null,groupId LONG not null,roleId LONG not null,primary key (userGroupId,
	 * groupId, roleId))";
	 *
	 * - multi => create table JournalArticle (uuid_ VARCHAR(75) null,id_ LONG
	 * not null primary key,resourcePrimKey LONG,groupId LONG,companyId
	 * LONG,userId LONG,userName VARCHAR(75) null,createDate DATE
	 * null,modifiedDate DATE null,folderId LONG,classNameId LONG,classPK
	 * LONG,treePath STRING null,articleId VARCHAR(75) null,version DOUBLE,title
	 * STRING null,urlTitle VARCHAR(150) null,description TEXT null,content TEXT
	 * null,type_ VARCHAR(75) null,structureId VARCHAR(75) null,templateId
	 * VARCHAR(75) null,layoutUuid VARCHAR(75) null,displayDate DATE
	 * null,expirationDate DATE null,reviewDate DATE null,indexable
	 * BOOLEAN,smallImage BOOLEAN,smallImageId LONG,smallImageURL STRING
	 * null,status INTEGER,statusByUserId LONG,statusByUserName VARCHAR(75)
	 * null,statusDate DATE null)
	 */

	private String primaryKeyAttribute = null;

	private String[] primaryKeyMultiAttribute = null;
	private String sqlCreate = null;

}