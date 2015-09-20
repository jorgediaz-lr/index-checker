package com.jorgediaz.indexchecker.model;

public class WikiPage extends DefaultStatusModel {
	
	public void init(String fullClassName, String tableName, String[] arrAttributes) {
		super.init(fullClassName, tableName, arrAttributes);

		this.primaryKey = "nodeId";
		this.attributes.add(this.primaryKey);
	}

	public String getSQLWhere() {
		return super.getSQLWhere() + " and head = [$TRUE$] and redirecttitle = ''";
	}
}