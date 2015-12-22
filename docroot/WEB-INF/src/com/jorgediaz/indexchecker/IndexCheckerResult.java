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
import com.liferay.portal.model.Group;
import com.liferay.portal.service.GroupLocalServiceUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

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
				_log.info("*** ClassName: "+ result.getModel().getName());
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

		if ((indexExactData != null) && !indexExactData.isEmpty()) {
			_log.info("==both-exact(index)==");

			for (Data d : indexExactData) {
				_log.info(d.getAllData(","));
			}

			_log.info("==both-exact(liferay)==");

			for (Data d : liferayExactData) {
				_log.info(d.getAllData(","));
			}
		}

		if ((indexNotExactData != null) && !indexNotExactData.isEmpty()) {
			_log.info("==both-notexact(index)==");

			for (Data d : indexNotExactData) {
				_log.info(d.getAllData(","));
			}

			_log.info("==both-notexact(liferay)==");

			for (Data d : liferayNotExactData) {
				_log.info(d.getAllData(","));
			}
		}

		if ((liferayOnlyData != null) && !liferayOnlyData.isEmpty()) {
			_log.info("==only liferay==");

			for (Data d : liferayOnlyData) {
				_log.info(d.getAllData(","));
			}
		}

		if ((indexOnlyData != null) && !indexOnlyData.isEmpty()) {
			_log.info("==only index==");

			for (Data d : indexOnlyData) {
				_log.info(d.getAllData(","));
			}
		}
	}

	public Set<Data> getIndexExactData() {
		return indexExactData;
	}

	public Set<Data> getIndexNotExactData() {
		return indexNotExactData;
	}

	public Set<Data> getIndexOnlyData() {
		return indexOnlyData;
	}

	public Set<Data> getLiferayExactData() {
		return liferayExactData;
	}

	public Set<Data> getLiferayNotExactData() {
		return liferayNotExactData;
	}

	public Set<Data> getLiferayOnlyData() {
		return liferayOnlyData;
	}

	public IndexCheckerModel getModel() {
		return model;
	}

	public List<String> reindex() {

		List<String> out = new ArrayList<String>();

		IndexCheckerModel model = this.getModel();
		Set<Data> exactDataSetIndex = this.getIndexExactData();
		Set<Data> notExactDataSetIndex = this.getIndexNotExactData();
		Set<Data> liferayOnlyData = this.getLiferayOnlyData();

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

		Map<Data, String> errors = model.reindex(objectsToReindex);

		for (Entry<Data, String> error : errors.entrySet()) {
			out.add(
				"Error reindexing " + error.getKey() +
				"EXCEPTION" + error.getValue());
		}

		return out;
	}

	public List<String> removeIndexOrphans() {

		List<String> out = new ArrayList<String>();

		IndexCheckerModel model = this.getModel();
		Set<Data> indexOnlyData = this.getIndexOnlyData();

		if ((indexOnlyData != null) && !indexOnlyData.isEmpty()) {
			Map<Data, String> errors = model.deleteAndCheck(indexOnlyData);

			for (Entry<Data, String> error : errors.entrySet()) {
				out.add(
					"Error deleting from index " + error.getKey() +
					"EXCEPTION" + error.getValue());
			}
		}

		return out;
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

		this.model = model;
		this.indexExactData = indexExactData;
		this.indexNotExactData = indexNotExactData;
		this.indexOnlyData = indexOnlyData;
		this.liferayExactData = liferayExactData;
		this.liferayNotExactData = liferayNotExactData;
		this.liferayOnlyData = liferayOnlyData;
	}

	protected Set<Data> indexExactData;
	protected Set<Data> indexNotExactData;
	protected Set<Data> indexOnlyData;
	protected Set<Data> liferayExactData;
	protected Set<Data> liferayNotExactData;
	protected Set<Data> liferayOnlyData;
	protected IndexCheckerModel model;

	private static Log _log = LogFactoryUtil.getLog(IndexCheckerResult.class);

}