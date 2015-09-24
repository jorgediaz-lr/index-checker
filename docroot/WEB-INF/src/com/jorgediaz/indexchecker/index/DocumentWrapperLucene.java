package com.jorgediaz.indexchecker.index;

import org.apache.lucene.document.Document;
public class DocumentWrapperLucene extends DocumentWrapper {

	public DocumentWrapperLucene(Object document) {
		super(document);
	}

	public String get(String key) {
		return ((Document)document).get(key);
	}
}