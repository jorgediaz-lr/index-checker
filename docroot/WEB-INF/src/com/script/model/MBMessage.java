package com.script.model;

public class MBMessage extends BaseModel {

	public String getSQLWhere() {
		return super.getSQLWhere() + " and not (categoryid = -1 and parentmessageid = 0)";
	}
}