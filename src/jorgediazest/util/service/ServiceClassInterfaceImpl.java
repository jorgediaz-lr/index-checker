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
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.ClassedModel;
import com.liferay.portal.kernel.service.GroupLocalServiceUtil;

import java.util.List;

/**
 * @author Jorge Díaz
 */
public class ServiceClassInterfaceImpl extends ServiceImpl {

	public ServiceClassInterfaceImpl(
		Class<? extends ClassedModel> classInterface) {

		String classPackageName = classInterface.getPackage().getName();

		this.classSimpleName = classInterface.getSimpleName();
		this.className = classPackageName + "." + classSimpleName;
		this.classInterface = classInterface;
	}

	public List<?> executeDynamicQuery(DynamicQuery dynamicQuery)
		throws SystemException {

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

	static Log _log = LogFactoryUtil.getLog(ServiceClassInterfaceImpl.class);

}