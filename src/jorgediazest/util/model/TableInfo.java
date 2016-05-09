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

import jorgediazest.util.model.ModelUtil;
import jorgediazest.util.reflection.ReflectionUtil;

/**
 * @author Jorge Díaz
 */
public class TableInfo {

	public TableInfo(Class<?> classLiferayModelImpl) {
		this(classLiferayModelImpl, "TABLE_");
	}

	public TableInfo(Class<?> classLiferayModelImpl, String fieldPrefix) {
		attributesArr = (Object[][])ReflectionUtil.getLiferayModelImplField(
				classLiferayModelImpl, fieldPrefix + "COLUMNS");
		name = (String)ReflectionUtil.getLiferayModelImplField(
				classLiferayModelImpl, fieldPrefix + "NAME");
		sqlCreate = (String)ReflectionUtil.getLiferayModelImplField(
				classLiferayModelImpl, fieldPrefix + "SQL_CREATE");
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

	public Object[][] getAttributesArr() {
		return attributesArr;
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

	protected String getCreateTableAttributes() {
		String aux = attributesStr;

		if (aux.indexOf('#') > 0) {
			aux = aux.split("#")[0];
		}

		return aux;
	}

	private Object[][] attributesArr = null;
	private String attributesStr = null;
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