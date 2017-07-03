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

import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.messageboards.model.MBCategoryConstants;

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

	protected static String[] validPermissionClassNames = new String[] {
		"com.liferay.portlet.blogs.model.BlogsEntry",
		"com.liferay.portlet.documentlibrary.model.DLFileEntry",
		"com.liferay.portlet.messageboards.model.MBMessage",
		"com.liferay.portlet.wiki.model.WikiPage"};

}