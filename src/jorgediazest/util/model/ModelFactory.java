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

import com.liferay.exportimport.kernel.lar.PortletDataException;
import com.liferay.exportimport.kernel.lar.PortletDataHandler;
import com.liferay.exportimport.kernel.lar.PortletDataHandlerControl;
import com.liferay.portal.kernel.concurrent.ConcurrentHashSet;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Portlet;
import com.liferay.portal.kernel.service.PortletLocalServiceUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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

		fillClassNamePortletMapping();
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

	public Set<Portlet> getPortletSet(String className) {
		if (!classNamePortletMap.containsKey(className)) {
			return new HashSet<>();
		}

		return classNamePortletMap.get(className);
	}

	public interface ModelClassFactory {

		public Class<? extends Model> getModelClass(String className);

	}

	protected void fillClassNamePortletMapping() {
		for (Portlet portlet : PortletLocalServiceUtil.getPortlets()) {
			PortletDataHandler portletDataHandler =
				portlet.getPortletDataHandlerInstance();

			PortletDataHandlerControl[] pdhControlArr;

			try {
				pdhControlArr = portletDataHandler.getExportControls();
			}
			catch (PortletDataException pde) {
				_log.warn(pde, pde);

				continue;
			}

			for (PortletDataHandlerControl pdhControl : pdhControlArr) {
				addClassNamePortletMapping(pdhControl.getClassName(), portlet);
			}
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
	protected Map<String, Set<Portlet>> classNamePortletMap = new HashMap<>();
	protected Class<? extends Model> defaultModelClass = null;

	protected ModelClassFactory modelClassFactory = new ModelClassFactory() {

		@Override
		public Class<? extends Model> getModelClass(String className) {
			return DefaultModel.class;
		}

	};

	private void addClassNamePortletMapping(String className, Portlet portlet) {
		if (!classNamePortletMap.containsKey(className)) {
			classNamePortletMap.put(className, new HashSet<Portlet>());
		}

		Set<Portlet> portletSet = classNamePortletMap.get(className);

		if (!portletSet.contains(portlet)) {
			portletSet.add(portlet);

			if (_log.isDebugEnabled()) {
				_log.debug("Adding: " + className + " portlet " + portlet);
			}
		}
	}

	private static Log _log = LogFactoryUtil.getLog(ModelFactory.class);

}