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

import com.liferay.portal.kernel.dao.orm.Criterion;

import java.util.List;

import jorgediazest.indexchecker.util.ConfigurationUtil;

import jorgediazest.util.model.Model;
import jorgediazest.util.model.ModelFactory;
import jorgediazest.util.model.ModelUtil;
import jorgediazest.util.model.ModelWrapper;

/**
 * @author Jorge Díaz
 */
public class IndexCheckerModelFactory extends ModelFactory {

	long companyId;

	public IndexCheckerModelFactory() {
		this.companyId = 0L;
	}

	public IndexCheckerModelFactory(long companyId) {
		this.companyId = companyId;
	}

	@Override
	public Model getModelObject(String className) {
		Model model = super.getModelObject(className);

		if (model == null) {
			return null;
		}

		if (model instanceof ModelWrapper) {
			return model;
		}

		List<String> keyAttributes = ConfigurationUtil.getKeyAttributes(model);

		String sqlFilter = ConfigurationUtil.getStringFilter(model);

		Criterion criterion = ModelUtil.generateSQLCriterion(sqlFilter);

		if (companyId > 0) {
			criterion = ModelUtil.generateConjunctionCriterion(
				model.getAttributeCriterion("companyId", companyId), criterion);
		}

		if ((criterion == null)&& ((keyAttributes == null) ||
			 keyAttributes.isEmpty())) {

			return model;
		}

		ModelWrapper modelWrapper = new ModelWrapper(model);

		if (criterion != null) {
			modelWrapper.setCriterion(criterion);
		}

		if ((keyAttributes != null) && !keyAttributes.isEmpty()) {
			modelWrapper.setKeyAttributes(keyAttributes);
		}

		cacheModelObject.put(className, modelWrapper);

		return modelWrapper;
	}

}