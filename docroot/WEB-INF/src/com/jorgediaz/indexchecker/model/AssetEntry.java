package com.jorgediaz.indexchecker.model;

import com.test.ModelUtil;

public class AssetEntry extends BaseModel {

	public void init(ModelUtil modelutil, String fullClassName) throws Exception {
		super.init(modelutil, fullClassName);

		this.indexedModel = false;
	}
}