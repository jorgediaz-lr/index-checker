package com.jorgediaz.indexchecker.model;

import com.test.ModelUtil;

public class WikiPage extends DefaultStatusModel {
	
	public void init(ModelUtil modelUtil, String fullClassName) throws Exception {
		super.init(modelUtil, fullClassName);

		this.primaryKey = "nodeId";
		this.attributes.add(this.primaryKey);
	}

	public String getSQLWhere() {
		return super.getSQLWhere() + " and head = [$TRUE$] and redirecttitle = ''";
	}
}