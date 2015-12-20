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

package com.jorgediaz.indexchecker.model;

import com.jorgediaz.util.service.Service;

import com.liferay.portal.kernel.dao.orm.Criterion;

/**
 * @author Jorge Díaz
 */
public class WikiPage extends IndexCheckerModel {

	@Override
	public Criterion generateQueryFilter() {
		return this.generateCriterionFilter("head=true,redirectTitle=");
	}

	@Override
	public void init(
			String classPackageName, String classSimpleName, Service service)
		throws Exception {

		super.init(classPackageName, classSimpleName, service);

		this.setIndexPrimaryKey("nodeId");
	}

}