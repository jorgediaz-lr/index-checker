package com.jorgediaz.util.model;

import com.liferay.portal.kernel.dao.orm.Criterion;
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.PropertyFactoryUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.search.Indexer;
import com.liferay.portal.kernel.search.IndexerRegistryUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.model.ClassedModel;
import com.liferay.portal.model.GroupedModel;

import java.util.List;
public abstract class ModelImpl implements Model {

	public Model clone() {
		ModelImpl model;
		try {
			model = this.getClass().newInstance();
			model.name = this.name;
			model.modelFactory = this.modelFactory;
			model.attributesString = this.attributesString;
			model.attributesArray = this.attributesArray;
			model.modelClass = this.modelClass;
			model.filter = this.filter;
		}
		catch (Exception e) {
			_log.error("Error executing clone");
			throw new RuntimeException(e);
		}

		return model;
	}

	public List<?> executeDynamicQuery(
		Class<? extends ClassedModel> clazz, DynamicQuery dynamicQuery)
			throws Exception {

		return modelFactory.executeDynamicQuery(clazz, dynamicQuery);
	}

	public List<?> executeDynamicQuery(DynamicQuery dynamicQuery)
		throws Exception {

		int[] statuses = this.getValidStatuses();

		if (statuses != null) {
			dynamicQuery.add(
				PropertyFactoryUtil.forName("status").in(statuses));

			if (_log.isDebugEnabled()) {
				_log.debug("adding filter by status: " + statuses);
			}
		}

		if (filter != null) {
			dynamicQuery.add(filter);

			if (_log.isDebugEnabled()) {
				_log.debug("adding custom filter: " + filter);
			}
		}

		return modelFactory.executeDynamicQuery(this.modelClass, dynamicQuery);
	}

	public ClassedModel fetchObject(
		Class<? extends ClassedModel> clazz, long primaryKey) {

		return modelFactory.fetchObject(clazz, primaryKey);
	}

	public ClassedModel fetchObject(long primaryKey) {
		return modelFactory.fetchObject(this.modelClass, primaryKey);
	}

	public int getAttributePos(String name) {
		Object[][] values = this.getAttributes();

		for (int i = 0; i < values.length; i++) {
			if (values[i][0].equals(name)) {
				return i;
			}
		}

		return -1;
	}

	public Object[][] getAttributes() {
		if (attributesArray == null) {
			attributesArray = modelFactory.getDatabaseAttributesArr(modelClass);
		}

		return attributesArray;
	}

	public String[] getAttributesName() {
		Object[][] values = this.getAttributes();

		String[] names = new String[values.length];

		for (int i = 0; i < values.length; i++) {
			names[i] = (String)values[i][0];
		}

		return names;
	}

	public int[] getAttributesType() {
		Object[][] values = this.getAttributes();

		int[] types = new int[values.length];

		for (int i = 0; i < values.length; i++) {
			types[i] = (int)values[i][1];
		}

		return types;
	}

	public int getAttributeType(String name) {
		int pos = this.getAttributePos(name);

		if (pos == -1) {
			return 0;
		}

		return (int)this.getAttributes()[pos][1];
	}

	public String getClassName() {
		return modelClass.getCanonicalName();
	}

	public Criterion getFilter() {
		return filter;
	}

	public Indexer getIndexer() {
		return IndexerRegistryUtil.nullSafeGetIndexer(modelClass);
	}

	public Class<?> getModelClass() {
		return modelClass;
	}

	public String getName() {
		if (name == null) {
			return getClassName();
		}

		return name;
	}

	public String getPrimaryKeyAttribute() {
		if (primaryKeyAttribute == null) {
			String[] arrDatabaseAttributes =
				getCreateTableAttributes().split(",");

			for (String attr : arrDatabaseAttributes) {
				String[] aux = attr.split(" ");

				if (aux.length < 2) {
					continue;
				}

				String col = aux[0];

				if (col.endsWith("_")) {
					col = col.substring(0, col.length() - 1);
				}

				if (attr.endsWith("not null primary key")) {
					primaryKeyAttribute = col;
				}
			}

			if (primaryKeyAttribute == null) {
				primaryKeyAttribute = StringPool.BLANK;
			}
		}

		return primaryKeyAttribute;
	}

	public String[] getPrimaryKeyMultiAttribute() {
		if (primaryKeyMultiAttribute == null) {
			String aux = modelFactory.getDatabaseAttributesStr(modelClass);

			if (aux.indexOf('#') > 0) {
				aux = aux.split("#")[1];
				primaryKeyMultiAttribute = aux.split(",");

				for (int i = 0; i < primaryKeyMultiAttribute.length; i++) {
					primaryKeyMultiAttribute[i] =
						primaryKeyMultiAttribute[i].trim();
				}
			}
			else {
				primaryKeyMultiAttribute = new String[0];
			}
		}

		return primaryKeyMultiAttribute;
	}

	public int[] getValidStatuses() {
		return null;
	}

	public boolean hasAttribute(String attribute) {
		Object[][] attributes = getAttributes();

		for (int i = 0; i < attributes.length; i++) {
			if (((String)attributes[i][0]).equals(attribute)) {
				return true;
			}
		}

		return false;
	}

	public boolean hasGroupId() {
		return this.modelExtendsClass(GroupedModel.class) ||
			 this.hasAttribute("groupId");
	}

	public boolean hasIndexer() {
		return (IndexerRegistryUtil.getIndexer(modelClass) != null);
	}

	public void init(
		ModelFactory modelFactory, Class<? extends ClassedModel> modelClass)
			throws Exception {

		this.modelFactory = modelFactory;
		this.modelClass = modelClass;
	}

	public boolean modelExtendsClass(Class<?> clazz) {
		return clazz.isAssignableFrom(this.modelClass);
	}

	public DynamicQuery newDynamicQuery() {
		return modelFactory.newDynamicQuery(this.modelClass);
	}

	public DynamicQuery newDynamicQuery(Class<? extends ClassedModel> clazz) {
		return modelFactory.newDynamicQuery(clazz);
	}

	public DynamicQuery newDynamicQuery(
		Class<? extends ClassedModel> clazz, String alias) {

		return modelFactory.newDynamicQuery(clazz, alias);
	}

	public DynamicQuery newDynamicQuery(String alias) {
		return modelFactory.newDynamicQuery(this.modelClass, alias);
	}

	public void setFilter(Criterion filter) {
		this.filter = filter;
	}

	public void setNameSuffix(String suffix) {
		this.name = getClassName() + "_" + suffix;
	}

	public String toString() {
		return getName();
	}

	protected String getCreateTableAttributes() {
		if (attributesString == null) {
			String aux = modelFactory.getDatabaseAttributesStr(modelClass);

			if (aux.indexOf('#') > 0) {
				aux = aux.split("#")[0];
			}

			attributesString = aux;
		}

		return attributesString;
	}

	protected Object[][] attributesArray = null;
	protected String attributesString = null;
	protected Criterion filter = null;
	protected Class<? extends ClassedModel> modelClass = null;
	protected ModelFactory modelFactory = null;
	protected String name = null;

	/**
	 * primaries keys can be at following ways:
	 *
	 * - single => create table UserGroupGroupRole (userGroupId LONG not
	 * null,groupId LONG not null,roleId LONG not null,primary key (userGroupId,
	 * groupId, roleId))";
	 *
	 * - multi => create table JournalArticle (uuid_ VARCHAR(75) null,id_ LONG
	 * not null primary key,resourcePrimKey LONG,groupId LONG,companyId
	 * LONG,userId LONG,userName VARCHAR(75) null,createDate DATE
	 * null,modifiedDate DATE null,folderId LONG,classNameId LONG,classPK
	 * LONG,treePath STRING null,articleId VARCHAR(75) null,version DOUBLE,title
	 * STRING null,urlTitle VARCHAR(150) null,description TEXT null,content TEXT
	 * null,type_ VARCHAR(75) null,structureId VARCHAR(75) null,templateId
	 * VARCHAR(75) null,layoutUuid VARCHAR(75) null,displayDate DATE
	 * null,expirationDate DATE null,reviewDate DATE null,indexable
	 * BOOLEAN,smallImage BOOLEAN,smallImageId LONG,smallImageURL STRING
	 * null,status INTEGER,statusByUserId LONG,statusByUserName VARCHAR(75)
	 * null,statusDate DATE null)
	 */
	protected String primaryKeyAttribute = null;

	protected String[] primaryKeyMultiAttribute = null;

	private static Log _log = LogFactoryUtil.getLog(ModelImpl.class);

}