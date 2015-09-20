package com.jorgediaz.indexchecker.model;

import com.jorgediaz.indexchecker.data.Data;
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.ProjectionFactoryUtil;
import com.liferay.portal.kernel.dao.orm.ProjectionList;
import com.liferay.portal.kernel.dao.orm.Property;
import com.liferay.portal.kernel.dao.orm.PropertyFactoryUtil;
import com.liferay.portal.kernel.dao.orm.RestrictionsFactoryUtil;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portlet.documentlibrary.model.DLFileVersion;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

public class DLFileEntry extends IndexCheckerModel {

	public Map<Long,Data> getLiferayData(Long companyId, List<Long> listGroupId) throws Exception {
		Map<Long,Data> dataMap = super.getLiferayData(companyId, listGroupId);

		DynamicQuery query = getDLFileVersionQuery(companyId, listGroupId);

		@SuppressWarnings("unchecked")
		List<Object[]> results = (List<Object[]>) this.executeDynamicQuery(DLFileVersion.class, query);

		for(Object[] result : results) {
			long fileEntryId = (long) result[0];
			int status = (int) result[1];

			if (status == WorkflowConstants.STATUS_APPROVED || status == WorkflowConstants.STATUS_IN_TRASH) {
				dataMap.get(fileEntryId).setStatus(status);
			}
			else {
				dataMap.remove(fileEntryId);
			}
		}

		return dataMap;
	}

	protected DynamicQuery getDLFileVersionQuery(Long companyId, List<Long> listGroupId)
			throws IllegalAccessException, InvocationTargetException {
		DynamicQuery query = this.newDynamicQuery(DLFileVersion.class);

		ProjectionList projectionList = ProjectionFactoryUtil.projectionList();
		projectionList.add(ProjectionFactoryUtil.property("fileEntryId"));
		projectionList.add(ProjectionFactoryUtil.property("status"));

		query.setProjection(projectionList);

		Property propertyCompanyId = PropertyFactoryUtil.forName("companyId");

		Property propertyGroupId = PropertyFactoryUtil.forName("groupId");

		query.add(RestrictionsFactoryUtil.conjunction()
						.add(propertyCompanyId.eq(companyId))
						.add(propertyGroupId.in(listGroupId)));

		return query;
	}
}