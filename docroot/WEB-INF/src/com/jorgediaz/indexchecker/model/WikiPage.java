/**
 * Space for Copyright
 */

package com.jorgediaz.indexchecker.model;

import com.jorgediaz.util.model.ModelFactory;

import com.liferay.portal.kernel.dao.orm.Criterion;
import com.liferay.portal.model.ClassedModel;
public class WikiPage extends IndexCheckerModel {

	@Override
	public Criterion generateQueryFilter() {
		return this.generateCriterionFilter("head=true,redirectTitle=");
	}

	public void init(
			ModelFactory modelUtil, Class<? extends ClassedModel> clazz)
		throws Exception {

		super.init(modelUtil, clazz);

		this.setIndexPrimaryKey("nodeId");
	}

}