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

package com.jorgediaz.util.model;

import com.jorgediaz.util.service.Service;
import com.jorgediaz.util.service.ServiceUtil;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Jorge Díaz
 */
public class ModelFactory {

	public ModelFactory() {
		this(null, null);
	}

	public ModelFactory(Class<? extends Model> defaultModelClass) {
		this(defaultModelClass, null);
	}

	public ModelFactory(
		Class<? extends Model> defaultModelClass,
		Map<String, Class<? extends Model>> modelClassMap) {

		if (defaultModelClass == null) {
			this.defaultModelClass = DefaultModel.class;
		}
		else {
			this.defaultModelClass = defaultModelClass;
		}

		this.modelClassMap = modelClassMap;
	}

	public Map<String, Model> getModelMap(Collection<String> classNames) {

		ClassLoader classLoader = ModelUtil.getClassLoader();

		Map<String, Model> modelMap = new LinkedHashMap<String, Model>();

		for (String classname : classNames) {
			if (Validator.isNull(classname) || !classname.contains(".model.")) {
				continue;
			}

			Model model = null;
			String[] attributes = null;
			try {
				model = this.getModelObject(classLoader, classname);

				if (model != null) {
					attributes = model.getAttributesName();
				}
			}
			catch (Exception e) {
				if (_log.isDebugEnabled()) {
					_log.debug(e, e);
				}
				else if (_log.isInfoEnabled()) {
					_log.info(
						"Cannot get model object of " + classname +
						" EXCEPTION: " + e.getClass().getName() + ": " +
						e.getMessage());
				}
			}

			if ((model != null) && (attributes != null)) {
				modelMap.put(model.getName(), model);
			}
		}

		return modelMap;
	}

	public final Model getModelObject(
		ClassLoader classLoader, String className) {

		String classPackageName = StringPool.BLANK;
		String classSimpleName = className;

		int pos = className.lastIndexOf(".");

		if (pos > 0) {
			classPackageName = className.substring(0, pos);
			classSimpleName = className.substring(pos + 1, className.length());
		}

		Class<? extends Model> modelClass = this.defaultModelClass;

		if ((this.modelClassMap != null) &&
			this.modelClassMap.containsKey(className)) {

			modelClass = this.modelClassMap.get(className);
		}

		Model model = null;
		try {
			model = (Model)modelClass.newInstance();

			Service service = ServiceUtil.getService(
				classLoader, classPackageName, classSimpleName);

			model.setModelFactory(this);

			model.init(classPackageName, classSimpleName, service);
		}
		catch (Exception e) {
			_log.error(
				"getModelObject(" + className + ") ERROR " +
				e.getClass().getName() + ": " + e.getMessage());
			throw new RuntimeException(e);
		}

		return model;
	}

	protected Class<? extends Model> defaultModelClass = null;
	protected Map<String, Class<? extends Model>> modelClassMap = null;

	private static Log _log = LogFactoryUtil.getLog(ModelFactory.class);

}