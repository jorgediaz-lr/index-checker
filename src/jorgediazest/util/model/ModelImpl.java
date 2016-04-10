/**
 * Copyright (c) 2015-present Jorge Díaz All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package jorgediazest.util.model;

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
import com.liferay.portal.kernel.lar.StagedModelDataHandler;
import com.liferay.portal.kernel.lar.StagedModelDataHandlerRegistryUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.search.Indexer;
import com.liferay.portal.kernel.search.IndexerRegistryUtil;
import com.liferay.portal.kernel.trash.TrashHandler;
import com.liferay.portal.kernel.trash.TrashHandlerRegistryUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.workflow.WorkflowHandler;
import com.liferay.portal.kernel.workflow.WorkflowHandlerRegistryUtil;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.security.permission.ResourceActionsUtil;
import com.liferay.portal.util.PortalUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jorgediazest.util.data.Data;
import jorgediazest.util.data.DataComparator;
import jorgediazest.util.model.ModelFactory.DataComparatorFactory;
import jorgediazest.util.reflection.ReflectionUtil;
import jorgediazest.util.service.Service;

/**
 * @author Jorge Díaz
 */
public abstract class ModelImpl implements Model {

	/* Oracle limitation */
	public static final int MAX_NUMBER_OF_CLAUSES = 1000;

	public void addFilter(Criterion filter) {
		service.setFilter(
			ModelUtil.generateConjunctionQueryFilter(
				service.getFilter(), filter));
	}

	public Set<Portlet> calculatePortlets() {
		Set<Portlet> portletsSet = new HashSet<Portlet>();
		portletsSet.addAll(modelFactory.getPortletSet(this.getIndexer()));
		portletsSet.addAll(
			modelFactory.getPortletSet(this.getStagedModelDataHandler()));
		portletsSet.addAll(modelFactory.getPortletSet(this.getTrashHandler()));
		portletsSet.addAll(
			modelFactory.getPortletSet(this.getWorkflowHandler()));

		return portletsSet;
	}

	public Model clone() {
		ModelImpl model;
		try {
			model = this.getClass().newInstance();

			model.attributesArray = this.attributesArray;
			model.attributesString = this.attributesString;
			model.className = this.className;
			model.classPackageName = this.classPackageName;
			model.classSimpleName = this.classSimpleName;
			model.dataComparator = this.dataComparator;
			model.modelFactory = this.modelFactory;
			model.name = this.name;
			model.primaryKeyAttribute = this.primaryKeyAttribute;
			model.primaryKeyMultiAttribute = this.primaryKeyMultiAttribute;
			model.service = this.service.clone();
			model.suffix = this.suffix;
		}
		catch (Exception e) {
			_log.error("Error executing clone");
			throw new RuntimeException(e);
		}

		return model;
	}

	public int compareTo(Model o) {
		return this.getClassName().compareTo(o.getClassName());
	}

	public long count() {
		return count(null);
	}

	public long count(Criterion condition) {
		DynamicQuery dynamicQuery = this.getService().newDynamicQuery();
		dynamicQuery.setProjection(ProjectionFactoryUtil.rowCount());

		if (condition != null) {
			dynamicQuery.add(condition);
		}

		try {
			List<?> list = this.getService().executeDynamicQuery(dynamicQuery);

			if (list != null) {
				return (Long)list.get(0);
			}
		}
		catch (Exception e) {
			_log.error("Error executing count");
			throw new RuntimeException(e);
		}

		return -1;
	}

	public Data createDataObject(String[] attributes, Object[] result) {
		Data data = new Data(this, this.dataComparator);

		int i = 0;

		for (String attrib : attributes) {
			data.set(attrib, result[i++]);
		}

		return data;
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

	public Criterion generateInCriteria(String property, List<Long> list) {
		int size = MAX_NUMBER_OF_CLAUSES;

		if (list.size() <= size) {
			return getProperty(property).in(list);
		}

		Disjunction disjunction = RestrictionsFactoryUtil.disjunction();

		for (int i = 0; i<((list.size() + size - 1) / size); i++) {
			int start = i * size;
			int end = Math.min(start + size, list.size());
			List<Long> listAux = list.subList(start, end);
			disjunction.add(this.getProperty(property).in(listAux));
		}

		return disjunction;
	}

	public Criterion generateSingleCriterion(String filter) {

		return ModelUtil.generateSingleCriterion(this, filter);
	}

	public Criterion generateSingleCriterion(
		String attrName, String attrValue, String op) {

		return ReflectionUtil.generateSingleCriterion(
			this, attrName, attrValue, op);
	}

	public int getAttributePos(String name) {
		if (mapAttributePosition.containsKey(name)) {
			return mapAttributePosition.get(name);
		}

		Object[][] values = this.getAttributes();

		if (name.endsWith(StringPool.UNDERLINE)) {
			name = name.substring(0, name.length() - 1);
		}

		String nameWithUnderline = name + StringPool.UNDERLINE;

		int pos = -1;

		for (int i = 0; i < values.length; i++) {
			if (((String)values[i][0]).endsWith(StringPool.UNDERLINE) &&
				((String)values[i][0]).equals(nameWithUnderline)) {

				pos = i;
			}
			else if (((String)values[i][0]).equals(name)) {
				pos = i;
			}
		}

		mapAttributePosition.put(name, pos);

		return pos;
	}

	public Object[][] getAttributes() {
		if (attributesArray == null) {
			attributesArray = ModelUtil.getDatabaseAttributesArr(
				service.getLiferayModelImplClass());
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
		return className;
	}

	public long getClassNameId() {
		return PortalUtil.getClassNameId(getClassName());
	}

	public String getClassPackageName() {
		return classPackageName;
	}

	public String getClassSimpleName() {
		return classSimpleName;
	}

	public Criterion getCompanyFilter(long companyId) {
		return getCompanyGroupFilter(companyId, 0);
	}

	public Criterion getCompanyGroupFilter(long companyId, long groupId) {
		Conjunction conjunction = RestrictionsFactoryUtil.conjunction();

		if (this.hasAttribute("companyId")) {
			conjunction.add(getProperty("companyId").eq(companyId));
		}

		if (this.hasAttribute("groupId") && (groupId != 0)) {
			conjunction.add(getProperty("groupId").eq(groupId));
		}

		return conjunction;
	}

	public Map<Long, Data> getData() throws Exception {
		return getData(null, null);
	}

	public Map<Long, Data> getData(Criterion filter) throws Exception {
		return getData(null, filter);
	}

	public Map<Long, Data> getData(String[] attributes) throws Exception {
		return getData(attributes, null);
	}

	public Map<Long, Data> getData(String[] attributes, Criterion filter)
		throws Exception {

		Map<Long, Data> dataMap = new HashMap<Long, Data>();

		DynamicQuery query = service.newDynamicQuery();

		if (attributes == null) {
			attributes = this.getAttributesName();
		}

		List<String> validAttributes = new ArrayList<String>();
		ProjectionList projectionList = this.getPropertyProjection(
			attributes, validAttributes);

		query.setProjection(ProjectionFactoryUtil.distinct(projectionList));

		if (filter != null) {
			query.add(filter);
		}

		@SuppressWarnings("unchecked")
		List<Object[]> results = (List<Object[]>)service.executeDynamicQuery(
			query);

		String[] validAttributesArr = validAttributes.toArray(
			new String[validAttributes.size()]);

		long i = -1;

		for (Object[] result : results) {
			Data data = createDataObject(validAttributesArr, result);
			long pk = data.get("pk", i--);
			dataMap.put(pk, data);
		}

		return dataMap;
	}

	public DataComparator getDataComparator() {
		return dataComparator;
	}

	public Map<Long, Data> getDataWithCache() throws Exception {
		return getDataWithCache(null);
	}

	public Map<Long, Data> getDataWithCache(String[] attr) throws Exception {
		Map<Long, Data> values = cachedDifferentAttributeValues.get(
			Arrays.toString(attr));

		if (values == null) {
			values = this.getData(attr);

			cachedDifferentAttributeValues.put(Arrays.toString(attr), values);
		}

		return values;
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
		return service.getFilter();
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
		return IndexerRegistryUtil.getIndexer(getClassName());
	}

	public Indexer getIndexerNullSafe() {
		return IndexerRegistryUtil.nullSafeGetIndexer(getClassName());
	}

	public ModelFactory getModelFactory() {
		return modelFactory;
	}

	public String getName() {
		if (name == null) {
			return getClassName();
		}

		return name;
	}

	public Portlet getPortlet() {
		if (portlets.isEmpty()) {
			return null;
		}

		return portlets.toArray(new Portlet[portlets.size()])[0];
	}

	public String getPortletId() {
		Portlet portlet = this.getPortlet();

		if (portlet == null) {
			return null;
		}

		return portlet.getPortletId();
	}

	public Set<Portlet> getPortlets() {
		return portlets;
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
			String aux = ModelUtil.getDatabaseAttributesStr(
				service.getLiferayModelImplClass());

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

		List<String> validAttributes = new ArrayList<String>();
		ProjectionList projectionList = getPropertyProjection(
			attributes, validAttributes);

		if (attributes.length != validAttributes.size()) {
			throw new IllegalArgumentException(Arrays.toString(attributes));
		}

		return projectionList;
	}

	public ProjectionList getPropertyProjection(
		String[] attributes, List<String> validAttributes) {

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
			Projection projection = getPropertyProjection(
				attributesAux[i], op[i]);

			if (projection != null) {
				projectionList.add(projection);

				if (validAttributes != null) {
					validAttributes.add(attributes[i]);
				}
			}
		}

		if (attributesAux.length == 1) {
			projectionList.add(getPropertyProjection(attributes[0], op[0]));
		}

		return projectionList;
	}

	public Service getService() {
		return service;
	}

	public StagedModelDataHandler<?> getStagedModelDataHandler() {
		return StagedModelDataHandlerRegistryUtil.getStagedModelDataHandler(
			getClassName());
	}

	public TrashHandler getTrashHandler() {
		return TrashHandlerRegistryUtil.getTrashHandler(getClassName());
	}

	public WorkflowHandler getWorkflowHandler() {
		return WorkflowHandlerRegistryUtil.getWorkflowHandler(getClassName());
	}

	public boolean hasAttribute(String attribute) {

		return (getAttributePos(attribute) != -1);
	}

	public boolean hasAttributes(String[] attributes) {
		for (String attribute : attributes) {
			if (!hasAttribute(attribute)) {
				return false;
			}
		}

		return true;
	}

	public boolean hasIndexer() {
		return getIndexer() != null;
	}

	public void init(
			String classPackageName, String classSimpleName, Service service,
			DataComparatorFactory dataComparatorFactory)
		throws Exception {

		this.service = service;

		this.className = classPackageName + "." + classSimpleName;
		this.classPackageName = classPackageName;
		this.classSimpleName = classSimpleName;

		this.dataComparator = dataComparatorFactory.getDataComparator(this);

		this.portlets = calculatePortlets();
	}

	public boolean isAuditedModel() {
		if (hasAttribute("companyId") && hasAttribute("createDate") &&
			hasAttribute("modifiedDate") && hasAttribute("userId") &&
			hasAttribute("userName")) {

			return true;
		}

		return false;
	}

	public boolean isGroupedModel() {
		if (isAuditedModel() && hasAttribute("groupId") &&
			!getPrimaryKeyAttribute().equals("groupId")) {

			return true;
		}
		else {
			return false;
		}
	}

	public boolean isPartOfPrimaryKeyMultiAttribute(String attribute) {

		for (String primaryKeyAttribute : this.getPrimaryKeyMultiAttribute()) {
			if (primaryKeyAttribute.equals(attribute)) {
				return true;
			}
		}

		return false;
	}

	public boolean isResourcedModel() {
		if (hasAttribute("resourcePrimKey") &&
			!getPrimaryKeyAttribute().equals("resourcePrimKey") &&
			!isPartOfPrimaryKeyMultiAttribute("resourcePrimKey")) {

			return true;
		}

		return false;
	}

	public boolean isStagedModel() {
		if (hasAttribute("uuid") && hasAttribute("companyId") &&
			hasAttribute("createDate") &&
			hasAttribute("modifiedDate")) {

			return true;
		}

		return false;
	}

	public boolean isTrashEnabled() {
		return (getTrashHandler() != null);
	}

	public boolean isWorkflowEnabled() {
		if (hasAttribute("status") && hasAttribute("statusByUserId") &&
			hasAttribute("statusByUserName") && hasAttribute("statusDate")) {

			return true;
		}

		return false;
	}

	public boolean modelEqualsClass(Class<?> clazz) {
		return this.getClassName().equals(clazz.getName());
	}

	public void setFilter(Criterion filter) {
		service.setFilter(filter);
	}

	public void setModelFactory(ModelFactory modelFactory) {
		this.modelFactory = modelFactory;
	}

	public void setNameSuffix(String suffix) {
		this.suffix = suffix;
		this.name = getClassName() + "_" + this.suffix;
	}

	public String toString() {
		return getName();
	}

	protected String getCreateTableAttributes() {

		if (attributesString == null) {
			String aux = ModelUtil.getDatabaseAttributesStr(
				service.getLiferayModelImplClass());

			if (aux.indexOf('#') > 0) {
				aux = aux.split("#")[0];
			}

			attributesString = aux;
		}

		return attributesString;
	}

	protected Projection getPropertyProjection(String attribute, String op) {

		if ("rowCount".equals(op)) {
			return ProjectionFactoryUtil.rowCount();
		}

		if (!this.hasAttribute(attribute)) {
			return null;
		}

		if (isPartOfPrimaryKeyMultiAttribute(attribute)) {
			attribute = "primaryKey." + attribute;
		}

		Projection property = null;

		if (Validator.isNull(op)) {
			property = ProjectionFactoryUtil.property(attribute);
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

	protected static Log _log = LogFactoryUtil.getLog(ModelImpl.class);

	protected Object[][] attributesArray = null;
	protected String attributesString = null;
	protected Map<String, Map<Long, Data>> cachedDifferentAttributeValues =
		new HashMap<String, Map<Long, Data>>();
	protected String className = null;
	protected String classPackageName = null;
	protected String classSimpleName = null;
	protected DataComparator dataComparator;
	protected Map<String, Integer> mapAttributePosition =
		new ConcurrentHashMap<String, Integer>();
	protected ModelFactory modelFactory = null;
	protected String name = null;
	protected Set<Portlet> portlets = null;

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
	protected Service service = null;
	protected String suffix = null;

}