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

package jorgediazest.indexchecker.data;

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

import jorgediazest.indexchecker.ExecutionMode;
import jorgediazest.indexchecker.model.IndexCheckerModel;

/**
 * @author Jorge Díaz
 */
public class Results {

	public static void dumpToLog(
			boolean groupBySite,
			Map<Long, List<Results>> resultDataMap)
		throws SystemException {

		if (!_log.isInfoEnabled()) {
			return;
		}

		for (Entry<Long, List<Results>> entry : resultDataMap.entrySet()) {
			String groupTitle = null;
			Group group = GroupLocalServiceUtil.fetchGroup(entry.getKey());

			if ((group == null) && groupBySite) {
				groupTitle = "N/A";
			}
			else if (group != null) {
				groupTitle = group.getGroupId() + " - " + group.getName();
			}

			if (groupTitle != null) {
				_log.info("");
				_log.info("---------------");
				_log.info("GROUP: " + groupTitle);
				_log.info("---------------");
			}

			for (Results result : entry.getValue()) {
				result.dumpToLog();
			}
		}
	}

	public static Results getError(IndexCheckerModel model, Exception e) {
		_log.error(
			"Model: " + model.getName() + " EXCEPTION: " +
				e.getClass() + " - " + e.getMessage(),e);

		return new Results(model, e.getClass() + " - " + e.getMessage());
	}

	public static Results getIndexCheckResult(
		IndexCheckerModel model, Set<Data> liferayData, Set<Data> indexData,
		Set<ExecutionMode> executionMode) {

		Map<String, Set<Data>> data = new HashMap<String, Set<Data>>();

		if (executionMode.contains(ExecutionMode.SHOW_BOTH_EXACT)) {
			data.put("both-exact-liferay", new HashSet<Data>());
			data.put("both-exact-index", new HashSet<Data>());
		}

		if (executionMode.contains(ExecutionMode.SHOW_BOTH_NOTEXACT)) {
			data.put("both-notexact-liferay", new HashSet<Data>());
			data.put("both-notexact-index", new HashSet<Data>());
		}

		Data[] bothArrSetLiferay = Results.getBothDataArray(
			liferayData, indexData);
		Data[] bothArrSetIndex = Results.getBothDataArray(
			indexData, liferayData);

		if (executionMode.contains(ExecutionMode.SHOW_BOTH_EXACT) ||
			executionMode.contains(ExecutionMode.SHOW_BOTH_NOTEXACT)) {

			for (int i = 0; i<bothArrSetIndex.length; i++) {
				Data dataIndex = bothArrSetIndex[i];
				Data dataLiferay = bothArrSetLiferay[i];

				if (!dataIndex.equals(dataLiferay)) {
					throw new RuntimeException("Inconsistent data");
				}
				else if (dataIndex.exact(dataLiferay)) {
					if (executionMode.contains(ExecutionMode.SHOW_BOTH_EXACT)) {
						data.get("both-exact-index").add(dataIndex);
						data.get("both-exact-liferay").add(dataLiferay);
					}
				}
				else if (executionMode.contains(
							ExecutionMode.SHOW_BOTH_NOTEXACT)) {

					data.get("both-notexact-index").add(dataIndex);
					data.get("both-notexact-liferay").add(dataLiferay);
				}
			}
		}

		Set<Data> bothDataSet = new HashSet<Data>(indexData);
		bothDataSet.retainAll(liferayData);

		if (executionMode.contains(ExecutionMode.SHOW_LIFERAY)) {
			Set<Data> liferayOnlyData = liferayData;
			liferayOnlyData.removeAll(bothDataSet);
			data.put("only-liferay", liferayOnlyData);
		}

		if (executionMode.contains(ExecutionMode.SHOW_INDEX)) {
			Set<Data> indexOnlyData = indexData;
			indexOnlyData.removeAll(bothDataSet);
			data.put("only-index", indexOnlyData);
		}

		return new Results(model, data);
	}

	public void dumpToLog() {

		if (!_log.isInfoEnabled()) {
			return;
		}

		_log.info("*** ClassName: "+ model.getName());

		for (Entry<String, Set<Data>> entry : data.entrySet()) {
			if (entry.getValue().size() != 0) {
				_log.info("==" + entry.getKey() + "==");

				for (Data d : entry.getValue()) {
					_log.info(d.getAllData(","));
				}
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

	public String getError() {
		return error;
	}

	public IndexCheckerModel getModel() {
		return model;
	}

	public Map<Data, String> reindex() {

		Set<Data> objectsToReindex = new HashSet<Data>();

		for (Entry<String, Set<Data>> entry : data.entrySet()) {
			if (entry.getKey().endsWith("-liferay") &&
				(entry.getValue() != null)) {

				objectsToReindex.addAll(entry.getValue());
			}
		}

		IndexCheckerModel model = this.getModel();

		return model.reindex(objectsToReindex);
	}

	public Map<Data, String> removeIndexOrphans() {
		Set<Data> indexOnlyData = this.getData("only-index");

		if ((indexOnlyData == null) || indexOnlyData.isEmpty()) {
			return null;
		}

		IndexCheckerModel model = this.getModel();

		return model.deleteAndCheck(indexOnlyData);
	}

	protected static Data[] getBothDataArray(Set<Data> set1, Set<Data> set2) {
		Set<Data> both = new TreeSet<Data>(set1);
		both.retainAll(set2);
		return both.toArray(new Data[0]);
	}

	protected Results(IndexCheckerModel model, Map<String, Set<Data>> data) {
		this.data = data;
		this.error = null;
		this.model = model;
	}

	protected Results(IndexCheckerModel model, String error) {
		this.data = new HashMap<String, Set<Data>>();
		this.error = error;
		this.model = model;
	}

	private static Log _log = LogFactoryUtil.getLog(Results.class);

	private Map<String, Set<Data>> data;
	private String error;
	private IndexCheckerModel model;

}