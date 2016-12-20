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

package jorgediazest.indexchecker.portlet;

import com.liferay.portal.kernel.dao.orm.Conjunction;
import com.liferay.portal.kernel.dao.orm.Disjunction;
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.OrderFactoryUtil;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.dao.orm.RestrictionsFactoryUtil;
import com.liferay.portal.kernel.dao.shard.ShardUtil;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.JavaConstants;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.PrefsPropsUtil;
import com.liferay.portal.kernel.util.SetUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.Group;
import com.liferay.portal.model.Repository;
import com.liferay.portal.model.User;
import com.liferay.portal.security.auth.CompanyThreadLocal;
import com.liferay.portal.service.ClassNameLocalServiceUtil;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.documentlibrary.model.DLFileEntry;
import com.liferay.portlet.journal.model.JournalArticle;
import com.liferay.portlet.wiki.model.WikiPage;
import com.liferay.util.bridges.mvc.MVCPortlet;
import com.liferay.util.portlet.PortletProps;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.portlet.PortletException;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;
import javax.portlet.ResourceURL;

import jorgediazest.indexchecker.ExecutionMode;
import jorgediazest.indexchecker.data.DataIndexCheckerModelComparator;
import jorgediazest.indexchecker.data.DataIndexCheckerResourceModelComparator;
import jorgediazest.indexchecker.index.IndexSearchUtil;
import jorgediazest.indexchecker.model.IndexCheckerModelFactory;
import jorgediazest.indexchecker.model.IndexCheckerModelQuery;
import jorgediazest.indexchecker.model.IndexCheckerModelQueryFactory;
import jorgediazest.indexchecker.output.IndexCheckerOutput;

import jorgediazest.util.data.Comparison;
import jorgediazest.util.data.ComparisonUtil;
import jorgediazest.util.data.Data;
import jorgediazest.util.data.DataComparator;
import jorgediazest.util.model.Model;
import jorgediazest.util.model.ModelFactory;
import jorgediazest.util.model.ModelUtil;
import jorgediazest.util.modelquery.ModelQuery;
import jorgediazest.util.modelquery.ModelQueryFactory;
import jorgediazest.util.modelquery.ModelQueryFactory.DataComparatorFactory;
import jorgediazest.util.output.OutputUtils;
import jorgediazest.util.service.Service;

/**
 * Portlet implementation class IndexCheckerPortlet
 *
 * @author Jorge Díaz
 */
public class IndexCheckerPortlet extends MVCPortlet {

	public static List<Future<Comparison>> executeCallableCheckGroupAndModel(
		ExecutorService executor, List<ModelQuery> mqList, long companyId,
		List<Long> groupIds, Set<ExecutionMode> executionMode) {

		List<Future<Comparison>> futureResultList =
			new ArrayList<Future<Comparison>>();

		for (ModelQuery mq : mqList) {
			if (!mq.getModel().hasIndexerEnabled()) {
				continue;
			}

			CallableCheckGroupAndModel c =
				new CallableCheckGroupAndModel(
					companyId, groupIds, (IndexCheckerModelQuery)mq,
					executionMode);

			futureResultList.add(executor.submit(c));
		}

		return futureResultList;
	}

	public static Map<Long, List<Comparison>> executeCheck(
		ModelQueryFactory mqFactory, Company company, List<Long> groupIds,
		List<String> classNames, Set<ExecutionMode> executionMode,
		int threadsExecutor)
	throws ExecutionException, InterruptedException, SystemException {

		final String dateAttributes = PortletProps.get(
			"data-comparator.date.attributes");
		final String dateAttributesUser = PortletProps.get(
			"data-comparator.date.attributes.user");
		final String basicAttributes = PortletProps.get(
			"data-comparator.basic.attributes");
		final String basicAttributesNoVersion = PortletProps.get(
			"data-comparator.basic.attributes.noversion");
		final String categoriesTagsAttributes = PortletProps.get(
			"data-comparator.categories-tags.attributes");
		final String assetEntryAttributes = PortletProps.get(
			"data-comparator.assetentry.attributes");

		DataComparatorFactory dataComparatorFactory =
			new DataComparatorFactory() {

			protected boolean indexAllVersions =
				PrefsPropsUtil.getBoolean(
					"journal.articles.index.all.versions");

			protected DataComparator defaultComparator =
				new DataIndexCheckerModelComparator(
					(dateAttributes + "," + basicAttributes + "," +
						assetEntryAttributes + "," +
						categoriesTagsAttributes).split(","));

			protected DataComparator userComparator =
				new DataIndexCheckerModelComparator(
					(dateAttributesUser + "," + basicAttributes + "," +
						categoriesTagsAttributes).split(","));

			protected DataComparator dlFileEntryComparator =
				new DataIndexCheckerModelComparator(
					(dateAttributes + "," + basicAttributesNoVersion + "," +
						assetEntryAttributes + "," +
							categoriesTagsAttributes).split(","));

			protected DataComparator wikiPageComparator =
				new DataIndexCheckerResourceModelComparator(
					(dateAttributes + "," + basicAttributesNoVersion + "," +
						assetEntryAttributes + "," +
							categoriesTagsAttributes).split(","));

			protected DataComparator resourceComparator =
				new DataIndexCheckerResourceModelComparator(
					(dateAttributes + "," + basicAttributes + "," +
						assetEntryAttributes + "," +
						categoriesTagsAttributes).split(","));

			@Override
			public DataComparator getDataComparator(ModelQuery query) {
				Model model = query.getModel();

				if (JournalArticle.class.getName().equals(
						model.getClassName()) && indexAllVersions) {

					return defaultComparator;
				}
				else if (User.class.getName().equals(model.getClassName())) {
					return userComparator;
				}
				else if (DLFileEntry.class.getName().equals(
							model.getClassName())) {

					return dlFileEntryComparator;
				}
				else if (WikiPage.class.getName().equals(
							model.getClassName())) {

					return wikiPageComparator;
				}

				if (model.isResourcedModel()) {
					return resourceComparator;
				}

				return defaultComparator;
			}

		};

		mqFactory.setDataComparatorFactory(dataComparatorFactory);

		List<ModelQuery> mqList = getModelQueryList(mqFactory, classNames);

		IndexSearchUtil.autoAdjustIndexSearchLimit(mqList);

		long companyId = company.getCompanyId();

		ExecutorService executor = Executors.newFixedThreadPool(
			threadsExecutor);

		Map<Long, List<Future<Comparison>>> futureResultDataMap =
			new TreeMap<Long, List<Future<Comparison>>>();

		if (executionMode.contains(ExecutionMode.QUERY_BY_SITE)) {
			for (long groupId : groupIds) {
				List<Long> groupIdsAux = new ArrayList<Long>();
				groupIdsAux.add(groupId);

				List<Future<Comparison>> futureResultList =
					executeCallableCheckGroupAndModel(
						executor, mqList, companyId, groupIdsAux,
						executionMode);

				futureResultDataMap.put(groupId, futureResultList);
			}
		}
		else {
			List<Future<Comparison>> futureResultList =
				executeCallableCheckGroupAndModel(
					executor, mqList, companyId, groupIds, executionMode);

			futureResultDataMap.put(0L, futureResultList);
		}

		Map<Long, List<Comparison>> resultDataMap =
			new TreeMap<Long, List<Comparison>>();

		for (
			Entry<Long, List<Future<Comparison>>> entry :
				futureResultDataMap.entrySet()) {

			List<Comparison> resultList = new ArrayList<Comparison>();

			for (Future<Comparison> f : entry.getValue()) {
				Comparison results = f.get();

				if (results != null) {
					resultList.add(results);
				}
			}

			resultDataMap.put(entry.getKey(), resultList);
		}

		executor.shutdownNow();

		return resultDataMap;
	}

	public static EnumSet<ExecutionMode>
		getExecutionMode(ActionRequest request) {

		EnumSet<ExecutionMode> executionMode = EnumSet.noneOf(
			ExecutionMode.class);

		if (ParamUtil.getBoolean(request, "outputGroupBySite")) {
			executionMode.add(ExecutionMode.GROUP_BY_SITE);
		}

		if (ParamUtil.getBoolean(request, "queryBySite")) {
			executionMode.add(ExecutionMode.QUERY_BY_SITE);
		}

		if (ParamUtil.getBoolean(request, "outputBothExact")) {
			executionMode.add(ExecutionMode.SHOW_BOTH_EXACT);
		}

		if (ParamUtil.getBoolean(request, "outputBothNotExact")) {
			executionMode.add(ExecutionMode.SHOW_BOTH_NOTEXACT);
		}

		if (ParamUtil.getBoolean(request, "outputLiferay")) {
			executionMode.add(ExecutionMode.SHOW_LIFERAY);
		}

		if (ParamUtil.getBoolean(request, "outputIndex")) {
			executionMode.add(ExecutionMode.SHOW_INDEX);
		}

		if (ParamUtil.getBoolean(request, "dumpAllObjectsToLog")) {
			executionMode.add(ExecutionMode.DUMP_ALL_OBJECTS_TO_LOG);
		}

		return executionMode;
	}

	public static Log getLogger() {
		return _log;
	}

	public static List<ModelQuery> getModelQueryList(
		ModelQueryFactory mqFactory, List<String> classNames) {

		List<ModelQuery> mqList = new ArrayList<ModelQuery>();

		for (String className : classNames) {
			ModelQuery mq = mqFactory.getModelQueryObject(className);

			if (mq != null) {
				mqList.add(mq);
			}
		}

		return mqList;
	}

	public static Map<Data, String> reindex(
		ModelQueryFactory mqFactory, Comparison comparison) {

		Set<Data> objectsToReindex = new HashSet<Data>();

		for (String type : comparison.getOutputTypes()) {
			if (!type.endsWith("-right")) {
				Set<Data> aux = comparison.getData(type);

				if (aux != null) {
					objectsToReindex.addAll(aux);
				}
			}
		}

		IndexCheckerModelQuery modelQuery =
			(IndexCheckerModelQuery)mqFactory.getModelQueryObject(
				comparison.getModel());

		if (modelQuery == null) {
			return null;
		}

		return modelQuery.reindex(objectsToReindex);
	}

	public static Map<Data, String> removeIndexOrphans(
		ModelQueryFactory mqFactory, Comparison comparison) {

		Set<Data> indexOnlyData = comparison.getData("only-right");

		if ((indexOnlyData == null) || indexOnlyData.isEmpty()) {
			return null;
		}

		IndexCheckerModelQuery modelQuery =
			(IndexCheckerModelQuery)mqFactory.getModelQueryObject(
				comparison.getModel());

		if (modelQuery == null) {
			return null;
		}

		return modelQuery.deleteAndCheck(indexOnlyData);
	}

	public ModelQueryFactory createModelQueryFactory() throws Exception {
		ModelFactory modelFactory = new IndexCheckerModelFactory();
		return new IndexCheckerModelQueryFactory(modelFactory);
	}

	public void doView(
			RenderRequest renderRequest, RenderResponse renderResponse)
		throws IOException, PortletException {

		PortletConfig portletConfig =
			(PortletConfig)renderRequest.getAttribute(
				JavaConstants.JAVAX_PORTLET_CONFIG);

		List<String> outputList = IndexCheckerOutput.generateCSVOutput(
			portletConfig, renderRequest);

		String outputScript = OutputUtils.listStringToString(outputList);

		FileEntry exportCsvFileEntry = null;

		try {
			InputStream inputStream = null;

			if (Validator.isNotNull(outputScript)) {
				inputStream = new ByteArrayInputStream(
					outputScript.getBytes(StringPool.UTF8));
			}

			String portletId = portletConfig.getPortletName();

			Repository repository = OutputUtils.getPortletRepository(portletId);

			OutputUtils.cleanupPortletFileEntries(repository, 8 * 60);

			long userId = PortalUtil.getUserId(renderRequest);

			String fileName =
				"output_" + userId + "_" + System.currentTimeMillis() + ".csv";

			exportCsvFileEntry = OutputUtils.addPortletFileEntry(
				repository, inputStream, userId, fileName, "text/plain");

			if (exportCsvFileEntry != null) {
				ResourceURL exportCsvResourceURL =
					renderResponse.createResourceURL();
				exportCsvResourceURL.setResourceID(
					exportCsvFileEntry.getTitle());
				renderRequest.setAttribute(
					"exportCsvResourceURL", exportCsvResourceURL.toString());
			}
		}
		catch (Exception e) {
			_log.error(e, e);
		}

		try {
			List<Long> siteGroupIds = this.getSiteGroupIds();
			renderRequest.setAttribute("groupIdList", siteGroupIds);

			List<String> groupDescriptionList = getSiteGroupDescriptions(
				siteGroupIds);
			renderRequest.setAttribute(
				"groupDescriptionList", groupDescriptionList);
		}
		catch (Exception e) {
			throw new PortletException(e);
		}

		try {
			List<Model> modelList = this.getModelList();
			renderRequest.setAttribute("modelList", modelList);
		}
		catch (SystemException se) {
			throw new PortletException(se);
		}

		int numberOfThreads = getNumberOfThreads(renderRequest);
		renderRequest.setAttribute("numberOfThreads", numberOfThreads);

		super.doView(renderRequest, renderResponse);
	}

	public void executeCheck(ActionRequest request, ActionResponse response)
		throws Exception {

		PortalUtil.copyRequestParameters(request, response);

		EnumSet<ExecutionMode> executionMode = getExecutionMode(request);

		String[] filterClassNameArr = ParamUtil.getParameterValues(
			request,"filterClassName");

		response.setRenderParameter("filterClassName", new String[0]);

		request.setAttribute(
			"filterClassNameSelected", SetUtil.fromArray(filterClassNameArr));

		String[] filterGroupIdArr = ParamUtil.getParameterValues(
			request,"filterGroupId");

		response.setRenderParameter("filterGroupId", new String[0]);

		request.setAttribute(
			"filterGroupIdSelected", SetUtil.fromArray(filterGroupIdArr));

		Map<Company, Map<Long, List<Comparison>>> companyResultDataMap =
			new LinkedHashMap<Company, Map<Long, List<Comparison>>>();

		Map<Company, Long> companyProcessTime =
			new LinkedHashMap<Company, Long>();

		Map<Company, String> companyError =
			new LinkedHashMap<Company, String>();

		for (Company company : getCompanyList()) {
			try {
				CompanyThreadLocal.setCompanyId(company.getCompanyId());

				ShardUtil.pushCompanyService(company.getCompanyId());

				List<String> classNames = getClassNames(filterClassNameArr);

				List<Long> groupIds = getGroupIds(
					company, executionMode, filterGroupIdArr);

				if ((groupIds != null) && groupIds.isEmpty()) {
					if (_log.isInfoEnabled()) {
						_log.info(
							"Skipping company " + company.getCompanyId() + " " +
							"because groupId list is empty");
					}

					continue;
				}

				long startTime = System.currentTimeMillis();

				ModelQueryFactory mqf = createModelQueryFactory();

				Map<Long, List<Comparison>> resultDataMap =
					IndexCheckerPortlet.executeCheck(
						mqf, company, groupIds, classNames, executionMode,
						getNumberOfThreads(request));

				boolean groupBySite = executionMode.contains(
					ExecutionMode.GROUP_BY_SITE);

				if (groupBySite && (resultDataMap.keySet().size() == 1)) {
					List<Comparison> listComparison =
						(List<Comparison>)resultDataMap.values().toArray()[0];

					resultDataMap = new TreeMap<Long, List<Comparison>>();

					for (Comparison c : listComparison) {
						Map<Long, Comparison> map = c.splitByAttribute(
							"groupId");

						for (Entry<Long, Comparison> e : map.entrySet()) {
							List<Comparison> list = resultDataMap.get(
								e.getKey());

							if (list == null) {
								list = new ArrayList<Comparison>();

								resultDataMap.put(e.getKey(), list);
							}

							list.add(e.getValue());
						}
					}
				}

				if (!groupBySite && (resultDataMap.keySet().size() > 1)) {
					List<Comparison> tempComparisonList =
						new ArrayList<Comparison>();

					for (List<Comparison> auxList : resultDataMap.values()) {
						tempComparisonList.addAll(auxList);
					}

					List<Comparison> resultComparisonLisn =
						ComparisonUtil.mergeComparisons(tempComparisonList);

					resultDataMap = new TreeMap<Long, List<Comparison>>();

					resultDataMap.put(0L, resultComparisonLisn);
				}

				long endTime = System.currentTimeMillis();

				if (_log.isInfoEnabled() &&
					executionMode.contains(
							ExecutionMode.DUMP_ALL_OBJECTS_TO_LOG)) {

					_log.info("COMPANY: " + company);

					ComparisonUtil.dumpToLog(groupBySite, resultDataMap);
				}

				companyResultDataMap.put(company, resultDataMap);

				companyProcessTime.put(company, (endTime - startTime));
			}
			catch (Throwable t) {
				StringWriter swt = new StringWriter();
				PrintWriter pwt = new PrintWriter(swt);
				pwt.println("Error during execution: " + t.getMessage());
				t.printStackTrace(pwt);
				companyError.put(company, swt.toString());
				_log.error(t, t);
			}
			finally {
				ShardUtil.popCompanyService();
			}
		}

		request.setAttribute("title", "Check Index");
		request.setAttribute("executionMode", executionMode);
		request.setAttribute("companyProcessTime", companyProcessTime);
		request.setAttribute("companyResultDataMap", companyResultDataMap);
		request.setAttribute("companyError", companyError);
	}

	public void executeReindex(ActionRequest request, ActionResponse response)
		throws Exception {

		PortalUtil.copyRequestParameters(request, response);

		EnumSet<ExecutionMode> executionMode = getExecutionMode(request);

		String[] filterClassNameArr = ParamUtil.getParameterValues(
			request,"filterClassName");

		response.setRenderParameter("filterClassName", new String[0]);

		request.setAttribute(
			"filterClassNameSelected", SetUtil.fromArray(filterClassNameArr));

		String[] filterGroupIdArr = ParamUtil.getParameterValues(
			request,"filterGroupId");

		response.setRenderParameter("filterGroupId", new String[0]);

		request.setAttribute(
			"filterGroupIdSelected", SetUtil.fromArray(filterGroupIdArr));

		Map<Company, Long> companyProcessTime =
			new LinkedHashMap<Company, Long>();

		Map<Company, String> companyError =
			new LinkedHashMap<Company, String>();

		for (Company company : getCompanyList()) {
			StringWriter sw = new StringWriter();

			PrintWriter pw = new PrintWriter(sw);

			try {
				ShardUtil.pushCompanyService(company.getCompanyId());

				List<String> classNames = getClassNames(filterClassNameArr);

				List<Long> groupIds = getGroupIds(
					company, executionMode, filterGroupIdArr);

				if ((groupIds != null) && groupIds.isEmpty()) {
					if (_log.isInfoEnabled()) {
						_log.info(
							"Skipping company " + company.getCompanyId() + " " +
							"because groupId list is empty");
					}

					continue;
				}

				long startTime = System.currentTimeMillis();

				ModelQueryFactory mqf = createModelQueryFactory();

				Map<Long, List<Comparison>> resultDataMap =
					IndexCheckerPortlet.executeCheck(
						mqf, company, groupIds, classNames, executionMode,
						getNumberOfThreads(request));

				for (
					Entry<Long, List<Comparison>> entry :
						resultDataMap.entrySet()) {

					List<Comparison> resultList = entry.getValue();

					for (Comparison result : resultList) {
						Map<Data, String> errors = reindex(mqf, result);
/* TODO Mover todo esto al JSP */
						if (((errors!= null) && (errors.size() > 0)) ||
							(result.getError() != null)) {

							pw.println();
							pw.println("----");
							pw.println(result.getModel().getName());
							pw.println("----");

							for (Entry<Data, String> e : errors.entrySet()) {
								pw.println(
									" * " + e.getKey() + " - Exception: " +
										e.getValue());
							}

							pw.println(" * " + result.getError());
						}
					}
				}

				long endTime = System.currentTimeMillis();

				companyProcessTime.put(company, endTime - startTime);
			}
			catch (Throwable t) {
				StringWriter swt = new StringWriter();
				PrintWriter pwt = new PrintWriter(swt);
				pwt.println("Error during execution: " + t.getMessage());
				t.printStackTrace(pwt);
				companyError.put(company, swt.toString());
				_log.error(t, t);
			}
			finally {
				ShardUtil.popCompanyService();
			}

			companyError.put(company, sw.toString());
		}

		request.setAttribute("title", "Reindex");
		request.setAttribute("executionMode", executionMode);
		request.setAttribute("companyProcessTime", companyProcessTime);
		request.setAttribute("companyError", companyError);
	}

	public void executeRemoveOrphans(
			ActionRequest request, ActionResponse response)
		throws Exception {

		PortalUtil.copyRequestParameters(request, response);

		EnumSet<ExecutionMode> executionMode = getExecutionMode(request);

		String[] filterClassNameArr = ParamUtil.getParameterValues(
			request,"filterClassName");

		response.setRenderParameter("filterClassName", new String[0]);

		request.setAttribute(
			"filterClassNameSelected", SetUtil.fromArray(filterClassNameArr));

		String[] filterGroupIdArr = ParamUtil.getParameterValues(
			request,"filterGroupId");

		response.setRenderParameter("filterGroupId", new String[0]);

		request.setAttribute(
			"filterGroupIdSelected", SetUtil.fromArray(filterGroupIdArr));

		Map<Company, Long> companyProcessTime =
			new LinkedHashMap<Company, Long>();

		Map<Company, String> companyError =
			new LinkedHashMap<Company, String>();

		for (Company company : getCompanyList()) {
			StringWriter sw = new StringWriter();

			PrintWriter pw = new PrintWriter(sw);

			try {
				ShardUtil.pushCompanyService(company.getCompanyId());

				List<String> classNames = getClassNames(filterClassNameArr);

				List<Long> groupIds = getGroupIds(
					company, executionMode, filterGroupIdArr);

				if ((groupIds != null) && groupIds.isEmpty()) {
					if (_log.isInfoEnabled()) {
						_log.info(
							"Skipping company " + company.getCompanyId() + " " +
							"because groupId list is empty");
					}

					continue;
				}

				long startTime = System.currentTimeMillis();

				ModelQueryFactory mqf = createModelQueryFactory();

				Map<Long, List<Comparison>> resultDataMap =
					IndexCheckerPortlet.executeCheck(
						mqf, company, groupIds, classNames, executionMode,
						getNumberOfThreads(request));

				for (
					Entry<Long, List<Comparison>> entry :
						resultDataMap.entrySet()) {

					List<Comparison> resultList = entry.getValue();

					for (Comparison result : resultList) {
						Map<Data, String> errors = removeIndexOrphans(
							mqf, result);
						/* TODO Mover todo esto al JSP */
						if (((errors != null) && (errors.size() > 0)) ||
							(result.getError() != null)) {

							pw.println();
							pw.println("----");
							pw.println(result.getModel().getName());
							pw.println("----");

							for (Entry<Data, String> e : errors.entrySet()) {
								pw.println(
									" * " + e.getKey() + " - Exception: " +
										e.getValue());
							}

							pw.println(" * " + result.getError());
						}
					}
				}

				long endTime = System.currentTimeMillis();

				companyProcessTime.put(company, endTime - startTime);
			}
			catch (Throwable t) {
				StringWriter swt = new StringWriter();
				PrintWriter pwt = new PrintWriter(swt);
				pwt.println("Error during execution: " + t.getMessage());
				t.printStackTrace(pwt);
				companyError.put(company, swt.toString());
				_log.error(t, t);
			}
			finally {
				ShardUtil.popCompanyService();
			}

			companyError.put(company, sw.toString());
		}

		request.setAttribute("title", "Remove index orphan");
		request.setAttribute("executionMode", executionMode);
		request.setAttribute("companyProcessTime", companyProcessTime);
		request.setAttribute("companyError", companyError);
	}

	public List<String> getClassNames() throws SystemException {
		return getClassNames(null);
	}

	public List<String> getClassNames(String[] filterClassNameArr)
		throws SystemException {

		if ((filterClassNameArr == null)||(filterClassNameArr.length == 0)||
			((filterClassNameArr.length == 1) &&
			 Validator.isNull(filterClassNameArr[0]))) {

			filterClassNameArr = null;
		}

		List<String> allClassName =
			ModelUtil.getClassNameValues(
				ClassNameLocalServiceUtil.getClassNames(
					QueryUtil.ALL_POS, QueryUtil.ALL_POS));

		List<String> classNames = new ArrayList<String>();

		for (String className : allClassName) {
			if (ignoreClassName(className)) {
				continue;
			}

			if (filterClassNameArr == null) {
				classNames.add(className);
				continue;
			}

			for (String filterClassName : filterClassNameArr) {
				if (className.equals(filterClassName)) {
					classNames.add(className);
					break;
				}
			}
		}

		return classNames;
	}

	@SuppressWarnings("unchecked")
	public List<Company> getCompanyList() throws Exception {
		ModelFactory modelFactory = new ModelFactory();

		Model companyModel = modelFactory.getModelObject(Company.class);

		Service companyService = companyModel.getService();

		DynamicQuery companyDynamicQuery = companyService.newDynamicQuery();

		companyDynamicQuery.addOrder(OrderFactoryUtil.asc("companyId"));

		return (List<Company>)
			companyService.executeDynamicQuery(companyDynamicQuery);
	}

	public List<Long> getGroupIds(
			Company company, Set<ExecutionMode> executionMode,
			String[] filterGroupIdArr)
		throws SystemException {

		if ((filterGroupIdArr != null) && (filterGroupIdArr.length == 1) &&
			filterGroupIdArr[0].equals("-1000")) {

			filterGroupIdArr = null;
		}

		boolean queryBySite = executionMode.contains(
			ExecutionMode.QUERY_BY_SITE);

		if (!queryBySite && (filterGroupIdArr == null)) {
			return null;
		}

		List<Group> groups =
			GroupLocalServiceUtil.getCompanyGroups(
				company.getCompanyId(), QueryUtil.ALL_POS, QueryUtil.ALL_POS);

		List<Long> groupIds = new ArrayList<Long>();

		boolean allSites = false;
		boolean userSites = false;

		if (filterGroupIdArr != null) {
			for (String filterGroupId : filterGroupIdArr) {
				if ("0".equals(filterGroupId)) {
					groupIds.add(0L);
				}

				if ("-1".equals(filterGroupId)) {
					allSites = true;
				}

				if ("-2".equals(filterGroupId)) {
					userSites = true;
				}
			}
		}

		if (filterGroupIdArr == null) {
			groupIds.add(0L);
		}

		for (Group group : groups) {
			if (filterGroupIdArr == null) {
				groupIds.add(group.getGroupId());
				continue;
			}

			if (allSites && (group.isSite() || group.isStagingGroup() ||
				 group.isCompany())) {

				groupIds.add(group.getGroupId());
				continue;
			}

			if (userSites && (group.isUser() || group.isUserGroup())) {
				groupIds.add(group.getGroupId());
				continue;
			}

			String groupIdStr = "" + group.getGroupId();

			for (int i = 0; i < filterGroupIdArr.length; i++) {
				if (groupIdStr.equals(filterGroupIdArr[i])) {
					groupIds.add(group.getGroupId());
					break;
				}
			}
		}

		return groupIds;
	}

	public List<Model> getModelList() throws SystemException {
		return getModelList(null);
	}

	public List<Model> getModelList(String[] filterClassNameArr)
		throws SystemException {

		List<String> classNames = getClassNames(filterClassNameArr);

		ModelFactory modelFactory = new IndexCheckerModelFactory();

		List<Model> modelList = new ArrayList<Model>();

		for (String className : classNames) {
			Model model = modelFactory.getModelObject(className);

			if ((model == null) || !model.hasIndexerEnabled()) {
				continue;
			}

			modelList.add(model);
		}

		return modelList;
	}

	public int getNumberOfThreads(ActionRequest actionRequest) {
		int def = GetterUtil.getInteger(PortletProps.get("number.threads"),1);

		int num = ParamUtil.getInteger(actionRequest, "numberOfThreads", def);

		return (num == 0) ? def : num;
	}

	public int getNumberOfThreads(RenderRequest renderRequest) {
		int def = GetterUtil.getInteger(PortletProps.get("number.threads"), 1);

		int num = ParamUtil.getInteger(renderRequest, "numberOfThreads", def);

		return (num == 0) ? def : num;
	}

	public List<String> getSiteGroupDescriptions(List<Long> siteGroupIds)
		throws SystemException {

		List<String> groupDescriptionList = new ArrayList<String>();

		for (Long siteGroupId : siteGroupIds) {
			Group group = GroupLocalServiceUtil.fetchGroup(siteGroupId);
			String groupDescription = group.getName();
			groupDescription = groupDescription.replace(
				"LFR_ORGANIZATION", "(Org)");

			if (group.isCompany()) {
				if (!group.isStagingGroup()) {
					groupDescription = "Global";
				}

				groupDescription += " - " + group.getCompanyId();
			}

			groupDescriptionList.add(groupDescription);
		}

		return groupDescriptionList;
	}

	@SuppressWarnings("unchecked")
	public List<Long> getSiteGroupIds() throws Exception {

		ModelFactory modelFactory = new ModelFactory();

		Model groupModel = modelFactory.getModelObject(Group.class);

		long companyClassNameId = PortalUtil.getClassNameId(Company.class);

		Service groupService = groupModel.getService();

		DynamicQuery groupDynamicQuery = null;

		/* Get groupIds of live global groups */
		groupDynamicQuery = groupService.newDynamicQuery();

		groupDynamicQuery.add(
			groupModel.getProperty("classNameId").eq(companyClassNameId));
		groupDynamicQuery.add(groupModel.getProperty("liveGroupId").eq(0L));

		groupDynamicQuery.setProjection(
			groupModel.getPropertyProjection("groupId"));

		List<Long> liveGlobalGroupIds = (List<Long>)
			groupService.executeDynamicQuery(groupDynamicQuery);

		/* Get groupIds of staging and live global groups */
		groupDynamicQuery = groupService.newDynamicQuery();

		Disjunction disjunctionGlobal = RestrictionsFactoryUtil.disjunction();
		disjunctionGlobal.add(
			groupModel.getProperty("classNameId").eq(companyClassNameId));
		disjunctionGlobal.add(
			groupModel.generateInCriteria("liveGroupId", liveGlobalGroupIds));

		groupDynamicQuery.add(disjunctionGlobal);
		groupDynamicQuery.setProjection(
			groupModel.getPropertyProjection("groupId"));

		groupDynamicQuery.addOrder(OrderFactoryUtil.asc("companyId"));
		groupDynamicQuery.addOrder(OrderFactoryUtil.asc("friendlyURL"));

		List<Long> globalSitesGroupIds =
			(List<Long>)groupService.executeDynamicQuery(groupDynamicQuery);

		/* Get groupIds of staging and live normal groups */
		groupDynamicQuery = groupService.newDynamicQuery();

		Conjunction stagingSites = RestrictionsFactoryUtil.conjunction();
		stagingSites.add(groupModel.getProperty("site").eq(false));
		stagingSites.add(groupModel.getProperty("liveGroupId").ne(0L));
		stagingSites.add(
			RestrictionsFactoryUtil.not(
				groupModel.generateInCriteria(
					"liveGroupId", liveGlobalGroupIds)));

		Conjunction normalSites = RestrictionsFactoryUtil.conjunction();
		normalSites.add(groupModel.getProperty("site").eq(true));
		normalSites.add(
			groupModel.getProperty("classNameId").ne(companyClassNameId));

		Disjunction disjunction = RestrictionsFactoryUtil.disjunction();
		disjunction.add(stagingSites);
		disjunction.add(normalSites);

		groupDynamicQuery.add(disjunction);
		groupDynamicQuery.setProjection(
			groupModel.getPropertyProjection("groupId"));

		groupDynamicQuery.addOrder(OrderFactoryUtil.asc("name"));

		List<Long> normalSitesGroupIds =
			(List<Long>)groupService.executeDynamicQuery(groupDynamicQuery);

		List<Long> result = new ArrayList<Long>();
		result.addAll(globalSitesGroupIds);
		result.addAll(normalSitesGroupIds);

		return result;
	}

	public boolean ignoreClassName(String className) {
		if (Validator.isNull(className)) {
			return true;
		}

		for (String ignoreClassName : ignoreClassNames) {
			if (ignoreClassName.equals(className)) {
				return true;
			}
		}

		return false;
	}

	public void init() throws PortletException {
		super.init();

		try {
			List<Model> modelList = getModelList();

			IndexSearchUtil.autoAdjustIndexSearchLimitModelList(modelList);
		}
		catch (Exception e) {
			_log.error(e, e);
		}
	}

	public void serveResource(
			ResourceRequest request, ResourceResponse response)
		throws IOException, PortletException {

		PortletConfig portletConfig =
			(PortletConfig)request.getAttribute(
				JavaConstants.JAVAX_PORTLET_CONFIG);

		String resourceId = request.getResourceID();
		String portletId = portletConfig.getPortletName();

		OutputUtils.servePortletFileEntry(portletId, resourceId, response);
	}

	private static Log _log = LogFactoryUtil.getLog(IndexCheckerPortlet.class);

	private static String[] ignoreClassNames = new String[] {
		"com.liferay.portal.repository.liferayrepository.model.LiferayFileEntry",
		"com.liferay.portal.kernel.repository.model.FileEntry",
		"com.liferay.portal.kernel.repository.model.Folder",
		"com.liferay.portal.model.UserPersonalSite"};

}