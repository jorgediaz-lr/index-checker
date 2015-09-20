package com.jorgediaz.indexchecker.index;

import com.jorgediaz.indexchecker.data.Data;
import com.jorgediaz.indexchecker.model.BaseModelIndexChecker;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;



public abstract class IndexWrapperLucene extends IndexWrapper {

	protected Object index = null;

	protected abstract int maxDoc();

	protected abstract boolean isDeleted(int i);

	protected abstract DocumentWrapper document(int i);

	abstract public Set<String> getTermValues(String term);

	abstract public int numDocs();

	@Override
	public Set<Data> getClassNameData(BaseModelIndexChecker modelClass) {
		Set<Data> indexData = new HashSet<Data>();
		for(int i=0;i<maxDoc();i++) {
		
			try {
				if(!isDeleted(i)) {
	
					DocumentWrapper doc = document(i);

					String entryClassName = doc.getEntryClassName();

					if(entryClassName != null && entryClassName.equals(modelClass.getFullClassName()))
					{
						Data data = new Data(modelClass);
						data.init(doc);

						indexData.add(data);
					}
	
				}
			}
			catch(Exception e) {
				System.err.println("\t" + "EXCEPTION: " + e.getClass() + " - " + e.getMessage());
			}
		}
		return indexData;
	}

	@Override
	public Map<Long,Set<Data>> getClassNameDataByGroupId(BaseModelIndexChecker modelClass) {
		Map<Long,Set<Data>> indexData = new HashMap<Long,Set<Data>>();
		for(int i=0;i<maxDoc();i++) {
		
			try {
				if(!isDeleted(i)) {
	
					DocumentWrapper doc = document(i);

					String entryClassName = doc.getEntryClassName();

					if(entryClassName != null && entryClassName.equals(modelClass.getFullClassName()))
					{
						Data data = new Data(modelClass);
						data.init(doc);

						Long groupId = data.getGroupId();

						Set<Data> indexDataSet = indexData.get(groupId);
						if(indexDataSet == null) {
							indexDataSet = new HashSet<Data>();
							indexData.put(groupId, indexDataSet);
						}

						indexDataSet.add(data);
					}
	
				}
			}
			catch(Exception e) {
				System.err.println("\t" + "EXCEPTION: " + e.getClass() + " - " + e.getMessage());
			}
		}
		return indexData;
	}

}
