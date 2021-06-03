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
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.ResourceAction;
import com.liferay.portal.kernel.model.ResourceConstants;
import com.liferay.portal.kernel.model.ResourcePermission;
import com.liferay.portal.kernel.model.Role;
import com.liferay.portal.kernel.model.RoleConstants;
import com.liferay.portal.kernel.search.Indexer;
import com.liferay.portal.kernel.search.IndexerRegistryUtil;
import com.liferay.portal.kernel.security.permission.ActionKeys;
import com.liferay.portal.kernel.service.GroupLocalServiceUtil;
import com.liferay.portal.kernel.service.ResourceActionLocalServiceUtil;
import com.liferay.portal.kernel.service.RoleLocalServiceUtil;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.util.Validator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jorgediazest.util.data.Data;
import jorgediazest.util.model.Model;
import jorgediazest.util.model.ModelFactory;
public class IndexCheckerPermissionsHelper {

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

		long groupId = 0L;

		Object groupIdObj = data.get("groupId", 0L);

		if (groupIdObj instanceof Number) {
			groupId = ((Number)groupIdObj).longValue();

			Group group = GroupLocalServiceUtil.fetchGroup(groupId);

			if ((group != null) && group.isLayout()) {
				groupId = group.getParentGroupId();
			}
		}

		data.set("permissionsClassName", className);
		data.set("permissionsClassPK", classPK);
		data.set("permissionsGroupId", groupId);
	}

	public void addRolesFields(Data data)
		throws PortalException, SystemException {

		String className = data.get("permissionsClassName", StringPool.BLANK);

		addRolesFieldsToData(className, data, ResourcePermission.class.getName());
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
					roleIds.add(String.valueOf(roleId));
				}
			}
		}

		ModelFactory modelFactory = data.getModel().getModelFactory();

		Model permissionsModel = modelFactory.getModelObject(
			permissionsClassName);

		data.addModelTableInfo(permissionsModel);

		data.set("permissionsRoleId", roleIds);
		data.set("permissionsGroupRoleId", groupRoleIds);
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
		if (isRelatedEntry(data)) {
			long classNameId = data.get("classNameId", 0L);

			return PortalUtil.getClassName(classNameId);
		}

		return data.getEntryClassName();
	}

	protected long getPermissionsClassPK(Data data) {
		if (isRelatedEntry(data)) {
			return data.get("classPK", -1L);
		}

		if (data.getModel().isResourcedModel()) {
			return data.getResourcePrimKey();
		}

		return data.getPrimaryKey();
	}

	protected boolean isRelatedEntry(Data data) {
		return false;
	}

	protected Map<String, Long> cacheActionIdBitwiseValue =
		new HashMap<String, Long>();

	private static Log _log = LogFactoryUtil.getLog(
		IndexCheckerPermissionsHelper.class);

}