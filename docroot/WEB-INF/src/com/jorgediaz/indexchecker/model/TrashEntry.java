package com.jorgediaz.indexchecker.model;

import com.test.ModelUtil;

public class TrashEntry extends BaseModel {

	public void init(ModelUtil modelUtil, String fullClassName) throws Exception {
		super.init(modelUtil, fullClassName);

		this.indexedModel = false;
	}
}