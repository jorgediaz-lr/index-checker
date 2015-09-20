package com.jorgediaz.indexchecker.model;

import com.jorgediaz.indexchecker.data.Data;
import com.jorgediaz.util.model.ModelFactory;
import com.liferay.portal.kernel.dao.orm.Conjunction;
import com.liferay.portal.kernel.dao.orm.Criterion;
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.ProjectionFactoryUtil;
import com.liferay.portal.kernel.dao.orm.ProjectionList;
import com.liferay.portal.kernel.dao.orm.Property;
import com.liferay.portal.kernel.dao.orm.PropertyFactoryUtil;
import com.liferay.portal.kernel.dao.orm.RestrictionsFactoryUtil;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.search.SearchException;
import com.liferay.portal.kernel.util.PrefsPropsUtil;
import com.liferay.portal.model.ClassedModel;

import java.util.List;
import java.util.Map;


public class JournalArticle extends IndexCheckerModel {

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

	public Map<Long,Data> getLiferayData(Criterion filter) throws Exception {
		Map<Long,Data> dataMap = super.getLiferayData(filter);

		if(!indexAllVersions) {
			addMissingJournalArticles(filter, dataMap);
		}

		return dataMap;
	}

	public void addMissingJournalArticles(Criterion filter, Map<Long, Data> dataMap) throws Exception {
		DynamicQuery query = this.newDynamicQuery();

		ProjectionList projectionList = ProjectionFactoryUtil.projectionList();

		for (String attrib : getIndexAttributes()) {
			projectionList.add(ProjectionFactoryUtil.property(attrib));
		}

		query.setProjection(ProjectionFactoryUtil.distinct(projectionList));

		query.add(filter);

		DynamicQuery articleVersionDynamicQuery = this.newDynamicQuery("articleVersion");

		articleVersionDynamicQuery.setProjection(ProjectionFactoryUtil.alias(ProjectionFactoryUtil.max("articleVersion.version"), "articleVersion.version"));

		// We need to use the "this" default alias to make sure the
		// database engine handles this subquery as a correlated
		// subquery

		articleVersionDynamicQuery.add(RestrictionsFactoryUtil.eqProperty("this.resourcePrimKey", "articleVersion.resourcePrimKey"));

		query.add(PropertyFactoryUtil.forName("version").eq(articleVersionDynamicQuery));

		boolean indexAllVersionsOld = indexAllVersions;
		indexAllVersions = true;

		@SuppressWarnings("unchecked")
		List<Object[]> results = (List<Object[]>) this.executeDynamicQuery(query);

		indexAllVersions = indexAllVersionsOld;

		for(Object[] result : results) {
			long pk = (long) result[0];
			if(!dataMap.containsKey(pk)) {
				Data data = new Data(this);
				data.init(result);
				dataMap.put(data.getPrimaryKey(), data);
			}
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