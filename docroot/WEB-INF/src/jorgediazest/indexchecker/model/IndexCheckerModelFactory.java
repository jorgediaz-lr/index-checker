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

package jorgediazest.indexchecker.model;

import com.liferay.portal.kernel.bean.ClassLoaderBeanHandler;
import com.liferay.portal.kernel.search.BaseIndexer;
import com.liferay.portal.kernel.search.Indexer;

import java.lang.reflect.Proxy;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import jorgediazest.util.model.Model;
import jorgediazest.util.model.ModelFactory;

/**
 * @author Jorge Díaz
 */
public class IndexCheckerModelFactory extends ModelFactory {

	public static Class<? extends Model> defaultModelClass =
		IndexCheckerModel.class;

	public static Map<String, Class<? extends Model>> modelClassMap =
		new HashMap<String, Class<? extends Model>>();

	static {
		modelClassMap.put(
			"com.liferay.portlet.asset.model.AssetEntry", NotIndexed.class);
		modelClassMap.put(
			"com.liferay.portlet.calendar.model.CalendarBooking",
			CalendarBooking.class);
		modelClassMap.put("com.liferay.portal.model.Contact", Contact.class);
		modelClassMap.put(
			"com.liferay.portlet.documentlibrary.model.DLFileEntry",
			DLFileEntry.class);
		modelClassMap.put(
			"com.liferay.portlet.journal.model.JournalArticle",
			JournalArticle.class);
		modelClassMap.put(
			"com.liferay.portlet.messageboards.model.MBMessage",
			MBMessage.class);
		modelClassMap.put(
			"com.liferay.portlet.trash.model.TrashEntry", NotIndexed.class);
		modelClassMap.put("com.liferay.portal.model.User", User.class);
		modelClassMap.put(
			"com.liferay.portlet.wiki.model.WikiNode", WikiNode.class);
		modelClassMap.put(
			"com.liferay.portlet.wiki.model.WikiPage", WikiPage.class);
	}

	public IndexCheckerModelFactory() {
		super(defaultModelClass, modelClassMap);
	}

	public IndexCheckerModelFactory(
		Class<? extends Model> defaultModelClass,
		Map<String, Class<? extends Model>> modelClassMap) {

		super(defaultModelClass, modelClassMap);
	}

	@Override
	public Map<String, Model> getModelMap(Collection<String> classNames) {

		Map<String, Model> originalModelMap = super.getModelMap(classNames);

		Map<String, Model> modelMap = new LinkedHashMap<String, Model>();

		for (Entry<String, Model> entry : originalModelMap.entrySet()) {
			Model model = entry.getValue();

			if (model.hasIndexer()) {
				BaseIndexer baseindexer =
					IndexCheckerModelFactory.getBaseIndexer(model.getIndexer());

				if ((baseindexer != null) && baseindexer.isIndexerEnabled()) {
					modelMap.put(entry.getKey(), model);
				}
			}
		}

		return modelMap;
	}

	protected static BaseIndexer getBaseIndexer(Indexer indexer) {
		BaseIndexer baseindexer = null;

		if (indexer instanceof BaseIndexer) {
			baseindexer = (BaseIndexer)indexer;
		}
		else if (indexer instanceof Proxy) {
			ClassLoaderBeanHandler classLoaderBeanHandler =
				(ClassLoaderBeanHandler)Proxy.getInvocationHandler(indexer);
			baseindexer = (BaseIndexer)classLoaderBeanHandler.getBean();
		}

		return baseindexer;
	}

}