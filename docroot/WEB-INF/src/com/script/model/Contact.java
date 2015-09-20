package com.script.model;


public class Contact extends BaseModel {

	public String getSQLWhere() {
		return super.getSQLWhere() + " and classPK not in (select userId from User_ where defaultuser = [$TRUE$] or status != 0)";
	}
}