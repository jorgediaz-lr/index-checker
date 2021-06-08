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
import com.liferay.portal.kernel.dao.orm.DynamicQueryFactoryUtil;
import com.liferay.portal.kernel.model.ClassedModel;
import com.liferay.portal.kernel.service.GroupLocalServiceUtil;

import java.util.List;

/**
 * @author Jorge Díaz
 */
public class ServiceClassInterfaceImpl extends ServiceImpl {

	public ServiceClassInterfaceImpl(
		Class<? extends ClassedModel> classInterface) {

		Package classPackage = classInterface.getPackage();

		this.classInterface = classInterface;

		classSimpleName = classInterface.getSimpleName();

		className = classPackage.getName() + "." + classSimpleName;
	}

	public List<?> executeDynamicQuery(DynamicQuery dynamicQuery) {
		return GroupLocalServiceUtil.dynamicQuery(dynamicQuery);
	}

	public ClassLoader getClassLoader() {
		return classInterface.getClassLoader();
	}

	public DynamicQuery newDynamicQuery() {
		return DynamicQueryFactoryUtil.forClass(
			classInterface, null, classInterface.getClassLoader());
	}

	public DynamicQuery newDynamicQuery(String alias) {
		return DynamicQueryFactoryUtil.forClass(
			classInterface, alias, classInterface.getClassLoader());
	}

	protected Class<? extends ClassedModel> classInterface = null;

}