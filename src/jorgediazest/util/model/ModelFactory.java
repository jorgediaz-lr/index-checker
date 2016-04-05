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
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.service.PortletLocalServiceUtil;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jorgediazest.util.data.DataComparator;
import jorgediazest.util.data.DataModelComparator;
import jorgediazest.util.reflection.ReflectionUtil;
import jorgediazest.util.service.Service;
import jorgediazest.util.service.ServiceUtil;

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

		fillHandlerPortletIdMap();
	}

	public ModelFactory(Map<String, Class<? extends Model>> modelClassMap) {
		this(null, modelClassMap);
	}

	public DataComparatorFactory getDataComparatorFactory() {
		return dataComparatorFactory;
	}

	public Map<String, Model> getModelMap(Collection<String> classNames) {

		List<ClassLoader> classLoaders = ModelUtil.getClassLoaders();

		Map<String, Model> modelMap = new LinkedHashMap<String, Model>();

		for (String classname : classNames) {
			if (Validator.isNull(classname) || !classname.contains(".model.")) {
				continue;
			}

			Model model = getModelObject(classLoaders, classname);

			if (model != null) {
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

		Service service = ServiceUtil.getService(
				classLoader, classPackageName, classSimpleName);

		if (service == null) {
			return null;
		}

		return getModelObject(classPackageName, classSimpleName, service);
	}

	public Model getModelObject(
		List<ClassLoader> classLoaders, String classname) {

		try {
			Model model = this.getModelObjectFromPortal(classname);

			if ((model != null) && (model.getAttributesName() != null)) {
				return model;
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

		for (ClassLoader classLoader : classLoaders) {
			try {
				Model model = this.getModelObject(classLoader, classname);

				if ((model != null) && (model.getAttributesName() != null)) {
					return model;
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
		}

		return null;
	}

	public Model getModelObject(
		String classPackageName, String classSimpleName, Service service) {

		String className = classPackageName + "." + classSimpleName;

		Class<? extends Model> modelClass = this.defaultModelClass;

		if ((this.modelClassMap != null) &&
			this.modelClassMap.containsKey(className)) {

			modelClass = this.modelClassMap.get(className);
		}

		Model model = null;
		try {
			model = (Model)modelClass.newInstance();

			model.setModelFactory(this);

			model.init(
				classPackageName, classSimpleName, service,
				dataComparatorFactory);
		}
		catch (Exception e) {
			if (_log.isWarnEnabled()) {
				_log.warn(
					"getModelObject(" + className + ") EXCEPTION " +
					e.getClass().getName() + ": " + e.getMessage());
			}

			throw new RuntimeException(e);
		}

		return model;
	}

	public final Model getModelObjectFromPortal(String className) {
		return getModelObject((ClassLoader)null, className);
	}

	public Set<Portlet> getPortletSet(Object handler) {

		Object handlerAux = ReflectionUtil.unWrapProxy(handler);

		if ((handlerAux == null) ||
			!handlerPortletMap.containsKey(handlerAux.getClass().getName())) {

			return new HashSet<Portlet>();
		}

		return handlerPortletMap.get(handlerAux.getClass().getName());
	}

	public void setDataComparatorFactory(
		DataComparatorFactory dataComparatorFactory) {

		this.dataComparatorFactory = dataComparatorFactory;
	}

	public interface DataComparatorFactory {
		public DataComparator getDataComparator(Model model);
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

	protected DataComparatorFactory dataComparatorFactory =
		new DataComparatorFactory() {

		protected DataComparator dataComparator = new DataModelComparator(
			new String[] {
				"createDate", "status", "version", "name", "title",
				"description", "size" });

		@Override
		public DataComparator getDataComparator(Model model) {
			return dataComparator;
		}

	};

	protected Class<? extends Model> defaultModelClass = null;
	protected Map<String, Set<Portlet>> handlerPortletMap =
		new HashMap<String, Set<Portlet>>();
	protected Map<String, Class<? extends Model>> modelClassMap = null;

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