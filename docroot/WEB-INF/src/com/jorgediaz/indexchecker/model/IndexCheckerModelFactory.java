package com.jorgediaz.indexchecker.model;

import com.jorgediaz.util.model.Model;
import com.jorgediaz.util.model.ModelFactory;

import com.liferay.portal.kernel.bean.ClassLoaderBeanHandler;
import com.liferay.portal.kernel.search.BaseIndexer;
import com.liferay.portal.kernel.search.Indexer;
import com.liferay.portal.model.ClassedModel;

import java.lang.reflect.Proxy;

import java.util.HashMap;
import java.util.Map;
public class IndexCheckerModelFactory extends ModelFactory {

	public static Class<? extends Model> defaultModelClass = DefaultModelIndexChecker.class;

	public static Map<String, Class<? extends Model>> modelClassMap = new HashMap<String, Class<? extends Model>>();

	static {
		modelClassMap.put("com.liferay.portlet.asset.model.AssetEntry", NotIndexed.class);
		modelClassMap.put("com.liferay.portlet.calendar.model.CalendarBooking", CalendarBooking.class);
		modelClassMap.put("com.liferay.portal.model.Contact", Contact.class);
		modelClassMap.put("com.liferay.portlet.documentlibrary.model.DLFileEntry", DLFileEntry.class);
		modelClassMap.put("com.liferay.portlet.journal.model.JournalArticle", JournalArticle.class);
		modelClassMap.put("com.liferay.portlet.messageboards.model.MBMessage", MBMessage.class);
		modelClassMap.put("com.liferay.portlet.trash.model.TrashEntry", NotIndexed.class);
		modelClassMap.put("com.liferay.portal.model.User", User.class);
		modelClassMap.put("com.liferay.portlet.wiki.model.WikiNode", WikiNode.class);
		modelClassMap.put("com.liferay.portlet.wiki.model.WikiPage", WikiPage.class);
	}

	public IndexCheckerModelFactory() {
		super(defaultModelClass, modelClassMap);
	}

	public IndexCheckerModelFactory(Class<? extends Model> defaultModelClass, Map<String, Class<? extends Model>> modelClassMap) {
		super(defaultModelClass, modelClassMap);
	}

	@Override
	public Model getModelObject(Class<? extends ClassedModel> clazz) {

		Model model = super.getModelObject(clazz);

		if (!model.hasIndexer()) {
			return null;
		}

		BaseIndexer baseindexer = IndexCheckerModelFactory.getBaseIndexer(model.getIndexer());

		if (baseindexer == null || !baseindexer.isIndexerEnabled()) {
			return null;
		}

		return model;
	}

	protected static BaseIndexer getBaseIndexer(Indexer indexer) {
		BaseIndexer baseindexer = null;

		if (indexer instanceof BaseIndexer) {
			baseindexer = (BaseIndexer)indexer;
		}
		else if (indexer instanceof Proxy) {
			ClassLoaderBeanHandler classLoaderBeanHandler = (ClassLoaderBeanHandler)Proxy.getInvocationHandler(indexer);
			baseindexer = (BaseIndexer)classLoaderBeanHandler.getBean();
		}

		return baseindexer;
	}

}