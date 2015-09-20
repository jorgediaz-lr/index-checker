package com.jorgediaz.indexchecker.model;

import com.jorgediaz.indexchecker.data.Data;
import com.jorgediaz.util.model.ModelFactory;
import com.liferay.portal.kernel.dao.orm.Conjunction;
import com.liferay.portal.kernel.dao.orm.Property;
import com.liferay.portal.kernel.dao.orm.PropertyFactoryUtil;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.search.SearchException;
import com.liferay.portal.kernel.util.PrefsPropsUtil;
import com.liferay.portal.model.ClassedModel;


public class JournalArticle extends IndexCheckerModel {
/* TODO Falta añadir la ultima version cuando no hay ninguna version aprobada (LPS de Jose) */
	protected boolean indexAllVersions;

	@Override
	public void init(ModelFactory modelUtil, Class<? extends ClassedModel> clazz) throws Exception {
		super.init(modelUtil, clazz);

		try {
			indexAllVersions = PrefsPropsUtil.getBoolean("journal.articles.index.all.versions");
		} catch (SystemException e) {

			throw new RuntimeException(e);
		}

		if(!indexAllVersions) {
			this.removeIndexedAttribute("id");
			this.setIndexPrimaryKey("resourcePrimKey");
		}
	}

	@Override
	public void reindex(Data value) throws SearchException {
		if(indexAllVersions) {
			super.reindex(value);
		}
		else {
			getIndexer().reindex(this.getClassName(), value.getResourcePrimKey());
		}
	}

	@Override
	public int[] getValidStatuses() {
		if(indexAllVersions) {
			return null;
		}

		return super.getValidStatuses();
	}

	@Override
	public Conjunction generateQueryFilter() {
		
		Conjunction conjunction = super.generateQueryFilter();

		Property propertyClassnameid = PropertyFactoryUtil.forName("classNameId");

		conjunction.add(propertyClassnameid.eq(0L));

		Property propertyIndexable = PropertyFactoryUtil.forName("indexable");

		conjunction.add(propertyIndexable.eq(true));

		return conjunction;
	}
}