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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import jorgediazest.indexchecker.util.ConfigurationUtil;

import jorgediazest.util.data.Data;
import jorgediazest.util.model.Model;
import jorgediazest.util.model.ModelUtil;
import jorgediazest.util.query.Query;

/**
 * @author Jorge Díaz
 */
public class ObjectDefinitionQueryHelper extends IndexCheckerQueryHelper {

	@Override
	public Map<Long, Data> getLiferayData(Model model, List<Long> groupIds)
		throws Exception {

		Criterion criterion = model.getAttributeCriterion("groupId", groupIds);

		String className = model.getClassName();

		String[] classNameArr = className.split("#");

		if (classNameArr.length == 2) {
			try {
				long objectDefinitionId = Long.parseLong(classNameArr[1]);

				criterion = ModelUtil.generateConjunctionCriterion(
					criterion,
					model.getAttributeCriterion(
						"objectDefinitionId", objectDefinitionId));
			}
			catch (NumberFormatException numberFormatException) {
			}
		}

		Collection<String> attributesToQuery =
			ConfigurationUtil.getModelAttributesToQuery(model);

		String[] attributesToQueryArr = attributesToQuery.toArray(
			new String[0]);

		return Query.getData(model, attributesToQueryArr, criterion);
	}

	protected boolean indexAllVersions;

}