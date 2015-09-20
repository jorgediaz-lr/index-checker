package com.jorgediaz.indexchecker.model;


public class User extends BaseModel {

	public void init(String fullClassName, String tableName, String[] arrAttributes) {
		super.init(fullClassName, tableName, arrAttributes);

		attributes.remove("createDate");
	}

	public String getSQLWhere() {
		return super.getSQLWhere() + " and defaultuser = [$FALSE$]";
	}
}