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

import com.liferay.portal.kernel.search.Document;
import com.liferay.portal.kernel.search.Field;
import com.liferay.portal.kernel.util.PrefsPropsUtil;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import jorgediazest.indexchecker.util.ConfigurationUtil;
import jorgediazest.util.data.Data;

/**
 * @author Jorge Díaz
 */
public class JournalArticleIndexSearchHelper extends IndexSearchHelper {

	public JournalArticleIndexSearchHelper() throws Exception {
		indexAllVersions =
			ConfigurationUtil.getJournalArticleIndexAllVersions();
	}

	@Override
	public void fillDataObject(Data data, String[] attributes, Document doc) {
		super.fillDataObject(data, attributes, doc);

		if (indexAllVersions) {
			long id = getIdFromUID(doc.get(Field.UID));
			data.setPrimaryKey(id);
		}
	}

	@Override
	public Map<Data, String> reindex(Collection<Data> dataCollection) {

		Map<Long, Data> articles = new HashMap<Long, Data>();

		for (Data data : dataCollection) {
			articles.put(data.getResourcePrimKey(), data);
		}

		return super.reindex(articles.values());
	}

	protected boolean indexAllVersions;

}