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

import jorgediazest.indexchecker.data.Data;
import jorgediazest.indexchecker.index.IndexWrapper;
import jorgediazest.indexchecker.model.IndexCheckerModel;
import jorgediazest.indexchecker.model.IndexCheckerModelFactory;
import jorgediazest.util.model.Model;
import jorgediazest.util.model.ModelFactory;

import com.liferay.portal.kernel.dao.orm.Criterion;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.Group;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * @author Jorge Díaz
 */
public class IndexChecker {

	public static Map<Long, List<IndexCheckerResult>>
		executeScript(
			Class<? extends IndexWrapper> indexWrapperClass, Company company,
			List<Group> groups, List<String> classNames,
			Set<ExecutionMode> executionMode)
		throws SystemException {

		IndexWrapper indexWrapper = getIndexWrapper(indexWrapperClass, company);

		ModelFactory modelFactory = new IndexCheckerModelFactory();

		Map<String, Model> modelMap = modelFactory.getModelMap(classNames);

		Map<Long, List<IndexCheckerResult>> resultDataMap =
			new LinkedHashMap<Long, List<IndexCheckerResult>>();

		for (Model model : modelMap.values()) {
			try {
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

				if (icModel == null) {
					continue;
				}

				Map<Long, IndexCheckerResult> modelDataMap =
					getData(
						indexWrapper, company.getCompanyId(), groups, icModel,
						executionMode);

				for (
					Entry<Long, IndexCheckerResult> entry :
						modelDataMap.entrySet()) {

					if (!resultDataMap.containsKey(entry.getKey())) {
						resultDataMap.put(
							entry.getKey(),
							new ArrayList<IndexCheckerResult>());
					}

					resultDataMap.get(entry.getKey()).add(entry.getValue());
				}
			}
			catch (Exception e) {
				_log.error(
					"Model: " + model.getName() + " EXCEPTION: " +
						e.getClass() + " - " + e.getMessage(),e);
			}
		}

		return resultDataMap;
	}

	public static List<String> executeScriptGetIndexMissingClassNames(
		Class<? extends IndexWrapper> indexWrapperClass, Company company,
		List<String> classNames)
	throws SystemException {

		List<String> out = new ArrayList<String>();

		IndexWrapper indexWrapper = getIndexWrapper(indexWrapperClass, company);

		ModelFactory modelFactory = new IndexCheckerModelFactory();

		Map<String, Model> modelMap = modelFactory.getModelMap(classNames);

		Set<String> classNamesNotAvailable =
			indexWrapper.getMissingClassNamesAtLiferay(modelMap);

		if (classNamesNotAvailable.size() == 0) {
			out.add(StringPool.BLANK);
			out.add("All classNames at Index also exists at Liferay :-)");

			return out;
		}

		out.add(StringPool.BLANK);
		out.add("---------------");
		out.add("The following classNames exists at Index but not at Liferay!");
		out.add("---------------");

		for (String className : classNamesNotAvailable) {
			out.add(className);
		}

		return out;
	}

	protected static Map<Long, IndexCheckerResult> getData(
			IndexWrapper indexWrapper, long companyId, List<Group> groups,
			IndexCheckerModel icModel, Set<ExecutionMode> executionMode)
		throws Exception {

		Map<Long, IndexCheckerResult> dataMap =
			new LinkedHashMap<Long, IndexCheckerResult>();

		if (icModel.hasAttribute("groupId") &&
			executionMode.contains(ExecutionMode.GROUP_BY_SITE)) {

			Map<Long, Set<Data>> indexDataMap =
				indexWrapper.getClassNameDataByGroupId(icModel);

			for (Group group : groups) {
				Set<Data> indexData = indexDataMap.get(group.getGroupId());

				if (indexData == null) {
					indexData = new HashSet<Data>();
				}

				List<Group> listGroupAux = new ArrayList<Group>();
				listGroupAux.add(group);

				IndexCheckerResult data =
					getIndexCheckResult(
						companyId, listGroupAux, icModel, indexData,
						executionMode);

				if (data != null) {
					dataMap.put(group.getGroupId(), data);
				}
			}
		}
		else {
			Set<Data> indexData = indexWrapper.getClassNameData(icModel);

			IndexCheckerResult data = getIndexCheckResult(
				companyId, groups, icModel, indexData, executionMode);

			if (data != null) {
				dataMap.put(0L, data);
			}
		}

		return dataMap;
	}

	protected static IndexCheckerResult getIndexCheckResult(
			long companyId, List<Group> groups, IndexCheckerModel icModel,
			Set<Data> indexData, Set<ExecutionMode> executionMode)
		throws Exception {

		List<Long> listGroupId = new ArrayList<Long>();

		for (Group group : groups) {
			listGroupId.add(group.getGroupId());
		}

		Criterion filter = icModel.getCompanyGroupFilter(
			companyId, listGroupId);

		Set<Data> liferayData = new HashSet<Data>(
			icModel.getLiferayData(filter).values());

		IndexCheckerResult data = IndexCheckerResult.getIndexCheckResult(
			icModel, liferayData, indexData, executionMode);

		return data;
	}

	protected static IndexWrapper getIndexWrapper(
			Class<? extends IndexWrapper> indexWrapperClass, Company company)
		throws SystemException {

		IndexWrapper indexWrapper;

		try {
			indexWrapper =
				indexWrapperClass.getDeclaredConstructor(
					long.class).newInstance(company.getCompanyId());
		}
		catch (Exception e) {
			_log.error(
				"EXCEPTION: " + e.getClass() + " - " +
					e.getMessage());
			throw new SystemException(e);
		}

		return indexWrapper;
	}

	static Log _log = LogFactoryUtil.getLog(IndexChecker.class);

}