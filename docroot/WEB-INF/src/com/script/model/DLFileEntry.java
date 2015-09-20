package com.script.model;

public class DLFileEntry extends BaseModel {

	public void init(String fullClassName, String tableName, String[] arrAttributes) {
		super.init(fullClassName, tableName, arrAttributes);

		this.tableName = "dlfileversion v, dlfileentry";
		conditions.add("status");
	}

	public String getSQLAttributes() {
		return super.getSQLAttributes() + ",status";
	}

	public String getSQLWhere() {
		return super.getSQLWhere() + " and t.fileentryid = v.fileentryid and t.version = v.version";
	}

}