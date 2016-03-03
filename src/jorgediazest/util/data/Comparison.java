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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import jorgediazest.util.model.Model;

/**
 * @author Jorge Díaz
 */
public class Comparison {

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

	public Set<String> getOutputTypes() {
		Set<String> outputTypes = new HashSet<String>();

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

	protected Comparison(Model model, Map<String, Set<Data>> data) {
		this.data = data;
		this.error = null;
		this.model = model;
	}

	protected Comparison(Model model, String error) {
		this.data = new HashMap<String, Set<Data>>();
		this.error = error;
		this.model = model;
	}

	private static Log _log = LogFactoryUtil.getLog(Comparison.class);

	private Map<String, Set<Data>> data;
	private String error;
	private Model model;

}