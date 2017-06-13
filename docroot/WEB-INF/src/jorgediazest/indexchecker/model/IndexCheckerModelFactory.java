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

import jorgediazest.indexchecker.util.ConfigurationUtil;

import jorgediazest.util.model.Model;
import jorgediazest.util.model.ModelFactory;
import jorgediazest.util.model.ModelWrapper;

/**
 * @author Jorge Díaz
 */
public class IndexCheckerModelFactory extends ModelFactory {

	@Override
	public Model getModelObject(String className) {
		Model model = super.getModelObject(className);

		if (model == null) {
			return null;
		}

		String stringFilter = ConfigurationUtil.getStringFilter(model);

		Criterion filter = model.generateCriterionFilter(stringFilter);

		if (filter == null) {
			return model;
		}

		ModelWrapper modelWrapper = new ModelWrapper(model);

		modelWrapper.setFilter(filter);

		return modelWrapper;
	}

}