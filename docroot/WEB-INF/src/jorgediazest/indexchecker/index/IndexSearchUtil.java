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
import com.liferay.portal.kernel.search.Document;
import com.liferay.portal.kernel.search.Hits;
import com.liferay.portal.kernel.search.SearchContext;
import com.liferay.portal.kernel.search.SearchEngineUtil;
import com.liferay.portal.kernel.search.SearchException;
import com.liferay.portal.kernel.util.PortalClassLoaderUtil;

import java.util.List;

import jorgediazest.indexchecker.model.IndexCheckerModel;

import jorgediazest.util.model.ModelUtil;
import jorgediazest.util.reflection.ReflectionUtil;

/**
 * @author Jorge Díaz
 */
public class IndexSearchUtil {

	public static void autoAdjustIndexSearchLimit(
		List<IndexCheckerModel> modelList) {

		try {
			int indexSearchLimit = Math.max(20000, getIndexSearchLimit());

			for (IndexCheckerModel m : modelList) {
				indexSearchLimit = Math.max(
					indexSearchLimit, (int)m.count() * 2);
			}

			setIndexSearchLimit(indexSearchLimit);
		}
		catch (Exception e) {
			_log.error(e, e);
		}
	}

	public static Document[] executeSearch(
			SearchContext searchContext, BooleanQuery contextQuery, int size,
			int step)
		throws SearchException {

		searchContext.setStart(0);

		for (int i = 0;; i++) {
			if (_log.isDebugEnabled()) {
				_log.debug("searchContext.setEnd: " + (size + step*i));
			}

			searchContext.setEnd(size + step*i);

			if (_log.isDebugEnabled()) {
				_log.debug("Executing search: " + contextQuery);
			}

			Hits hits = SearchEngineUtil.search(searchContext, contextQuery);

			Document[] docs = hits.getDocs();

			if (_log.isDebugEnabled()) {
				_log.debug(docs.length + " hits returned");
			}

			if (docs.length < (size + step*i)) {
				return docs;
			}
		}
	}

	public static int getIndexSearchLimit() {
		try {
			Class<?> propsValues =
				PortalClassLoaderUtil.getClassLoader().loadClass(
					"com.liferay.portal.util.PropsValues");

			java.lang.reflect.Field indexSearchLimitFiled =
				propsValues.getDeclaredField("INDEX_SEARCH_LIMIT");

			return (Integer)indexSearchLimitFiled.get(null);
		}
		catch (Throwable t) {
			_log.error("Error at getIndexSearchLimit: " + t);
			return -1;
		}
	}

	public static void setIndexSearchLimit(int indexSearchLimit) {

		try {
			if (_log.isDebugEnabled()) {
				_log.debug("SetIndexSearchLimit: " + indexSearchLimit);
			}

			Class<?> propsValues =
				PortalClassLoaderUtil.getClassLoader().loadClass(
					"com.liferay.portal.util.PropsValues");

			java.lang.reflect.Field indexSearchLimitField =
				propsValues.getDeclaredField("INDEX_SEARCH_LIMIT");

			ReflectionUtil.updateStaticFinalInt(
				indexSearchLimitField, indexSearchLimit);

			if (_log.isDebugEnabled()) {
				_log.debug(
					"New value of INDEX_SEARCH_LIMIT: " +
						getIndexSearchLimit());
			}
		}
		catch (Throwable t) {
			_log.error("Error at setIndexSearchLimit: " + t);
		}

		try {
			ClassLoader classLoader = ModelUtil.getClassLoader();

			Class<?> solrIndexSearcher = classLoader.loadClass(
				"com.liferay.portal.search.solr.SolrIndexSearcher");

			if (solrIndexSearcher != null) {
				java.lang.reflect.Field solrIndexSearchLimitField =
					solrIndexSearcher.getDeclaredField("INDEX_SEARCH_LIMIT");

				if (solrIndexSearchLimitField != null) {
					ReflectionUtil.updateStaticFinalInt(
						solrIndexSearchLimitField, indexSearchLimit);
				}
			}
		}
		catch (Throwable t) {
			if (_log.isDebugEnabled()) {
				_log.debug(
					"EXCEPTION: " + t.getClass() + " - " + t.getMessage());
			}
		}
	}

	private static Log _log = LogFactoryUtil.getLog(IndexSearchUtil.class);

}