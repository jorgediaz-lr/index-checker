package com.jorgediaz.indexchecker.model;

import com.liferay.portal.kernel.dao.orm.Criterion;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
public class CalendarBooking extends IndexCheckerModel {

	public static final int STATUS_MAYBE = 9;

	@Override
	public Criterion generateQueryFilter() {
		return this.generateCriterionFilter(
			"status=" + WorkflowConstants.STATUS_APPROVED + "+" +
			"status=" + STATUS_MAYBE);
	}

}