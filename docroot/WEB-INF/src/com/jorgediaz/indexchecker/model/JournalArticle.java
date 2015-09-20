package com.jorgediaz.indexchecker.model;

import com.jorgediaz.indexchecker.data.Data;
import com.jorgediaz.util.model.ModelUtil;
import com.liferay.portal.kernel.dao.orm.Conjunction;
import com.liferay.portal.kernel.dao.orm.Property;
import com.liferay.portal.kernel.dao.orm.PropertyFactoryUtil;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.search.SearchException;
import com.liferay.portal.kernel.util.PrefsPropsUtil;
import com.liferay.portal.model.ClassedModel;


public class JournalArticle extends BaseModelIndexChecker {
/* TODO Falta añadir la ultima version cuando no hay ninguna version aprobada (LPS de Jose) */
	protected boolean indexAllVersions;

	@Override
	public void init(ModelUtil modelUtil, Class<? extends ClassedModel> clazz) throws Exception {
		super.init(modelUtil, clazz);

		try {
			indexAllVersions = PrefsPropsUtil.getBoolean("journal.articles.index.all.versions");
		} catch (SystemException e) {

			throw new RuntimeException(e);
		}

		if(!indexAllVersions) {
			this.removeIndexedAttribute("id_");
			this.setIndexPrimaryKey("resourcePrimKey");
		}
	}

	public void reindex(Data value) throws SearchException {
		if(indexAllVersions) {
			super.reindex(value);
		}
		else {
			getIndexer().reindex(this.getFullClassName(), value.getResourcePrimKey());
		}
	}

	@Override
	public int[] getIndexedStatuses() {
		if(indexAllVersions) {
			return null;
		}

		return super.getIndexedStatuses();
	}


	public void addQueryCriterias(Conjunction conjunction) {
		
		super.addQueryCriterias(conjunction);

		Property propertyClassnameid = PropertyFactoryUtil.forName("classNameId");

		conjunction.add(propertyClassnameid.eq(0L));

		Property propertyIndexable = PropertyFactoryUtil.forName("indexable");

		conjunction.add(propertyIndexable.eq(true));
	}
}