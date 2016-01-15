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

import jorgediazest.indexchecker.model.IndexCheckerModel;

/**
 * @author Jorge Díaz
 */
public class Data implements Comparable<Data> {

	public Data(IndexCheckerModel baseModel) {
		this.model = baseModel;
	}

	@Override
	public int compareTo(Data data) {
		return model.compareTo(this, data);
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

		return model.equals(this, data);
	}

	public boolean exact(Data data) {

		return model.exact(this, data);
	}

	public String getAllData(String sep) {
		return this.getEntryClassName() + sep + companyId + sep + groupId +
			sep + primaryKey + sep + resourcePrimKey + sep + uid + sep +
			createDate + sep + modifiedDate + sep + status + sep + version;
	}

	public Long getCompanyId() {
		return companyId;
	}

	public Long getCreateDate() {
		return createDate;
	}

	public String getEntryClassName() {
		return model.getClassName();
	}

	public Long getGroupId() {
		return groupId;
	}

	public IndexCheckerModel getModel() {
		return model;
	}

	public Long getModifiedDate() {
		return modifiedDate;
	}

	public long getPrimaryKey() {
		return primaryKey;
	}

	public long getResourcePrimKey() {
		return resourcePrimKey;
	}

	public Integer getStatus() {
		return status;
	}

	public String getUid() {
		return uid;
	}

	public String getVersion() {
		return version;
	}

	public int hashCode() {
		Integer hashCode = model.hashCode(this);

		if (hashCode != null) {
			return hashCode;
		}
		else {
			return super.hashCode();
		}
	}

	public void setCompanyId(Long companyId) {
		this.companyId = companyId;
	}

	public void setCreateDate(Long createDate) {
		this.createDate = createDate;
	}

	public void setGroupId(Long groupId) {
		this.groupId = groupId;
	}

	public void setModifiedDate(Long modifiedDate) {
		this.modifiedDate = modifiedDate;
	}

	public void setPrimaryKey(long primaryKey) {
		this.primaryKey = primaryKey;
	}

	public void setProperty(String attribute, Object value) {
		if ("companyId".equals(attribute) ||
			"entryClassPK".equals(attribute) ||
			"groupId".equals(attribute) ||
			"scopeGroupId".equals(attribute) ||
			"resourcePrimKey".equals(attribute)) {

			Long longValue = DataUtil.castLong(value);

			if (longValue == null) {
				return;
			}
			else if ("companyId".equals(attribute)) {
				setCompanyId(longValue);
			}
			else if ("entryClassPK".equals(attribute)) {
				if (model.isResourcedModel()) {
					setResourcePrimKey(longValue);
				}
				else {
					setPrimaryKey(longValue);
				}
			}
			else if ("groupId".equals(attribute) ||
					 "scopeGroupId".equals(attribute)) {

				setGroupId(longValue);
			}
			else if ("resourcePrimKey".equals(attribute)) {
				setResourcePrimKey(longValue);
			}
		}
		else if ("status".equals(attribute)) {
			Integer intValue = DataUtil.castInt(value);

			if (intValue == null) {
				return;
			}

			setStatus(intValue);
		}
		else if ("version".equals(attribute)) {
			String doubleString = null;
			Double doubleValue = DataUtil.castDouble(value);

			if (doubleValue != null) {
				doubleString = doubleValue.toString().replace(',', '.');
			}
			else {
				doubleString = DataUtil.castString(value);
			}

			setVersion(doubleString);
		}
		else if ("createDate".equals(attribute) ||
				 "modifiedDate".equals(attribute) ||
				 "modified".equals(attribute) ||
				 "uid".equals(attribute)) {

			String strValue = DataUtil.castString(value);

			if (strValue == null) {
				return;
			}

			if ("createDate".equals(attribute)) {
				setCreateDate(DataUtil.stringToTime(strValue));
			}
			else if ("modifiedDate".equals(attribute) ||
					 "modified".equals(attribute)) {

				setModifiedDate(DataUtil.stringToTime(strValue));
			}
			else if ("uid".equals(attribute)) {
				setUid(strValue);
			}
		}
	}

	public void setResourcePrimKey(long resourcePrimKey) {
		this.resourcePrimKey = resourcePrimKey;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String toString() {
		return this.getEntryClassName() + " " +
				primaryKey + " " + resourcePrimKey + " " + uid;
	}

	protected Long companyId = null;
	protected Long createDate = null;
	protected Long groupId = null;
	protected IndexCheckerModel model = null;
	protected Long modifiedDate = null;
	protected long primaryKey = -1;
	protected long resourcePrimKey = -1; /* only Liferay */
	protected Integer status = null;
	protected String uid = null; /* only Index */
	protected String version = null;

}