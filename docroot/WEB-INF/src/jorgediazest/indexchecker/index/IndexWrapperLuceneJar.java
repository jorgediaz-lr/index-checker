/**
 * Copyright (c) 2015-present Jorge Díaz All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.jorgediaz.indexchecker.index;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;

import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.IndexSearcher;

/**
 * @author Jorge Díaz
 */
public class IndexWrapperLuceneJar extends IndexWrapperLucene {

	public IndexWrapperLuceneJar(long companyId) {
		IndexSearcher indexSearcher;
		try {
			indexSearcher =
				(IndexSearcher)IndexWrapperLuceneReflection.getIndexSearcher(
					companyId);
		}
		catch (Exception e) {
			_log.error("Error: " + e.getClass() + " - " + e.getMessage(), e);
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
			_log.error("Error: " + e.getClass() + " - " + e.getMessage(), e);
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
		}
		catch (Exception e) {
			_log.error("Error: " + e.getClass() + " - " + e.getMessage(), e);
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

	private static Log _log = LogFactoryUtil.getLog(
		IndexWrapperLuceneJar.class);

}