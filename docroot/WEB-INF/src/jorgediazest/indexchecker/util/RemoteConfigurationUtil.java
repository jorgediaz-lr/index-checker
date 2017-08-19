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

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.HttpUtil;
import com.liferay.portal.kernel.util.ReleaseInfo;
import com.liferay.portal.kernel.util.Validator;

import java.util.Collections;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

/**
 * @author Jorge Díaz
 */
public class RemoteConfigurationUtil {

	public static Object getConfigurationEntry(String configurationEntry) {
		Map<String, Object> configuration = getConfiguration();

		if (configuration == null) {
			return null;
		}

		return configuration.get(configurationEntry);
	}

	protected static Map<String, Object> getConfiguration() {
		if ((configuration != null) && !isOutdatedConfiguration()) {
			return configuration;
		}

		synchronized(RemoteConfigurationUtil.class) {
			if ((configuration == null) || isOutdatedConfiguration()) {
				String remoteConfigurationUrl =
					(String)ConfigurationUtil.getConfigurationEntry(
						"remoteConfigurationUrl");

				configuration = readConfiguration(remoteConfigurationUrl);

				configurationTimestamp = System.currentTimeMillis();
			}

			return configuration;
		}
	}

	private static boolean isOutdatedConfiguration() {
		long dayTimeMillis = GetterUtil.getLong(
			ConfigurationUtil.getConfigurationEntry(
				"remoteConfigurationTimeoutMilis"), 0L);
		long validCfgTimestamp = (configurationTimestamp + dayTimeMillis);

		return (validCfgTimestamp < System.currentTimeMillis());
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> readConfiguration(
			String configurationURL) {

		Map<String, Object> tempConfiguration;

		try {
			String configurationContent = HttpUtil.URLtoString(
				configurationURL);

			Yaml yaml = new Yaml();

			tempConfiguration = (Map<String, Object>)yaml.load(
				configurationContent);
		}
		catch (Exception e) {
			_log.error(e, e);

			tempConfiguration =
				(Map<String, Object>)ConfigurationUtil.getConfigurationEntry(
					"remoteConfigurationBackup");
		}

		if (Validator.isNull(tempConfiguration)) {
			return Collections.emptyMap();
		}

		int liferayBuildNumber = ReleaseInfo.getBuildNumber();

		if (tempConfiguration.containsKey(liferayBuildNumber)) {
			return (Map<String, Object>)tempConfiguration.get(
				liferayBuildNumber);
		}

		int liferayVersion = liferayBuildNumber / 100;

		if (tempConfiguration.containsKey(liferayVersion)) {
			return (Map<String, Object>)tempConfiguration.get(liferayVersion);
		}

		return Collections.emptyMap();
	}

	private static Log _log = LogFactoryUtil.getLog(
		RemoteConfigurationUtil.class);

	private static Map<String, Object> configuration = null;
	private static long configurationTimestamp = 0;

}