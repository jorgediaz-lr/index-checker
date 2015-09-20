package com.script.index;

import com.script.data.Data;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public abstract class IndexWrapper {

	public PrintWriter out = null;

	protected Object index = null;

	abstract public  int numDocs();
	abstract public int maxDoc();
	abstract public boolean isDeleted(int i);
	abstract public DocumentWrapper document(int i);

	public Set<Data> getClassNameData(String fullClassName, Long groupId) {
		Set<Data> indexData = new HashSet<Data>();
		for(int i=0;i<maxDoc();i++) {
		
			try {
				if(!isDeleted(i)) {
	
					DocumentWrapper doc = document(i);

					String entryClassName = doc.getEntryClassName();

					if(entryClassName != null && entryClassName.equals(fullClassName))
					{
						Data data = new Data();
						data.init(doc, fullClassName);

						if((groupId == null) || (data.groupId == groupId)) {
							indexData.add(data);
						}
					}
	
				}
			}
			catch(Exception e) {
				out.println("\t" + "EXCEPTION: " + e.getClass() + " - " + e.getMessage());
			}
		}
		return indexData;
	}

	public Map<String,Long> getClassNameNum(String filter) {
		Map<String,Long> classNameNum = new HashMap<String,Long>();
		for(int i=0;i<maxDoc();i++) {
		
			try {
				if(!isDeleted(i)) {
	
					DocumentWrapper doc = document(i);
	
					String entryClassName = doc.getEntryClassName();
	
					if(entryClassName == null) {
						continue;
					}
	
					if(filter == null || (filter != null && entryClassName.contains(filter))) {
						Long numEntries = classNameNum.get(entryClassName);
						if(numEntries == null) {
							numEntries = Long.valueOf(0);
						}
						classNameNum.put(entryClassName, numEntries+1);
					}
				}
			}
			catch(Exception e) {
				out.println("\t" + "EXCEPTION: " + e.getClass() + " - " + e.getMessage());
			}
		}
		return classNameNum;
	}

}
