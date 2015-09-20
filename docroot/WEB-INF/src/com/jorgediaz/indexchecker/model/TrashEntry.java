package com.jorgediaz.indexchecker.model;

public class TrashEntry extends BaseModel {

	public void init(String fullClassName, String tableName, String[] arrAttributes) {
		super.init(fullClassName, tableName, arrAttributes);

		this.indexedModel = false;
	}
}