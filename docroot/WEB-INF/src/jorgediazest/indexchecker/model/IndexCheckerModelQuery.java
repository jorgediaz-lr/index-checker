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
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.Layout;
import com.liferay.portal.kernel.model.ResourceAction;
import com.liferay.portal.kernel.model.ResourceBlock;
import com.liferay.portal.kernel.model.ResourceBlockPermission;
import com.liferay.portal.kernel.model.ResourceConstants;
import com.liferay.portal.kernel.model.ResourcePermission;
import com.liferay.portal.kernel.model.Role;
import com.liferay.portal.kernel.model.RoleConstants;
import com.liferay.portal.kernel.search.BooleanClauseOccur;
import com.liferay.portal.kernel.search.BooleanQuery;
import com.liferay.portal.kernel.search.BooleanQueryFactoryUtil;
import com.liferay.portal.kernel.search.Document;
import com.liferay.portal.kernel.search.Field;
import com.liferay.portal.kernel.search.Indexer;
import com.liferay.portal.kernel.search.IndexerRegistryUtil;
import com.liferay.portal.kernel.search.ParseException;
import com.liferay.portal.kernel.search.SearchContext;
import com.liferay.portal.kernel.search.SearchException;
import com.liferay.portal.kernel.search.Sort;
import com.liferay.portal.kernel.search.TermRangeQuery;
import com.liferay.portal.kernel.search.TermRangeQueryFactoryUtil;
import com.liferay.portal.kernel.security.permission.ActionKeys;
import com.liferay.portal.kernel.service.ResourceActionLocalServiceUtil;
import com.liferay.portal.kernel.service.ResourceBlockLocalServiceUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.LocalizationUtil;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;

import java.util.ArrayList;
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
import jorgediazest.util.modelquery.ModelQuery;
import jorgediazest.util.modelquery.ModelQueryImpl;
public class IndexCheckerModelQuery extends ModelQueryImpl {

	public void addPermissionsClassNameGroupIdFields(
			Map<Long, Data> groupMap, Data data)
		throws SystemException {

		String className = getPermissionsClassName(data);
		long classPK = getPermissionsClassPK(data);

		if (Validator.isNull(classPK) || Validator.isNull(className) ||
			(classPK <= 0)) {

			return;
		}

		Indexer<?> indexer = IndexerRegistryUtil.getIndexer(className);

		if (!indexer.isPermissionAware()) {
			return;
		}

		long groupId = data.get("groupId", 0L);

		Data group = groupMap.get(groupId);

		if (group != null) {
			long layoutClassNameId = PortalUtil.getClassNameId(Layout.class);

			if (group.get("classNameId", -1L) == layoutClassNameId) {
				groupId = group.get("parentGroupId", groupId);
			}

			data.set("permissionsGroupId", groupId);
		}

		data.set("permissionsClassName", className);
		data.set("permissionsClassPK", classPK);
	}

	public void delete(Data value) throws SearchException {
		Object uid = value.get(Field.UID);

		if (uid == null) {
			return;
		}

		getModel().getIndexerNullSafe().delete(
			value.getCompanyId(), uid.toString());
	}

	public Map<Data, String> deleteAndCheck(Collection<Data> dataCollection) {

		Map<Data, String> errors = new HashMap<Data, String>();

		if (_log.isDebugEnabled()) {
			_log.debug(
				"Deleting " + dataCollection.size() + " objects of type " +
					getModel().getClassName());
		}

		int i = 0;

		for (Data data : dataCollection) {
			/* Delete object from index */
			try {
				this.delete(data);

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
				this.reindex(data);
			}
			catch (Exception e) {
			}
		}

		return errors;
	}

	public void fillDataObject(Data data, String[] attributes, Document doc) {
		data.set(Field.UID, doc.getUID());

		Locale[] locales = LanguageUtil.getAvailableLocales().toArray(new Locale[0]);
		Locale siteLocale = LocaleUtil.getSiteDefault();

		for (String attribute : attributes) {
			String attrDoc = IndexSearchUtil.getAttributeForDocument(
				getModel(), attribute);

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

	public Map<Long, Data> getData(
		String[] attributesModel, String[] attributesRelated, Criterion filter)
	throws Exception {

		Map<Long, Data> dataMap = super.getData(
			attributesModel, attributesRelated, filter);

		addPermissionFields(dataMap);

		return dataMap;
	}

	public Set<Data> getIndexData(
		Set<Model> relatedModels, String[] attributes,
		SearchContext searchContext, BooleanQuery contextQuery)
	throws ParseException, SearchException {

		String[] sortAttributes = {"createDate", "modifiedDate"};

		Sort[] sorts = getIndexSorting(sortAttributes);

		return getIndexData(
			relatedModels, attributes, sorts, searchContext, contextQuery);
	}

	public Set<Data> getIndexData(
			Set<Model> relatedModels, String[] attributes, Sort[] sorts,
			SearchContext searchContext, BooleanQuery contextQuery)
		throws ParseException, SearchException {

		int size = Math.min((int)getModel().count() * 2, 10000);

		Set<Data> indexData = new HashSet<Data>();

		TermRangeQuery termRangeQuery = null;

		do {
			Document[] docs = IndexSearchUtil.executeSearch(
				searchContext, contextQuery, sorts, termRangeQuery, size);

			if ((docs == null) || (docs.length == 0)) {
				break;
			}

			for (Document doc : docs) {
				String entryClassName = doc.get(Field.ENTRY_CLASS_NAME);

				if ((entryClassName == null) ||
					!entryClassName.equals(getModel().getClassName())) {

					_log.error("Wrong entryClassName: " + entryClassName);

					continue;
				}

				Data data = new Data(getModel(), this.dataComparator);

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

	public BooleanQuery getIndexQuery(
			List<Long> groupIds, SearchContext searchContext)
		throws ParseException {

		BooleanQuery query = BooleanQueryFactoryUtil.create(searchContext);
		query.addRequiredTerm(
			Field.ENTRY_CLASS_NAME, getModel().getClassName());

		if (getModel().hasAttribute("groupId") && (groupIds != null)) {
			BooleanQuery groupQuery = BooleanQueryFactoryUtil.create(
				searchContext);

			for (Long groupId : groupIds) {
				groupQuery.addTerm(Field.SCOPE_GROUP_ID, groupId);
			}

			query.add(groupQuery, BooleanClauseOccur.MUST);
		}

		return query;
	}

	public SearchContext getIndexSearchContext(long companyId) {
		SearchContext searchContext = new SearchContext();
		searchContext.setCompanyId(companyId);
		searchContext.setEntryClassNames(
			new String[] {getModel().getClassName()});

		return searchContext;
	}

	public Sort[] getIndexSorting(String[] attributes) {
		List<String> sortAttributesList = new ArrayList<String>();

		for (String attribute : attributes) {
			if (model.hasAttribute(attribute)) {
				String sortableFieldName =
					IndexSearchUtil.getAttributeForDocument(model, attribute);

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

	public boolean hasActionId(long actionIds, String name, String actionId)
		throws PortalException {

		Long bitwiseValue = getActionIdBitwiseValue(name, actionId);

		if (Validator.isNull(bitwiseValue)) {
			return false;
		}

		if ((actionIds & bitwiseValue) == bitwiseValue) {
			return true;
		}
		else {
			return false;
		}
	}

	public Map<Data, String> reindex(Collection<Data> dataCollection) {

		Map<Data, String> errors = new HashMap<Data, String>();

		if (_log.isDebugEnabled()) {
			_log.debug(
				"Reindexing " + dataCollection.size() + " objects of type " +
					getModel().getClassName());
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
		getModel().getIndexerNullSafe().reindex(
			getModel().getClassName(), value.getPrimaryKey());
	}

	protected void addPermissionFields(Map<Long, Data> dataMap)
		throws Exception, PortalException, SystemException {

		ModelQuery groupQuery = mqFactory.getModelQueryObject(Group.class);

		Map<Long, Data> groupMap = groupQuery.getDataWithCache(
			"pk,classNameId,parentGroupId".split(","));

		for (Data data : dataMap.values()) {
			addPermissionsClassNameGroupIdFields(groupMap, data);
		}

		addRelatedModelData(
			dataMap, ResourcePermission.class.getName(),
			" =primKey,roleId,actionIds,scope".split(","),
			"permissionsClassPK=primKey".split(","), false, true);

		addRelatedModelData(
			dataMap, ResourceBlock.class.getName(),
			" =groupId, =name,resourceBlockId".split(","),
			"permissionsGroupId=groupId,permissionsClassName=name".split(","),
			false, false);

		addRelatedModelData(
			dataMap, ResourceBlockPermission.class.getName(),
			" =resourceBlockId,roleId,actionIds".split(","),
			"resourceBlockId".split(","), false, true);

		ModelQuery roleQuery = mqFactory.getModelQueryObject(Role.class);

		Map<Long, Data> roleMap = roleQuery.getDataWithCache(
			"pk,type".split(","));

		for (Data data : dataMap.values()) {
			addRolesFieldsToData(roleMap, data);
		}
	}

	protected void addRolesFieldsToData(Map<Long, Data> roleMap, Data data)
		throws PortalException {

		String className = data.get("permissionsClassName", StringPool.BLANK);

		String permissionsField;

		if (ResourceBlockLocalServiceUtil.isSupported(className)) {
			permissionsField = ResourceBlockPermission.class.getName();
		}
		else {
			permissionsField = ResourcePermission.class.getName();
		}

		addRolesFieldsToData(roleMap, className, data, permissionsField);
	}

	@SuppressWarnings("unchecked")
	protected void addRolesFieldsToData(
			Map<Long, Data> roleMap, String className, Data data,
			String permissionsField)
		throws PortalException {

		String actionId = getPermissionsActionId(data);

		Object aux = data.get(permissionsField);

		Set<List<Object>> resourcePermissions = null;

		if (aux instanceof List) {
			resourcePermissions = new HashSet<List<Object>>();
			resourcePermissions.add((List<Object>)aux);
		}
		else if (aux instanceof Set) {
			resourcePermissions = (Set<List<Object>>)aux;
		}

		if (resourcePermissions == null) {
			return;
		}

		Set<String> roleIds = new HashSet<String>();
		Set<String> groupRoleIds = new HashSet<String>();

		for (List<Object> resourcePermission : resourcePermissions) {
			long roleId = (Long)resourcePermission.get(0);
			long actionIds = (Long)resourcePermission.get(1);

			if (resourcePermission.size() > 2) {
				int scope = (Integer)resourcePermission.get(2);

				if (scope != ResourceConstants.SCOPE_INDIVIDUAL) {
					continue;
				}
			}

			if (hasActionId(actionIds, className, actionId)) {
				Data role = roleMap.get(roleId);

				if (role == null) {
					continue;
				}

				long groupId = data.get("permissionsGroupId", 0L);

				int type = role.get("type", -1);

				if ((type == RoleConstants.TYPE_ORGANIZATION) ||
					(type == RoleConstants.TYPE_SITE)) {

					groupRoleIds.add(groupId + StringPool.DASH + roleId);
				}
				else {
					roleIds.add(Long.toString(roleId));
				}
			}
		}

		data.set("roleId", roleIds);
		data.set("groupRoleId", groupRoleIds);
	}

	protected long getActionIdBitwiseValue(String name, String actionId)
		throws PortalException {

		String key = name + "_" + actionId;

		Long bitwiseValue = cacheActionIdBitwiseValue.get(key);

		if (bitwiseValue == null) {
			ResourceAction resourceAction =
				ResourceActionLocalServiceUtil.fetchResourceAction(
					name, actionId);

			if (resourceAction == null) {
				bitwiseValue = 0L;
			}
			else {
				bitwiseValue = resourceAction.getBitwiseValue();
			}

			cacheActionIdBitwiseValue.put(key, bitwiseValue);
		}

		return bitwiseValue;
	}

	protected String getPermissionsActionId(Data data) {
		return ActionKeys.VIEW;
	}

	protected String getPermissionsClassName(Data data) {
		return data.getEntryClassName();
	}

	protected long getPermissionsClassPK(Data data) {
		if (data.getModel().isResourcedModel()) {
			return data.getResourcePrimKey();
		}

		return data.getPrimaryKey();
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

	protected Map<String, Long> cacheActionIdBitwiseValue =
		new HashMap<String, Long>();

	private static Log _log = LogFactoryUtil.getLog(
		IndexCheckerModelQuery.class);

}