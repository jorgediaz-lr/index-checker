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
import com.liferay.portal.kernel.util.StringUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import jorgediazest.util.model.Model;

/**
 * @author Jorge Díaz
 */
public class Comparison {

	public static List<Comparison> mergeComparisons(
		Collection<Comparison> collection) {

		List<Comparison> result = new ArrayList<>();

		Comparison merged = null;

		Set<String> errors = new TreeSet<>();

		for (Comparison c : collection) {
			if ((c.data != null) && (merged == null)) {
				merged = c;

				if (c.getError() != null) {
					errors.add(c.getError());
				}

				continue;
			}

			Model mergedModel = merged.getModel();

			if ((c.data == null) || !mergedModel.equals(c.getModel())) {
				result.add(c);

				continue;
			}

			if (c.getError() != null) {
				errors.add(c.getError());
			}

			for (Map.Entry<String, Set<Data>> e : c.data.entrySet()) {
				Set<Data> dataSet = merged.data.get(e.getKey());

				if (dataSet == null) {
					dataSet = new TreeSet<>();

					merged.data.put(e.getKey(), dataSet);
				}

				dataSet.addAll(e.getValue());
			}
		}

		if (!errors.isEmpty()) {
			merged.error = StringUtil.merge(errors, ", ");
		}

		result.add(merged);

		return result;
	}

	public void dumpToLog() {
		if (!_log.isInfoEnabled()) {
			return;
		}

		_log.info("*** ClassName: " + model.getName());

		for (Map.Entry<String, Set<Data>> entry : data.entrySet()) {
			Set<Data> value = entry.getValue();

			if (!value.isEmpty()) {
				_log.info("==" + entry.getKey() + "==");

				for (Data d : value) {
					Map<String, Object> map = d.getMap();

					_log.info(d.getEntryClassName() + " " + map.toString());
				}
			}
		}
	}

	public Set<Data> getData(String type) {
		if (Objects.equals("both-exact", type)) {
			type = "both-exact-left";
		}
		else if (Objects.equals("both-notexact", type)) {
			type = "both-notexact-left";
		}

		return data.get(type);
	}

	public String getError() {
		return error;
	}

	public Model getModel() {
		return model;
	}

	public String getModelDisplayName(Locale locale) {
		return model.getDisplayName(locale);
	}

	public String getModelName() {
		return model.getName();
	}

	public Set<String> getOutputTypes() {
		Set<String> outputTypes = new TreeSet<>();

		for (String key : data.keySet()) {
			if (key.startsWith("both-exact")) {
				key = "both-exact";
			}
			else if (key.startsWith("both-notexact")) {
				key = "both-notexact";
			}

			outputTypes.add(key);
		}

		return outputTypes;
	}

	public Map<Long, Comparison> splitByAttribute(String attribute) {
		Map<Long, Comparison> result = new TreeMap<>();

		if (error != null) {
			result.put(0L, this);

			return result;
		}

		for (Map.Entry<String, Set<Data>> entry : data.entrySet()) {
			String key = entry.getKey();

			for (Data d : entry.getValue()) {
				Long id;

				try {
					id = d.get(attribute, 0L);
				}
				catch (Exception e) {
					id = 0L;
				}

				Comparison c = result.get(id);

				if (c == null) {
					c = new Comparison(model, new TreeMap<String, Set<Data>>());

					result.put(id, c);
				}

				Set<Data> set = c.data.get(key);

				if (set == null) {
					set = new TreeSet<>();

					c.data.put(key, set);
				}

				set.add(d);
			}
		}

		return result;
	}

	protected Comparison(Model model, Map<String, Set<Data>> data) {
		this.model = model;
		this.data = data;

		error = null;
	}

	protected Comparison(Model model, String error) {
		this.model = model;
		this.error = error;

		data = new TreeMap<>();
	}

	protected Map<String, Set<Data>> data;
	protected String error;
	protected Model model;

	private static Log _log = LogFactoryUtil.getLog(Comparison.class);

}