package com.jorgediaz.indexchecker.index;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;

import java.lang.reflect.Method;
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