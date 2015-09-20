package com.jorgediaz.indexchecker.index;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;

public class IndexWrapperLucene extends IndexWrapper {

	public IndexWrapperLucene(long companyId) {
		IndexSearcher indexSearcher;
		try {
			indexSearcher = (IndexSearcher) IndexWrapperLuceneReflection.getIndexSearcher(companyId);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		index = indexSearcher.getIndexReader();
	}

	public int numDocs() {
		if(index == null) {
			return 0;
		}
		return ((IndexReader)index).numDocs();
	}

	public int maxDoc() {
		if(index == null) {
			return 0;
		}
		return ((IndexReader)index).maxDoc();
	}

	public boolean isDeleted(int i) {
		if(index == null) {
			return true;
		}
		return ((IndexReader)index).isDeleted(i);
	}

	public DocumentWrapper document(int i) {
		if(index == null) {
			return null;
		}
		try {
			return new DocumentWrapperLucene(((IndexReader)index).document(i));
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

}
