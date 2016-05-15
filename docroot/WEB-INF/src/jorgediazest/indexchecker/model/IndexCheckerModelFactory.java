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

import java.util.HashMap;
import java.util.Map;

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
			"com.liferay.portlet.dynamicdatalists.model.DDLRecord",
			DDLRecord.class);
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

}