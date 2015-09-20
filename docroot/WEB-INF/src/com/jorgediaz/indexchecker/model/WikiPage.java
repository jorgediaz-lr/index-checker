package com.jorgediaz.indexchecker.model;

import com.jorgediaz.util.model.ModelFactory;
import com.liferay.portal.kernel.dao.orm.Conjunction;
import com.liferay.portal.kernel.dao.orm.Property;
import com.liferay.portal.kernel.dao.orm.PropertyFactoryUtil;
import com.liferay.portal.model.ClassedModel;

public class WikiPage extends IndexCheckerModel {
	
	public void init(ModelFactory modelUtil, Class<? extends ClassedModel> clazz) throws Exception {
		super.init(modelUtil, clazz);

		this.setIndexPrimaryKey("nodeId");
	}

	@Override
	public Conjunction generateQueryFilter() {
		
		Conjunction conjunction = super.generateQueryFilter();

		Property propertyHead = PropertyFactoryUtil.forName("head");

		conjunction.add(propertyHead.eq(true));

		Property propertyRedirectTitle = PropertyFactoryUtil.forName("redirectTitle");

		conjunction.add(propertyRedirectTitle.eq(""));

		return conjunction;
	}
}