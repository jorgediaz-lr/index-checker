package com.script.model;


public class User extends BaseModel {

	public void init(String fullClassName, String tableName, String[] arrAttributes) {
		super.init(fullClassName, tableName, arrAttributes);

		conditions.remove("status");
	}

	public String getSQLWhere() {
		return super.getSQLWhere() + " and defaultuser = [$FALSE$]";
	}
}