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
		if ((_configuration != null) && !_isOutdatedConfiguration()) {
			return _configuration;
		}

		synchronized (RemoteConfigurationUtil.class) {
			if ((_configuration == null) || _isOutdatedConfiguration()) {
				String remoteConfigurationUrl =
					(String)ConfigurationUtil.getConfigurationEntry(
						"remoteConfigurationUrl");

				_configuration = _readConfiguration(remoteConfigurationUrl);

				_configurationTimestamp = System.currentTimeMillis();
			}

			return _configuration;
		}
	}

	private static boolean _isOutdatedConfiguration() {
		long dayTimeMillis = GetterUtil.getLong(
			ConfigurationUtil.getConfigurationEntry(
				"remoteConfigurationTimeoutMilis"));

		long validCfgTimestamp = _configurationTimestamp + dayTimeMillis;

		if (validCfgTimestamp < System.currentTimeMillis()) {
			return true;
		}

		return false;
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> _readConfiguration(
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
			if (_log.isDebugEnabled()) {
				_log.warn("Cannot connect to github " + e.getMessage(), e);
			}
			else if (_log.isWarnEnabled()) {
				_log.warn("Cannot connect to github " + e.getMessage());
			}

			tempConfiguration =
				(Map<String, Object>)ConfigurationUtil.getConfigurationEntry(
					"remoteConfigurationBackup");
		}

		if (tempConfiguration == null) {
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

	private static Map<String, Object> _configuration = null;
	private static long _configurationTimestamp = 0;

}