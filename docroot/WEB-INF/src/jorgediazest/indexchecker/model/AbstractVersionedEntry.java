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
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.ProjectionFactoryUtil;
import com.liferay.portal.kernel.dao.orm.ProjectionList;
import com.liferay.portal.kernel.workflow.WorkflowConstants;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import jorgediazest.indexchecker.data.Data;
import jorgediazest.indexchecker.data.DataUtil;

import jorgediazest.util.model.Model;

/**
 * @author Jorge Díaz
 */
public abstract class AbstractVersionedEntry extends IndexCheckerModel {

	public Map<Long, Data> getLiferayData(Criterion filter, String className)
		throws Exception {

		Map<Long, Data> dataMapAux = super.getLiferayData(filter);

		Model modelVersion = this.getModelFactory().getModelObject(
			null, className);

		DynamicQuery queryModelVersion =
			modelVersion.getService().newDynamicQuery();

		ProjectionList projectionList = ProjectionFactoryUtil.projectionList();
		projectionList.add(
			modelVersion.getPropertyProjection(this.getPrimaryKeyAttribute()));
		projectionList.add(modelVersion.getPropertyProjection("status"));
		projectionList.add(modelVersion.getPropertyProjection("version"));

		queryModelVersion.setProjection(projectionList);

		queryModelVersion.add(filter);

		@SuppressWarnings("unchecked")
		List<Object[]> results =
			(List<Object[]>)modelVersion.getService().executeDynamicQuery(
				queryModelVersion);

		for (Object[] result : results) {
			long fileEntryId = (Long)result[0];
			int status = (Integer)result[1];
			String version = (String)result[2];

			Data data = dataMapAux.get(fileEntryId);

			if ((data == null) ||
				!DataUtil.exactStrings(version, data.getVersion())) {

				continue;
			}

			if ((status == WorkflowConstants.STATUS_APPROVED) ||
				(status == WorkflowConstants.STATUS_IN_TRASH)) {

				data.setStatus(status);
			}
		}

		Map<Long, Data> dataMap = new HashMap<Long, Data>();

		for (Entry<Long, Data> entry : dataMapAux.entrySet()) {
			if (entry.getValue().getStatus() != null) {
				dataMap.put(entry.getKey(), entry.getValue());
			}
		}

		return dataMap;
	}

}