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

import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.dao.orm.Criterion;
import com.liferay.portal.kernel.util.GetterUtil;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jorgediazest.indexchecker.util.ConfigurationUtil;

import jorgediazest.util.data.Data;
import jorgediazest.util.model.Model;
import jorgediazest.util.model.ModelFactory;
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

		if (className.indexOf(StringPool.POUND) != -1) {
			long objectDefinitionId = _getObjectDefinitionId(className, model);

			Criterion objectDefinitionIdCriterion = model.getAttributeCriterion(
				"objectDefinitionId", objectDefinitionId);

			criterion = ModelUtil.generateConjunctionCriterion(
				criterion, objectDefinitionIdCriterion);
		}

		Collection<String> attributesToQuery =
			ConfigurationUtil.getModelAttributesToQuery(model);

		String[] attributesToQueryArr = attributesToQuery.toArray(
			new String[0]);

		return Query.getData(model, attributesToQueryArr, criterion);
	}

	protected boolean indexAllVersions;

	/**
	 * Returns the object definition identifier of a custom object model.
	 *
	 * <p>
	 * All the object entries of every object definition share the same table,
	 * so the object definition has to be part of the query. Otherwise the
	 * entries of all the object definitions are returned and reported as not
	 * indexed.
	 * </p>
	 *
	 * <p>
	 * Liferay names these models
	 * <code>com.liferay.object.model.ObjectDefinition#&lt;shortName&gt;</code>,
	 * where the short name is a four character sequence such as
	 * <code>T8D5</code>, so the suffix cannot be parsed as an identifier. Older
	 * releases used the identifier itself, which is still honored.
	 * </p>
	 */
	private long _getObjectDefinitionId(String className, Model model)
		throws Exception {

		String[] classNameArr = className.split(StringPool.POUND);

		if (classNameArr.length != 2) {
			throw new IllegalArgumentException(
				"Unable to obtain the object definition of " + className);
		}

		long objectDefinitionId = GetterUtil.getLong(classNameArr[1]);

		if (objectDefinitionId > 0) {
			return objectDefinitionId;
		}

		ModelFactory modelFactory = model.getModelFactory();

		Model objectDefinitionModel = modelFactory.getModelObject(
			classNameArr[0]);

		if (objectDefinitionModel == null) {
			throw new IllegalStateException(
				"Unable to obtain the model of " + classNameArr[0]);
		}

		Criterion criterion = objectDefinitionModel.getAttributeCriterion(
			"className", className);

		String primaryKeyAttribute =
			objectDefinitionModel.getPrimaryKeyAttribute();

		Map<Long, Data> dataMap = Query.getData(
			objectDefinitionModel, new String[] {primaryKeyAttribute},
			criterion);

		Set<Long> objectDefinitionIds = dataMap.keySet();

		if (objectDefinitionIds.size() != 1) {
			throw new IllegalStateException(
				"Found " + objectDefinitionIds.size() +
					" object definitions of " + className);
		}

		Iterator<Long> iterator = objectDefinitionIds.iterator();

		return iterator.next();
	}

}