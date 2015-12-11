/**
 * Space for Copyright
 */

package com.jorgediaz.indexchecker.model;

import com.liferay.portal.kernel.dao.orm.Criterion;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
public class WikiNode extends IndexCheckerModel {

	@Override
	public Criterion generateQueryFilter() {
		return this.generateCriterionFilter(
			"status=" + WorkflowConstants.STATUS_IN_TRASH);
	}

}