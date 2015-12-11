/**
 * Space for Copyright
 */

package com.jorgediaz.indexchecker.index;

import com.jorgediaz.indexchecker.data.Data;
import com.jorgediaz.indexchecker.model.IndexCheckerModel;
import com.jorgediaz.util.model.Model;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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