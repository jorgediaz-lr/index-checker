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

import jorgediazest.indexchecker.util.ConfigurationUtil;

import jorgediazest.util.model.ModelFactory;
import jorgediazest.util.modelquery.ModelQuery;
import jorgediazest.util.modelquery.ModelQueryFactory;

/**
 * @author Jorge Díaz
 */
public class IndexCheckerModelQueryFactory extends ModelQueryFactory {

	public IndexCheckerModelQueryFactory(ModelFactory modelFactory)
		throws Exception {

		super(modelFactory, indexCheckerClassFactory);
	}

	protected static ModelQueryClassFactory indexCheckerClassFactory =
		new ModelQueryClassFactory() {

		@Override
		public Class<? extends ModelQuery> getModelQueryClass(
			String className) {

			return ConfigurationUtil.getModelQueryClass(className);
		}

	};

}