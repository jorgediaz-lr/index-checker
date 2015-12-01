package com.jorgediaz.indexchecker.model;

import com.liferay.portal.kernel.dao.orm.Criterion;
import com.liferay.portal.kernel.dao.orm.RestrictionsFactoryUtil;
public class MBMessage extends IndexCheckerModel {

	@Override
	public Criterion generateQueryFilter() {

		return RestrictionsFactoryUtil.not(
			this.generateCriterionFilter("categoryId=-1,parentMessageId=0"));
	}

}