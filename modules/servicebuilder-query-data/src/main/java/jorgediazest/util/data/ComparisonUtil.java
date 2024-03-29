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

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import jorgediazest.util.comparator.DataComparator;
import jorgediazest.util.model.Model;

/**
 * @author Jorge Díaz
 */
public class ComparisonUtil {

	public static Comparison getComparison(
		Model model, DataComparator exactDataComparator, Set<Data> leftData,
		Set<Data> rightData, boolean showBothExact, boolean showBothNotExact,
		boolean showOnlyLeft, boolean showOnlyRight) {

		Map<String, Set<Data>> dataSetMap = new TreeMap<>();

		if (showBothExact) {
			dataSetMap.put("both-exact-left", new TreeSet<Data>());
			dataSetMap.put("both-exact-right", new TreeSet<Data>());
		}

		if (showBothNotExact) {
			dataSetMap.put("both-notexact-left", new TreeSet<Data>());
			dataSetMap.put("both-notexact-right", new TreeSet<Data>());
		}

		Data[] bothArrSetLeft = DataUtil.getArrayCommonData(
			leftData, rightData);
		Data[] bothArrSetRight = DataUtil.getArrayCommonData(
			rightData, leftData);

		if (showBothExact || showBothNotExact) {
			Set<Data> bothExactLeftSet = dataSetMap.get("both-exact-left");
			Set<Data> bothExactRightSet = dataSetMap.get("both-exact-right");
			Set<Data> bothNotExactLeftSet = dataSetMap.get(
				"both-notexact-left");
			Set<Data> bothNotExactRightSet = dataSetMap.get(
				"both-notexact-right");

			for (int i = 0; i < bothArrSetRight.length; i++) {
				Data dataLeft = bothArrSetLeft[i];
				Data dataRight = bothArrSetRight[i];

				if (!dataLeft.equals(dataRight)) {
					throw new RuntimeException("Inconsistent data");
				}

				boolean exact = exactDataComparator.equals(dataLeft, dataRight);

				if (exact && showBothExact) {
					bothExactLeftSet.add(dataLeft);
					bothExactRightSet.add(dataRight);
				}

				if (!exact && showBothNotExact) {
					bothNotExactLeftSet.add(dataLeft);
					bothNotExactRightSet.add(dataRight);
				}
			}
		}

		Set<Data> bothDataSet = new HashSet<>(rightData);

		bothDataSet.retainAll(leftData);

		if (showOnlyLeft) {
			Set<Data> leftOnlyData = leftData;

			leftOnlyData.removeAll(bothDataSet);

			dataSetMap.put("only-left", new TreeSet<Data>(leftOnlyData));
		}

		if (showOnlyRight) {
			Set<Data> rightOnlyData = rightData;

			rightOnlyData.removeAll(bothDataSet);

			dataSetMap.put("only-right", new TreeSet<Data>(rightOnlyData));
		}

		return new Comparison(model, dataSetMap);
	}

	public static Comparison getError(Model model, String error) {
		_log.error("Model: " + model.getName() + " ERROR: " + error);

		return new Comparison(model, error);
	}

	public static Comparison getError(Model model, Throwable t) {
		_log.error(
			"Model: " + model.getName() + " EXCEPTION: " + t.getClass() +
				" - " + t.getMessage(),
			t);

		return new Comparison(model, t.getClass() + " - " + t.getMessage());
	}

	public static List<Comparison> mergeComparisons(
		Collection<Comparison> collection) {

		Map<Model, List<Comparison>> modelMap = new LinkedHashMap<>();

		for (Comparison c : collection) {
			List<Comparison> comparisonList = modelMap.get(c.getModel());

			if (comparisonList == null) {
				comparisonList = new ArrayList<>();

				modelMap.put(c.getModel(), comparisonList);
			}

			comparisonList.add(c);
		}

		List<Comparison> resultComparison = new ArrayList<>();

		for (List<Comparison> comparisonList : modelMap.values()) {
			resultComparison.addAll(
				Comparison.mergeComparisons(comparisonList));
		}

		return resultComparison;
	}

	private static Log _log = LogFactoryUtil.getLog(ComparisonUtil.class);

}