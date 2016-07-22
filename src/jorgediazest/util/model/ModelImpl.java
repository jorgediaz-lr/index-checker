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
import com.liferay.portal.kernel.search.BaseIndexer;
import com.liferay.portal.kernel.search.Indexer;
import com.liferay.portal.kernel.search.IndexerRegistryUtil;
import com.liferay.portal.kernel.trash.TrashHandler;
import com.liferay.portal.kernel.trash.TrashHandlerRegistryUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
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
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jorgediazest.util.data.Data;
import jorgediazest.util.data.DataComparator;
import jorgediazest.util.data.DataUtil;
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

	public void addRelatedModelData(
			Map<Long, Data> dataMap, String classNameRelated,
			String[] attrRelated, String[] mappings, boolean removeUnmatched,
			boolean rawData)
		throws Exception {

		addRelatedModelData(
			dataMap, classNameRelated, attrRelated, mappings, removeUnmatched,
			rawData, null);
	}

	@SuppressWarnings("unchecked")
	public void addRelatedModelData(
			Map<Long, Data> dataMap, String classNameRelated,
			String[] attrRelated, String[] mappings, boolean removeUnmatched,
			boolean rawData, Criterion filter)
		throws Exception {

			String[] attrRelatedOrig = new String[attrRelated.length];
			String[] attrRelatedDest = new String[attrRelated.length];
			String[] mappingsSource = new String[mappings.length];
			String[] mappingsDest = new String[mappings.length];

			splitSourceDest(attrRelated, attrRelatedOrig, attrRelatedDest);
			splitSourceDest(mappings, mappingsSource, mappingsDest);

			Map<Long, List<Data>> relatedMap = getRelatedModelData(
				classNameRelated, attrRelatedDest, mappingsDest[0], filter);

			Set<Long> unmatchedDataKeys = new HashSet<Long>();

			for (Entry<Long, Data> entry : dataMap.entrySet()) {
				Data data = entry.getValue();

				List<Data> relatedList = new ArrayList<Data>();

				Object key = data.get(mappingsSource[0]);

				if (key instanceof Set) {
					for (Object k : (Set<Object>)key) {
						List<Data> list = relatedMap.get(k);

						if (list != null) {
							relatedList.addAll(list);
						}
					}
				}
				else {
					List<Data> list = relatedMap.get(key);

					if (list != null) {
						relatedList.addAll(list);
					}
				}

				List<Data> matched = new ArrayList<Data>();

				for (Data related : relatedList) {
					boolean equalsAttributes = true;

					for (int j = 1; j<mappings.length; j++) {
						equalsAttributes = data.includesValueOfAttribute(
								related, mappingsSource[j], mappingsDest[j]);

						if (!equalsAttributes) {
							break;
						}
					}

					if (equalsAttributes) {
						matched.add(related);
					}
				}

				if (matched.isEmpty()) {
					if (removeUnmatched) {
						unmatchedDataKeys.add(entry.getKey());
					}

					continue;
				}

				if (rawData) {
					if (matched.size() == 1) {
						Object value =
							getRawRelatedData(
								matched.get(0), attrRelatedOrig,
								attrRelatedDest);

						data.set(classNameRelated, value);

						continue;
					}

					Set<Object> values = new HashSet<Object>(matched.size());

					for (Data m : matched) {
						Object value =
							getRawRelatedData(
								m, attrRelatedOrig, attrRelatedDest);
						values.add(value);
					}

					data.set(classNameRelated, values);

					continue;
				}

				data.addModelTableInfo(matched.get(0).getModel());

				for (int k = 0; k<attrRelatedOrig.length; k++) {
					if (Validator.isNotNull(attrRelatedOrig[k])) {
						if (matched.size() == 1) {
							Object value = matched.get(
								0).get(attrRelatedDest[k]);

							data.set(attrRelatedOrig[k], value);

							continue;
						}

						Set<Object> values = new HashSet<Object>(
							matched.size());

						for (Data m : matched) {
							values.add(m.get(attrRelatedDest[k]));
						}

						data.set(attrRelatedOrig[k], values);
					}
				}
			}

			for (Long key : unmatchedDataKeys) {
				dataMap.remove(key);
			}
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

	public void clearCache() {
		cachedDifferentAttributeValues = new HashMap<String, Map<Long, Data>>();
		cachedDifferentAttributeValuesDup =
				new HashMap<String, Map<Long, List<Data>>>();
	}

	public Model clone() {
		ModelImpl model;
		try {
			model = this.getClass().newInstance();

			model.className = this.className;
			model.classPackageName = this.classPackageName;
			model.classSimpleName = this.classSimpleName;
			model.dataComparator = this.dataComparator;
			model.modelFactory = this.modelFactory;
			model.name = this.name;
			model.service = this.service.clone();
			model.suffix = this.suffix;
			model.tableInfo = this.tableInfo;
			model.tableInfoMappings = this.tableInfoMappings;
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
		Model model = this;
		DataComparator dataComparator = this.dataComparator;

		return DataUtil.createDataObject(
			model, dataComparator, attributes, result);
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
		return this.getTableInfo().getAttributePos(name);
	}

	public Object[][] getAttributes() {
		return getTableInfo().getAttributes();
	}

	public String[] getAttributesName() {
		return getTableInfo().getAttributesName();
	}

	public int[] getAttributesType() {
		return getTableInfo().getAttributesType();
	}

	public int getAttributeType(String name) {
		return getTableInfo().getAttributeType(name);
	}

	public Class<?> getAttributeTypeClass(String name) {
		return getTableInfo().getAttributeTypeClass(name);
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
		return getCompanyGroupFilter(companyId, null);
	}

	public Criterion getCompanyGroupFilter(
			long companyId, List<Long> groupIds) {

		Conjunction conjunction = RestrictionsFactoryUtil.conjunction();

		if (this.hasAttribute("companyId")) {
			conjunction.add(getProperty("companyId").eq(companyId));
		}

		if (this.hasAttribute("groupId") && Validator.isNotNull(groupIds)) {
			Disjunction disjunction = RestrictionsFactoryUtil.disjunction();

			for (Long groupId : groupIds) {
				disjunction.add(getProperty("groupId").eq(groupId));
			}

			conjunction.add(disjunction);
		}

		return conjunction;
	}

	public Criterion getCompanyGroupFilter(long companyId, long groupId) {

		List<Long> groupIds = null;

		if (groupId != 0) {
			groupIds = new ArrayList<Long>();

			groupIds.add(groupId);
		}

		return getCompanyGroupFilter(companyId, groupIds);
	}

	public final Map<Long, Data> getData() throws Exception {
		return getData(null, "pk", null);
	}

	public final Map<Long, Data> getData(Criterion filter) throws Exception {
		return getData(null, filter);
	}

	public final Map<Long, Data> getData(String[] attributes) throws Exception {
		return getData(attributes, "pk", null);
	}

	public final Map<Long, Data> getData(String[] attributes, Criterion filter)
		throws Exception {

		return getData(attributes, "pk", filter);
	}

	public final Map<Long, Data> getData(
			String[] attributes, String mapKeyAttribute)
		throws Exception {

		return getData(attributes, mapKeyAttribute, null);
	}

	public Map<Long, Data> getData(
			String[] attributes, String mapKeyAttribute, Criterion filter)
		throws Exception {

		Map<Long, List<Data>> dataMapWithDuplicates = getDataWithDuplicates(
			attributes, mapKeyAttribute, filter);

		Map<Long, Data> dataMap = new HashMap<Long, Data>();

		for (Entry<Long, List<Data>> e : dataMapWithDuplicates.entrySet()) {
			Data data = e.getValue().get(0);
			dataMap.put(e.getKey(), data);
		}

		return dataMap;
	}

	public Map<Long, Data> getData(
			String[] attributesModel, String[] attributesRelated,
			Criterion filter)
		throws Exception {

		return getData(attributesModel, attributesRelated, "pk", filter);
	}

	public Map<Long, Data> getData(
			String[] attributesModel, String[] attributesRelated,
			String mapKeyAttribute, Criterion filter)
		throws Exception {

			Map<Long, Data> dataMap = getData(
				attributesModel, mapKeyAttribute, filter);

			for (String attributeRelated : attributesRelated) {
				String[] relatedDataArr = attributeRelated.split(":");

				if (relatedDataArr.length > 3) {
					if (Validator.isNull(relatedDataArr[3])) {
						filter = null;
					}
					else {
						Model relatedModel = modelFactory.getModelObject(
							relatedDataArr[0]);
						filter =
							relatedModel.generateCriterionFilter(
								relatedDataArr[3]);
					}
				}

				boolean removeUnmatched = false;
				String mappings = relatedDataArr[1];

				if (mappings.endsWith("-")) {
					removeUnmatched = true;

					mappings = mappings.substring(0, mappings.length()-1);
				}

				boolean rawData = false;
				String attrRelated = relatedDataArr[2];

				if (attrRelated.startsWith("[") && attrRelated.endsWith("]")) {
					rawData = true;

					attrRelated = attrRelated.substring(
						1, attrRelated.length()-1);
				}

				addRelatedModelData(
					dataMap, relatedDataArr[0], attrRelated.split(","),
					mappings.split(","), removeUnmatched, rawData, filter);
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
		return getDataWithCache(attr, "pk");
	}

	public Map<Long, Data> getDataWithCache(
			String[] attr, String mapKeyAttribute)
		throws Exception {

		Map<Long, Data> values = cachedDifferentAttributeValues.get(
			Arrays.toString(attr) + "key: " + mapKeyAttribute);

		if (values == null) {
			values = this.getData(attr, mapKeyAttribute);

			cachedDifferentAttributeValues.put(
				Arrays.toString(attr) + "key: " + mapKeyAttribute, values);
		}

		return values;
	}

	public final Map<Long, List<Data>> getDataWithDuplicates(
			String[] attributes, String mapKeyAttribute)
		throws Exception {

		return getDataWithDuplicates(attributes, mapKeyAttribute, null);
	}

	public Map<Long, List<Data>> getDataWithDuplicates(
			String[] attributes, String mapKeyAttribute, Criterion filter)
		throws Exception {

		Map<Long, List<Data>> dataMap = new HashMap<Long, List<Data>>();

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
			Long mappingAttributeValue = DataUtil.castLong(
				data.get(mapKeyAttribute));

			if (Validator.isNull(mappingAttributeValue)) {
				mappingAttributeValue = i--;
			}

			if (!dataMap.containsKey(mappingAttributeValue)) {
				List<Data> list = new ArrayList<Data>();
				list.add(data);
				dataMap.put(mappingAttributeValue, list);
			}
			else {
				dataMap.get(mappingAttributeValue).add(data);
			}
		}

		return dataMap;
	}

	public Map<Long, List<Data>> getDataWithDuplicatesCache(
			String[] attr, String mapKeyAttribute)
		throws Exception {

		Map<Long, List<Data>> values = cachedDifferentAttributeValuesDup.get(
			Arrays.toString(attr) + "key: " + mapKeyAttribute);

		if (values == null) {
			values = this.getDataWithDuplicates(attr, mapKeyAttribute);

			cachedDifferentAttributeValuesDup.put(
				Arrays.toString(attr) + "key: " + mapKeyAttribute, values);
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
		return getTableInfo().getPrimaryKeyAttribute();
	}

	public String[] getPrimaryKeyMultiAttribute() {
		return getTableInfo().getPrimaryKeyMultiAttribute();
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

				if ("pk".equals(attribute)) {
					attribute = this.getPrimaryKeyAttribute();
				}

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

		Projection firstProjection = null;

		int numProjections = 0;

		for (int i = 0; i<attributesAux.length; i++) {
			Projection projection = getPropertyProjection(
				attributesAux[i], op[i]);

			if (projection != null) {
				projectionList.add(projection);

				numProjections++;

				if (firstProjection == null) {
					firstProjection = projection;
				}

				if (validAttributes != null) {
					validAttributes.add(attributes[i]);
				}
			}
		}

		if (numProjections == 1) {
			projectionList.add(firstProjection);
		}

		return projectionList;
	}

	public Map<Long, Data> getRelatedData(
		String[] attributes, Criterion filter,
		String mappingAttributeName) throws Exception {

		Map<Long, Data> dataMap = this.getData(attributes, filter);

		Map <Long, Data> relatedData = new HashMap<Long, Data>();

		for (Data data : dataMap.values()) {
			long mappingAttributeValue = data.get(mappingAttributeName, -1L);

			if (mappingAttributeValue > 0) {
				relatedData.put(mappingAttributeValue, data);
			}
		}

		return relatedData;
	}

	public Map<Long, List<Data>> getRelatedModelData(
			String classNameRelated, String[] attributes, String mappingAttr,
			Criterion filter)
		throws Exception {

		Model model = modelFactory.getModelObject(classNameRelated);

		if (model == null) {
			return new HashMap<Long, List<Data>>();
		}

		if ("MappingTable".equals(mappingAttr)) {
			TableInfo tableInfo =
				model.getTableInfoMappings().get(attributes[0]);
			return tableInfo.queryTable(model.getPrimaryKeyAttribute());
		}

		if ("classPK".equals(mappingAttr)) {
			Criterion classNameIdFilter = model.getProperty(
				"classNameId").eq(this.getClassNameId());
			filter = ModelUtil.generateConjunctionQueryFilter(
				classNameIdFilter, filter);
		}

		if (this.getClassName().equals(model.getClassName())) {
			return new HashMap<Long, List<Data>>();
		}

		if (filter == null) {
			return model.getDataWithDuplicatesCache(attributes, mappingAttr);
		}

		return model.getDataWithDuplicates(attributes, mappingAttr, filter);
	}

	public Service getService() {
		return service;
	}

	public StagedModelDataHandler<?> getStagedModelDataHandler() {
		return StagedModelDataHandlerRegistryUtil.getStagedModelDataHandler(
			getClassName());
	}

	public TableInfo getTableInfo() {
		if (tableInfo == null) {
			tableInfo = new TableInfo(this);
		}

		return tableInfo;
	}

	public Map<String, TableInfo> getTableInfoMappings() {
		if (tableInfoMappings == null) {
			tableInfoMappings = new ConcurrentHashMap<String, TableInfo>();

			List<String> mappingTables =
				ReflectionUtil.getLiferayModelImplMappingTablesFields(
					service.getLiferayModelImplClass());

			for (String mappingTable : mappingTables) {
				String prefix = StringUtil.replace(
					mappingTable, "_NAME", StringPool.BLANK);

				TableInfo tableInfo = new TableInfo(this, prefix);

				String destinationAttr = tableInfo.getDestinationAttr(
					getPrimaryKeyAttribute());
				tableInfoMappings.put(destinationAttr, tableInfo);
			}
		}

		return tableInfoMappings;
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

	public boolean hasIndexerEnabled() {
		Indexer indexer = getIndexer();

		if (indexer == null) {
			return false;
		}

		Object aux = ReflectionUtil.unWrapProxy(indexer);

		if ((aux == null) || !(aux instanceof BaseIndexer)) {
			return false;
		}

		return ((BaseIndexer)aux).isIndexerEnabled();
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

	protected Object getRawRelatedData(
		Data data, String[] attrRelatedOrig, String[] attrRelatedDest) {

		List<Object> list = new ArrayList<Object>();

		for (int k = 0; k<attrRelatedOrig.length; k++) {
			if (Validator.isNotNull(attrRelatedOrig[k])) {
				Object value = data.get(attrRelatedDest[k]);

				list.add(value);
			}
		}

		return list;
	}

	protected void splitSourceDest(
		String[] dataArray, String[] dataArrayOrigin, String[] dataArrayDest) {

		int i = 0;

		for (String data : dataArray) {
			String[] aux = data.split("=");
			dataArrayOrigin[i] = aux[0];
			int posDest = 0;

			if (aux.length > 1) {
				posDest = 1;
			}

			dataArrayDest[i] = aux[posDest];
			i++;
		}
	}

	protected static Log _log = LogFactoryUtil.getLog(ModelImpl.class);

	protected Map<String, Map<Long, Data>> cachedDifferentAttributeValues =
		new ConcurrentHashMap<String, Map<Long, Data>>();

	protected Map<String, Map<Long, List<Data>>>
		cachedDifferentAttributeValuesDup =
			new ConcurrentHashMap<String, Map<Long, List<Data>>>();
	protected String className = null;
	protected String classPackageName = null;
	protected String classSimpleName = null;
	protected DataComparator dataComparator;
	protected ModelFactory modelFactory = null;
	protected String name = null;
	protected Set<Portlet> portlets = null;
	protected Service service = null;
	protected String suffix = null;
	protected TableInfo tableInfo = null;
	protected Map<String, TableInfo> tableInfoMappings = null;

}