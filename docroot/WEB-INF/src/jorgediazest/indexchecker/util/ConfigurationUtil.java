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

package jorgediazest.indexchecker.util;

import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.PrefsPropsUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;

import java.io.IOException;
import java.io.InputStream;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import jorgediazest.indexchecker.data.DataIndexCheckerModelComparator;

import jorgediazest.util.data.DataComparator;
import jorgediazest.util.model.Model;
import jorgediazest.util.modelquery.ModelQuery;

import org.yaml.snakeyaml.Yaml;

/**
 * @author Jorge Díaz
 */
public class ConfigurationUtil {

	public static DataComparator getDataComparator(Model model) {
		String primaryKeyAttribute = getIndexPrimaryKeyAttribute(model);

		@SuppressWarnings("unchecked")
		Collection<String> exactAttributesList =
			(Collection<String>) getModelInfo(model, "exactAttributesToCheck");

		String[] exactAttributes = exactAttributesList.toArray(
			new String[exactAttributesList.size()]);

		DataIndexCheckerModelComparator comparator =
			new DataIndexCheckerModelComparator(
				primaryKeyAttribute, exactAttributes);

		return comparator;
	}

	public static int getDefaultNumberThreads() {
		return PortletPropsValues.NUMBER_THREADS;
	}

	public static String getIndexAttributeName(Model model, String attribute) {

		@SuppressWarnings("unchecked")
		Map<String, Object> indexAttributeNameMapping =
			(Map<String, Object>)getModelInfo(
				model, "indexAttributeNameMapping");

		String indexAttribute = (String)indexAttributeNameMapping.get(
			attribute);

		if ((indexAttribute == null) &&
			model.getPrimaryKeyAttribute().equals(attribute)) {

			indexAttribute = (String)indexAttributeNameMapping.get("pk");
		}

		if (indexAttribute == null) {
			indexAttribute = attribute;
		}

		return indexAttribute;
	}

	@SuppressWarnings("unchecked")
	public static Collection<String> getModelAttributesToQuery(Model model) {
		Collection<String> attributesToCheck =
			(Collection<String>) getModelInfo(model, "attributesToQuery");

		if (attributesToCheck == null) {
			return Collections.emptySet();
		}

		return attributesToCheck;
	}

	public static Map<String, Map<String, Object>> getModelInfo() {
		if (modelInfo != null) {
			return modelInfo;
		}

		synchronized(ConfigurationUtil.class) {
			if (modelInfo == null) {
				Map<String, Map<String, Object>> modelInfoAux =
					new HashMap<String, Map<String, Object>>();

				@SuppressWarnings("unchecked")
				Collection<Map<String, Object>> modelInfoList =
					(Collection<Map<String, Object>>)getConfiguration().get(
						"modelInfo");

				for (Map<String, Object> modelMap : modelInfoList) {
					String model = (String) modelMap.get("model");
					modelInfoAux.put(model, modelMap);
				}

				modelInfo = modelInfoAux;
			}

			return modelInfo;
		}
	}

	public static Object getModelInfo(Model model, String entry) {
		Object value = null;

		Map<String, Object> modelMap = getModelInfo().get(model.getClassName());

		if (modelMap != null) {
			value = modelMap.get(entry);
		}

		if (Validator.isNotNull(value)) {
			return value;
		}

		if (model.isWorkflowEnabled()) {
			value = getModelInfo().get("workflowedModel").get(entry);

			if (Validator.isNotNull(value)) {
				return value;
			}
		}

		if (model.isResourcedModel()) {
			value = getModelInfo().get("resourcedModel").get(entry);

			if (Validator.isNotNull(value)) {
				return value;
			}
		}

		return getModelInfo().get("default").get(entry);
	}

	@SuppressWarnings("unchecked")
	public static Class<? extends ModelQuery> getModelQueryClass(
			String className) {

		Object object = null;

		Map<String, Object> modelMap = getModelInfo().get(className);

		if (modelMap != null) {
			object = modelMap.get("modelQueryClass");
		}

		if (object != null) {
			return (Class<? extends ModelQuery>)object.getClass();
		}

		modelMap = getModelInfo().get("default");

		object = modelMap.get("modelQueryClass");

		return (Class<? extends ModelQuery>)object.getClass();
	}

	public static Collection<String> getRelatedAttributesToCheck(Model model) {
		@SuppressWarnings("unchecked")
		Collection<String> relatedAttributesToCheck =
			(Collection<String>)getModelInfo(model, "relatedAttributesToCheck");

		if (relatedAttributesToCheck == null) {
			return Collections.emptySet();
		}

		return relatedAttributesToCheck;
	}

	public static String getStringFilter(Model model) {

		return (String) getModelInfo(model, "filter");
	}

	public static boolean ignoreClassName(String className) {
		if (Validator.isNull(className)) {
			return true;
		}

		return configurationListEntryContainsValue(
			"ignoreClassNames", className);
	}

	public static boolean modelNotIndexed(String className) {
		return configurationListEntryContainsValue(
			"modelNotIndexed", className);
	}

	protected static boolean configurationListEntryContainsValue(
		String configurationEntry, String value) {

		@SuppressWarnings("unchecked")
		Collection<String> list =
			(Collection<String>)getConfiguration().get(configurationEntry);

		return (list.contains(value));
	}

	protected static Map<String, Object> getConfiguration() {
		if (configuration != null) {
			return configuration;
		}

		synchronized(ConfigurationUtil.class) {
			try {
				if (configuration == null) {
					configuration = readConfiguration(CONFIGURATION_FILE);
				}
			}
			catch (IOException ioe) {
				_log.error(ioe);

				throw new RuntimeException(ioe);
			}
			catch (SystemException se) {
				_log.error(se);

				throw new RuntimeException(se);
			}

			return configuration;
		}
	}

	/**
	 * @param model
	 * @return
	 */
	protected static String getIndexPrimaryKeyAttribute(Model model) {
		String primaryKeyAttribute = (String)getModelInfo(
			model, "indexPrimaryKeyAttribute");

		if (Validator.isNotNull(primaryKeyAttribute)) {
			return primaryKeyAttribute;
		}

		return model.getPrimaryKeyAttribute();
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> readConfiguration(
			String configurationFile)
		throws IOException, SystemException {

		ClassLoader classLoader = ConfigurationUtil.class.getClassLoader();

		InputStream inputStream = classLoader.getResourceAsStream(
			configurationFile);

		String configuration = StringUtil.read(inputStream);

		String journalArticleIndexPrimaryKeyAttribute;

		if (PrefsPropsUtil.getBoolean("journal.articles.index.all.versions")) {
			journalArticleIndexPrimaryKeyAttribute = "pk";
		}
		else {
			journalArticleIndexPrimaryKeyAttribute = "resourcePrimKey";
		}

		configuration = configuration.replace(
			"$$JOURNAL_ARTICLE_INDEX_PRIMARY_KEY_ATTRIBUTE$$",
			journalArticleIndexPrimaryKeyAttribute);

		Yaml yaml = new Yaml();

		return (Map<String, Object>)yaml.load(configuration);
	}

	private static final String CONFIGURATION_FILE = "configuration.yml";

	private static Log _log = LogFactoryUtil.getLog(ConfigurationUtil.class);

	private static Map<String, Object> configuration = null;
	private static Map<String, Map<String, Object>> modelInfo = null;

}