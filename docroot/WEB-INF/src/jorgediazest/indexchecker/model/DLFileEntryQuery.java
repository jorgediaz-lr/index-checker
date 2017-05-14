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
import com.liferay.portal.model.Repository;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.documentlibrary.model.DLFolder;

import java.util.Map;

import jorgediazest.util.data.Data;

/**
 * @author Jorge Díaz
 */
public class DLFileEntryQuery extends IndexCheckerModelQuery {

	protected void addPermissionFields(Map<Long, Data> dataMap)
		throws Exception, PortalException, SystemException {

		addRelatedModelData(
			dataMap, Repository.class.getName(),
			"repositoryId,repoFolderId=dlFolderId".split(","),
			"repositoryId".split(","), false, false);

		addRelatedModelData(
			dataMap, DLFolder.class.getName(),
			"folderId,hiddenFolder=hidden".split(","),
			"repoFolderId=folderId".split(","), false, false);

		super.addPermissionFields(dataMap);
	}

	@Override
	protected String getPermissionsClassName(Data data) {

		boolean hidden = data.get("hiddenFolder", false);

		if (!hidden) {
			return super.getPermissionsClassName(data);
		}

		long classNameId = data.get("classNameId", 0L);

		String permissionsClassName = null;

		if (classNameId != 0) {
			permissionsClassName = PortalUtil.getClassName(classNameId);
		}

		for (String validPermissionsClassName : validPermissionClassNames) {
			if (validPermissionsClassName.equals(permissionsClassName)) {
				return permissionsClassName;
			}
		}

		return super.getPermissionsClassName(data);
	}

	@Override
	protected long getPermissionsClassPK(Data data) {

		String permissionsClassName = this.getPermissionsClassName(data);

		if ("com.liferay.portlet.documentlibrary.model.DLFileEntry".equals(
				permissionsClassName)) {

			return super.getPermissionsClassPK(data);
		}

		return data.get("classPK", -1L);
	}

	protected static String[] validPermissionClassNames = new String[] {
		"com.liferay.portlet.blogs.model.BlogsEntry",
		"com.liferay.portlet.documentlibrary.model.DLFileEntry",
		"com.liferay.portlet.messageboards.model.MBMessage",
		"com.liferay.portlet.wiki.model.WikiPage"};

}