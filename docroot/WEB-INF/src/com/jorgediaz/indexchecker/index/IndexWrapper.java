package com.jorgediaz.indexchecker.index;

import com.jorgediaz.indexchecker.data.Data;
import com.jorgediaz.indexchecker.model.BaseModel;
import com.jorgediaz.indexchecker.model.ModelInfo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public abstract class IndexWrapper {

	protected Object index = null;

	abstract public  int numDocs();
	abstract public int maxDoc();
	abstract public boolean isDeleted(int i);
	abstract public DocumentWrapper document(int i);

	public Set<Data> getClassNameData(BaseModel modelClass) {
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
				System.out.println("\t" + "EXCEPTION: " + e.getClass() + " - " + e.getMessage());
			}
		}
		return indexData;
	}

	public Map<Long,Set<Data>> getClassNameDataByGroupId(BaseModel modelClass) {
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
				System.out.println("\t" + "EXCEPTION: " + e.getClass() + " - " + e.getMessage());
			}
		}
		return indexData;
	}

	public Map<String,Long> getClassNameNum(ModelInfo modelUtil) {
		Map<String,Long> classNameNum = new HashMap<String,Long>();
		for(int i=0;i<maxDoc();i++) {
		
			try {
				if(!isDeleted(i)) {
	
					DocumentWrapper doc = document(i);
	
					String entryClassName = doc.getEntryClassName();
	
					if(entryClassName == null) {
						continue;
					}
	
					if(modelUtil.hasModel(entryClassName)) {
						Long numEntries = classNameNum.get(entryClassName);
						if(numEntries == null) {
							numEntries = Long.valueOf(0);
						}
						classNameNum.put(entryClassName, numEntries+1);
					}
				}
			}
			catch(Exception e) {
				System.out.println("\t" + "EXCEPTION: " + e.getClass() + " - " + e.getMessage());
			}
		}
		return classNameNum;
	}

}
