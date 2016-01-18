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

package jorgediazest.indexchecker;

import com.liferay.portal.kernel.dao.orm.Criterion;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.Group;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jorgediazest.indexchecker.data.Data;
import jorgediazest.indexchecker.model.IndexCheckerModel;
import jorgediazest.indexchecker.model.IndexCheckerModelFactory;

import jorgediazest.util.model.Model;
import jorgediazest.util.model.ModelFactory;

/**
 * @author Jorge Díaz
 */
public class IndexChecker {

	public static IndexCheckerModel castModel(Model model) {
		IndexCheckerModel icModel;
		try {
			icModel = (IndexCheckerModel)model;
		}
		catch (Exception e) {
			if (_log.isWarnEnabled()) {
				_log.warn(
					"Model: " + model.getName() + " EXCEPTION: " +
						e.getClass() + " - " + e.getMessage(), e);
			}

			icModel = null;
		}

		return icModel;
	}

	public static Map<Long, List<IndexCheckerResult>>
		executeScript(
			Company company, List<Group> groups, List<String> classNames,
			Set<ExecutionMode> executionMode)
		throws SystemException {

		ModelFactory modelFactory = new IndexCheckerModelFactory();

		Map<String, Model> modelMap = modelFactory.getModelMap(classNames);

		List<IndexCheckerModel> modelList = new ArrayList<IndexCheckerModel>();

		for (Model model : modelMap.values()) {
			IndexCheckerModel icModel = castModel(model);
			modelList.add(icModel);
		}

		Map<Long, List<IndexCheckerResult>> resultDataMap =
			new LinkedHashMap<Long, List<IndexCheckerResult>>();

		long companyId = company.getCompanyId();

		List<Long> groupIds = new ArrayList<Long>();
		groupIds.add(0L);

		if (executionMode.contains(ExecutionMode.GROUP_BY_SITE)) {
			for (Group group : groups) {
				groupIds.add(group.getGroupId());
			}
		}

		for (long groupId : groupIds) {
			List<IndexCheckerResult> resultList =
				new ArrayList<IndexCheckerResult>();

			for (IndexCheckerModel icModel : modelList) {
				try {
					if ((groupId == 0L) &&
						icModel.hasAttribute("groupId") &&
						executionMode.contains(ExecutionMode.GROUP_BY_SITE)) {

						continue;
					}

					if ((groupId != 0L) &&
						!icModel.hasAttribute("groupId") &&
						executionMode.contains(ExecutionMode.GROUP_BY_SITE)) {

						continue;
					}

					Criterion filter = icModel.getCompanyGroupFilter(
						companyId, groupId);

					Set<Data> liferayData = new HashSet<Data>(
						icModel.getLiferayData(filter).values());

					Set<Data> indexData;

					if (executionMode.contains(ExecutionMode.SHOW_INDEX) ||
						!liferayData.isEmpty()) {

						indexData = icModel.getIndexData(companyId, groupId);
					}
					else {
						indexData = new HashSet<Data>();
					}

					IndexCheckerResult data =
						IndexCheckerResult.getIndexCheckResult(
							icModel, liferayData, indexData, executionMode);

					resultList.add(data);
				}
				catch (Exception e) {
					_log.error(
						"Model: " + icModel.getName() + " EXCEPTION: " +
							e.getClass() + " - " + e.getMessage(),e);
				}
			}

			resultDataMap.put(groupId, resultList);
		}

		return resultDataMap;
	}

	private static Log _log = LogFactoryUtil.getLog(IndexChecker.class);

}