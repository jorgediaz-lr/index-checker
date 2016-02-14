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

package jorgediazest.indexchecker.portlet;

import java.util.Set;
import java.util.concurrent.Callable;

import jorgediazest.indexchecker.ExecutionMode;
import jorgediazest.indexchecker.data.Results;
import jorgediazest.indexchecker.model.IndexCheckerModel;

/**
 * @author Jorge Díaz
 */
public class CallableCheckGroupAndModel implements Callable<Results> {

	CallableCheckGroupAndModel(
		long companyId, long groupId, IndexCheckerModel model,
		Set<ExecutionMode> executionMode) {

		this.companyId = companyId;
		this.groupId = groupId;
		this.model = model;
		this.executionMode = executionMode;
	}

	@Override
	public Results call() throws Exception {

		return IndexCheckerPortlet.executeCheckGroupAndModel(
			companyId, groupId, model, executionMode);
	}

	private long companyId = -1;
	private Set<ExecutionMode> executionMode = null;
	private long groupId = -1;
	private IndexCheckerModel model = null;

}