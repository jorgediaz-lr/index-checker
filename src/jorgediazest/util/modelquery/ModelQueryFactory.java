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

	public ModelFactory getModelFactory() {
		return modelFactory;
	}

	public ModelQuery getModelQueryObject(Class<?> clazz) {
		return getModelQueryObject(clazz.getName());
	}

	public ModelQuery getModelQueryObject(Model model) {
		if ((model == null) || Validator.isNull(model.getClassName())) {
			return null;
		}

		if (cacheNullModelObject.contains(model.getClassName())) {
			return null;
		}

		ModelQuery modelQuery = cacheModelObject.get(model);

		if (modelQuery != null) {
			return modelQuery;
		}

		modelQuery = getModelQueryObjectNoCached(model);

		if (modelQuery == null) {
			cacheNullModelObject.add(model.getClassName());
			return null;
		}

		cacheModelObject.put(model, modelQuery);
		return modelQuery;
	}

	public ModelQuery getModelQueryObject(String className) {
		Model model = modelFactory.getModelObject(className);

		if (model == null) {
			return null;
		}

		return this.getModelQueryObject(model);
	}

	public void setDataComparatorFactory(
		DataComparatorFactory dataComparatorFactory) {

		this.dataComparatorFactory = dataComparatorFactory;
	}

	public interface DataComparatorFactory {
		public DataComparator getDataComparator(ModelQuery query);
	}

	public interface ModelQueryClassFactory {
		public Class<? extends ModelQuery> getModelQueryClass(String className);
	}

	protected ModelQuery getModelQueryObjectNoCached(Model model) {

		String className = model.getClassName();

		Class<? extends ModelQuery> modelClass = null;

		ModelQuery modelQuery = null;
		try {
			modelClass = mqClassFactory.getModelQueryClass(className);

			if (modelClass == null) {
				return null;
			}

			modelQuery = (ModelQuery)modelClass.newInstance();

			modelQuery.setModelQueryFactory(this);

			modelQuery.init(model, dataComparatorFactory);
		}
		catch (Exception e) {
			if (_log.isWarnEnabled()) {
				_log.warn(
					"getModelObject(" + className + ") EXCEPTION " +
					e.getClass().getName() + ": " + e.getMessage());
			}

			modelQuery = null;
		}

		return modelQuery;
	}

	protected Map<Model, ModelQuery> cacheModelObject =
		new ConcurrentHashMap<Model, ModelQuery>();
	protected Set<String> cacheNullModelObject =
		new ConcurrentHashSet<String>();

	protected DataComparatorFactory dataComparatorFactory =
		new DataComparatorFactory() {

		protected DataComparator dataComparator = new DataModelComparator(
			new String[] {
				"createDate", "status", "version", "name", "title",
				"description", "size" });

		@Override
		public DataComparator getDataComparator(ModelQuery query) {
			return dataComparator;
		}

	};

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