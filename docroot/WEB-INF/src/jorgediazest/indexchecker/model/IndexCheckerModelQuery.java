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

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
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
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.LocalizationUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.model.Group;
import com.liferay.portal.model.ResourceAction;
import com.liferay.portal.model.ResourceBlockPermission;
import com.liferay.portal.model.ResourceConstants;
import com.liferay.portal.model.ResourcePermission;
import com.liferay.portal.model.Role;
import com.liferay.portal.model.RoleConstants;
import com.liferay.portal.security.permission.ActionKeys;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.ResourceActionLocalServiceUtil;
import com.liferay.portal.service.ResourceBlockLocalServiceUtil;
import com.liferay.portal.service.RoleLocalServiceUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import jorgediazest.indexchecker.index.IndexSearchUtil;
import jorgediazest.indexchecker.util.ConfigurationUtil;

import jorgediazest.util.data.Data;
import jorgediazest.util.model.Model;
import jorgediazest.util.model.ModelFactory;
import jorgediazest.util.modelquery.ModelQueryImpl;
public class IndexCheckerModelQuery extends ModelQueryImpl {

	public void addPermissionsClassNameGroupIdFields(Data data)
		throws SystemException {

		String className = getPermissionsClassName(data);
		long classPK = getPermissionsClassPK(data);

		if (Validator.isNull(classPK) || Validator.isNull(className) ||
			(classPK <= 0)) {

			return;
		}

		Indexer indexer = IndexerRegistryUtil.getIndexer(className);

		if (!indexer.isPermissionAware()) {
			return;
		}

		long groupId = data.get("groupId", 0L);

		Group group = GroupLocalServiceUtil.fetchGroup(groupId);

		if ((group != null) && group.isLayout()) {
			groupId = group.getParentGroupId();
		}

		data.set("permissionsClassName", className);
		data.set("permissionsClassPK", classPK);
		data.set("permissionsGroupId", groupId);
	}

	public void addRolesFields(Data data)
		throws PortalException, SystemException {

		String className = data.get("permissionsClassName", StringPool.BLANK);

		String permissionsClassName;

		if (ResourceBlockLocalServiceUtil.isSupported(className)) {
			permissionsClassName = ResourceBlockPermission.class.getName();
		}
		else {
			permissionsClassName = ResourcePermission.class.getName();
		}

		addRolesFieldsToData(className, data, permissionsClassName);
	}

	public void fillDataObject(Data data, String[] attributes, Document doc) {
		data.set(Field.UID, doc.getUID());

		Locale[] locales = LanguageUtil.getAvailableLocales();
		Locale siteLocale = LocaleUtil.getSiteDefault();

		for (String attribute : attributes) {
			String attrDoc = ConfigurationUtil.getIndexAttributeName(
				data.getModel(), attribute);

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

		Model model = getModel();

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

	protected static TermRangeQuery getTermRangeQuery(
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

	@SuppressWarnings("unchecked")
	protected void addRolesFieldsToData(
			String className, Data data, String permissionsClassName)
		throws PortalException, SystemException {

		String actionId = getPermissionsActionId(data);

		Object aux = data.get(permissionsClassName);

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

		Set<Long> roleIds = new HashSet<Long>();
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
				Role role = RoleLocalServiceUtil.fetchRole(roleId);

				if (role == null) {
					continue;
				}

				long groupId = data.get("permissionsGroupId", 0L);

				int type = role.getType();

				if ((type == RoleConstants.TYPE_ORGANIZATION) ||
					(type == RoleConstants.TYPE_SITE)) {

					groupRoleIds.add(groupId + StringPool.DASH + roleId);
				}
				else {
					roleIds.add(roleId);
				}
			}
		}

		data.set("roleId", roleIds);
		data.set("groupRoleId", groupRoleIds);

		ModelFactory modelFactory = model.getModelFactory();

		Model permissionsModel = modelFactory.getModelObject(
			permissionsClassName);

		data.addModelTableInfo(permissionsModel);
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

	protected Map<String, Long> cacheActionIdBitwiseValue =
		new HashMap<String, Long>();

	private static Log _log = LogFactoryUtil.getLog(
		IndexCheckerModelQuery.class);

}