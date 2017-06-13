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

package jorgediazest.util.service;

import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.model.ClassedModel;

import java.util.List;

/**
 * @author Jorge Díaz
 */
public interface Service {

	public ClassedModel addObject(ClassedModel object);

	public ClassedModel createObject(long primaryKey);

	public ClassedModel deleteObject(ClassedModel object);

	public ClassedModel deleteObject(long primaryKey);

	public List<?> executeDynamicQuery(DynamicQuery dynamicQuery)
		throws Exception;

	public ClassedModel fetchObject(long primaryKey);

	public ClassLoader getClassLoader();

	public String getClassPackageName();

	public String getClassSimpleName();

	public Class<?> getLiferayModelImplClass();

	public DynamicQuery newDynamicQuery();

	public DynamicQuery newDynamicQuery(String alias);

	public ClassedModel updateObject(ClassedModel object);

}