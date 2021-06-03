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

package jorgediazest.util.model;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.Validator;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jorgediazest.util.service.Service;
import jorgediazest.util.service.ServiceUtil;

/**
 * @author Jorge Díaz
 */
public class ModelFactory {

	public Model getModelObject(Class<?> clazz) {
		return getModelObject(clazz.getName());
	}

	public Model getModelObject(String className) {
		String key = className;

		if (Validator.isNull(key)) {
			return null;
		}

		if (cacheNullModelObject.contains(key)) {
			return null;
		}

		Model model = cacheModelObject.get(key);

		if (model != null) {
			return model;
		}

		Service service = ServiceUtil.getService(className);

		if (service != null) {
			model = getModelObject(service);
		}

		if (model == null) {
			cacheNullModelObject.add(key);
			return null;
		}

		cacheModelObject.put(key, model);
		return model;
	}

	protected Model getModelObject(Service service) {
		String className = service.getClassName();

		Model model = null;
		try {
			model = new ModelImpl(this, className, service);

			if (model.getAttributesName() == null) {
				throw new Exception(
					model.getName() + " error retrieving attributes");
			}

			if (model.count()==-1) {
				model = null;
			}
		}
		catch (Exception e) {
			if (_log.isWarnEnabled()) {
				_log.warn(
					"getModelObject(" + className + ") EXCEPTION " +
					e.getClass().getName() + ": " + e.getMessage());
			}

			model = null;
		}

		return model;
	}

	protected Map<String, Model> cacheModelObject =
		new ConcurrentHashMap<String, Model>();
	protected Set<String> cacheNullModelObject =
		Collections.newSetFromMap(new ConcurrentHashMap<>());

	private static Log _log = LogFactoryUtil.getLog(ModelFactory.class);

}