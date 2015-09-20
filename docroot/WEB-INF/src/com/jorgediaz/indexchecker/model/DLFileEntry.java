package com.jorgediaz.indexchecker.model;

import java.util.HashSet;
import java.util.Set;

public class DLFileEntry extends DefaultStatusModel {

	Set<String> attrAux = new HashSet<String>();

	public void init(String fullClassName, String tableName, String[] arrAttributes) {
		super.init(fullClassName, tableName, arrAttributes);

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