package com.jorgediaz.indexchecker.index;

import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.IndexSearcher;
public class IndexWrapperLuceneJar extends IndexWrapperLucene {

	public IndexWrapperLuceneJar(long companyId) {
		IndexSearcher indexSearcher;
		try {
			indexSearcher = (IndexSearcher)IndexWrapperLuceneReflection.getIndexSearcher(companyId);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}

		index = indexSearcher.getIndexReader();
	}

	@Override
	public Set<String> getTermValues(String field) {

		Set<String> values = new HashSet<String>();
		try {
			TermEnum terms = ((IndexReader)index).terms(new Term(field));
			Term currTerm = terms.term();
			while ((currTerm != null) && currTerm.field().equals(field)) {
				values.add(currTerm.text());
				terms.next();
				currTerm = terms.term();
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}

		return values;
	}

	@Override
	public int numDocs() {
		if (index == null) {
			return -1;
		}

		return ((IndexReader)index).numDocs();
	}

	@Override
	protected DocumentWrapper document(int i) {
		if (index == null) {
			return null;
		}

		try {
			return new DocumentWrapperLucene(((IndexReader)index).document(i));
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	@Override
	protected boolean isDeleted(int i) {
		if (index == null) {
			return true;
		}

		return ((IndexReader)index).isDeleted(i);
	}

	@Override
	protected int maxDoc() {
		if (index == null) {
			return 0;
		}

		return ((IndexReader)index).maxDoc();
	}

}