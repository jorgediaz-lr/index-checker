package com.jorgediaz.indexchecker.model;

import com.liferay.portal.kernel.workflow.WorkflowConstants;

public class CalendarBooking extends BaseModel {

	public static final int STATUS_MAYBE = 9;

	protected String getSQLWhere() {

		return super.getSQLWhere() + " and status in (" + WorkflowConstants.STATUS_APPROVED + "," + STATUS_MAYBE + ")";
	}
}