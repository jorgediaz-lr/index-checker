package com.jorgediaz.indexchecker.data;

import com.jorgediaz.indexchecker.index.DocumentWrapper;
import com.jorgediaz.indexchecker.model.IndexCheckerModel;
import com.liferay.portal.kernel.search.Field;

import java.sql.Timestamp;

import jodd.bean.BeanUtil;

import org.apache.lucene.document.DateTools;


public class Data implements Comparable<Data> {
	
	protected static String[] indexAttributes =
		{Field.UID, Field.CREATE_DATE, Field.MODIFIED_DATE,
		Field.ENTRY_CLASS_PK, Field.STATUS, Field.COMPANY_ID, Field.GROUP_ID};

	protected static Long stringToTime(String dateString) {
		if(dateString == null) {
			return null;
		}

		try {
			return (DateTools.stringToTime(dateString))/1000;
		} catch (Exception e) {}

		try {
			return (Long.valueOf(dateString))/1000;
		} catch (Exception e) {}

		try {
			return (Timestamp.valueOf(dateString).getTime())/1000;
		} catch (Exception e) {}

		return null;
	}

	public Data(IndexCheckerModel baseModel) {
		this.model = baseModel;
	}

	public void init(DocumentWrapper  doc) {

		for(String attrib : indexAttributes) {
			BeanUtil.setPropertySilent(this, attrib, doc.get(attrib));
		}
	}

	public void init(Object[] dataArray) {

		this.setPrimaryKey((long) dataArray[0]);

		int i = 0;
		for(String attrib : model.getIndexAttributes()) {
			BeanUtil.setPropertySilent(this,attrib,dataArray[i++]);
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
		return model.getClassName();
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

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	public boolean equals(Object obj) {
		if(!(obj instanceof Data)) {
			return false;
		}
		Data data=(Data)obj;
		if(this.model != data.model) {
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

		if(this.model.hasGroupId() && !exactLongs(this.groupId,data.groupId)) {
			return false;
		}

		if(!exactLongs(this.createDate,data.createDate)) {
			return false;
		}

		if(!exactLongs(this.modifiedDate,data.modifiedDate)) {
			return false;
		}

		if(!exactIntegers(this.status,data.status)) {
			return false;
		}

		return true;
	}

	public static boolean exactIntegers(Integer i1, Integer i2) {
		if(i1 == null) {
			return (i1 == i2);
		}

		return i1.equals(i2);
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

	public String getAllData(String sep) {
		return this.getEntryClassName() + sep + companyId + sep + groupId + sep + entryClassPK + sep + primaryKey + sep + resourcePrimKey + sep + uid + sep + createDate + sep + modifiedDate + sep + status;
	}

	public String toString() {
		return this.getEntryClassName() + " " + entryClassPK + " " + primaryKey + " " + resourcePrimKey + " " + uid;
	}

	protected IndexCheckerModel model = null;
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
	protected Integer status = null;
}