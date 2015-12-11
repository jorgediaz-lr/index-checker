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

package com.jorgediaz.indexchecker;

import com.jorgediaz.indexchecker.data.Data;
import com.jorgediaz.indexchecker.model.IndexCheckerModel;

import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.model.Group;
import com.liferay.portal.service.GroupLocalServiceUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * @author Jorge Díaz
 */
public class IndexCheckerUtil {

	public static List<String> generateOutput(
			int maxLength, Set<ExecutionMode> executionMode,
			Map<Long, List<IndexCheckerResult>> resultDataMap)
		throws SystemException {

		List<String> out = new ArrayList<String>();

		for (
			Entry<Long, List<IndexCheckerResult>> entry :
				resultDataMap.entrySet()) {

			String groupTitle = null;
			Group group = GroupLocalServiceUtil.fetchGroup(entry.getKey());

			if ((group == null) &&
				executionMode.contains(ExecutionMode.GROUP_BY_SITE)) {

				groupTitle = "N/A";
			}
			else if (group != null) {
				groupTitle = group.getGroupId() + " - " + group.getName();
			}

			if (groupTitle != null) {
				out.add("\n---------------");
				out.add("GROUP: " + groupTitle);
				out.add("---------------");

				if (_log.isInfoEnabled() &&
					executionMode.contains(
						ExecutionMode.DUMP_ALL_OBJECTS_TO_LOG)) {

					_log.info("\n---------------");
					_log.info("GROUP: " + groupTitle);
					_log.info("---------------");
				}
			}

			List<IndexCheckerResult> resultList = entry.getValue();

			int i = 0;

			for (IndexCheckerResult result : resultList) {
				IndexCheckerModel model = result.getModel();
				out.add("*** ClassName["+ i +"]: "+ model.getName());

				if (_log.isInfoEnabled() &&
					executionMode.contains(
						ExecutionMode.DUMP_ALL_OBJECTS_TO_LOG)) {

					_log.info("*** ClassName["+ i +"]: "+ model.getName());
				}

				out.addAll(dumpData(result, maxLength, executionMode));

				i++;
			}
		}

		return out;
	}

	protected static List<String> dumpData(
		IndexCheckerResult data, int maxLength,
		Set<ExecutionMode> executionMode) {

		List<String> out = new ArrayList<String>();

		IndexCheckerModel model = data.getModel();
		Set<Data> exactDataSetIndex = data.getIndexExactData();
		Set<Data> exactDataSetLiferay = data.getLiferayExactData();
		Set<Data> notExactDataSetIndex = data.getIndexNotExactData();
		Set<Data> notExactDataSetLiferay = data.getLiferayNotExactData();
		Set<Data> liferayOnlyData = data.getLiferayOnlyData();
		Set<Data> indexOnlyData = data.getIndexOnlyData();

		if ((exactDataSetIndex != null) && !exactDataSetIndex.isEmpty()) {
			out.add("==both-exact==");
			out.addAll(
				dumpData(model.getName(), exactDataSetLiferay, maxLength));

			if (_log.isInfoEnabled() &&
				executionMode.contains(
					ExecutionMode.DUMP_ALL_OBJECTS_TO_LOG)) {

				_log.info("==both-exact(index)==");

				for (Data d : exactDataSetIndex) {
					_log.info(d.getAllData(","));
				}

				_log.info("==both-exact(liferay)==");

				for (Data d : exactDataSetLiferay) {
					_log.info(d.getAllData(","));
				}
			}
		}

		if ((notExactDataSetIndex != null) && !notExactDataSetIndex.isEmpty()) {
			out.add("==both-notexact==");
			out.addAll(
				dumpData(model.getName(), notExactDataSetIndex, maxLength));

			if (_log.isInfoEnabled() &&
				executionMode.contains(
					ExecutionMode.DUMP_ALL_OBJECTS_TO_LOG)) {

				_log.info("==both-notexact(index)==");

				for (Data d : notExactDataSetIndex) {
					_log.info(d.getAllData(","));
				}

				_log.info("==both-notexact(liferay)==");

				for (Data d : notExactDataSetLiferay) {
					_log.info(d.getAllData(","));
				}
			}
		}

		if ((liferayOnlyData != null) && !liferayOnlyData.isEmpty()) {
			out.add("==only liferay==");
			out.addAll(dumpData(model.getName(), liferayOnlyData, maxLength));

			if (_log.isInfoEnabled() &&
				executionMode.contains(
					ExecutionMode.DUMP_ALL_OBJECTS_TO_LOG)) {

				_log.info("==only liferay==");

				for (Data d : liferayOnlyData) {
					_log.info(d.getAllData(","));
				}
			}
		}

		if ((indexOnlyData != null) && !indexOnlyData.isEmpty()) {
			out.add("==only index==");
			out.addAll(dumpData(model.getName(), indexOnlyData, maxLength));

			if (_log.isInfoEnabled() &&
				executionMode.contains(
					ExecutionMode.DUMP_ALL_OBJECTS_TO_LOG)) {

				_log.info("==only index==");

				for (Data d : indexOnlyData) {
					_log.info(d.getAllData(","));
				}
			}
		}

		return out;
	}

	protected static List<String> dumpData(
		String entryClassName, Collection<Data> liferayData, int maxLength) {

		List<String> out = new ArrayList<String>();

		List<Long> valuesPK = new ArrayList<Long>();
		List<Long> valuesRPK = new ArrayList<Long>();

		for (Data value : liferayData) {
			valuesPK.add(value.getPrimaryKey());

			if (value.getResourcePrimKey() != -1) {
				valuesRPK.add(value.getResourcePrimKey());
			}
		}

		String listPK = StringUtil.shorten(
			StringUtil.merge(valuesPK), maxLength);
		out.add(
			"\tnumber of primary keys: "+valuesPK.size()+
			"\n\tprimary keys values: ["+listPK+"]");

		Set<Long> valuesRPKset = new HashSet<Long>(valuesRPK);

		if (valuesRPKset.size()>0) {
			String listRPK = StringUtil.shorten(
				StringUtil.merge(valuesRPKset), maxLength);
			out.add(
				"\tnumber of resource primary keys: "+ valuesRPKset.size()+
				"\n\tresource primary keys values: ["+listRPK+"]");
		}

		return out;
	}

	private static Log _log = LogFactoryUtil.getLog(IndexCheckerUtil.class);

}