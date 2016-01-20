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

import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.model.Group;
import com.liferay.portal.service.GroupLocalServiceUtil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import jorgediazest.indexchecker.data.Data;
import jorgediazest.indexchecker.model.IndexCheckerModel;

/**
 * @author Jorge Díaz
 */
public class IndexCheckerResult {

	public static void dumpToLog(
			boolean groupBySite,
			Map<Long, List<IndexCheckerResult>> resultDataMap)
		throws SystemException {

		if (!_log.isInfoEnabled()) {
			return;
		}

		for (Entry<Long, List<IndexCheckerResult>> entry :
				resultDataMap.entrySet()) {

			String groupTitle = null;
			Group group = GroupLocalServiceUtil.fetchGroup(entry.getKey());

			if ((group == null) && groupBySite) {
				groupTitle = "N/A";
			}
			else if (group != null) {
				groupTitle = group.getGroupId() + " - " + group.getName();
			}

			if (groupTitle != null) {
				_log.info("\n---------------");
				_log.info("GROUP: " + groupTitle);
				_log.info("---------------");
			}

			for (IndexCheckerResult result : entry.getValue()) {
				result.dumpToLog();
			}
		}
	}

	public static IndexCheckerResult getIndexCheckResult(
		IndexCheckerModel model, Set<Data> liferayData, Set<Data> indexData,
		Set<ExecutionMode> executionMode) {

		Data[] bothArrSetLiferay = IndexCheckerResult.getBothDataArray(
			liferayData, indexData);
		Data[] bothArrSetIndex = IndexCheckerResult.getBothDataArray(
			indexData, liferayData);

		Set<Data> exactDataSetIndex = new HashSet<Data>();
		Set<Data> exactDataSetLiferay = new HashSet<Data>();
		Set<Data> notExactDataSetIndex = new HashSet<Data>();
		Set<Data> notExactDataSetLiferay = new HashSet<Data>();

		if ((executionMode.contains(ExecutionMode.SHOW_BOTH_EXACT) ||
			 executionMode.contains(ExecutionMode.SHOW_BOTH_NOTEXACT)) &&
			(bothArrSetIndex.length > 0) &&
			(bothArrSetLiferay.length > 0)) {

			for (int i = 0; i<bothArrSetIndex.length; i++) {
				Data dataIndex = bothArrSetIndex[i];
				Data dataLiferay = bothArrSetLiferay[i];

				if (!dataIndex.equals(dataLiferay)) {
					throw new RuntimeException("Inconsistent data");
				}
				else if (dataIndex.exact(dataLiferay)) {
					if (executionMode.contains(ExecutionMode.SHOW_BOTH_EXACT)) {
						exactDataSetIndex.add(dataIndex);
						exactDataSetLiferay.add(dataLiferay);
					}
				}
				else if (executionMode.contains(
							ExecutionMode.SHOW_BOTH_NOTEXACT)) {

					notExactDataSetIndex.add(dataIndex);
					notExactDataSetLiferay.add(dataLiferay);
				}
			}
		}

		Set<Data> liferayOnlyData = liferayData;
		Set<Data> indexOnlyData = indexData;
		Set<Data> bothDataSet = new HashSet<Data>(indexData);
		bothDataSet.retainAll(liferayData);

		if (executionMode.contains(ExecutionMode.SHOW_LIFERAY)) {
			liferayOnlyData.removeAll(bothDataSet);
		}
		else {
			liferayOnlyData = new HashSet<Data>();
		}

		if (executionMode.contains(ExecutionMode.SHOW_INDEX)) {
			indexOnlyData.removeAll(bothDataSet);
		}
		else {
			indexOnlyData = new HashSet<Data>();
		}

		return new IndexCheckerResult(
			model, exactDataSetIndex, exactDataSetLiferay, notExactDataSetIndex,
			notExactDataSetLiferay, liferayOnlyData, indexOnlyData);
	}

	public void dumpToLog() {

		if (!_log.isInfoEnabled()) {
			return;
		}

		_log.info("*** ClassName: "+ model.getName());

		for (Entry<String, Set<Data>> entry : data.entrySet()) {
			_log.info("==" + entry.getKey() + "==");

			for (Data d : entry.getValue()) {
				_log.info(d.getAllData(","));
			}
		}
	}

	public Set<Data> getData(String type) {
		if ("both-exact".equals(type)) {
			type = "both-exact-liferay";
		}
		else if ("both-notexact".equals(type)) {
			type = "both-notexact-liferay";
		}

		return data.get(type);
	}

	public IndexCheckerModel getModel() {
		return model;
	}

	public Map<Data, String> reindex() {
		IndexCheckerModel model = this.getModel();
		Set<Data> exactDataSetIndex = this.getData("both-exact-index");
		Set<Data> notExactDataSetIndex = this.getData("both-notexact-index");
		Set<Data> liferayOnlyData = this.getData("only-liferay");

		Set<Data> objectsToReindex = new HashSet<Data>();

		if (exactDataSetIndex != null) {
			objectsToReindex.addAll(exactDataSetIndex);
		}

		if (notExactDataSetIndex != null) {
			objectsToReindex.addAll(notExactDataSetIndex);
		}

		if (liferayOnlyData != null) {
			objectsToReindex.addAll(liferayOnlyData);
		}

		return model.reindex(objectsToReindex);
	}

	public Map<Data, String> removeIndexOrphans() {
		IndexCheckerModel model = this.getModel();
		Set<Data> indexOnlyData = this.getData("only-index");

		if ((indexOnlyData == null) || indexOnlyData.isEmpty()) {
			return null;
		}

		return model.deleteAndCheck(indexOnlyData);
	}

	protected static Data[] getBothDataArray(Set<Data> set1, Set<Data> set2) {
		Set<Data> both = new TreeSet<Data>(set1);
		both.retainAll(set2);
		return both.toArray(new Data[0]);
	}

	protected IndexCheckerResult(
		IndexCheckerModel model, Set<Data> indexExactData,
		Set<Data> liferayExactData, Set<Data> indexNotExactData,
		Set<Data> liferayNotExactData, Set<Data> liferayOnlyData,
		Set<Data> indexOnlyData) {

		data = new HashMap<String, Set<Data>>();
		data.put("both-exact-liferay", liferayExactData);
		data.put("both-exact-index", indexExactData);
		data.put("both-notexact-liferay", liferayNotExactData);
		data.put("both-notexact-index", indexNotExactData);
		data.put("only-liferay", liferayOnlyData);
		data.put("only-index", indexOnlyData);

		this.model = model;
	}

	private static Log _log = LogFactoryUtil.getLog(IndexCheckerResult.class);

	private Map<String, Set<Data>> data;
	private IndexCheckerModel model;

}