package com.jorgediaz.indexchecker.model;

import com.test.ModelUtil;

import java.util.HashSet;
import java.util.Set;

public class DLFileEntry extends DefaultStatusModel {

	Set<String> attrAux = new HashSet<String>();

	public void init(ModelUtil modelUtil, String fullClassName) throws Exception {
		super.init(modelUtil, fullClassName);

		attrAux.addAll(this.attributes);
		attrAux.add("status");

		this.tableName = "dlfileversion v, dlfileentry";
	}

	public Set<String> getAttributes() {
		return attrAux;
	}

	public String getSQLAttributes() {
		return super.getSQLAttributes() + ",status";
	}

	public String getSQLWhere() {
		return super.getSQLWhere() + " and t.fileentryid = v.fileentryid and t.version = v.version";
	}

}