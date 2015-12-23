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

package jorgediazest.indexchecker.index;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.search.BooleanQuery;
import com.liferay.portal.kernel.search.BooleanQueryFactoryUtil;
import com.liferay.portal.kernel.search.Document;
import com.liferay.portal.kernel.search.Field;
import com.liferay.portal.kernel.search.Hits;
import com.liferay.portal.kernel.search.SearchContext;
import com.liferay.portal.kernel.search.SearchEngineUtil;
import com.liferay.portal.kernel.search.SearchException;
import com.liferay.portal.kernel.util.PortalClassLoaderUtil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jorgediazest.indexchecker.data.Data;
import jorgediazest.indexchecker.model.IndexCheckerModel;

import jorgediazest.util.model.ModelUtil;
import jorgediazest.util.reflection.ReflectionUtil;

/**
 * @author Jorge Díaz
 */
public class IndexWrapperSearch extends IndexWrapper {

	public IndexWrapperSearch(long companyId) {
		this.companyId = companyId;
	}

	@Override
	public Set<Data> getClassNameData(IndexCheckerModel model) {

		Set<Data> indexData = new HashSet<Data>();

		SearchContext searchContext = new SearchContext();
		searchContext.setCompanyId(companyId);
		BooleanQuery contextQuery = BooleanQueryFactoryUtil.create(
			searchContext);
		contextQuery.addRequiredTerm(Field.COMPANY_ID, companyId);
		contextQuery.addRequiredTerm(
			Field.ENTRY_CLASS_NAME, model.getClassName());

		int indexSearchLimit = -1;

		try {
			indexSearchLimit = getIndexSearchLimit();

			Document[] docs = executeSearch(
				searchContext, contextQuery, 50000, 200000);

			if (docs != null) {
				for (int i = 0; i < docs.length; i++) {
					DocumentWrapper doc = new DocumentWrapperSearch(docs[i]);

					String entryClassName = doc.getEntryClassName();

					if ((entryClassName != null) &&
						entryClassName.equals(model.getClassName())) {

						Data data = new Data(model);
						data.init(doc);

						indexData.add(data);
					}
				}
			}
		}
		catch (Exception e) {
			_log.error("EXCEPTION: " + e.getClass() + " - " + e.getMessage(),e);
		}
		finally {
			if (indexSearchLimit != -1) {
				try {
					setIndexSearchLimit(indexSearchLimit);
				}
				catch (Exception e) {
					if (_log.isWarnEnabled()) {
						_log.warn(
							"Error restoring INDEX_SEARCH_LIMIT: " +
								e.getMessage(), e);
					}
				}
			}
		}

		return indexData;
	}

	@Override
	public Map<Long, Set<Data>> getClassNameDataByGroupId(
		IndexCheckerModel model) {

		Map<Long, Set<Data>> indexData = new HashMap<Long, Set<Data>>();

		SearchContext searchContext = new SearchContext();
		searchContext.setCompanyId(companyId);
		BooleanQuery contextQuery = BooleanQueryFactoryUtil.create(
			searchContext);
		contextQuery.addRequiredTerm(Field.COMPANY_ID, companyId);
		contextQuery.addRequiredTerm(
			Field.ENTRY_CLASS_NAME, model.getClassName());

		try {
			Hits hits = SearchEngineUtil.search(searchContext, contextQuery);

			Document[] docs = hits.getDocs();

			if (docs != null) {
				for (int i = 0; i < docs.length; i++) {
					DocumentWrapper doc = new DocumentWrapperSearch(docs[i]);

					String entryClassName = doc.getEntryClassName();

					if ((entryClassName != null) &&
						entryClassName.equals(model.getClassName())) {

						Data data = new Data(model);
						data.init(doc);

						Long groupId = data.getGroupId();

						Set<Data> indexDataSet = indexData.get(groupId);

						if (indexDataSet == null) {
							indexDataSet = new HashSet<Data>();
							indexData.put(groupId, indexDataSet);
						}

						indexDataSet.add(data);
					}
				}
			}
		}
		catch (Exception e) {
			_log.error(
				"EXCEPTION: " + e.getClass() + " - " + e.getMessage(), e);
		}

		return indexData;
	}

	@Override
	public Set<String> getTermValues(String term) {

		// TODO Pendiente

		Set<String> values = new HashSet<String>();
		values.add("Only implemented for 'Lucene' index wrapper");
		return values;
	}

	@Override
	public int numDocs() {

		// TODO Pendiente

		return -1;
	}

	protected Document[] executeSearch(
			SearchContext searchContext, BooleanQuery contextQuery, int start,
			int step)
		throws Exception, SearchException {

		for (int i = 0;; i++) {
			if (_log.isDebugEnabled()) {
				_log.debug("SetIndexSearchLimit: " + (start + step*i));
			}

			setIndexSearchLimit(start + step*i);

			Hits hits = SearchEngineUtil.search(searchContext, contextQuery);

			Document[] docs = hits.getDocs();

			if (docs.length < (start + step*i)) {
				return docs;
			}
		}
	}

	protected int getIndexSearchLimit() throws Exception {
		Class<?> propsValues =
			PortalClassLoaderUtil.getClassLoader().loadClass(
				"com.liferay.portal.util.PropsValues");

		java.lang.reflect.Field indexSearchLimitFiled =
			propsValues.getDeclaredField("INDEX_SEARCH_LIMIT");

		return (Integer)indexSearchLimitFiled.get(null);
	}

	protected void setIndexSearchLimit(int indexSearchLimit) throws Exception {
		Class<?> propsValues =
			PortalClassLoaderUtil.getClassLoader().loadClass(
				"com.liferay.portal.util.PropsValues");

		java.lang.reflect.Field indexSearchLimitField =
			propsValues.getDeclaredField("INDEX_SEARCH_LIMIT");

		ReflectionUtil.setFieldValue(
			null, indexSearchLimitField, indexSearchLimit);

		try {
			ClassLoader classLoader = ModelUtil.getClassLoader();

			Class<?> solrIndexSearcher = classLoader.loadClass(
				"com.liferay.portal.search.solr.SolrIndexSearcher");

			if (solrIndexSearcher != null) {
				java.lang.reflect.Field solrIndexSearchLimitField =
					solrIndexSearcher.getDeclaredField("INDEX_SEARCH_LIMIT");

				if (solrIndexSearchLimitField != null) {
					ReflectionUtil.setFieldValue(
						null, solrIndexSearchLimitField, indexSearchLimit);
				}
			}
		}
		catch (Exception e) {
			if (_log.isWarnEnabled()) {
				_log.warn(
					"EXCEPTION: " + e.getClass() + " - " + e.getMessage(), e);
			}
		}
	}

	private static Log _log = LogFactoryUtil.getLog(IndexWrapperSearch.class);

	private long companyId;

}