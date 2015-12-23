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

import java.util.HashMap;
import java.util.Map;

import jorgediazest.indexchecker.data.Data;

/**
 * @author Jorge Díaz
 */
public class NotIndexed extends IndexCheckerModel {

	@Override
	public Map<Long, Data> getLiferayData(Criterion filter) throws Exception {
		/* Return empty data as it is not indexed */
		return new HashMap<Long, Data>();
	}

}