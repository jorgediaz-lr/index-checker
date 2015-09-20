package com.script.model;

public class AssetEntry extends BaseModel {

	public void init(String fullClassName, String tableName, String[] arrAttributes) {
		super.init(fullClassName, tableName, arrAttributes);

		this.indexedModel = false;
	}
}