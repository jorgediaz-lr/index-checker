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

import com.jorgediaz.indexchecker.data.Data;

import com.liferay.portal.kernel.dao.orm.Criterion;
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.ProjectionFactoryUtil;
import com.liferay.portal.kernel.dao.orm.ProjectionList;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portlet.documentlibrary.model.DLFileVersion;

import java.util.List;
import java.util.Map;

/**
 * @author Jorge Díaz
 */
public class DLFileEntry extends IndexCheckerModel {

	public Map<Long, Data> getLiferayData(Criterion filter) throws Exception {
		Map<Long, Data> dataMap = super.getLiferayData(filter);

		DynamicQuery queryDLFileVersion = this.newDynamicQuery(
			DLFileVersion.class);

		ProjectionList projectionList = ProjectionFactoryUtil.projectionList();
		projectionList.add(this.getPropertyProjection("fileEntryId"));
		projectionList.add(this.getPropertyProjection("status"));

		queryDLFileVersion.setProjection(projectionList);

		queryDLFileVersion.add(filter);

		@SuppressWarnings("unchecked")
		List<Object[]> results =
			(List<Object[]>)this.executeDynamicQuery(
				DLFileVersion.class, queryDLFileVersion);

		for (Object[] result : results) {
			long fileEntryId = (Long)result[0];
			int status = (Integer)result[1];

			if ((status == WorkflowConstants.STATUS_APPROVED) ||
				(status == WorkflowConstants.STATUS_IN_TRASH)) {

				dataMap.get(fileEntryId).setStatus(status);
			}
			else {
				dataMap.remove(fileEntryId);
			}
		}

		return dataMap;
	}

}