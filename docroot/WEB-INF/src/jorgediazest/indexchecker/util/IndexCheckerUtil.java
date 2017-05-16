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

package jorgediazest.indexchecker.util;

import java.lang.reflect.Method;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.module.configuration.ConfigurationProviderUtil;

import jorgediazest.util.reflection.ReflectionUtil;

/**
 * @author Jorge Díaz
 */
public class IndexCheckerUtil {

	public static Object getCompanyConfigurationKey(
			long companyId, ClassLoader classLoader,
			String configurationClassName, String configurationKey)
		throws Exception {

		Class<?> configurationClass = classLoader.loadClass(
				configurationClassName);

		return getCompanyConfigurationKey(
				companyId, configurationClass, configurationKey);
	}

	public static Object getCompanyConfigurationKey(
			long companyId, Class<?> configurationClass,
			String configurationKey)
		throws Exception {

		Object journalServiceConfiguration =
			ConfigurationProviderUtil.getCompanyConfiguration(
				configurationClass, companyId);

		Method indexAllVersionsMethod =
			configurationClass.getMethod(configurationKey);

		return indexAllVersionsMethod.invoke(journalServiceConfiguration);
	}

	private static Log _log = LogFactoryUtil.getLog(IndexCheckerUtil.class);

	public static String getPortletPropertiesKey(
			ClassLoader classLoader, String configurationClassName,
			String configurationKey)
		throws ClassNotFoundException {
	
		Class<?> portletServiceConfigurationValuesClass =
			classLoader.loadClass(configurationClassName);
	
		return ReflectionUtil.getWrappedStaticString(
				portletServiceConfigurationValuesClass,
					configurationKey);
	}

}