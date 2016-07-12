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

import com.liferay.portal.NoSuchResourceException;
import com.liferay.portal.kernel.dao.orm.Criterion;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.search.BooleanQuery;
import com.liferay.portal.kernel.search.BooleanQueryFactoryUtil;
import com.liferay.portal.kernel.search.Document;
import com.liferay.portal.kernel.search.Field;
import com.liferay.portal.kernel.search.Indexer;
import com.liferay.portal.kernel.search.IndexerRegistryUtil;
import com.liferay.portal.kernel.search.SearchContext;
import com.liferay.portal.kernel.search.SearchException;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.LocalizationUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portal.model.Group;
import com.liferay.portal.model.ResourceConstants;
import com.liferay.portal.model.Role;
import com.liferay.portal.model.RoleConstants;
import com.liferay.portal.security.permission.ActionKeys;
import com.liferay.portal.security.permission.ResourceActionsUtil;
import com.liferay.portal.security.permission.ResourceBlockIdsBag;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.ResourceBlockLocalServiceUtil;
import com.liferay.portal.service.ResourceBlockPermissionLocalServiceUtil;
import com.liferay.portal.service.ResourcePermissionLocalServiceUtil;
import com.liferay.portal.service.RoleLocalServiceUtil;

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

	public void addPermissionsData(Data data) throws Exception {

		String className = getPermissionsClassName(data);
		long classPK = getPermissionsClassPK(data);

		if (Validator.isNull(classPK) || (classPK <= 0) ||
			Validator.isNull(className)) {

			return;
		}

		Indexer indexer = IndexerRegistryUtil.getIndexer(className);

		if (!indexer.isPermissionAware()) {
			return;
		}

		long companyId = data.getCompanyId();
		long groupId = 0;

		Group group = GroupLocalServiceUtil.fetchGroup(data.get("groupId", 0L));

		if (group != null) {
			group = getSiteGroup(group);

			groupId = group.getGroupId();
		}

		List<Role> roles = ListUtil.copy(
			ResourceActionsUtil.getRoles(companyId, group, className, null));

		if (groupId > 0) {
			List<Role> teamRoles = RoleLocalServiceUtil.getTeamRoles(groupId);

			roles.addAll(teamRoles);
		}

		boolean[] hasResourcePermissions = hasResourcePermissions(
				companyId, groupId, className, classPK, roles);

		Set<String> roleIds = new HashSet<String>();
		Set<String> groupRoleIds = new HashSet<String>();

		for (int i = 0; i < hasResourcePermissions.length; i++) {
			if (!hasResourcePermissions[i]) {
				continue;
			}

			Role role = roles.get(i);

			if ((role.getType() == RoleConstants.TYPE_ORGANIZATION) ||
				(role.getType() == RoleConstants.TYPE_SITE)) {

				groupRoleIds.add(groupId + StringPool.DASH + role.getRoleId());
			}
			else {
				roleIds.add(Long.toString(role.getRoleId()));
			}
		}

		data.set("roleId", roleIds);
		data.set("groupRoleId", groupRoleIds);
	}

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
		Locale siteLocale = LocaleUtil.getSiteDefault();

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

	public Criterion generateQueryFilter() {
		if (!this.isWorkflowEnabled()) {
			return null;
		}

		return this.generateCriterionFilter(
			"status=" + WorkflowConstants.STATUS_APPROVED +"+" +
			"status=" + WorkflowConstants.STATUS_IN_TRASH);
	}

	public Map<Long, Data> getData(
		String[] attributesModel, String[] attributesRelated, Criterion filter)
	throws Exception {

		Map<Long, Data> dataMap = super.getData(
			attributesModel, attributesRelated, filter);

		for (Data data : dataMap.values()) {
			try {
				addPermissionsData(data);
			}
			catch (NoSuchResourceException nsre) {
			}
			catch (Exception e) {
				_log.error(e, e);
			}
		}

		return dataMap;
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

	protected String getPermissionsClassName(Data data) {
		return data.getEntryClassName();
	}

	protected long getPermissionsClassPK(Data data) {
		if (data.getModel().isResourcedModel()) {
			return data.getResourcePrimKey();
		}

		return data.getPrimaryKey();
	}

	protected Group getSiteGroup(Group group) {
		try {
			if (group.isLayout()) {
				return group.getParentGroup();
			}
		}
		catch (Exception e) {
			if (_log.isDebugEnabled()) {
				_log.debug(e, e);
			}
		}

		return group;
	}

	protected boolean[] hasResourcePermissions(
			long companyId, long groupId, String className, long classPK,
			List<Role> roles)
		throws PortalException, SystemException {

		long[] roleIdsArray = new long[roles.size()];

		for (int i = 0; i < roleIdsArray.length; i++) {
			Role role = roles.get(i);

			roleIdsArray[i] = role.getRoleId();
		}

		boolean[] hasResourcePermissions = null;

		if (ResourceBlockLocalServiceUtil.isSupported(className)) {
			ResourceBlockIdsBag resourceBlockIdsBag =
				ResourceBlockLocalServiceUtil.getResourceBlockIdsBag(
					companyId, groupId, className, roleIdsArray);

			long actionId = ResourceBlockLocalServiceUtil.getActionId(
				className, ActionKeys.VIEW);

			List<Long> resourceBlockIds =
				resourceBlockIdsBag.getResourceBlockIds(actionId);

			hasResourcePermissions = new boolean[roleIdsArray.length];

			for (long resourceBlockId : resourceBlockIds) {
				for (int i = 0; i < roleIdsArray.length; i++) {
					int count =
						ResourceBlockPermissionLocalServiceUtil.
							getResourceBlockPermissionsCount(
								resourceBlockId, roleIdsArray[i]);

					hasResourcePermissions[i] = (count > 0);
				}
			}
		}
		else {
			hasResourcePermissions =
				ResourcePermissionLocalServiceUtil.hasResourcePermissions(
					companyId, className, ResourceConstants.SCOPE_INDIVIDUAL,
					Long.toString(classPK), roleIdsArray, ActionKeys.VIEW);
		}

		return hasResourcePermissions;
	}

	private static Log _log = LogFactoryUtil.getLog(IndexCheckerModel.class);

}