package com.jorgediaz.indexchecker.model;

import com.jorgediaz.util.model.ModelFactory;

import com.liferay.portal.kernel.dao.orm.Conjunction;
import com.liferay.portal.kernel.dao.orm.Property;
import com.liferay.portal.kernel.dao.orm.PropertyFactoryUtil;
import com.liferay.portal.model.ClassedModel;
public class User extends IndexCheckerModel {

	@Override
	public void init(ModelFactory modelUtil, Class<? extends ClassedModel> clazz) throws Exception {
		super.init(modelUtil, clazz);

		this.removeIndexedAttribute("createDate");
		this.addIndexedAttribute("status");
	}

	@Override
	public Conjunction generateQueryFilter() {

		Conjunction conjunction = super.generateQueryFilter();

		Property property = PropertyFactoryUtil.forName("defaultUser");

		conjunction.add(property.eq(false));

		return conjunction;
	}
}