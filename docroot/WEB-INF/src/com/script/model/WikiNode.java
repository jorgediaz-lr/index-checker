package com.script.model;


public class WikiNode extends BaseModel {

	public void init(String fullClassName, String tableName, String[] arrAttributes) {
		super.init(fullClassName, tableName, arrAttributes);

		conditions.remove("status");
	}
}