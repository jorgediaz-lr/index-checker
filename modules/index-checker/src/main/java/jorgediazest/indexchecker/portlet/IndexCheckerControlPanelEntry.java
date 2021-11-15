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

package jorgediazest.indexchecker.portlet;

import com.liferay.application.list.BasePanelApp;
import com.liferay.application.list.PanelApp;
import com.liferay.application.list.constants.PanelCategoryKeys;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.Portlet;
import com.liferay.portal.kernel.security.permission.PermissionChecker;
import com.liferay.portal.kernel.util.JavaConstants;
import com.liferay.portal.kernel.util.ResourceBundleUtil;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import jorgediazest.indexchecker.portlet.constants.IndexCheckerKeys;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Control panel entry class IndexCheckerControlPanelEntry
 *
 * @author Jorge Díaz
 */
@Component(
	immediate = true,
	property = "panel.category.key=" + PanelCategoryKeys.CONTROL_PANEL_APPS,
	service = PanelApp.class
)
public class IndexCheckerControlPanelEntry extends BasePanelApp {

	@Override
	public String getLabel(Locale locale) {
		try {
			ResourceBundle resourceBundle = ResourceBundleUtil.getBundle(
				locale, getClass());

			return LanguageUtil.get(
				resourceBundle,
				JavaConstants.JAVAX_PORTLET_TITLE + StringPool.PERIOD +
					getPortletId());
		}
		catch (MissingResourceException missingResourceException) {
			if (_log.isDebugEnabled()) {
				_log.debug(missingResourceException, missingResourceException);
			}
		}

		return LanguageUtil.get(
			locale,
			JavaConstants.JAVAX_PORTLET_TITLE + StringPool.PERIOD +
				getPortletId());
	}

	@Override
	public String getPortletId() {
		return IndexCheckerKeys.INDEXCHECKER;
	}

	@Override
	public boolean isShow(PermissionChecker permissionChecker, Group group)
		throws PortalException {

		if (permissionChecker.isOmniadmin() ||
			permissionChecker.isCompanyAdmin()) {

			return true;
		}

		return false;
	}

	@Override
	@Reference(
		target = "(javax.portlet.name=" + IndexCheckerKeys.INDEXCHECKER + ")",
		unbind = "-"
	)
	public void setPortlet(Portlet portlet) {
		super.setPortlet(portlet);
	}

	private static final Log _log = LogFactoryUtil.getLog(
		IndexCheckerControlPanelEntry.class);

}