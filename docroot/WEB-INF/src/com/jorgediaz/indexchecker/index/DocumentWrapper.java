package com.jorgediaz.indexchecker.index;

public abstract class DocumentWrapper {

	public String getEntryClassName() {
		return get("entryClassName");
	}

	abstract public String get(String key); public DocumentWrapper(Object document) {
		this.document = document;
	}

	protected Object document = null;

}