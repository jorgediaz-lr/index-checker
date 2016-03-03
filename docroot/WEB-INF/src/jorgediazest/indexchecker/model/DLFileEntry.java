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
import com.liferay.portlet.documentlibrary.model.DLFileVersion;

import java.util.Map;

import jorgediazest.util.data.Data;

/**
 * @author Jorge Díaz
 */
public class DLFileEntry extends AbstractVersionedEntry {

	public Map<Long, Data> getData(String[] attributes, Criterion filter)
		throws Exception {

		return getData(attributes, filter, DLFileVersion.class.getName());
	}

}