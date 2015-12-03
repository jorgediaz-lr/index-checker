package com.jorgediaz.indexchecker.model;

import com.liferay.portal.kernel.dao.orm.Conjunction;
import com.liferay.portal.kernel.dao.orm.Criterion;
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.RestrictionsFactoryUtil;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portal.model.User;
public class Contact extends IndexCheckerModel {

	@Override
	public Criterion generateQueryFilter() {

		Conjunction conjunction = RestrictionsFactoryUtil.conjunction();

		DynamicQuery userDynamicQuery = this.newDynamicQuery(User.class);

		userDynamicQuery.setProjection(getPropertyProjection("userId"));

		userDynamicQuery.add(RestrictionsFactoryUtil.disjunction().add(
			getProperty("defaultUser").eq(true)).add(
				getProperty("status").ne(WorkflowConstants.STATUS_APPROVED)));

		conjunction.add(getProperty("classPK").notIn(userDynamicQuery));

		return conjunction;
	}

}