package com.jorgediaz.indexchecker.model;

import com.liferay.portal.kernel.workflow.WorkflowConstants;

public class CalendarBooking extends IndexCheckerModel {

	public static final int STATUS_MAYBE = 9;

	@Override
	public int[] getValidStatuses() {

		int[] statuses = {
				WorkflowConstants.STATUS_APPROVED,
				STATUS_MAYBE
			};
		return statuses;
	}
}