package com.jorgediaz.util.model;

import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.search.Indexer;
import com.liferay.portal.model.ClassedModel;

import java.util.List;

public interface Model {

	public void init(ModelFactory modelFactory, Class<? extends ClassedModel> modelClass) throws Exception;

	public String getFullClassName();

	public Class<?> getModelClass();

	public boolean modelExtendsClass(Class<?> clazz);

	public boolean hasIndexer();

	public Indexer getIndexer();

	public String getAttributes();

	public String[] getAttributesArray();

	public String getPrimaryKeyAttribute();

	public boolean hasGroupId();

	public DynamicQuery newDynamicQuery();

	public DynamicQuery newDynamicQuery(Class<? extends ClassedModel> clazz);

	public List<?> executeDynamicQuery(DynamicQuery dynamicQuery) throws Exception;

	public List<?> executeDynamicQuery(Class<? extends ClassedModel> clazz, DynamicQuery dynamicQuery) throws Exception;

}
