package com.jorgediaz.util.model;

import com.liferay.portal.kernel.dao.orm.Criterion;
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.search.Indexer;
import com.liferay.portal.model.ClassedModel;

import java.util.List;

public interface Model extends Cloneable {

	public void init(ModelFactory modelFactory, Class<? extends ClassedModel> modelClass) throws Exception;

	public String getName();

	public void setNameSuffix(String suffix);

	public String getClassName();

	public Class<?> getModelClass();

	public boolean modelExtendsClass(Class<?> clazz);

	public boolean hasIndexer();

	public Indexer getIndexer();

	public Object[][] getAttributes();

	public String[] getAttributesName();

	public int[] getAttributesType();

	public int getAttributePos(String name);

	public int getAttributeType(String name);

	public String getPrimaryKeyAttribute();

	public String[] getPrimaryKeyMultiAttribute();

	public boolean hasAttribute(String attribute);

	public boolean hasGroupId();

	public DynamicQuery newDynamicQuery();

	public DynamicQuery newDynamicQuery(String alias);

	public DynamicQuery newDynamicQuery(Class<? extends ClassedModel> clazz);

	public DynamicQuery newDynamicQuery(Class<? extends ClassedModel> clazz, String alias);

	public List<?> executeDynamicQuery(DynamicQuery dynamicQuery) throws Exception;

	public List<?> executeDynamicQuery(Class<? extends ClassedModel> clazz, DynamicQuery dynamicQuery) throws Exception;

	public ClassedModel fetchObject(long primaryKey);

	public ClassedModel fetchObject(Class<? extends ClassedModel> clazz, long primaryKey);

	public Criterion getFilter();

	public void setFilter(Criterion filter);

	public int[] getValidStatuses();

	public Model clone();

}
