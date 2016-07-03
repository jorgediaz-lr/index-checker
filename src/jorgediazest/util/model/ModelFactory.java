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

import com.liferay.portal.kernel.concurrent.ConcurrentHashSet;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Portlet;
import com.liferay.portal.kernel.service.PortletLocalServiceUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jorgediazest.util.reflection.ReflectionUtil;
import jorgediazest.util.service.Service;
import jorgediazest.util.service.ServiceUtil;

/**
 * @author Jorge Díaz
 */
public class ModelFactory {

	public ModelFactory() {
		this(null);
	}

	public ModelFactory(ModelClassFactory modelClassFactory) {
		if (modelClassFactory != null) {
			this.modelClassFactory = modelClassFactory;
		}

		fillHandlerPortletIdMap();
	}

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

		String classPackageName = StringPool.BLANK;
		String classSimpleName = className;

		int pos = className.lastIndexOf(".");

		if (pos > 0) {
			classPackageName = className.substring(0, pos);
			classSimpleName = className.substring(pos + 1, className.length());
		}

		Service service = ServiceUtil.getService(
			classPackageName, classSimpleName);

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

	public Set<Portlet> getPortletSet(Object handler) {
		Object handlerAux = ReflectionUtil.unWrapProxy(handler);

		if ((handlerAux == null) ||
			!handlerPortletMap.containsKey(handlerAux.getClass().getName())) {

			return new HashSet<>();
		}

		return handlerPortletMap.get(handlerAux.getClass().getName());
	}

	public interface ModelClassFactory {

		public Class<? extends Model> getModelClass(String className);

	}

	protected void fillHandlerPortletIdMap() {
		for (Portlet portlet : PortletLocalServiceUtil.getPortlets()) {
			addHandlersToMap(portlet.getIndexerClasses(), portlet);
			addHandlersToMap(
				portlet.getStagedModelDataHandlerClasses(), portlet);
			addHandlersToMap(portlet.getTrashHandlerClasses(), portlet);
			addHandlersToMap(portlet.getWorkflowHandlerClasses(), portlet);
		}
	}

	protected Model getModelObject(Service service) {
		String classPackageName = service.getClassPackageName();
		String classSimpleName = service.getClassSimpleName();

		String className = classPackageName + "." + classSimpleName;

		Class<? extends Model> modelClass = modelClassFactory.getModelClass(
			className);

		Model model = null;

		try {
			model = (Model)modelClass.newInstance();

			model.setModelFactory(this);

			model.init(classPackageName, classSimpleName, service);

			if (model.getAttributesName() == null) {
				throw new Exception(
					model.getName() + " error retrieving attributes");
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

	protected Map<String, Model> cacheModelObject = new ConcurrentHashMap<>();
	protected Set<String> cacheNullModelObject = new ConcurrentHashSet<>();
	protected Class<? extends Model> defaultModelClass = null;
	protected Map<String, Set<Portlet>> handlerPortletMap = new HashMap<>();

	protected ModelClassFactory modelClassFactory = new ModelClassFactory() {

		@Override
		public Class<? extends Model> getModelClass(String className) {
			return DefaultModel.class;
		}

	};

	private void addHandlersToMap(List<String> handlerList, Portlet portlet) {
		for (String handler : handlerList) {
			if (!handlerPortletMap.containsKey(handler)) {
				handlerPortletMap.put(handler, new HashSet<Portlet>());
			}

			Set<Portlet> portletSet = handlerPortletMap.get(handler);

			if (!portletSet.contains(portlet)) {
				portletSet.add(portlet);

				if (_log.isDebugEnabled()) {
					_log.debug("Adding: " + handler + " portlet " + portlet);
				}
			}
		}
	}

	private static Log _log = LogFactoryUtil.getLog(ModelFactory.class);

}