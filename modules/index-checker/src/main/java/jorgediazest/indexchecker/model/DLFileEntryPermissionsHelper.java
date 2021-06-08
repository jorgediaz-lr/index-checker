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

import com.liferay.document.library.kernel.model.DLFolder;
import com.liferay.document.library.kernel.service.DLFolderLocalServiceUtil;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Repository;
import com.liferay.portal.kernel.service.RepositoryLocalServiceUtil;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.PortalUtil;

import jorgediazest.util.data.Data;

/**
 * @author Jorge Díaz
 */
public class DLFileEntryPermissionsHelper
	extends IndexCheckerPermissionsHelper {

	@Override
	protected boolean isRelatedEntry(Data data) {
		long groupId = data.getGroupId();
		long repositoryId = data.get("repositoryId", -1L);

		boolean hiddenFolder = _isHiddenFolder(groupId, repositoryId);

		if (!hiddenFolder) {
			return false;
		}

		long classNameId = data.get("classNameId", 0L);

		if (classNameId == 0) {
			return false;
		}

		String permissionsClassName = PortalUtil.getClassName(classNameId);

		return ArrayUtil.contains(
			validPermissionClassNames, permissionsClassName);
	}

	protected static String[] validPermissionClassNames = {
		"com.liferay.blogs.model.BlogsEntry",
		"com.liferay.document.library.kernel.model.DLFileEntry",
		"com.liferay.message.boards.kernel.model.MBMessage",
		"com.liferay.message.boards.model.MBMessage",
		"com.liferay.wiki.model.WikiPage"
	};

	private boolean _isHiddenFolder(long groupId, long repositoryId) {
		try {
			if (groupId == repositoryId) {
				return false;
			}

			Repository repository = RepositoryLocalServiceUtil.fetchRepository(
				repositoryId);

			if (repository == null) {
				return false;
			}

			DLFolder dlFolder = DLFolderLocalServiceUtil.fetchDLFolder(
				repository.getDlFolderId());

			if (dlFolder == null) {
				return false;
			}

			return dlFolder.isHidden();
		}
		catch (SystemException se) {
			_log.error(se);

			return false;
		}
	}

	private static Log _log = LogFactoryUtil.getLog(
		DLFileEntryPermissionsHelper.class);

}