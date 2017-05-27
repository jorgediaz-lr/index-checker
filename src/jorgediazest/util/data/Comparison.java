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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
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

		List<Comparison> result = new ArrayList<Comparison>();

		Comparison merged = null;

		for (Comparison c : collection) {
			if ((c.data != null) && (merged == null)) {
				merged = c;

				continue;
			}

			if ((c.data == null) || !merged.getModel().equals(c.getModel())) {
				result.add(c);

				continue;
			}

			for (Entry<String, Set<Data>> e : c.data.entrySet()) {
				Set<Data> dataSet = merged.data.get(e.getKey());

				if (dataSet == null) {
					dataSet = new TreeSet<Data>();

					merged.data.put(e.getKey(), dataSet);
				}

				dataSet.addAll(e.getValue());
			}
		}

		result.add(merged);

		return result;
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
					_log.info(
						d.getEntryClassName() + " " + d.getMap().toString());
				}
			}
		}
	}

	public Set<Data> getData(String type) {
		if ("both-exact".equals(type)) {
			type = "both-exact-left";
		}
		else if ("both-notexact".equals(type)) {
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
		Set<String> outputTypes = new TreeSet<String>();

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
		Map<Long, Comparison> result = new TreeMap<Long, Comparison>();

		if (error != null) {
			result.put(0L, this);

			return result;
		}

		for (Entry<String, Set<Data>> entry : data.entrySet()) {
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
					set = new TreeSet<Data>();
					result.get(id).data.put(key, set);
				}

				set.add(d);
			}
		}

		return result;
	}

	protected Comparison(Model model, Map<String, Set<Data>> data) {
		this.data = data;
		this.error = null;
		this.model = model;
	}

	protected Comparison(Model model, String error) {
		this.data = new TreeMap<String, Set<Data>>();
		this.error = error;
		this.model = model;
	}

	private static Log _log = LogFactoryUtil.getLog(Comparison.class);

	private Map<String, Set<Data>> data;
	private String error;
	private Model model;

}