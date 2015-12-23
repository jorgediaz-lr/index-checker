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

package jorgediazest.indexchecker.index;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jorgediazest.indexchecker.data.Data;
import jorgediazest.indexchecker.model.IndexCheckerModel;

import jorgediazest.util.model.Model;

/**
 * @author Jorge Díaz
 */
public abstract class IndexWrapper {

	abstract public Set<String> getTermValues(String term);

	abstract public int numDocs();

	abstract public Set<Data> getClassNameData(IndexCheckerModel modelClass);

	abstract public Map<Long, Set<Data>> getClassNameDataByGroupId(
		IndexCheckerModel modelClass);

	public Set<String> getMissingClassNamesAtLiferay(
		Map<String, Model> modelMap) {

		Set<String> classNamesNotAvailable = new HashSet<String>();

		Set<String> indexClassNameSet = this.getTermValues("entryClassName");

		for (String indexClassName : indexClassNameSet) {
			if (!modelMap.containsKey(indexClassName)) {
				classNamesNotAvailable.add(indexClassName);
			}
		}

		return classNamesNotAvailable;
	}

}