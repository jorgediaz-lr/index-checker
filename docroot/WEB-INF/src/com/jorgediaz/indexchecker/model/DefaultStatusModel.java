package com.jorgediaz.indexchecker.model;

import com.liferay.portal.kernel.workflow.WorkflowConstants;

public class DefaultStatusModel extends BaseModel {

	protected String getSQLWhere() {

		return super.getSQLWhere() + " and status in (" + WorkflowConstants.STATUS_APPROVED + "," + WorkflowConstants.STATUS_IN_TRASH + ")";
	}
}