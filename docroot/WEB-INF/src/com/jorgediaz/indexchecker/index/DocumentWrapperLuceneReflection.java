package com.jorgediaz.indexchecker.index;

import java.lang.reflect.Method;
public class DocumentWrapperLuceneReflection extends DocumentWrapper {

	public String get(String key) {
		try {
			return (String)getMethod.invoke(document, key);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	} public DocumentWrapperLuceneReflection(Object document) {
		super(document);

		if (getMethod == null) {
			try {
				getMethod = document.getClass().getMethod("get", String.class);
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
	}

	private static Method getMethod = null;

}