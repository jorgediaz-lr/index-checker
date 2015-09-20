package com.script.data;

import com.script.index.DocumentWrapper;


public class Data implements Comparable<Data> {
			
	public void init(DocumentWrapper  doc, String fullClassName) {
		
		this.setUid(doc.get("uid"));
		this.createDate = doc.get("createDate");
		this.modifyDate = doc.get("modifyDate");
		this.entryClassName = fullClassName;
		try {
			this.setEntryClassPK(Long.valueOf(doc.get("entryClassPK")));
		}
		catch(Exception e) {
			this.entryClassPK = -1;
		}
		try {
			this.status = Long.valueOf(doc.get("status"));
		}
		catch(Exception e) {
			this.status = -1;
		}
		try {
			this.companyId = Long.valueOf(doc.get("companyId"));
		}
		catch(Exception e) {
			this.companyId = -1;
		}
		try {
			this.groupId = Long.valueOf(doc.get("groupId"));
		}
		catch(Exception e) {
			this.groupId = -1;
		}
	}

	public void setEntryClassPK(long entryClassPK) {
		this.entryClassPK = entryClassPK;

		if(primaryKey != -1 && resourcePrimaryKey == -1 && primaryKey != entryClassPK) {
			this.resourcePrimaryKey = entryClassPK;
		}
	}

	public void setUid(String uid) {
		this.uid = uid;

		try {
			this.primaryKey=Long.valueOf(uid.split("_")[2]);
		}
		catch(Exception e) {
			
		}

		if(resourcePrimaryKey == -1 && primaryKey != entryClassPK) {
			this.resourcePrimaryKey = entryClassPK;
		}
	}

	public boolean equals(Object obj) {
		if(!(obj instanceof Data)) {
			return false;
		}
		Data data=(Data)obj;
		if(this.primaryKey != -1 && data.primaryKey != -1) {
			return (this.primaryKey == data.primaryKey);
		}
		else if(this.resourcePrimaryKey != -1 && data.resourcePrimaryKey != -1) {
			return (this.resourcePrimaryKey == data.resourcePrimaryKey);
		}
		else {
			return super.equals(obj);
		}
	}

	public int hashCode() {
		if(this.primaryKey != -1) {
			return entryClassName.hashCode() * Long.valueOf(this.primaryKey).hashCode();
		}
		else if(this.resourcePrimaryKey != -1) {
			return -1 * entryClassName.hashCode() * Long.valueOf(this.resourcePrimaryKey).hashCode();
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
		else if(this.resourcePrimaryKey != -1 && data.resourcePrimaryKey != -1) {
			return Long.compare(this.resourcePrimaryKey,data.resourcePrimaryKey);
		}
		else {
			return 0;
		}
	}

	public String toString() {
		return entryClassName+ " - " + entryClassPK + " - " + primaryKey + " - " + resourcePrimaryKey + " - " + uid;
	}

	public String entryClassName = null;
	/* Liferay */
	public long primaryKey = -1;
	public long resourcePrimaryKey = -1;
	/* Index */
	public long entryClassPK = -1;
	public String uid = null;
	/* Comun */
	public long companyId = -1;
	public long groupId = -1;
	public String createDate = null;
	public String modifyDate = null;
	public long status = -1;
}