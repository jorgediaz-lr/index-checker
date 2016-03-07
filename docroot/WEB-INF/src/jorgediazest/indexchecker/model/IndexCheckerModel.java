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

package jorgediazest.indexchecker.model;

import com.liferay.portal.kernel.dao.orm.Criterion;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.search.BooleanQuery;
import com.liferay.portal.kernel.search.BooleanQueryFactoryUtil;
import com.liferay.portal.kernel.search.Document;
import com.liferay.portal.kernel.search.Field;
import com.liferay.portal.kernel.search.SearchContext;
import com.liferay.portal.kernel.search.SearchException;
import com.liferay.portal.kernel.workflow.WorkflowConstants;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import jorgediazest.indexchecker.index.IndexSearchUtil;

import jorgediazest.util.data.Data;
import jorgediazest.util.data.DataUtil;
import jorgediazest.util.model.Model;
import jorgediazest.util.model.ModelImpl;
import jorgediazest.util.service.Service;

/**
 * @author Jorge Díaz
 */
public class IndexCheckerModel extends ModelImpl {

	public static String[] auditedModelAttributes =
		new String[] { "companyId", "createDate", "modifiedDate"};
	public static String[] groupedModelAttributes = new String[] { "groupId" };
	public static String[] resourcedModelAttributes =
		new String[] { "resourcePrimKey" };
	public static String[] stagedModelAttributes =
		new String[] { "companyId", "createDate", "modifiedDate" };
	public static String[] workflowModelAttributes = new String[] { "status" };

	public Set<String> calculateAttributesToCheck() {
		Set<String> attributesToCheckAux = new LinkedHashSet<String>();

		attributesToCheckAux.add(this.getPrimaryKeyAttribute());
		attributesToCheckAux.add("companyId");

		if (this.isAuditedModel()) {
			attributesToCheckAux.addAll(Arrays.asList(auditedModelAttributes));
		}

		if (this.isGroupedModel()) {
			attributesToCheckAux.addAll(Arrays.asList(groupedModelAttributes));
		}

		if (this.isResourcedModel()) {
			attributesToCheckAux.addAll(
				Arrays.asList(resourcedModelAttributes));
		}

		if (this.isStagedModel()) {
			attributesToCheckAux.addAll(Arrays.asList(stagedModelAttributes));
		}

		if (this.isWorkflowEnabled()) {
			attributesToCheckAux.addAll(Arrays.asList(workflowModelAttributes));
		}

		attributesToCheckAux.add("version");

		Set<String> attributesToCheck = new LinkedHashSet<String>();

		for (String attr : attributesToCheckAux) {
			if (this.hasAttribute(attr)) {
				attributesToCheck.add(attr);
			}
		}

		return attributesToCheck;
	}

	@Override
	public Model clone() {
		IndexCheckerModel model;
		try {
			model = (IndexCheckerModel)super.clone();
			model.attributesToCheck = new LinkedHashSet<String>(
				this.attributesToCheck);
		}
		catch (Exception e) {
			_log.error("Error executing clone");
			throw new RuntimeException(e);
		}

		return model;
	}

	public int compareTo(Data data1, Data data2) {
		if ((data1.getPrimaryKey() != -1) && (data2.getPrimaryKey() != -1) &&
			!this.isResourcedModel()) {

			return super.compareTo(data1, data2);
		}
		else if ((data1.getResourcePrimKey() != -1) &&
				 (data2.getResourcePrimKey() != -1)) {

			return DataUtil.compareLongs(
				data1.getResourcePrimKey(), data2.getResourcePrimKey());
		}
		else {
			return 0;
		}
	}

	public Data createDataObject(Document doc) {
		Data data = new Data(this);

		for (String attrib : this.getIndexAttributes()) {
			data.set(attrib, doc.get(attrib));
		}

		return data;
	}

	public void delete(Data value) throws SearchException {
		getIndexer().delete(value.getCompanyId(), value.get("uid").toString());
	}

	public Map<Data, String> deleteAndCheck(Collection<Data> dataCollection) {

		Map<Data, String> errors = new HashMap<Data, String>();

		if (_log.isDebugEnabled()) {
			_log.debug(
				"Deleting " + dataCollection.size() + " objects of type " +
					this.getClassName());
		}

		int i = 0;

		for (Data data : dataCollection) {
			/* Delete object from index */
			try {
				this.delete(data);

				if (_log.isDebugEnabled()) {
					_log.debug(
						"Deleting " + (i++) + " uid: " + data.get("uid"));
				}
			}
			catch (SearchException e) {
				errors.put(data, e.getClass() + " - " + e.getMessage());

				if (_log.isDebugEnabled()) {
					_log.debug(e.getClass() + " - " + e.getMessage(), e);
				}
			}

			/* Reindex object, perhaps we deleted it by error */
			try {
				this.reindex(data);
			}
			catch (Exception e) {
			}
		}

		return errors;
	}

	public boolean equals(Data data1, Data data2) {
		if ((data1.getPrimaryKey() != -1) && (data2.getPrimaryKey() != -1) &&
			!this.isResourcedModel()) {

			return super.equals(data1, data2);
		}
		else if ((data1.getResourcePrimKey() != -1) &&
				 (data2.getResourcePrimKey() != -1)) {

			return (data1.getResourcePrimKey() == data2.getResourcePrimKey());
		}
		else {
			return false;
		}
	}

	public Criterion generateQueryFilter() {
		if (!this.isWorkflowEnabled()) {
			return null;
		}

		return this.generateCriterionFilter(
			"status=" + WorkflowConstants.STATUS_APPROVED +"+" +
			"status=" + WorkflowConstants.STATUS_IN_TRASH);
	}

	public final Set<String> getAttributesToCheck() {
		if (attributesToCheck == null) {
			attributesToCheck = calculateAttributesToCheck();
		}

		return attributesToCheck;
	}

	public String[] getIndexAttributes() {
		return indexAttributes;
	}

	public Set<Data> getIndexData(long companyId, long groupId)
		throws SearchException {

		Set<Data> indexData = new HashSet<Data>();
		SearchContext searchContext = new SearchContext();
		searchContext.setCompanyId(companyId);
		searchContext.setEntryClassNames(new String[] {this.getClassName()});

		BooleanQuery contextQuery = BooleanQueryFactoryUtil.create(
			searchContext);
		contextQuery.addRequiredTerm(
			Field.ENTRY_CLASS_NAME, this.getClassName());

		if (groupId != 0) {
			contextQuery.addRequiredTerm(Field.SCOPE_GROUP_ID, groupId);
		}

		int size = Math.max((int)this.count() * 2, 50000);

		Document[] docs = IndexSearchUtil.executeSearch(
			searchContext, contextQuery, size, 50000);

		if (docs != null) {
			for (int i = 0; i < docs.length; i++) {
				Data data = this.createDataObject(docs[i]);

				indexData.add(data);
			}
		}

		return indexData;
	}

	public Integer hashCode(Data data) {
		if ((data.getPrimaryKey() != -1) && !this.isResourcedModel()) {
			return super.hashCode(data);
		}
		else if (data.getResourcePrimKey() != -1) {
			return -1 * data.getEntryClassName().hashCode() *
				Long.valueOf(data.getResourcePrimKey()).hashCode();
		}

		return null;
	}

	@Override
	public void init(
			String classPackageName, String classSimpleName, Service service)
		throws Exception {

		super.init(classPackageName, classSimpleName, service);

		this.setFilter(this.generateQueryFilter());

		this.setExactAttributes(
			new String[] {"createDate", "modifiedDate", "status", "version"});
	}

	public Map<Data, String> reindex(Collection<Data> dataCollection) {

		Map<Data, String> errors = new HashMap<Data, String>();

		if (_log.isDebugEnabled()) {
			_log.debug(
				"Reindexing " + dataCollection.size() + " objects of type " +
					this.getClassName());
		}

		int i = 0;

		for (Data data : dataCollection) {
			try {
				this.reindex(data);

				if (_log.isDebugEnabled()) {
					_log.debug(
						"Reindexing " + (i++) + " pk: " + data.getPrimaryKey());
				}
			}
			catch (SearchException e) {
				errors.put(data, e.getClass() + " - " + e.getMessage());

				if (_log.isDebugEnabled()) {
					_log.debug(e.getClass() + " - " + e.getMessage(), e);
				}
			}
		}

		return errors;
	}

	public void reindex(Data value) throws SearchException {
		getIndexer().reindex(getClassName(), value.getPrimaryKey());
	}

	public String toString() {
		String toString = this.getClassSimpleName()+":";

		for (String attr : this.getAttributesToCheck()) {
			toString += " " + attr;
		}

		return toString;
	}

	protected static String[] indexAttributes =
		{Field.UID, Field.CREATE_DATE, Field.MODIFIED_DATE,
		Field.ENTRY_CLASS_PK, Field.STATUS, Field.COMPANY_ID,
		Field.SCOPE_GROUP_ID, Field.VERSION};

	protected Set<String> attributesToCheck = null;

	private static Log _log = LogFactoryUtil.getLog(IndexCheckerModel.class);

}