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

package jorgediazest.util.data;

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

import jorgediazest.util.model.Model;

/**
 * @author Jorge Díaz
 */
public class ComparisonUtil {

	public static void dumpToLog(
			boolean groupBySite,
			Map<Long, List<Comparison>> comparisonDataMap)
		throws SystemException {

		if (!_log.isInfoEnabled()) {
			return;
		}

		for (
			Entry<Long, List<Comparison>> entry :
				comparisonDataMap.entrySet()) {

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

			for (Comparison comparison : entry.getValue()) {
				comparison.dumpToLog();
			}
		}
	}

	public static Comparison getComparation(
		Model model, Set<Data> leftData, Set<Data> rightData,
		boolean showBothExact, boolean showBothNotExact, boolean showOnlyLeft,
		boolean showOnlyRight) {

		Map<String, Set<Data>> data = new HashMap<String, Set<Data>>();

		if (showBothExact) {
			data.put("both-exact-left", new HashSet<Data>());
			data.put("both-exact-right", new HashSet<Data>());
		}

		if (showBothNotExact) {
			data.put("both-notexact-left", new HashSet<Data>());
			data.put("both-notexact-right", new HashSet<Data>());
		}

		Data[] bothArrSetLeft = DataUtil.getArrayCommonData(
			leftData, rightData);
		Data[] bothArrSetRight = DataUtil.getArrayCommonData(
			rightData, leftData);

		if (showBothExact || showBothNotExact) {
			for (int i = 0; i< bothArrSetRight.length; i++) {
				Data dataLeft = bothArrSetLeft[i];
				Data dataRight = bothArrSetRight[i];

				if (!dataRight.equals(dataLeft)) {
					throw new RuntimeException("Inconsistent data");
				}
				else if (dataRight.exact(dataLeft)) {
					if (showBothExact) {
						data.get("both-exact-left").add(dataLeft);
						data.get("both-exact-right").add(dataRight);
					}
				}
				else if (showBothNotExact) {
					data.get("both-notexact-left").add(dataLeft);
					data.get("both-notexact-right").add(dataRight);
				}
			}
		}

		Set<Data> bothDataSet = new HashSet<Data>(rightData);
		bothDataSet.retainAll(leftData);

		if (showOnlyLeft) {
			Set<Data> leftOnlyData = leftData;
			leftOnlyData.removeAll(bothDataSet);
			data.put("only-left", leftOnlyData);
		}

		if (showOnlyRight) {
			Set<Data> rightOnlyData = rightData;
			rightOnlyData.removeAll(bothDataSet);
			data.put("only-right", rightOnlyData);
		}

		return new Comparison(model, data);
	}

	public static Comparison getError(Model model, Exception e) {
		_log.error(
			"Model: " + model.getName() + " EXCEPTION: " +
				e.getClass() + " - " + e.getMessage(),e);

		return new Comparison(model, e.getClass() + " - " + e.getMessage());
	}

	private static Log _log = LogFactoryUtil.getLog(ComparisonUtil.class);

}