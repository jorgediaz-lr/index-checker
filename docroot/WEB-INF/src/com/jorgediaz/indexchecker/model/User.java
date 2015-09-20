package com.jorgediaz.indexchecker.model;

import com.test.ModelUtil;


public class User extends BaseModel {

	public void init(ModelUtil modelUtil, String fullClassName) throws Exception {
		super.init(modelUtil, fullClassName);

		attributes.remove("createDate");
	}

	public String getSQLWhere() {
		return super.getSQLWhere() + " and defaultuser = [$FALSE$]";
	}
}