package com.jorgediaz.indexchecker.model;

public class MBMessage extends DefaultStatusModel {

	public String getSQLWhere() {
		return super.getSQLWhere() + " and not (categoryid = -1 and parentmessageid = 0)";
	}
}