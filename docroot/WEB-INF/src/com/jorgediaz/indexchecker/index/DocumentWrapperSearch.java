package com.jorgediaz.indexchecker.index;

import com.liferay.portal.kernel.search.Document;
public class DocumentWrapperSearch extends DocumentWrapper {

	public DocumentWrapperSearch(Object document) {
		super(document);
	}

	public String get(String key) {
		return ((Document)document).get(key);
	}

}