package com.jorgediaz.indexchecker.index;

import com.liferay.portal.kernel.util.MethodKey;
import com.liferay.portal.kernel.util.PortalClassInvoker;
import com.liferay.portal.kernel.util.PortalClassLoaderUtil;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

public class IndexWrapperLuceneReflection extends IndexWrapperLucene{

	protected Class<?> indexReaderClass = null;
	protected Class<?> termClass = null;
	protected Class<?> termEnumClass = null;
	protected Method getIndexReader = null;
	protected Method terms = null;
	protected Method numDocs = null;
	protected Method maxDoc = null;
	protected Method isDeleted = null;
	protected Method document = null;

	protected static Object getIndexSearcher(long companyId)
			throws ClassNotFoundException, Exception {
	
		//Ejecutamos desde Portlet: 
		//		IndexSearcher indexSearcher = LuceneHelperUtil.getIndexSearcher(company.getCompanyId());

		Class<?> luceneHelperUtil = PortalClassLoaderUtil.getClassLoader().loadClass("com.liferay.portal.search.lucene.LuceneHelperUtil");
		MethodKey getIndexSearcher1 = new MethodKey(luceneHelperUtil,"getIndexSearcher",long.class);
		MethodKey getIndexSearcher = getIndexSearcher1;
		return PortalClassInvoker.invoke(false,getIndexSearcher, companyId);
	}

	public IndexWrapperLuceneReflection(long companyId) {

		Object indexSearcher;
		try {
			indexSearcher = IndexWrapperLuceneReflection.getIndexSearcher(companyId);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		try {
			getIndexReader = indexSearcher.getClass().getMethod("getIndexReader");
			index = getIndexReader.invoke(indexSearcher);
			indexReaderClass = index.getClass().getSuperclass().getSuperclass(); /* ReadOnlyDirectoryReader => DirectoryReader => IndexReader*/
			termClass = indexReaderClass.getClassLoader().loadClass("org.apache.lucene.index.Term");
			termEnumClass = indexReaderClass.getClassLoader().loadClass("org.apache.lucene.index.TermEnum");
			terms = indexReaderClass.getMethod("terms", termClass);
			numDocs = indexReaderClass.getMethod("numDocs");
			maxDoc = indexReaderClass.getMethod("maxDoc");
			isDeleted = indexReaderClass.getMethod("isDeleted", int.class);
			document = indexReaderClass.getMethod("document", int.class);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	@Override
	public Set<String> getTermValues(String field) {

		Set<String> values=new HashSet<String>();
		try {
			Object termObj = termClass.getConstructor(String.class).newInstance(field);
			Object termEnum = terms.invoke(index, termObj);
			Method termMethod = termEnumClass.getMethod("term");
			Method nextMethod = termEnumClass.getMethod("next");
			Method termTextMethod = termClass.getMethod("text");
			Method termFieldMethod = termClass.getMethod("field");
			Object currTermObj = termMethod.invoke(termEnum);
			while((currTermObj != null) && ((String)termFieldMethod.invoke(currTermObj)).equals(field)) {
				values.add((String) termTextMethod.invoke(currTermObj));
				nextMethod.invoke(termEnum);
				currTermObj = termMethod.invoke(termEnum);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		return values;
	}

	@Override
	public int numDocs() {
		if(index == null) {
			return -1;
		}
		try {
			return (Integer) numDocs.invoke(index);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	@Override
	protected int maxDoc() {
		if(index == null) {
			return 0;
		}
		try {
			return (Integer) maxDoc.invoke(index);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	@Override
	protected boolean isDeleted(int i) {
		if(index == null) {
			return true;
		}
		try {
			return (Boolean) isDeleted.invoke(index,i);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	@Override
	protected DocumentWrapper document(int i) {
		if(index == null) {
			return null;
		}
		try {
			return new DocumentWrapperLuceneReflection(document.invoke(index,i));
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
}
