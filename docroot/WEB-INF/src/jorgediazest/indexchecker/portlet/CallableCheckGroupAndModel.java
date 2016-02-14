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

import com.liferay.portal.kernel.dao.orm.Criterion;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

import jorgediazest.indexchecker.ExecutionMode;
import jorgediazest.indexchecker.data.Data;
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

		Results result = null;
		
		try {
			if (_log.isInfoEnabled()) {
				_log.info(
					"Model: " + model.getName() + " - CompanyId: " +
						companyId + " - GroupId: " + groupId);
			}
		
			if ((groupId == 0L) &&
				model.hasAttribute("groupId") &&
				executionMode.contains(ExecutionMode.GROUP_BY_SITE)) {
		
				return null;
			}
		
			if ((groupId != 0L) &&
				!model.hasAttribute("groupId") &&
				executionMode.contains(ExecutionMode.GROUP_BY_SITE)) {
		
				return null;
			}
		
			Criterion filter = model.getCompanyGroupFilter(companyId, groupId);
		
			Set<Data> liferayData = new HashSet<Data>(
				model.getLiferayData(filter).values());
		
			Set<Data> indexData;
		
			if (executionMode.contains(ExecutionMode.SHOW_INDEX) ||
				!liferayData.isEmpty()) {
		
				indexData = model.getIndexData(companyId, groupId);
			}
			else {
				indexData = new HashSet<Data>();
			}
		
			result = Results.getIndexCheckResult(
					model, liferayData, indexData, executionMode);
		}
		catch (Exception e) {
			_log.error(
				"Model: " + model.getName() + " EXCEPTION: " +
					e.getClass() + " - " + e.getMessage(),e);
		}
		
		return result;
	}

	private long companyId = -1;
	private Set<ExecutionMode> executionMode = null;
	private long groupId = -1;
	private IndexCheckerModel model = null;

	private static Log _log = LogFactoryUtil.getLog(CallableCheckGroupAndModel.class);

}