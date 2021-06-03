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

import jorgediazest.util.data.Data;

/**
 * @author Jorge Díaz
 */
public class DDLRecordPermissionsHelper extends IndexCheckerPermissionsHelper {

	@Override
	protected String getPermissionsClassName(Data data) {
		return "com.liferay.dynamic.data.lists.model.DDLRecordSet";
	}

	@Override
	protected long getPermissionsClassPK(Data data) {
		return data.get("recordSetId", -1L);
	}
}