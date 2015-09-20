package com.jorgediaz.indexchecker.data;

import com.jorgediaz.indexchecker.index.DocumentWrapper;
import com.jorgediaz.indexchecker.model.BaseModel;
import com.liferay.portal.kernel.search.Field;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import jodd.bean.BeanUtil;

import org.apache.lucene.document.DateTools;


public class Data implements Comparable<Data> {
	
	protected static String[] indexAttributes =
		{Field.UID, Field.CREATE_DATE, Field.MODIFIED_DATE,
		Field.ENTRY_CLASS_PK, Field.STATUS, Field.COMPANY_ID, Field.GROUP_ID};

	public static Long stringToTime(String dateString) {
		if(dateString == null) {
			return null;
		}

		try {
			return (DateTools.stringToTime(dateString))/1000;
		} catch (Exception e) {}

		try {
			return (Timestamp.valueOf(dateString).getTime())/1000;
		} catch (Exception e) {}

		return null;
	}

	public Data(BaseModel modelClass) {
		this.modelClass = modelClass;
	}

	public void init(DocumentWrapper  doc) {

		for(String attrib : indexAttributes) {
			BeanUtil.setPropertySilent(this, attrib, doc.get(attrib));
		}
	}

	public void init(ResultSet rs) throws SQLException {

		this.setPrimaryKey(rs.getLong(modelClass.getPrimaryKey()));

		for(String attrib : modelClass.getAttributes()) {
			BeanUtil.setPropertySilent(this,attrib,rs.getString(attrib));
		}
	}

	public long getEntryClassPK() {
		return entryClassPK;
	}

	public void setEntryClassPK(long entryClassPK) {
		this.entryClassPK = entryClassPK;

		if(primaryKey != -1 && resourcePrimKey == -1 && primaryKey != entryClassPK) {
			this.resourcePrimKey = entryClassPK;
		}
	}

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;

		try {
			this.primaryKey=Long.valueOf(uid.split("_")[2]);
		}
		catch(Exception e) {
			
		}

		if(resourcePrimKey == -1 && primaryKey != entryClassPK) {
			this.resourcePrimKey = entryClassPK;
		}
	}

	public String getEntryClassName() {
		return modelClass.getFullClassName();
	}

	public long getPrimaryKey() {
		return primaryKey;
	}

	public void setPrimaryKey(long primaryKey) {
		this.primaryKey = primaryKey;
	}

	public long getResourcePrimKey() {
		return resourcePrimKey;
	}

	public void setResourcePrimKey(long resourcePrimKey) {
		this.resourcePrimKey = resourcePrimKey;
	}

	public Long getCompanyId() {
		return companyId;
	}

	public void setCompanyId(Long companyId) {
		this.companyId = companyId;
	}

	public Long getGroupId() {
		return groupId;
	}

	public void setGroupId(Long groupId) {
		this.groupId = groupId;
	}

	public Long getCreateDate() {
		return createDate;
	}

	public void setCreateDate(String createDate) {
		this.createDate = stringToTime(createDate);
	}

	public Long getModifiedDate() {
		return modifiedDate;
	}

	public void setModifiedDate(String modifyDate) {
		this.modifiedDate = stringToTime(modifyDate);
	}

	public void setModified(String modifiedDate) {
		this.setModifiedDate(modifiedDate);
	}

	public Long getStatus() {
		return status;
	}

	public void setStatus(Long status) {
		this.status = status;
	}

	public boolean equals(Object obj) {
		if(!(obj instanceof Data)) {
			return false;
		}
		Data data=(Data)obj;
		if(this.modelClass != data.modelClass) {
			return false;
		}
		if(this.primaryKey != -1 && data.primaryKey != -1) {
			return (this.primaryKey == data.primaryKey);
		}
		else if(this.resourcePrimKey != -1 && data.resourcePrimKey != -1) {
			return (this.resourcePrimKey == data.resourcePrimKey);
		}
		else {
			return super.equals(obj);
		}
	}

	public boolean exact(Data data) {
		if(!this.equals(data)) {
			return false;
		}

		if(!exactLongs(this.companyId,data.companyId)) {
			return false;
		}

		if(this.modelClass.hasGroupId() && !exactLongs(this.groupId,data.groupId)) {
			return false;
		}

		if(!exactLongs(this.createDate,data.createDate)) {
			return false;
		}

		if(!exactLongs(this.modifiedDate,data.modifiedDate)) {
			return false;
		}

		if(!exactLongs(this.status,data.status)) {
			return false;
		}

		return true;
	}

	public static boolean exactLongs(Long l1, Long l2) {
		if(l1 == null) {
			return (l1 == l2);
		}

		return l1.equals(l2);
	}

	public int hashCode() {
		if(this.primaryKey != -1) {
			return this.getEntryClassName().hashCode() * Long.valueOf(this.primaryKey).hashCode();
		}
		else if(this.resourcePrimKey != -1) {
			return -1 * this.getEntryClassName().hashCode() * Long.valueOf(this.resourcePrimKey).hashCode();
		}
		else {
			return super.hashCode();
		}
	}

	@Override
	public int compareTo(Data data) {
		if(this.primaryKey != -1 && data.primaryKey != -1) {
			return Long.compare(this.primaryKey,data.primaryKey);
		}
		else if(this.resourcePrimKey != -1 && data.resourcePrimKey != -1) {
			return Long.compare(this.resourcePrimKey,data.resourcePrimKey);
		}
		else {
			return 0;
		}
	}

	public String toString() {
		return this.getEntryClassName() + " - " + entryClassPK + " - " + primaryKey + " - " + resourcePrimKey + " - " + uid;
	}

	protected BaseModel modelClass = null;
	/* Liferay */
	protected long primaryKey = -1;
	protected long resourcePrimKey = -1;
	/* Index */
	protected long entryClassPK = -1;
	protected String uid = null;
	/* Comun */
	protected Long companyId = null;
	protected Long groupId = null;
	protected Long createDate = null;
	protected Long modifiedDate = null;
	protected Long status = null;
}