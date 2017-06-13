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

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.model.ClassedModel;

/**
 * @author Jorge Díaz
 */
public abstract class ServiceImpl implements Service {

	public ClassedModel addObject(ClassedModel object) {
		throw new UnsupportedOperationException();
	}

	public ClassedModel createObject(long primaryKey) {
		throw new UnsupportedOperationException();
	}

	public ClassedModel deleteObject(ClassedModel object) {
		throw new UnsupportedOperationException();
	}

	public ClassedModel deleteObject(long primaryKey) {
		throw new UnsupportedOperationException();
	}

	public ClassedModel fetchObject(long primaryKey) {
		throw new UnsupportedOperationException();
	}

	public abstract ClassLoader getClassLoader();

	public String getClassPackageName() {
		return classPackageName;
	}

	public String getClassSimpleName() {
		return classSimpleName;
	}

	public Class<?> getLiferayModelImplClass() {
		if (liferayModelImplClassIsNull) {
			return null;
		}

		if (liferayModelImplClass == null) {
			String liferayModelImpl = ServiceUtil.getLiferayModelImplClassName(
				this);

			if (liferayModelImpl == null) {
				liferayModelImplClassIsNull = true;
				return null;
			}

			liferayModelImplClass = ServiceUtil.getLiferayModelImplClass(
				getClassLoader(), liferayModelImpl);

			if (liferayModelImplClass == null) {
				liferayModelImplClassIsNull = true;
				return null;
			}
		}

		return liferayModelImplClass;
	}

	public ClassedModel updateObject(ClassedModel object) {
		throw new UnsupportedOperationException();
	}

	protected String className = null;
	protected String classPackageName = null;
	protected String classSimpleName = null;
	protected Class<?> liferayModelImplClass = null;
	protected boolean liferayModelImplClassIsNull = false;

	private static Log _log = LogFactoryUtil.getLog(ServiceImpl.class);

}