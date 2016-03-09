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

import com.liferay.portal.kernel.dao.orm.Criterion;
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.model.ClassedModel;
import com.liferay.portal.service.BaseLocalService;

import java.util.List;

/**
 * @author Jorge Díaz
 */
public interface Service extends Cloneable {

	public ClassedModel addObject(ClassedModel object);

	public Service clone();

	public ClassedModel createObject(long primaryKey);

	public ClassedModel deleteObject(ClassedModel object);

	public ClassedModel deleteObject(long primaryKey);

	public List<?> executeDynamicQuery(DynamicQuery dynamicQuery)
		throws Exception;

	public Object executeServiceMethod(
		String methodName, Class<?> parameterType, Object arg);

	public ClassedModel fetchObject(long primaryKey);

	public Criterion getFilter();

	public Class<?> getLiferayModelImplClass() throws Exception;

	public void init(
		BaseLocalService modelService, String classPackageName,
		String classSimpleName);

	public void init(Class<? extends ClassedModel> classInterface);

	public DynamicQuery newDynamicQuery();

	public void setFilter(Criterion filter);

	public ClassedModel updateObject(ClassedModel object);

}