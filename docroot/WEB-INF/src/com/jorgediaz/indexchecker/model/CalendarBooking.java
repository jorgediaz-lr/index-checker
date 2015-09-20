package com.jorgediaz.indexchecker.model;

import com.liferay.portal.kernel.workflow.WorkflowConstants;

public class CalendarBooking extends BaseModelIndexChecker {

	public static final int STATUS_MAYBE = 9;

	@Override
	public int[] getIndexedStatuses() {

		int[] statuses = {
				WorkflowConstants.STATUS_APPROVED,
				STATUS_MAYBE
			};
		return statuses;
	}
}