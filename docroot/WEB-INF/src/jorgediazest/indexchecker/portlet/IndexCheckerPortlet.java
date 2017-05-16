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

import com.liferay.portal.kernel.bean.ClassLoaderBeanHandler;
import com.liferay.portal.kernel.dao.orm.Conjunction;
import com.liferay.portal.kernel.dao.orm.Disjunction;
import com.liferay.portal.kernel.dao.orm.Order;
import com.liferay.portal.kernel.dao.orm.OrderFactoryUtil;
import com.liferay.portal.kernel.dao.orm.Projection;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.dao.orm.RestrictionsFactoryUtil;
import com.liferay.portal.kernel.deploy.DeployManagerUtil;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.plugin.PluginPackage;
import com.liferay.portal.kernel.portlet.LiferayPortletContext;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCPortlet;
import com.liferay.portal.kernel.security.auth.CompanyThreadLocal;
import com.liferay.portal.kernel.service.ClassNameLocalServiceUtil;
import com.liferay.portal.kernel.service.CompanyLocalServiceUtil;
import com.liferay.portal.kernel.service.GroupLocalServiceUtil;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.search.BaseIndexer;
import com.liferay.portal.kernel.search.IndexerRegistryUtil;
import com.liferay.portal.kernel.util.JavaConstants;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.SetUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.Group;
import com.liferay.portal.model.GroupConstants;
import com.liferay.portal.security.auth.CompanyThreadLocal;
import com.liferay.portal.service.ClassNameLocalServiceUtil;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.util.PortalUtil;
import com.liferay.util.bridges.mvc.MVCPortlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import java.lang.reflect.Proxy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.portlet.PortletContext;
import javax.portlet.PortletException;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;
import javax.portlet.ResourceURL;

import jorgediazest.indexchecker.ExecutionMode;
import jorgediazest.indexchecker.index.IndexSearchHelper;
import jorgediazest.indexchecker.model.IndexCheckerModelFactory;
import jorgediazest.indexchecker.output.IndexCheckerOutput;
import jorgediazest.indexchecker.util.ConfigurationUtil;
import jorgediazest.indexchecker.util.RemoteConfigurationUtil;

import jorgediazest.util.data.Comparison;
import jorgediazest.util.data.ComparisonUtil;
import jorgediazest.util.data.Data;
import jorgediazest.util.model.Model;
import jorgediazest.util.model.ModelFactory;
import jorgediazest.util.model.ModelUtil;
import jorgediazest.util.output.OutputUtils;

/**
 * Portlet implementation class IndexCheckerPortlet
 *
 * @author Jorge Díaz
 */
public class IndexCheckerPortlet extends MVCPortlet {

	public static void dumpToLog(
			boolean groupBySite,
			Map<Long, List<Comparison>> comparisonDataMap)
		throws SystemException {

		if (!_log.isInfoEnabled()) {
			return;
		}

		for (
			Entry<Long, List<Comparison>> entry :
				comparisonDataMap.entrySet()) {

			String groupTitle = null;
			Group group = GroupLocalServiceUtil.fetchGroup(entry.getKey());

			if ((group == null) && groupBySite) {
				groupTitle = "N/A";
			}
			else if (group != null) {
				groupTitle = group.getGroupId() + " - " + group.getName();
			}

			if (groupTitle != null) {
				_log.info("");
				_log.info("---------------");
				_log.info("GROUP: " + groupTitle);
				_log.info("---------------");
			}

			for (Comparison comparison : entry.getValue()) {
				comparison.dumpToLog();
			}
		}
	}

	public static List<Future<Comparison>> executeCallableCheckGroupAndModel(
		Map<String, Map<Long, List<Data>>> queryCache, ExecutorService executor,
		List<Model> modelList, long companyId, List<Long> groupIds,
		Set<ExecutionMode> executionMode) {

		List<Future<Comparison>> futureResultList =
			new ArrayList<Future<Comparison>>();

		for (Model model : modelList) {
			String className = model.getClassName();

			if (!hasIndexerEnabled(className)) {
				continue;
			}

			if (ConfigurationUtil.modelNotIndexed(className)) {
				continue;
			}

			CallableCheckGroupAndModel c =
				new CallableCheckGroupAndModel(
					queryCache, companyId, groupIds, model, executionMode);

			futureResultList.add(executor.submit(c));
		}

		return futureResultList;
	}

	public static Map<Long, List<Comparison>> executeCheck(
			Company company, List<Long> groupIds, List<String> classNames,
			Set<ExecutionMode> executionMode, int threadsExecutor)
		throws ExecutionException, InterruptedException, SystemException {

		long companyId = company.getCompanyId();

		Map<String, Map<Long, List<Data>>> queryCache =
			new ConcurrentHashMap<String, Map<Long, List<Data>>>();

		ModelFactory modelFactory = new IndexCheckerModelFactory(companyId);

		List<Model> modelList = getModelList(modelFactory, classNames);

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
						queryCache, executor, modelList, companyId, groupIdsAux,
						executionMode);

				futureResultDataMap.put(groupId, futureResultList);
			}
		}
		else {
			List<Future<Comparison>> futureResultList =
				executeCallableCheckGroupAndModel(
					queryCache, executor, modelList, companyId, groupIds,
					executionMode);

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

	public static List<Model> getModelList(
		ModelFactory modelFactory, List<String> classNames) {

		List<Model> modelList = new ArrayList<Model>();

		for (String className : classNames) {
			Model model = modelFactory.getModelObject(className);

			if (model != null) {
				modelList.add(model);
			}
		}

		return modelList;
	}

	public static PluginPackage getPluginPackage(PortletConfig portletConfig) {
		if (portletConfig == null) {
			return null;
		}

		PortletContext portletContext = portletConfig.getPortletContext();

		String portletContextName = portletContext.getPortletContextName();

		return DeployManagerUtil.getInstalledPluginPackage(portletContextName);
	}

	public static boolean hasIndexerEnabled(String className) {
		Object indexer = IndexerRegistryUtil.getIndexer(className);

		if (indexer == null) {
			return false;
		}

		if (indexer instanceof Proxy) {
			try {
				ClassLoaderBeanHandler classLoaderBeanHandler =
					(ClassLoaderBeanHandler)
						Proxy.getInvocationHandler(indexer);
				indexer = classLoaderBeanHandler.getBean();

				if (indexer == null) {
					return false;
				}
			}
			catch (Exception e) {
				if (_log.isDebugEnabled()) {
					_log.debug(e, e);
				}
			}
		}

		if (indexer instanceof BaseIndexer) {
			BaseIndexer baseIndexer = (BaseIndexer)indexer;
			return baseIndexer.isIndexerEnabled();
		}

		return false;
	}

	public static Map<Data, String> reindex(Comparison comparison) {

		Model model = comparison.getModel();

		if (model == null) {
			return null;
		}

		Set<Data> objectsToReindex = new HashSet<Data>();

		for (String type : comparison.getOutputTypes()) {
			if (!type.endsWith("-right")) {
				Set<Data> aux = comparison.getData(type);

				if (aux != null) {
					objectsToReindex.addAll(aux);
				}
			}
		}

		IndexSearchHelper indexSearchHelper =
			ConfigurationUtil.getIndexSearchHelper(model);

		if (_log.isDebugEnabled()) {
			_log.debug(
				"Reindexing " + objectsToReindex.size() + " objects of type " +
					model.getClassName());
		}

		return indexSearchHelper.reindex(objectsToReindex);
	}

	public static Map<Data, String> removeIndexOrphans(Comparison comparison) {

		Set<Data> indexOnlyData = comparison.getData("only-right");

		if ((indexOnlyData == null) || indexOnlyData.isEmpty()) {
			return null;
		}

		Model model = comparison.getModel();

		if (model == null) {
			return null;
		}

		IndexSearchHelper indexSearchHelper =
			ConfigurationUtil.getIndexSearchHelper(model);

		if (_log.isDebugEnabled()) {
			_log.debug(
				"Deleting " + indexOnlyData.size() + " objects of type " +
					model.getClassName());
		}

		return indexSearchHelper.deleteAndCheck(indexOnlyData);
	}

	public void doView(
			RenderRequest renderRequest, RenderResponse renderResponse)
		throws IOException, PortletException {

		PortletConfig portletConfig =
			(PortletConfig)renderRequest.getAttribute(
				JavaConstants.JAVAX_PORTLET_CONFIG);

		String updateMessage = getUpdateMessage(portletConfig);

		renderRequest.setAttribute("updateMessage", updateMessage);

		List<String> outputList = IndexCheckerOutput.generateCSVOutput(
			portletConfig, renderRequest);

		String portletId = portletConfig.getPortletName();
		long userId = PortalUtil.getUserId(renderRequest);
		String outputContent = OutputUtils.listStringToString(outputList);

		FileEntry exportCsvFileEntry = OutputUtils.addPortletOutputFileEntry(
			portletId, userId, outputContent);

		if (exportCsvFileEntry != null) {
			ResourceURL exportCsvResourceURL =
				renderResponse.createResourceURL();
			exportCsvResourceURL.setResourceID(exportCsvFileEntry.getTitle());

			renderRequest.setAttribute(
				"exportCsvResourceURL", exportCsvResourceURL.toString());
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

				Map<Long, List<Comparison>> resultDataMap =
					IndexCheckerPortlet.executeCheck(
						company, groupIds, classNames, executionMode,
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

					dumpToLog(groupBySite, resultDataMap);
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

				Map<Long, List<Comparison>> resultDataMap =
					IndexCheckerPortlet.executeCheck(
						company, groupIds, classNames, executionMode,
						getNumberOfThreads(request));

				for (
					Entry<Long, List<Comparison>> entry :
						resultDataMap.entrySet()) {

					List<Comparison> resultList = entry.getValue();

					for (Comparison result : resultList) {
						Map<Data, String> errors = reindex(result);
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

				Map<Long, List<Comparison>> resultDataMap =
					IndexCheckerPortlet.executeCheck(
						company, groupIds, classNames, executionMode,
						getNumberOfThreads(request));

				for (
					Entry<Long, List<Comparison>> entry :
						resultDataMap.entrySet()) {

					List<Comparison> resultList = entry.getValue();

					for (Comparison result : resultList) {
						Map<Data, String> errors = removeIndexOrphans(result);
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
			if (ConfigurationUtil.ignoreClassName(className)) {
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

		return (List<Company>)
			companyModel.executeDynamicQuery(
				null, OrderFactoryUtil.asc("companyId"));
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
			if (!hasIndexerEnabled(className)) {
				continue;
			}

			if (ConfigurationUtil.modelNotIndexed(className)) {
				continue;
			}

			Model model = modelFactory.getModelObject(className);

			if (model != null) {
				modelList.add(model);
			}
		}

		return modelList;
	}

	public int getNumberOfThreads(ActionRequest actionRequest) {
		int def = ConfigurationUtil.getDefaultNumberThreads();

		int num = ParamUtil.getInteger(actionRequest, "numberOfThreads", def);

		return (num == 0) ? def : num;
	}

	public int getNumberOfThreads(RenderRequest renderRequest) {
		int def = ConfigurationUtil.getDefaultNumberThreads();

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

			if (group.isCompany() && !group.isStagingGroup()) {
				groupDescription = GroupConstants.GLOBAL;
				}

			if (GroupConstants.GUEST.equals(groupDescription) ||
				group.isCompany()) {

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

		Projection projection = groupModel.getPropertyProjection("groupId");

		long companyClassNameId = PortalUtil.getClassNameId(Company.class);

		Conjunction conjuntion = RestrictionsFactoryUtil.conjunction();

		conjuntion.add(
			groupModel.getProperty("classNameId").eq(companyClassNameId));
		conjuntion.add(groupModel.getProperty("liveGroupId").eq(0L));

		/* Get groupIds of live global groups */
		List<Long> liveGlobalGroupIds = (List<Long>)
			groupModel.executeDynamicQuery(conjuntion, projection);

		/* Get groupIds of staging and live global groups */
		Disjunction disjunctionGlobal = RestrictionsFactoryUtil.disjunction();
		disjunctionGlobal.add(
			groupModel.getProperty("classNameId").eq(companyClassNameId));
		disjunctionGlobal.add(
			groupModel.getAttributeCriterion(
				"liveGroupId", liveGlobalGroupIds));

		List<Order> orders = new ArrayList<Order>();
		orders.add(OrderFactoryUtil.asc("companyId"));
		orders.add(OrderFactoryUtil.asc("friendlyURL"));

		List<Long> globalSitesGroupIds =
			(List<Long>)groupModel.executeDynamicQuery(
				disjunctionGlobal, projection, orders);

		/* Get groupIds of staging and live normal groups */
		Conjunction stagingSites = RestrictionsFactoryUtil.conjunction();
		stagingSites.add(groupModel.getProperty("site").eq(false));
		stagingSites.add(groupModel.getProperty("liveGroupId").ne(0L));
		stagingSites.add(
			RestrictionsFactoryUtil.not(
				groupModel.getAttributeCriterion(
					"liveGroupId", liveGlobalGroupIds)));

		Conjunction normalSites = RestrictionsFactoryUtil.conjunction();
		normalSites.add(groupModel.getProperty("site").eq(true));
		normalSites.add(
			groupModel.getProperty("classNameId").ne(companyClassNameId));

		Disjunction disjunction = RestrictionsFactoryUtil.disjunction();
		disjunction.add(stagingSites);
		disjunction.add(normalSites);

		orders = Collections.singletonList(OrderFactoryUtil.asc("name"));

		List<Long> normalSitesGroupIds =
			(List<Long>)groupModel.executeDynamicQuery(
				disjunction, projection, orders);

		List<Long> result = new ArrayList<Long>();
		result.addAll(globalSitesGroupIds);
		result.addAll(normalSitesGroupIds);

		return result;
	}

	public String getUpdateMessage(PortletConfig portletConfig) {

		PluginPackage pluginPackage = getPluginPackage(portletConfig);

		if (pluginPackage == null) {
			return getUpdateMessageOffline(portletConfig);
		}

		@SuppressWarnings("unchecked")
		Collection<String> lastAvalibleVersion =
			(Collection<String>)RemoteConfigurationUtil.getConfigurationEntry(
				"lastAvalibleVersion");

		if ((lastAvalibleVersion == null) || lastAvalibleVersion.isEmpty()) {
			return getUpdateMessageOffline(portletConfig);
		}

		String portletVersion = pluginPackage.getVersion();

		if (lastAvalibleVersion.contains(portletVersion)) {
			return null;
		}

		return (String)RemoteConfigurationUtil.getConfigurationEntry(
				"updateMessage");
	}

	public String getUpdateMessageOffline(PortletConfig portletConfig) {
		LiferayPortletContext context =
			(LiferayPortletContext)portletConfig.getPortletContext();

		long installationTimestamp = context.getPortlet().getTimestamp();

		if (installationTimestamp == 0L) {
			return null;
		}

		long offlineUpdateTimeoutMilis =
			(Long)ConfigurationUtil.getConfigurationEntry(
				"offlineUpdateTimeoutMilis");

		long offlineUpdateTimestamp =
			(installationTimestamp + offlineUpdateTimeoutMilis);

		long currentTimeMillis = System.currentTimeMillis();

		if (offlineUpdateTimestamp > currentTimeMillis) {
			return null;
		}

		return (String)ConfigurationUtil.getConfigurationEntry(
				"offlineUpdateMessage");
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

}