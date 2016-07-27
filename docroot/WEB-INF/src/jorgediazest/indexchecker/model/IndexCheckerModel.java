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
import com.liferay.portal.kernel.workflow.WorkflowConstants;

import jorgediazest.util.model.ModelImpl;
import jorgediazest.util.service.Service;
public class IndexCheckerModel extends ModelImpl {

	public Criterion generateQueryFilter() {
		if (!isWorkflowEnabled()) {
			return null;
		}

		return generateCriterionFilter(
			"status=" + WorkflowConstants.STATUS_APPROVED +"+" +
			"status=" + WorkflowConstants.STATUS_IN_TRASH);
	}

	public void init(
		String classPackageName, String classSimpleName, Service service)
	throws Exception {
		super.init(classPackageName, classSimpleName, service);

		setFilter(generateQueryFilter());
	}

}