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

package jorgediazest.util.modelquery;

import com.liferay.portal.kernel.concurrent.ConcurrentHashSet;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.model.Portlet;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jorgediazest.util.data.DataComparator;
import jorgediazest.util.data.DataModelComparator;
import jorgediazest.util.model.Model;
import jorgediazest.util.model.ModelFactory;

/**
 * @author Jorge Díaz
 */
public class ModelQueryFactory {

	public ModelQueryFactory(ModelFactory modelFactory) throws Exception {
		this(modelFactory, null);
	}

	public ModelQueryFactory(
		ModelFactory modelFactory,
		ModelQueryClassFactory mqClassFactory) throws Exception {

		if (modelFactory == null) {
			throw new Exception("modelFactory cannot be null");
		}

		this.modelFactory = modelFactory;

		if (mqClassFactory != null) {
			this.mqClassFactory = mqClassFactory;
		}
	}

	public DataComparatorFactory getDataComparatorFactory() {
		return dataComparatorFactory;
	}

	public ModelQuery getModelQueryObject(Class<?> clazz) {
		return getModelQueryObject(clazz.getName());
	}

	public ModelQuery getModelQueryObject(Model model) {

		String className = model.getClassName();

		return getModelQueryObject(className);
	}

	public ModelQuery getModelQueryObject(String className) {
		if (Validator.isNull(className)) {
			return null;
		}

		if (cacheNullModelObject.contains(className)) {
			return null;
		}

		ModelQuery modelDataAccess = cacheModelObject.get(className);

		if (modelDataAccess != null) {
			return modelDataAccess;
		}

		Model model = modelFactory.getModelObject(className);

		if (model != null) {
			modelDataAccess = this.getModelQueryObjectAux(model);
		}

		if (modelDataAccess == null) {
			cacheNullModelObject.add(className);
			return null;
		}

		cacheModelObject.put(className, modelDataAccess);
		return modelDataAccess;
	}

	public void setDataComparatorFactory(
		DataComparatorFactory dataComparatorFactory) {

		this.dataComparatorFactory = dataComparatorFactory;
	}

	public interface DataComparatorFactory {
		public DataComparator getDataComparator(ModelQuery model);
	}

	public interface ModelQueryClassFactory {
		public Class<? extends ModelQuery> getModelQueryClass(String className);
	}

	protected ModelQuery getModelQueryObjectAux(Model model) {

		String className = model.getClassName();

		Class<? extends ModelQuery> modelClass = null;

		ModelQuery modelDataAccess = null;
		try {
			modelClass = mqClassFactory.getModelQueryClass(className);

			if (modelClass == null) {
				return null;
			}

			modelDataAccess = (ModelQuery)modelClass.newInstance();

			modelDataAccess.setModelQueryFactory(this);

			modelDataAccess.init(model, dataComparatorFactory);
		}
		catch (Exception e) {
			if (_log.isWarnEnabled()) {
				_log.warn(
					"getModelObject(" + className + ") EXCEPTION " +
					e.getClass().getName() + ": " + e.getMessage());
			}

			modelDataAccess = null;
		}

		return modelDataAccess;
	}

	protected Map<String, ModelQuery> cacheModelObject =
		new ConcurrentHashMap<String, ModelQuery>();
	protected Set<String> cacheNullModelObject =
		new ConcurrentHashSet<String>();

	protected DataComparatorFactory dataComparatorFactory =
		new DataComparatorFactory() {

		protected DataComparator dataComparator = new DataModelComparator(
			new String[] {
				"createDate", "status", "version", "name", "title",
				"description", "size" });

		@Override
		public DataComparator getDataComparator(ModelQuery model) {
			return dataComparator;
		}

	};

	protected Map<String, Set<Portlet>> handlerPortletMap =
		new HashMap<String, Set<Portlet>>();
	protected ModelFactory modelFactory = null;

	protected ModelQueryClassFactory mqClassFactory =
		new ModelQueryClassFactory() {

		@Override
		public Class<? extends ModelQuery> getModelQueryClass(
			String className) {

			return DefaultModelQuery.class;
		}

	};

	private static Log _log = LogFactoryUtil.getLog(ModelQueryFactory.class);

}