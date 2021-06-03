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

import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.util.portlet.PortletProps;

/**
 * @author Jorge Díaz
 */
public class PortletPropsValues {

	public static final int INDEX_SEARCH_LIMIT =
		GetterUtil.getInteger(
			PortletProps.get(PortletPropsKeys.INDEX_SEARCH_LIMIT), 10000);

	public static final int NUMBER_THREADS =
		GetterUtil.getInteger(
			PortletProps.get(PortletPropsKeys.NUMBER_THREADS), 1);

}