package com.jorgediaz.indexchecker.index;

import com.jorgediaz.indexchecker.data.Data;
import com.jorgediaz.indexchecker.model.IndexCheckerModel;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
public abstract class IndexWrapperLucene extends IndexWrapper {

	@Override
	public Set<Data> getClassNameData(IndexCheckerModel model) {
		Set<Data> indexData = new HashSet<Data>();

		for (int i = 0; i<maxDoc(); i++) {
			try {
				if (!isDeleted(i)) {
					DocumentWrapper doc = document(i);

					String entryClassName = doc.getEntryClassName();

					if ((entryClassName != null) &&
						entryClassName.equals(model.getClassName())) {

						Data data = new Data(model);
						data.init(doc);

						indexData.add(data);
					}
				}
			}
			catch (Exception e) {
				_log.error(
					"EXCEPTION: " + e.getClass() + " - " +
						e.getMessage());
			}
		}

		return indexData;
	}

	@Override
	public Map<Long, Set<Data>> getClassNameDataByGroupId(
		IndexCheckerModel model) {

		Map<Long, Set<Data>> indexData = new HashMap<Long, Set<Data>>();

		for (int i = 0; i<maxDoc(); i++) {
			try {
				if (!isDeleted(i)) {
					DocumentWrapper doc = document(i);

					String entryClassName = doc.getEntryClassName();

					if ((entryClassName != null) &&
						entryClassName.equals(model.getClassName())) {

						Data data = new Data(model);
						data.init(doc);

						Long groupId = data.getGroupId();

						Set<Data> indexDataSet = indexData.get(groupId);

						if (indexDataSet == null) {
							indexDataSet = new HashSet<Data>();
							indexData.put(groupId, indexDataSet);
						}

						indexDataSet.add(data);
					}
				}
			}
			catch (Exception e) {
				_log.error(
					"EXCEPTION: " + e.getClass() + " - " +
						e.getMessage(), e);
			}
		}

		return indexData;
	}

	protected abstract DocumentWrapper document(int i);

	abstract public Set<String> getTermValues(String term);

	abstract public int numDocs();

	protected abstract boolean isDeleted(int i);

	protected abstract int maxDoc();

	protected Object index = null;

	private static Log _log = LogFactoryUtil.getLog(IndexWrapperLucene.class);

}