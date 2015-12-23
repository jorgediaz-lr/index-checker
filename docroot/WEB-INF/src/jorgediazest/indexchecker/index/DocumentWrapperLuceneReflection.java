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

package jorgediazest.indexchecker.index;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;

import java.lang.reflect.Method;

/**
 * @author Jorge Díaz
 */
public class DocumentWrapperLuceneReflection extends DocumentWrapper {

	public DocumentWrapperLuceneReflection(Object document) {
		super(document);

		if (getMethod == null) {
			try {
				getMethod = document.getClass().getMethod("get", String.class);
			}
			catch (Exception e) {
				_log.error(
					"Error: " + e.getClass() + " - " + e.getMessage(), e);
				throw new RuntimeException(e);
			}
		}
	}

	public String get(String key) {
		try {
			return (String)getMethod.invoke(document, key);
		}
		catch (Exception e) {
			_log.error("Error: " + e.getClass() + " - " + e.getMessage(), e);
			throw new RuntimeException(e);
		}
	}

	private static Log _log = LogFactoryUtil.getLog(
		DocumentWrapperLuceneReflection.class);

	private static Method getMethod = null;

}