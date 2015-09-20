package com.jorgediaz.indexchecker.model;

import com.liferay.portal.kernel.workflow.WorkflowConstants;

public class WikiNode extends BaseModel {

	public String getSQLWhere() {

		return super.getSQLWhere() + " and status = " + WorkflowConstants.STATUS_IN_TRASH;
	}
}