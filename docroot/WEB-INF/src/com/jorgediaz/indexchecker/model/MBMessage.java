package com.jorgediaz.indexchecker.model;

import com.liferay.portal.kernel.dao.orm.Criterion;
public class MBMessage extends IndexCheckerModel {

	@Override
	public Criterion generateQueryFilter() {

		return this.generateCriterionFilter(
			"categoryId<>-1+parentMessageId<>0");
	}

}