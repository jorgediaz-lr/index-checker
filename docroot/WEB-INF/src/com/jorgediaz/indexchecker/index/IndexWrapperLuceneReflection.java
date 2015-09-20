package com.jorgediaz.indexchecker.index;

import com.liferay.portal.kernel.util.MethodKey;
import com.liferay.portal.kernel.util.PortalClassInvoker;
import com.liferay.portal.kernel.util.PortalClassLoaderUtil;

import java.lang.reflect.Method;

public class IndexWrapperLuceneReflection extends IndexWrapper{

	protected Method getIndexReader = null;
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
			Class<?> indexReaderClass = index.getClass().getSuperclass().getSuperclass(); /* ReadOnlyDirectoryReader => DirectoryReader => IndexReader*/
			numDocs = indexReaderClass.getMethod("numDocs");
			maxDoc = indexReaderClass.getMethod("maxDoc");
			isDeleted = indexReaderClass.getMethod("isDeleted", int.class);
			document = indexReaderClass.getMethod("document", int.class);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public int numDocs() {
		if(index == null) {
			return 0;
		}
		try {
			return (Integer) numDocs.invoke(index);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public int maxDoc() {
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

	public boolean isDeleted(int i) {
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

	public DocumentWrapper document(int i) {
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
