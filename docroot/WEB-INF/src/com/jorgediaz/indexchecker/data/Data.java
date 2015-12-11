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

package com.jorgediaz.indexchecker.data;

import com.jorgediaz.indexchecker.index.DocumentWrapper;
import com.jorgediaz.indexchecker.model.IndexCheckerModel;

import com.liferay.portal.kernel.search.Field;

/**
 * @author Jorge Díaz
 */
public class Data implements Comparable<Data> {

	public Data(IndexCheckerModel baseModel) {
		this.model = baseModel;
	}

	@Override
	public int compareTo(Data data) {
		if ((this.primaryKey != -1) && (data.primaryKey != -1)) {
			return DataUtil.compareLongs(this.primaryKey, data.primaryKey);
		}
		else if ((this.resourcePrimKey != -1) && (data.resourcePrimKey != -1)) {
			return DataUtil.compareLongs(
				this.resourcePrimKey, data.resourcePrimKey);
		}
		else {
			return 0;
		}
	}

	public boolean equals(Object obj) {
		if (!(obj instanceof Data)) {
			return false;
		}

		Data data =(Data)obj;

		if (this.model != data.model) {
			return false;
		}

		if ((this.primaryKey != -1) && (data.primaryKey != -1)) {
			return (this.primaryKey == data.primaryKey);
		}
		else if ((this.resourcePrimKey != -1) && (data.resourcePrimKey != -1)) {
			return (this.resourcePrimKey == data.resourcePrimKey);
		}
		else {
			return super.equals(obj);
		}
	}

	public boolean exact(Data data) {
		if (!this.equals(data)) {
			return false;
		}

		if (!DataUtil.exactLongs(this.companyId, data.companyId)) {
			return false;
		}

		if (this.model.hasGroupId() &&
			!DataUtil.exactLongs(this.groupId, data.groupId)) {

			return false;
		}

		if (!DataUtil.exactLongs(this.createDate, data.createDate)) {
			return false;
		}

		if (!DataUtil.exactLongs(this.modifiedDate, data.modifiedDate)) {
			return false;
		}

		if (!DataUtil.exactIntegers(this.status, data.status)) {
			return false;
		}

		return true;
	}

	public String getAllData(String sep) {
		return this.getEntryClassName() + sep + companyId + sep + groupId +
			sep + entryClassPK + sep + primaryKey + sep + resourcePrimKey +
			sep + uid + sep + createDate + sep + modifiedDate + sep + status;
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

	public long getEntryClassPK() {
		return entryClassPK;
	}

	public Long getGroupId() {
		return groupId;
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

	public int hashCode() {
		if (this.primaryKey != -1) {
			return this.getEntryClassName().hashCode() *
				Long.valueOf(this.primaryKey).hashCode();
		}
		else if (this.resourcePrimKey != -1) {
			return -1 * this.getEntryClassName().hashCode() *
				Long.valueOf(this.resourcePrimKey).hashCode();
		}
		else {
			return super.hashCode();
		}
	}

	public void init(DocumentWrapper doc) {

		for (String attrib : indexAttributes) {
			setProperty(attrib, doc.get(attrib));
		}
	}

	public void init(Object[] dataArray) {

		this.primaryKey = ((Long)dataArray[0]);

		int i = 0;

		for (String attrib : model.getIndexAttributes()) {
			setProperty(attrib, dataArray[i++]);
		}
	}

	public void setProperty(String attribute, Object value) {
		if ("companyId".equals(attribute) ||
			"entryClassPK".equals(attribute) ||
			"groupId".equals(attribute) ||
			"resourcePrimKey".equals(attribute)) {

			Long longValue = DataUtil.castLong(value);

			if (longValue == null) {
				return;
			}
			else if ("companyId".equals(attribute)) {
				this.companyId = longValue;
			}
			else if ("entryClassPK".equals(attribute)) {
				this.entryClassPK = longValue;

				if ((primaryKey != -1) && (resourcePrimKey == -1) &&
					(primaryKey != longValue)) {

					this.resourcePrimKey = longValue;
				}
			}
			else if ("groupId".equals(attribute)) {
				this.groupId = longValue;
			}
			else if ("resourcePrimKey".equals(attribute)) {
				this.resourcePrimKey = longValue;
			}
		}
		else if ("status".equals(attribute)) {
			Integer intValue = DataUtil.castInt(value);

			if (intValue == null) {
				return;
			}

			this.setStatus(intValue);
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
				this.createDate = DataUtil.stringToTime(strValue);
			}
			else if ("modifiedDate".equals(attribute) ||
					 "modified".equals(attribute)) {

				this.modifiedDate = DataUtil.stringToTime(strValue);
			}
			else if ("uid".equals(attribute)) {
				this.uid = strValue;

				this.primaryKey = DataUtil.castLong(strValue.split("_")[2]);

				if ((resourcePrimKey == -1) && (primaryKey != entryClassPK)) {
					this.resourcePrimKey = entryClassPK;
				}
			}
		}
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public String toString() {
		return this.getEntryClassName() + " " + entryClassPK + " " +
				primaryKey + " " + resourcePrimKey + " " + uid;
	}

	protected static String[] indexAttributes =
		{Field.UID, Field.CREATE_DATE, Field.MODIFIED_DATE,
		Field.ENTRY_CLASS_PK, Field.STATUS, Field.COMPANY_ID, Field.GROUP_ID};

	/* Comun */

	protected Long companyId = null;
	protected Long createDate = null;
	protected long entryClassPK = -1;
	protected Long groupId = null;
	protected IndexCheckerModel model = null;
	protected Long modifiedDate = null;
	protected long primaryKey = -1;
	protected long resourcePrimKey = -1;
	/* Liferay */
	protected Integer status = null;
	/* Index */
	protected String uid = null;

}