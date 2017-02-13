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

	public static final String DATA_COMPARATOR_ASSETENTRY_ATTRIBUTES =
		PortletProps.get(
			PortletPropsKeys.DATA_COMPARATOR_ASSETENTRY_ATTRIBUTES);

	public static final String DATA_COMPARATOR_BASIC_ATTRIBUTES =
		PortletProps.get(PortletPropsKeys.DATA_COMPARATOR_BASIC_ATTRIBUTES);

	public static final String DATA_COMPARATOR_BASIC_ATTRIBUTES_NOVERSION =
		PortletProps.get(
			PortletPropsKeys.DATA_COMPARATOR_BASIC_ATTRIBUTES_NOVERSION);

	public static final String DATA_COMPARATOR_CATEGORIESTAGS_ATTRIBUTES =
		PortletProps.get(
			PortletPropsKeys.DATA_COMPARATOR_CATEGORIESTAGS_ATTRIBUTES);

	public static final String DATA_COMPARATOR_DATE_ATTRIBUTES =
		PortletProps.get(PortletPropsKeys.DATA_COMPARATOR_DATE_ATTRIBUTES);

	public static final String DATA_COMPARATOR_DATE_ATTRIBUTES_USER =
		PortletProps.get(PortletPropsKeys.DATA_COMPARATOR_DATE_ATTRIBUTES_USER);

	public static final int NUMBER_THREADS =
		GetterUtil.getInteger(
				PortletProps.get(PortletPropsKeys.NUMBER_THREADS), 1);

}