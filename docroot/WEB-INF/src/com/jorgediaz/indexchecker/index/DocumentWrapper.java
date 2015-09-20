package com.jorgediaz.indexchecker.index;

public abstract class DocumentWrapper {

	protected Object document = null;

	public DocumentWrapper(Object document) {
		this.document = document;
	}

	public String getEntryClassName() {
		return get("entryClassName");
	}

	abstract public String get(String key);
}
