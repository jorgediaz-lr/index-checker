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

import com.liferay.portal.configuration.module.configuration.ConfigurationProviderUtil;

import java.lang.reflect.Method;

import jorgediazest.util.reflection.ReflectionUtil;

/**
 * @author Jorge Díaz
 */
public class IndexCheckerUtil {

	public static Object getCompanyConfigurationKey(
			long companyId, Class<?> configurationClass,
			String configurationKey)
		throws Exception {

		Object journalServiceConfiguration =
			ConfigurationProviderUtil.getCompanyConfiguration(
				configurationClass, companyId);

		Method indexAllVersionsMethod = configurationClass.getMethod(
			configurationKey);

		return indexAllVersionsMethod.invoke(journalServiceConfiguration);
	}

	public static Object getCompanyConfigurationKey(
			long companyId, ClassLoader classLoader,
			String configurationClassName, String configurationKey)
		throws Exception {

		Class<?> configurationClass = classLoader.loadClass(
			configurationClassName);

		return getCompanyConfigurationKey(
			companyId, configurationClass, configurationKey);
	}

	public static String getPortletPropertiesKey(
			ClassLoader classLoader, String configurationClassName,
			String configurationKey)
		throws ClassNotFoundException {

		Class<?> portletServiceConfigurationValuesClass = classLoader.loadClass(
			configurationClassName);

		Object value = ReflectionUtil.getStaticFieldValue(
			portletServiceConfigurationValuesClass, configurationKey);

		if (value == null) {
			return null;
		}

		return value.toString();
	}

}