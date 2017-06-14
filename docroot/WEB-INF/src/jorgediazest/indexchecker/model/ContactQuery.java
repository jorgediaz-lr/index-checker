/**
 * Copyright (c) 2015-present Jorge Díaz All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package jorgediazest.indexchecker.model;

import com.liferay.portal.kernel.dao.orm.Conjunction;
import com.liferay.portal.kernel.dao.orm.Criterion;
import com.liferay.portal.kernel.dao.orm.Projection;
import com.liferay.portal.kernel.dao.orm.RestrictionsFactoryUtil;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portal.model.User;

import java.util.List;
import java.util.Map;

import jorgediazest.util.data.Data;
import jorgediazest.util.model.Model;
import jorgediazest.util.model.ModelUtil;
import jorgediazest.util.modelquery.ModelQuery;

/**
 * @author Jorge Díaz
 */
public class ContactQuery extends IndexCheckerModelQuery {

	public Criterion generateQueryFilter() {

		Conjunction conjunction = RestrictionsFactoryUtil.conjunction();

		ModelQuery modelQueryUser = mqFactory.getModelQueryObject(
			User.class.getName());

		Model modelUser = modelQueryUser.getModel();

		Criterion filter = modelUser.generateCriterionFilter(
			"defaultUser=false,status=" + WorkflowConstants.STATUS_APPROVED);

		Projection projection = modelUser.getPropertyProjection("userId");

		try {
			@SuppressWarnings("unchecked")
			List<Long> users = (List<Long>)modelUser.executeDynamicQuery(
				filter, projection);

			conjunction.add(getModel().generateInCriteria("classPK",users));
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}

		return conjunction;
	}

	@Override
	public Map<Long, Data> getData(
			String[] attributes, String mapKeyAttribute, Criterion filter)
		throws Exception {

		filter = ModelUtil.generateConjunctionQueryFilter(
			filter, generateQueryFilter());

		return super.getData(attributes, mapKeyAttribute, filter);
	}

}