package com.jorgediaz.indexchecker.model;

import com.liferay.portal.kernel.workflow.WorkflowConstants;
public class WikiNode extends IndexCheckerModel {

	@Override
	public int[] getValidStatuses() {

		int[] statuses = {
			WorkflowConstants.STATUS_IN_TRASH
			};
		return statuses;
	}

}