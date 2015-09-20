package com.jorgediaz.indexchecker.index;

import com.jorgediaz.indexchecker.data.Data;
import com.jorgediaz.indexchecker.model.IndexCheckerModel;

import java.util.Map;
import java.util.Set;


public abstract class IndexWrapper {

	abstract public Set<String> getTermValues(String term);

	abstract public int numDocs();

	abstract public Set<Data> getClassNameData(IndexCheckerModel modelClass);

	abstract public Map<Long,Set<Data>> getClassNameDataByGroupId(IndexCheckerModel modelClass);

}
