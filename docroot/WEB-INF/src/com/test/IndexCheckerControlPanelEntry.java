package com.test;

import com.liferay.portal.model.Portlet;
import com.liferay.portal.security.permission.PermissionChecker;
import com.liferay.portlet.BaseControlPanelEntry;

/**
 * Control panel entry class IndexCheckerControlPanelEntry
 */
public class IndexCheckerControlPanelEntry extends BaseControlPanelEntry {

	@Override
	public boolean isVisible(PermissionChecker permissionChecker, Portlet portlet)
			throws Exception {
		return true;
	}

}