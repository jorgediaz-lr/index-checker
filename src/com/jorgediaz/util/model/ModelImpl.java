/**
 * Space for Copyright
 */

package com.jorgediaz.util.model;

import com.liferay.portal.kernel.dao.orm.Conjunction;
import com.liferay.portal.kernel.dao.orm.Criterion;
import com.liferay.portal.kernel.dao.orm.Disjunction;
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.Projection;
import com.liferay.portal.kernel.dao.orm.ProjectionFactoryUtil;
import com.liferay.portal.kernel.dao.orm.ProjectionList;
import com.liferay.portal.kernel.dao.orm.Property;
import com.liferay.portal.kernel.dao.orm.PropertyFactoryUtil;
import com.liferay.portal.kernel.dao.orm.RestrictionsFactoryUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.search.Indexer;
import com.liferay.portal.kernel.search.IndexerRegistryUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.model.ClassedModel;
import com.liferay.portal.model.GroupedModel;
import com.liferay.portal.security.permission.ResourceActionsUtil;
import com.liferay.portal.util.PortalUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
public abstract class ModelImpl implements Model {

	@SuppressWarnings("unchecked")
	public ClassedModel addObject(ClassedModel object) {
		return modelFactory.executeOperation(
		(Class<? extends ClassedModel>)object.getModelClass(), "add",
		object.getModelClass(), object);
	}

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

	public ClassedModel createObject(
		Class<? extends ClassedModel> clazz, long primaryKey) {

		return modelFactory.executeOperation(
			clazz, "create", long.class, primaryKey);
	}

	public ClassedModel createObject(long primaryKey) {
		return this.createObject(this.modelClass, primaryKey);
	}

	public ClassedModel deleteObject(
		Class<? extends ClassedModel> clazz, long primaryKey) {

		return modelFactory.executeOperation(
			clazz, "delete", long.class, primaryKey);
	}

	@SuppressWarnings("unchecked")
	public ClassedModel deleteObject(ClassedModel object) {
		return modelFactory.executeOperation(
		(Class<? extends ClassedModel>)object.getModelClass(), "delete",
		object.getModelClass(), object);
	}

	public ClassedModel deleteObject(long primaryKey) {
		return this.deleteObject(this.modelClass, primaryKey);
	}

	public List<?> executeDynamicQuery(
		Class<? extends ClassedModel> clazz, DynamicQuery dynamicQuery)
			throws Exception {

		return modelFactory.executeDynamicQuery(clazz, dynamicQuery);
	}

	public List<?> executeDynamicQuery(DynamicQuery dynamicQuery)
		throws Exception {

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

		return modelFactory.executeOperation(
			clazz, "fetch", long.class, primaryKey);
	}

	public ClassedModel fetchObject(long primaryKey) {
		return this.fetchObject(this.modelClass, primaryKey);
	}

	public Criterion generateCriterionFilter(String stringFilter) {

		Conjunction conjuntion = RestrictionsFactoryUtil.conjunction();

		String[] allFiltersArr = stringFilter.split(",");

		for (String filters : allFiltersArr) {
			Criterion criterion = this.generateDisjunctionCriterion(
				filters.split("\\+"));

			if (criterion == null) {
				conjuntion = null;

				break;
			}

			conjuntion.add(criterion);
		}

		if ((conjuntion == null) && _log.isWarnEnabled()) {
			_log.warn("Invalid filter: " + stringFilter + " for " + this);
		}

		return conjuntion;
	}

	public Criterion generateDisjunctionCriterion(String[] filters) {

		Criterion criterion = null;

		if (filters.length == 1) {
			criterion = this.generateSingleCriterion(filters[0]);
		}
		else {
			Disjunction disjunction = RestrictionsFactoryUtil.disjunction();

			for (String singleFilter : filters) {
				Criterion singleCriterion = this.generateSingleCriterion(
					singleFilter);

				if (singleCriterion == null) {
					disjunction = null;

					break;
				}

				disjunction.add(singleCriterion);
			}

			criterion = disjunction;
		}

		if ((criterion == null) && _log.isWarnEnabled()) {
			_log.warn(
				"Invalid filters: " + Arrays.toString(filters) + " for " +
				this);
		}

		return criterion;
	}

	public Criterion generateSingleCriterion(String filter) {

		return ModelUtil.generateSingleCriterion(this, filter);
	}

	public Criterion generateSingleCriterion(
		String attrName, String attrValue, String op) {

		return ModelUtil.generateSingleCriterion(this, attrName, attrValue, op);
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
			types[i] = (Integer)values[i][1];
		}

		return types;
	}

	public int getAttributeType(String name) {
		int pos = this.getAttributePos(name);

		if (pos == -1) {
			return 0;
		}

		return (Integer)this.getAttributes()[pos][1];
	}

	public String getClassName() {
		return modelClass.getName();
	}

	public long getClassNameId() {
		return PortalUtil.getClassNameId(modelClass);
	}

	public String getDisplayName(Locale locale) {
		String displayName = ResourceActionsUtil.getModelResource(
			locale, getClassName());

		if (displayName.startsWith(
				ResourceActionsUtil.getModelResourceNamePrefix())) {

			return StringPool.BLANK;
		}

		if (Validator.isNull(this.suffix)) {
			return displayName;
		}

		return displayName + " (" + suffix + ")";
	}

	public Criterion getFilter() {
		return filter;
	}

	public Model getFilteredModel(String filters) {
		return getFilteredModel(filters, filters);
	}

	public Model getFilteredModel(String filters, String nameSufix) {

		Model model = null;

		Criterion filter = this.generateCriterionFilter(filters);

		if (filter != null) {
			model = this.clone();
			model.setFilter(filter);
			model.setNameSuffix(nameSufix);
		}

		return model;
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

	public Property getProperty(String attribute) {
		if (isPartOfPrimaryKeyMultiAttribute(attribute)) {
			attribute = "primaryKey." + attribute;
		}

		return PropertyFactoryUtil.forName(attribute);
	}

	public Projection getPropertyProjection(String attribute) {

		String op = null;

		if (attribute.indexOf("(") > 0) {
			op = attribute.substring(0, attribute.indexOf("("));
			attribute = attribute.substring(
				attribute.indexOf("(") + 1, attribute.indexOf(")"));
		}

		return getPropertyProjection(attribute, op);
	}

	public ProjectionList getPropertyProjection(String[] attributes) {

		String[] op = new String[attributes.length];
		String[] attributesAux = new String[attributes.length];

		boolean grouping = false;

		for (int i = 0; i<attributes.length; i++) {
			String attribute = attributes[i];

			if (attribute.indexOf("(") > 0) {
				op[i] = attribute.substring(0, attribute.indexOf("("));
				attributesAux[i] = attribute.substring(
					attribute.indexOf("(") + 1, attribute.indexOf(")"));
				grouping = true;
			}
			else {
				op[i] = null;
				attributesAux[i] = attribute;
			}
		}

		if (grouping) {
			for (int i = 0; i<op.length; i++) {
				if (op[i] == null) {
					op[i] = "groupProperty";
				}
			}
		}

		ProjectionList projectionList = ProjectionFactoryUtil.projectionList();

		for (int i = 0; i<attributesAux.length; i++) {
			projectionList.add(getPropertyProjection(attributesAux[i], op[i]));
		}

		if (attributesAux.length == 1) {
			projectionList.add(getPropertyProjection(attributes[0], op[0]));
		}

		return projectionList;
	}

	public boolean hasAttribute(String attribute) {
		Object[][] modelAttributes = getAttributes();

		if (attribute.endsWith(StringPool.UNDERLINE)) {
			attribute = attribute.substring(0, attribute.length() - 1);
		}

		String attributeWithUnderline = attribute + StringPool.UNDERLINE;

		for (int i = 0; i < modelAttributes.length; i++) {
			if (((String)modelAttributes[i][0]).endsWith(
					StringPool.UNDERLINE) &&
				((String)modelAttributes[i][0]).equals(
					attributeWithUnderline)) {

				return true;
			}
			else if (((String)modelAttributes[i][0]).equals(attribute)) {
				return true;
			}
		}

		return false;
	}

	public boolean hasAttributes(String[] attributes) {
		for (String attribute : attributes) {
			if (!hasAttribute(attribute)) {
				return false;
			}
		}

		return true;
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

	public boolean isPartOfPrimaryKeyMultiAttribute(String attribute) {

		for (String primaryKeyAttribute : this.getPrimaryKeyMultiAttribute()) {
			if (primaryKeyAttribute.equals(attribute)) {
				return true;
			}
		}

		return false;
	}

	public boolean modelExtendsClass(Class<?> clazz) {
		return clazz.isAssignableFrom(this.modelClass);
	}

	public DynamicQuery newDynamicQuery() {
		return modelFactory.newDynamicQuery(this.modelClass, null);
	}

	public DynamicQuery newDynamicQuery(Class<? extends ClassedModel> clazz) {
		return modelFactory.newDynamicQuery(clazz, null);
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
		this.suffix = suffix;
		this.name = getClassName() + "_" + this.suffix;
	}

	public String toString() {
		return getName();
	}

	@SuppressWarnings("unchecked")
	public ClassedModel updateObject(ClassedModel object) {
		return modelFactory.executeOperation(
		(Class<? extends ClassedModel>)object.getModelClass(), "update",
		object.getModelClass(), object);
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

	protected Projection getPropertyProjection(String attribute, String op) {
		Projection property = null;

		if (isPartOfPrimaryKeyMultiAttribute(attribute)) {
			attribute = "primaryKey." + attribute;
		}

		if (op == null) {
			property = ProjectionFactoryUtil.property(attribute);
		}
		else if ("rowCount".equals(op)) {
			property = ProjectionFactoryUtil.rowCount();
		}
		else if ("count".equals(op)) {
			property = ProjectionFactoryUtil.count(attribute);
		}
		else if ("countDistinct".equals(op)) {
			property = ProjectionFactoryUtil.countDistinct(attribute);
		}
		else if ("groupProperty".equals(op)) {
			property = ProjectionFactoryUtil.groupProperty(attribute);
		}
		else if ("max".equals(op)) {
			property = ProjectionFactoryUtil.max(attribute);
		}
		else if ("min".equals(op)) {
			property = ProjectionFactoryUtil.min(attribute);
		}
		else if ("sum".equals(op)) {
			property = ProjectionFactoryUtil.sum(attribute);
		}

		return property;
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
	protected String suffix = null;

	private static Log _log = LogFactoryUtil.getLog(ModelImpl.class);

}