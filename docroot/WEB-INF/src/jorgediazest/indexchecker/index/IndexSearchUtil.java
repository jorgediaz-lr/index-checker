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
import com.liferay.portal.kernel.search.ParseException;
import com.liferay.portal.kernel.search.SearchContext;
import com.liferay.portal.kernel.search.SearchEngineUtil;
import com.liferay.portal.kernel.search.SearchException;
import com.liferay.portal.kernel.search.TermRangeQuery;
import com.liferay.portal.kernel.search.TermRangeQueryFactoryUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jorgediazest.util.model.Model;

/**
 * @author Jorge Díaz
 */
public class IndexSearchUtil {

	public static Document[] executeSearch(
			SearchContext searchContext, BooleanQuery query, String startUID,
			int size)
		throws ParseException, SearchException {

		BooleanQuery contextQuery = BooleanQueryFactoryUtil.create(
			searchContext);

		contextQuery.add(query, BooleanClauseOccur.MUST);

		if (startUID != null) {
			TermRangeQuery termRangeQuery =
				TermRangeQueryFactoryUtil.create(
					searchContext, Field.UID, startUID, null, false, false);

			contextQuery.add(termRangeQuery, BooleanClauseOccur.MUST);
		}

		if (_log.isDebugEnabled()) {
			_log.debug("startUID: " + startUID);
			_log.debug("size: " + size);
			_log.debug("Executing search: " + contextQuery);
		}

		searchContext.setStart(0);
		searchContext.setEnd(size);

		Hits hits = SearchEngineUtil.search(searchContext, contextQuery);

		Document[] docs = hits.getDocs();

		if (_log.isDebugEnabled()) {
			_log.debug(docs.length + " hits returned");
		}

		return docs;
	}

	public static String getAttributeForDocument(
		Model model, String attribute) {

		if ("groupId".equals(attribute)) {
			attribute = "scopeGroupId";
		}
		else if ("modifiedDate".equals(attribute)) {
			attribute = "modified";
		}
		else if ("averageScore".equals(attribute)) {
			attribute = "ratings";
		}
		else if ("AssetEntries_AssetCategories.categoryId".equals(attribute)) {
			attribute = "assetCategoryIds";
		}
		else if ("AssetCategory.title".equals(attribute)) {
			attribute = "assetCategoryTitles";
		}
		else if ("AssetEntries_AssetTags.tagId".equals(attribute)) {
			attribute = "assetTagIds";
		}
		else if ("AssetTag.name".equals(attribute)) {
			attribute = "assetTagNames";
		}
		else if ("resourcePrimKey".equals(attribute) &&
				 model.isResourcedModel()) {

			attribute = "entryClassPK";
		}
		else if (model.getPrimaryKeyAttribute().equals(attribute) &&
				 !model.isResourcedModel()) {

			attribute = "entryClassPK";
		}

		return attribute;
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