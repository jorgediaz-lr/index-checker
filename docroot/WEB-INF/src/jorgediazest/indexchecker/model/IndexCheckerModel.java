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

package jorgediazest.indexchecker.model;

import com.liferay.portal.kernel.dao.orm.Criterion;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.search.BooleanQuery;
import com.liferay.portal.kernel.search.BooleanQueryFactoryUtil;
import com.liferay.portal.kernel.search.Document;
import com.liferay.portal.kernel.search.Field;
import com.liferay.portal.kernel.search.SearchContext;
import com.liferay.portal.kernel.search.SearchException;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.LocalizationUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.workflow.WorkflowConstants;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import jorgediazest.indexchecker.index.IndexSearchUtil;

import jorgediazest.util.data.Data;
import jorgediazest.util.model.Model;
import jorgediazest.util.model.ModelFactory.DataComparatorFactory;
import jorgediazest.util.model.ModelImpl;
import jorgediazest.util.service.Service;

/**
 * @author Jorge Díaz
 */
public class IndexCheckerModel extends ModelImpl {

	public void delete(Data value) throws SearchException {
		Object uid = value.get("uid");

		if (uid == null) {
			return;
		}

		getIndexerNullSafe().delete(value.getCompanyId(), uid.toString());
	}

	public Map<Data, String> deleteAndCheck(Collection<Data> dataCollection) {

		Map<Data, String> errors = new HashMap<Data, String>();

		if (_log.isDebugEnabled()) {
			_log.debug(
				"Deleting " + dataCollection.size() + " objects of type " +
					this.getClassName());
		}

		int i = 0;

		for (Data data : dataCollection) {
			/* Delete object from index */
			try {
				this.delete(data);

				if (_log.isDebugEnabled()) {
					_log.debug(
						"Deleting " + (i++) + " uid: " + data.get("uid"));
				}
			}
			catch (SearchException e) {
				errors.put(data, e.getClass() + " - " + e.getMessage());

				if (_log.isDebugEnabled()) {
					_log.debug(e.getClass() + " - " + e.getMessage(), e);
				}
			}

			/* Reindex object, perhaps we deleted it by error */
			try {
				this.reindex(data);
			}
			catch (Exception e) {
			}
		}

		return errors;
	}

	public void fillDataObject(Data data, String[] attributes, Document doc) {
		data.set("uid", doc.getUID());

		Locale[] locales = LanguageUtil.getAvailableLocales();
		Locale defaultLocale = LocaleUtil.getDefault();

		for (String attribute : attributes) {
			String attrDoc = IndexSearchUtil.getAttributeForDocument(
				this, attribute);

			List<Map<Locale, String>> listValueMap = null;

			Class<?> typeClass = data.getAttributeTypeClass(attribute);

			if (typeClass.equals(String.class) ||
				typeClass.equals(Object.class)) {

				listValueMap = IndexSearchUtil.getLocalizedMap(
					locales, doc, attrDoc);
			}

			if ((listValueMap != null) && !listValueMap.isEmpty()) {
				String[] xml = new String[listValueMap.size()];

				int pos = 0;

				for (Map<Locale, String> valueMap : listValueMap) {
					xml[pos++] = updateLocalization(
						valueMap, "", "data",
						LocaleUtil.toLanguageId(defaultLocale));
				}

				data.set(attribute, xml);
			}
			else if (doc.getFields().containsKey(attrDoc)) {
				data.set(attribute, doc.getValues(attrDoc));
			}
		}
	}

	public String updateLocalization(
		Map<Locale, String> titleMap, String xml, String key,
		String defaultLanguageId) {

		if (titleMap == null) {
			return xml;
		}

		Locale[] locales = LanguageUtil.getAvailableLocales();

		for (Locale locale : locales) {
			String title = titleMap.get(locale);

			xml = updateLocalization(
					title, xml, key, locale, defaultLanguageId);
		}
		return xml;
	}

	public String updateLocalization(
		String title, String xml, String key, Locale locale,
		String defaultLanguageId) {

		String languageId = LocaleUtil.toLanguageId(locale);

		if (Validator.isNotNull(title)) {
			return LocalizationUtil.updateLocalization(
					xml, key, title, languageId, defaultLanguageId);
		}
		else {
			return LocalizationUtil.removeLocalization(xml, key, languageId);
		}
	}

	public Criterion generateQueryFilter() {
		if (!this.isWorkflowEnabled()) {
			return null;
		}

		return this.generateCriterionFilter(
			"status=" + WorkflowConstants.STATUS_APPROVED);
	}

	public Set<Data> getIndexData(
			Set<Model> relatedModels, String[] attributes,
			SearchContext searchContext, BooleanQuery contextQuery)
		throws SearchException {

		int size = Math.max((int)this.count() * 2, 50000);

		Document[] docs = IndexSearchUtil.executeSearch(
			searchContext, contextQuery, size, 50000);

		Set<Data> indexData = new HashSet<Data>();

		if (docs != null) {
			for (int i = 0; i < docs.length; i++) {
				Data data = new Data(this, this.dataComparator);

				data.addModelTableInfo(relatedModels);

				fillDataObject(data, attributes, docs[i]);

				indexData.add(data);
			}
		}

		return indexData;
	}

	public BooleanQuery getIndexQuery(
		long groupId, SearchContext searchContext) {

		BooleanQuery contextQuery = BooleanQueryFactoryUtil.create(
			searchContext);
		contextQuery.addRequiredTerm(
			Field.ENTRY_CLASS_NAME, this.getClassName());

		if (groupId != 0) {
			contextQuery.addRequiredTerm(Field.SCOPE_GROUP_ID, groupId);
		}

		return contextQuery;
	}

	public SearchContext getIndexSearchContext(long companyId) {
		SearchContext searchContext = new SearchContext();
		searchContext.setCompanyId(companyId);
		searchContext.setEntryClassNames(new String[] {this.getClassName()});
		return searchContext;
	}

	@Override
	public void init(
			String classPackageName, String classSimpleName, Service service,
			DataComparatorFactory dataComparatorFactory)
		throws Exception {

		super.init(
			classPackageName, classSimpleName, service, dataComparatorFactory);

		this.setFilter(this.generateQueryFilter());
	}

	public Map<Data, String> reindex(Collection<Data> dataCollection) {

		Map<Data, String> errors = new HashMap<Data, String>();

		if (_log.isDebugEnabled()) {
			_log.debug(
				"Reindexing " + dataCollection.size() + " objects of type " +
					this.getClassName());
		}

		int i = 0;

		for (Data data : dataCollection) {
			try {
				this.reindex(data);

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
		getIndexerNullSafe().reindex(getClassName(), value.getPrimaryKey());
	}

	private static Log _log = LogFactoryUtil.getLog(IndexCheckerModel.class);

}