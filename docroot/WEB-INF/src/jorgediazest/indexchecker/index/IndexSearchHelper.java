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
import com.liferay.portal.kernel.search.BooleanClauseOccur;
import com.liferay.portal.kernel.search.BooleanQuery;
import com.liferay.portal.kernel.search.BooleanQueryFactoryUtil;
import com.liferay.portal.kernel.search.Document;
import com.liferay.portal.kernel.search.DocumentImpl;
import com.liferay.portal.kernel.search.Field;
import com.liferay.portal.kernel.search.Hits;
import com.liferay.portal.kernel.search.Indexer;
import com.liferay.portal.kernel.search.IndexerRegistryUtil;
import com.liferay.portal.kernel.search.ParseException;
import com.liferay.portal.kernel.search.QueryConfig;
import com.liferay.portal.kernel.search.SearchContext;
import com.liferay.portal.kernel.search.SearchEngineUtil;
import com.liferay.portal.kernel.search.SearchException;
import com.liferay.portal.kernel.search.Sort;
import com.liferay.portal.kernel.search.TermRangeQuery;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jorgediazest.util.data.Data;
import jorgediazest.util.data.DataUtil;

/**
 * @author Jorge Díaz
 */
public class IndexSearchHelper {

	public static void delete(Data value) throws SearchException {
		Object uid = value.get(Field.UID);

		if (uid == null) {
			return;
		}

		String className = value.getModel().getClassName();
		Indexer indexer = IndexerRegistryUtil.nullSafeGetIndexer(className);

		indexer.delete(value.getCompanyId(), uid.toString());
	}

	public static Map<Data, String> deleteAndCheck(
		Collection<Data> dataCollection) {

		Map<Data, String> errors = new HashMap<Data, String>();

		int i = 0;

		for (Data data : dataCollection) {
			/* Delete object from index */
			try {
				delete(data);

				if (_log.isDebugEnabled()) {
					_log.debug(
						"Deleting " + (i++) + " uid: " + data.get(Field.UID));
				}
			}
			catch (SearchException e) {
				errors.put(data, e.getClass() + " - " + e.getMessage());

				if (_log.isDebugEnabled()) {
					_log.debug(e.getClass() + " - " + e.getMessage(), e);
				}
			}

			/* Reindex object, perhaps we deleted it by mistake */
			try {
				reindex(data);
			}
			catch (Exception e) {
			}
		}

		return errors;
	}

	public static Document[] executeSearch(
			SearchContext searchContext, BooleanQuery query, Sort[] sorts,
			TermRangeQuery termRangeQuery, int size)
		throws ParseException, SearchException {

		BooleanQuery contextQuery = BooleanQueryFactoryUtil.create(
			searchContext);

		contextQuery.add(query, BooleanClauseOccur.MUST);

		if (termRangeQuery != null) {
			contextQuery.add(termRangeQuery, BooleanClauseOccur.MUST);
		}

		if (sorts.length > 0) {
			searchContext.setSorts(sorts);
		}

		if (_log.isDebugEnabled()) {
			_log.debug("size: " + size);
			_log.debug("Executing search: " + contextQuery);
		}

		searchContext.setStart(0);
		searchContext.setEnd(size);

		QueryConfig queryConfig = new QueryConfig();
		queryConfig.setHighlightEnabled(false);
		queryConfig.setScoreEnabled(false);
		searchContext.setQueryConfig(queryConfig);

		Hits hits = SearchEngineUtil.search(searchContext, contextQuery);

		Document[] docs = hits.getDocs();

		if (_log.isDebugEnabled()) {
			_log.debug(docs.length + " hits returned");
		}

		return docs;
	}

	public static long getIdFromUID(String strValue) {
		long id = -1;
		String[] uidArr = strValue.split("_");

		if ((uidArr != null) && (uidArr.length >= 3)) {
			int pos = uidArr.length-2;
			while ((pos > 0) && !"PORTLET".equals(uidArr[pos])) {
				pos = pos - 2;
			}

			if ((pos > 0) && "PORTLET".equals(uidArr[pos])) {
				id = DataUtil.castLong(uidArr[pos+1]);
			}
		}

		return id;
	}

	public static List<Map<Locale, String>> getLocalizedMap(
		Locale[] locales, Document doc, String attribute) {

		List<Map<Locale, String>> listValueMap =
			new ArrayList<Map<Locale, String>>();

		int pos = 0;
		while (true) {
			Map<Locale, String> valueMap = IndexSearchUtil.getLocalizedMap(
				locales, doc, attribute, pos++);

			if (valueMap.isEmpty()) {
				break;
			}

			listValueMap.add(valueMap);
		}

		return listValueMap;
	}

	public static Map<Data, String> reindex(Collection<Data> dataCollection) {

		Map<Data, String> errors = new HashMap<Data, String>();

		int i = 0;

		for (Data data : dataCollection) {
			try {
				reindex(data);

				if (_log.isDebugEnabled()) {
					_log.debug(
						"Reindexing " + (i++) + " pk: " + data.getPrimaryKey());
				}
			}
			catch (SearchException e) {
				errors.put(data, e.getClass() + " - " + e.getMessage());

				if (_log.isDebugEnabled()) {
					_log.debug(e.getClass() + " - " + e.getMessage(), e);
				}
			}
		}

		return errors;
	}

	public static void reindex(Data value) throws SearchException {
		String className = value.getModel().getClassName();
		Indexer indexer = IndexerRegistryUtil.nullSafeGetIndexer(className);

		indexer.reindex(className, value.getPrimaryKey());
	}

	protected static Map<Locale, String> getLocalizedMap(
		Locale[] locales, Document doc, String attribute, int pos) {

		Map<Locale, String> valueMap = new HashMap<Locale, String>();

		for (int i = 0; i<locales.length; i++) {
			String localizedFieldName = DocumentImpl.getLocalizedName(
				locales[i], attribute);

			if (!doc.hasField(localizedFieldName)) {
				continue;
			}

			String[] values = doc.getField(localizedFieldName).getValues();

			if (values.length >= (pos + 1)) {
				valueMap.put(locales[i], values[pos]);
			}
		}

		return valueMap;
	}

	private static Log _log = LogFactoryUtil.getLog(IndexSearchUtil.class);

}