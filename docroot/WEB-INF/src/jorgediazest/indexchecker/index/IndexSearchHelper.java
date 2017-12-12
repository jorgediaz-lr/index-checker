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

import com.liferay.portal.kernel.language.LanguageUtil;
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
import com.liferay.portal.kernel.search.TermRangeQueryFactoryUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.LocalizationUtil;
import com.liferay.portal.kernel.util.Validator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import jorgediazest.indexchecker.model.IndexCheckerQueryHelper;
import jorgediazest.indexchecker.util.ConfigurationUtil;
import jorgediazest.indexchecker.util.PortletPropsValues;

import jorgediazest.util.data.Data;
import jorgediazest.util.data.DataUtil;
import jorgediazest.util.model.Model;

/**
 * @author Jorge Díaz
 */
public class IndexSearchHelper {

	public void delete(Data value) throws SearchException {
		Object uid = value.get(Field.UID);

		if (uid == null) {
			return;
		}

		String className = value.getEntryClassName();
		Indexer indexer = IndexerRegistryUtil.nullSafeGetIndexer(className);

		indexer.delete(value.getCompanyId(), uid.toString());
	}

	public Map<Data, String> deleteAndCheck(Collection<Data> dataCollection) {
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

	public void fillDataObject(Data data, String[] attributes, Document doc) {
		data.set(Field.UID, doc.getUID());

		Locale[] locales = LanguageUtil.getAvailableLocales();
		Locale siteLocale = LocaleUtil.getSiteDefault();

		for (String attribute : attributes) {
			String attrDoc = ConfigurationUtil.getIndexAttributeName(
				data.getModel(), attribute);

			List<Map<Locale, String>> listValueMap = null;

			Class<?> typeClass = data.getAttributeClass(attribute);

			if (typeClass.equals(String.class) ||
				typeClass.equals(Object.class)) {

				listValueMap = getLocalizedMap(locales, doc, attrDoc);
			}

			if ((listValueMap != null) && !listValueMap.isEmpty()) {
				String[] xml = new String[listValueMap.size()];

				int pos = 0;

				for (Map<Locale, String> valueMap : listValueMap) {
					xml[pos++] = LocalizationUtil.updateLocalization(
						valueMap, "", "data",
						LocaleUtil.toLanguageId(siteLocale));
				}

				data.set(attribute, xml);
			}
			else if (doc.hasField(attrDoc)) {
				data.set(attribute, doc.getField(attrDoc).getValues());
			}
		}
	}

	public Set<Data> getIndexData(
			Model model, Set<Model> relatedModels,
			Set<String> indexAttributesToQuery, long companyId,
			List<Long> groupIds, Date startModifiedDate, Date endModifiedDate)
		throws ParseException, SearchException {

		SearchContext searchContext = getIndexSearchContext(model, companyId);

		BooleanQuery query = getIndexQuery(
			model, groupIds, startModifiedDate, endModifiedDate, searchContext);

		String[] sortAttributes = {"createDate", "modifiedDate"};

		Sort[] sorts = getIndexSorting(model, sortAttributes);

		return getIndexData(
			model, relatedModels, indexAttributesToQuery.toArray(new String[0]),
			sorts, searchContext, query);
	}

	public Set<Data> getIndexData(
			Model model, Set<Model> relatedModels, String[] attributes,
			Sort[] sorts, SearchContext searchContext, BooleanQuery query)
		throws ParseException, SearchException {

		int indexSearchLimit = PortletPropsValues.INDEX_SEARCH_LIMIT;

		int size = Math.min((int)model.count() * 2, indexSearchLimit);

		Set<Data> indexData = new HashSet<Data>();

		TermRangeQuery termRangeQuery = null;

		do {
			Document[] docs = executeSearch(
				searchContext, query, sorts, termRangeQuery, size);

			if ((docs == null) || (docs.length == 0)) {
				break;
			}

			for (Document doc : docs) {
				String entryClassName = doc.get(Field.ENTRY_CLASS_NAME);

				if ((entryClassName == null) ||
					!entryClassName.equals(model.getClassName())) {

					_log.error("Wrong entryClassName: " + entryClassName);

					continue;
				}

				Data data = new Data(model);

				data.addModelTableInfo(relatedModels);

				fillDataObject(data, attributes, doc);

				indexData.add(data);
			}

			termRangeQuery = getTermRangeQuery(
				docs[docs.length - 1], termRangeQuery, sorts, searchContext);
		}
		while (termRangeQuery != null);

		return indexData;
	}

	public void postProcessData(Data data) {
		Object treePath = data.get("treePath");

		treePath = IndexCheckerQueryHelper.processTreePath(treePath);

		data.set("treePath", treePath);
	}

	public Map<Data, String> reindex(Collection<Data> dataCollection) {

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

	public void reindex(Data value) throws SearchException {
		String className = value.getEntryClassName();
		Indexer indexer = IndexerRegistryUtil.nullSafeGetIndexer(className);

		indexer.reindex(className, value.getPrimaryKey());
	}

	protected Document[] executeSearch(
			SearchContext searchContext, BooleanQuery query, Sort[] sorts,
			TermRangeQuery termRangeQuery, int size)
		throws ParseException, SearchException {

		BooleanQuery mainQuery = BooleanQueryFactoryUtil.create(searchContext);

		mainQuery.add(query, BooleanClauseOccur.MUST);

		if (termRangeQuery != null) {
			mainQuery.add(termRangeQuery, BooleanClauseOccur.MUST);
		}

		if (sorts.length > 0) {
			searchContext.setSorts(sorts);
		}

		if (_log.isDebugEnabled()) {
			_log.debug("size: " + size);
			_log.debug("Executing search: " + mainQuery);
		}

		searchContext.setStart(0);
		searchContext.setEnd(size);

		QueryConfig queryConfig = new QueryConfig();
		queryConfig.setHighlightEnabled(false);
		queryConfig.setScoreEnabled(false);

		mainQuery.setQueryConfig(queryConfig);
		searchContext.setQueryConfig(queryConfig);

		Hits hits = SearchEngineUtil.search(searchContext, mainQuery);

		Document[] docs = hits.getDocs();

		if (_log.isDebugEnabled()) {
			_log.debug(docs.length + " hits returned");
		}

		return docs;
	}

	protected TermRangeQuery getDateTermRangeQuery(
		SearchContext searchContext, Model model, String field, Date startDate,
		Date endDate) {

		String lowerTerm = null;
		String upperTerm = null;

		if (startDate != null) {
			lowerTerm = DataUtil.dateToString(startDate);
		}

		if (endDate != null) {
			upperTerm = DataUtil.dateToString(endDate);
		}

		field = ConfigurationUtil.getIndexAttributeName(model, field);

		return TermRangeQueryFactoryUtil.create(
			searchContext, field, lowerTerm, upperTerm, true, true);
	}

	protected long getIdFromUID(String strValue) {
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

	protected BooleanQuery getIndexQuery(
			Model model, List<Long> groupIds, Date startModifiedDate,
			Date endModifiedDate, SearchContext searchContext)
		throws ParseException {

		BooleanQuery query = BooleanQueryFactoryUtil.create(searchContext);
		query.addRequiredTerm(Field.ENTRY_CLASS_NAME, model.getClassName());

		if (model.hasAttribute("groupId") && (groupIds != null)) {
			BooleanQuery groupQuery = BooleanQueryFactoryUtil.create(
				searchContext);

			for (Long groupId : groupIds) {
				groupQuery.addTerm(Field.SCOPE_GROUP_ID, groupId);
			}

			query.add(groupQuery, BooleanClauseOccur.MUST);
		}

		if (model.hasAttribute("modifiedDate") &&
			((startModifiedDate != null) || (endModifiedDate != null))) {

			TermRangeQuery termRangeQuery = getDateTermRangeQuery(
					searchContext, model, "modifiedDate", startModifiedDate,
					endModifiedDate);

			query.add(termRangeQuery, BooleanClauseOccur.MUST);
		}

		return query;
	}

	protected SearchContext getIndexSearchContext(Model model, long companyId) {
		SearchContext searchContext = new SearchContext();
		searchContext.setCompanyId(companyId);
		searchContext.setEntryClassNames(new String[] {model.getClassName()});

		return searchContext;
	}

	protected Sort[] getIndexSorting(Model model, String[] attributes) {
		List<String> sortAttributesList = new ArrayList<String>();

		for (String attribute : attributes) {
			if (model.hasAttribute(attribute)) {
				String sortableFieldName =
					ConfigurationUtil.getIndexAttributeName(model, attribute);

				sortAttributesList.add(sortableFieldName);
			}
		}

		Sort[] sorts = new Sort[sortAttributesList.size()];

		for (int i = 0; i<sortAttributesList.size(); i++) {
			sorts[i] = new Sort(
				sortAttributesList.get(i), Sort.LONG_TYPE, false);
		}

		return sorts;
	}

	protected List<Map<Locale, String>> getLocalizedMap(
		Locale[] locales, Document doc, String attribute) {

		List<Map<Locale, String>> listValueMap =
			new ArrayList<Map<Locale, String>>();

		int pos = 0;
		while (true) {
			Map<Locale, String> valueMap = getLocalizedMap(
				locales, doc, attribute, pos++);

			if (valueMap.isEmpty()) {
				break;
			}

			listValueMap.add(valueMap);
		}

		return listValueMap;
	}

	protected TermRangeQuery getTermRangeQuery(
		Document lastDocument, TermRangeQuery previousTermRangeQuery,
		Sort[] sorts, SearchContext searchContext) {

		for (Sort sort : sorts) {
			String fieldName = sort.getFieldName();
			String lowerTerm = lastDocument.get(fieldName);

			if (Validator.isNull(lowerTerm)) {
				continue;
			}

			if (_log.isDebugEnabled()) {
				_log.debug("fieldName=" + fieldName);
				_log.debug("lowerTerm=" + lowerTerm);
			}

			boolean includesLower = true;

			if ((previousTermRangeQuery != null) &&
				fieldName.equals(previousTermRangeQuery.getField())) {

				includesLower = !lowerTerm.equals(
					previousTermRangeQuery.getLowerTerm());
			}

			return TermRangeQueryFactoryUtil.create(
					searchContext, fieldName, lowerTerm, null, includesLower,
					true);
		}

		return null;
	}

	private Map<Locale, String> getLocalizedMap(
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

	private static Log _log = LogFactoryUtil.getLog(IndexSearchHelper.class);

}