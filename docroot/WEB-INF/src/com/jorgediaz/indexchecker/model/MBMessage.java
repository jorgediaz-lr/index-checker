package com.jorgediaz.indexchecker.model;

import com.liferay.portal.kernel.dao.orm.Conjunction;
import com.liferay.portal.kernel.dao.orm.Property;
import com.liferay.portal.kernel.dao.orm.PropertyFactoryUtil;
import com.liferay.portal.kernel.dao.orm.RestrictionsFactoryUtil;

public class MBMessage extends BaseModelIndexChecker {

	public void addQueryCriterias(Conjunction conjunction) {
		
		super.addQueryCriterias(conjunction);

		Property propertyCategoryId = PropertyFactoryUtil.forName("categoryId");

		Property propertyParentMessageId = PropertyFactoryUtil.forName("parentMessageId");

		conjunction.add(
				RestrictionsFactoryUtil.not(
					RestrictionsFactoryUtil.conjunction()
						.add(propertyCategoryId.eq(-1L))
						.add(propertyParentMessageId.eq(0L))
				));
	}
}