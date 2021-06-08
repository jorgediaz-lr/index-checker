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

import com.liferay.message.boards.constants.MBCategoryConstants;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.PortalUtil;

import jorgediazest.util.data.Data;

/**
 * @author Jorge Díaz
 */
public class MBMessagePermissionsHelper extends IndexCheckerPermissionsHelper {

	@Override
	protected boolean isRelatedEntry(Data data) {
		long categoryId = data.get("categoryId", 0L);

		if (categoryId != MBCategoryConstants.DISCUSSION_CATEGORY_ID) {
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

}