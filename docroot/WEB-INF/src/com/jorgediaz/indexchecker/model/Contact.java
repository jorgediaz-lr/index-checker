package com.jorgediaz.indexchecker.model;

import com.liferay.portal.kernel.dao.orm.Conjunction;
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.ProjectionFactoryUtil;
import com.liferay.portal.kernel.dao.orm.Property;
import com.liferay.portal.kernel.dao.orm.PropertyFactoryUtil;
import com.liferay.portal.kernel.dao.orm.RestrictionsFactoryUtil;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portal.model.User;


public class Contact extends IndexCheckerModel {

	@Override
	public int[] getIndexedStatuses() {
		return null;
	}

	public void addQueryCriterias(Conjunction conjunction) {
		
		super.addQueryCriterias(conjunction);

		DynamicQuery userDynamicQuery = this.newDynamicQuery(User.class);

		userDynamicQuery.setProjection(ProjectionFactoryUtil.property("userId"));

		Property propertyDefaultUser = PropertyFactoryUtil.forName("defaultUser");

		Property propertyStatus = PropertyFactoryUtil.forName("status");

		userDynamicQuery.add(RestrictionsFactoryUtil.disjunction()
				.add(propertyDefaultUser.eq(true))
				.add(propertyStatus.ne(WorkflowConstants.STATUS_APPROVED)));

		Property property = PropertyFactoryUtil.forName("classPK");

		conjunction.add(property.notIn(userDynamicQuery));
	}
}