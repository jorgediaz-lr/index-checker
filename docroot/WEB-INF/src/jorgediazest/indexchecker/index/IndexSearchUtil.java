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

import jorgediazest.util.model.ModelUtil;
import jorgediazest.util.reflection.ReflectionUtil;

/**
 * @author Jorge Díaz
 */
public class IndexSearchUtil {

	public static Document[] executeSearch(
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

	public static int getIndexSearchLimit() throws Exception {
		Class<?> propsValues =
			PortalClassLoaderUtil.getClassLoader().loadClass(
				"com.liferay.portal.util.PropsValues");

		java.lang.reflect.Field indexSearchLimitFiled =
			propsValues.getDeclaredField("INDEX_SEARCH_LIMIT");

		return (Integer)indexSearchLimitFiled.get(null);
	}

	public static void setIndexSearchLimit(int indexSearchLimit)
		throws Exception {

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
					"EXCEPTION: " + e.getClass() + " - " + e.getMessage());
			}
		}
	}

	private static Log _log = LogFactoryUtil.getLog(IndexSearchUtil.class);

}