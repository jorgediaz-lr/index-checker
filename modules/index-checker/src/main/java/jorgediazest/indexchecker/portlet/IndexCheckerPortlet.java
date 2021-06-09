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

import com.liferay.petra.string.StringPool;
import com.liferay.portal.bundle.blacklist.BundleBlacklistManager;
import com.liferay.portal.kernel.bean.ClassLoaderBeanHandler;
import com.liferay.portal.kernel.dao.orm.Conjunction;
import com.liferay.portal.kernel.dao.orm.Disjunction;
import com.liferay.portal.kernel.dao.orm.Order;
import com.liferay.portal.kernel.dao.orm.OrderFactoryUtil;
import com.liferay.portal.kernel.dao.orm.Projection;
import com.liferay.portal.kernel.dao.orm.Property;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.dao.orm.RestrictionsFactoryUtil;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.GroupConstants;
import com.liferay.portal.kernel.portlet.LiferayPortletContext;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCPortlet;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.search.IndexerRegistryUtil;
import com.liferay.portal.kernel.security.auth.CompanyThreadLocal;
import com.liferay.portal.kernel.service.ClassNameLocalServiceUtil;
import com.liferay.portal.kernel.service.GroupLocalServiceUtil;
import com.liferay.portal.kernel.util.CalendarFactory;
import com.liferay.portal.kernel.util.CalendarFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.JavaConstants;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.SetUtil;
import com.liferay.portal.kernel.util.Validator;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import java.lang.reflect.Proxy;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.Portlet;
import javax.portlet.PortletConfig;
import javax.portlet.PortletException;
import javax.portlet.PortletPreferences;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;
import javax.portlet.ResourceURL;

import jorgediazest.indexchecker.ExecutionMode;
import jorgediazest.indexchecker.index.IndexSearchHelper;
import jorgediazest.indexchecker.model.IndexCheckerModelFactory;
import jorgediazest.indexchecker.output.IndexCheckerOutput;
import jorgediazest.indexchecker.portlet.constants.IndexCheckerKeys;
import jorgediazest.indexchecker.util.ConfigurationUtil;
import jorgediazest.indexchecker.util.RemoteConfigurationUtil;

import jorgediazest.util.data.Comparison;
import jorgediazest.util.data.ComparisonUtil;
import jorgediazest.util.data.Data;
import jorgediazest.util.model.Model;
import jorgediazest.util.model.ModelFactory;
import jorgediazest.util.model.ModelUtil;
import jorgediazest.util.output.OutputUtils;
import jorgediazest.util.reflection.ReflectionUtil;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.Version;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

/**
 * Portlet implementation class IndexCheckerPortlet
 *
 * @author Jorge Díaz
 */
@Component(
	immediate = true,
	property = {
		"com.liferay.portlet.control-panel-entry-category=apps",
		"com.liferay.portlet.css-class-wrapper=index_checker-portlet",
		"com.liferay.portlet.display-category=category.hidden",
		"com.liferay.portlet.preferences-company-wide=true",
		"com.liferay.portlet.preferences-unique-per-layout=false",
		"com.liferay.portlet.header-portlet-css=/css/main.css",
		"com.liferay.portlet.icon=/icon.png",
		"com.liferay.portlet.instanceable=false",
		"com.liferay.portlet.single-page-application=false",
		"javax.portlet.display-name=Index Checker",
		"javax.portlet.info.keywords=search,index,check,checker,verify,reindex,lucene,solr,elasticsearch",
		"javax.portlet.info.short-title=Index Checker",
		"javax.portlet.info.title=Index Checker",
		"javax.portlet.init-param.template-path=/",
		"javax.portlet.init-param.config-template=/html/indexchecker/config.jsp",
		"javax.portlet.init-param.view-template=/html/indexchecker/view.jsp",
		"javax.portlet.name=" + IndexCheckerKeys.INDEXCHECKER,
		"javax.portlet.portlet-name=index_checker",
		"javax.portlet.resource-bundle=content.Language",
		"javax.portlet.security-role-ref=administrator,power-user,user"
	},
	service = Portlet.class
)
public class IndexCheckerPortlet extends MVCPortlet {

	public static void dumpToLog(
		boolean groupBySite, Map<Long, List<Comparison>> comparisonDataMap,
		Locale locale) {

		if (!_log.isInfoEnabled()) {
			return;
		}

		for (Map.Entry<Long, List<Comparison>> entry :
				comparisonDataMap.entrySet()) {

			String groupTitle = null;
			Group group = GroupLocalServiceUtil.fetchGroup(entry.getKey());

			if ((group == null) && groupBySite) {
				groupTitle = "N/A";
			}
			else if (group != null) {
				groupTitle = group.getGroupId() + " - " + group.getName(locale);
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
		Date startModifiedDate, Date endModifiedDate,
		Set<ExecutionMode> executionMode) {

		List<Future<Comparison>> futureResultList = new ArrayList<>();

		for (Model model : modelList) {
			String className = model.getClassName();

			if (!hasIndexerEnabled(className)) {
				continue;
			}

			if (ConfigurationUtil.modelNotIndexed(className)) {
				continue;
			}

			CallableCheckGroupAndModel c = new CallableCheckGroupAndModel(
				queryCache, companyId, groupIds, startModifiedDate,
				endModifiedDate, model, executionMode);

			futureResultList.add(executor.submit(c));
		}

		return futureResultList;
	}

	public static Map<Long, List<Comparison>> executeCheck(
			Company company, List<Long> groupIds, List<String> classNames,
			Date startModifiedDate, Date endModifiedDate,
			Set<ExecutionMode> executionMode, int threadsExecutor)
		throws ExecutionException, InterruptedException {

		long companyId = company.getCompanyId();

		Map<String, Map<Long, List<Data>>> queryCache =
			new ConcurrentHashMap<>();

		ModelFactory modelFactory = new IndexCheckerModelFactory(
			companyId, startModifiedDate, endModifiedDate);

		List<Model> modelList = getModelList(modelFactory, classNames);

		ExecutorService executor = Executors.newFixedThreadPool(
			threadsExecutor);

		Map<Long, List<Future<Comparison>>> futureResultDataMap =
			new TreeMap<>();

		if (executionMode.contains(ExecutionMode.QUERY_BY_SITE)) {
			for (long groupId : groupIds) {
				List<Long> groupIdsAux = new ArrayList<>();

				groupIdsAux.add(groupId);

				List<Future<Comparison>> futureResultList =
					executeCallableCheckGroupAndModel(
						queryCache, executor, modelList, companyId, groupIdsAux,
						startModifiedDate, endModifiedDate, executionMode);

				futureResultDataMap.put(groupId, futureResultList);
			}
		}
		else {
			List<Future<Comparison>> futureResultList =
				executeCallableCheckGroupAndModel(
					queryCache, executor, modelList, companyId, groupIds,
					startModifiedDate, endModifiedDate, executionMode);

			futureResultDataMap.put(0L, futureResultList);
		}

		Map<Long, List<Comparison>> resultDataMap = new TreeMap<>();

		for (Map.Entry<Long, List<Future<Comparison>>> entry :
				futureResultDataMap.entrySet()) {

			List<Comparison> resultList = new ArrayList<>();

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

	public static EnumSet<ExecutionMode> getExecutionMode(
		ActionRequest request) {

		PortletPreferences portletPreferences = request.getPreferences();

		boolean outputGroupBySite = GetterUtil.getBoolean(
			portletPreferences.getValue("outputGroupBySite", StringPool.FALSE));

		boolean checkRelatedData = GetterUtil.getBoolean(
			portletPreferences.getValue("checkRelatedData", StringPool.FALSE));

		boolean queryBySite = GetterUtil.getBoolean(
			portletPreferences.getValue("queryBySite", StringPool.FALSE));

		boolean dumpAllObjectsToLog = GetterUtil.getBoolean(
			portletPreferences.getValue(
				"dumpAllObjectsToLog", StringPool.FALSE));

		EnumSet<ExecutionMode> executionMode = EnumSet.noneOf(
			ExecutionMode.class);

		if (outputGroupBySite) {
			executionMode.add(ExecutionMode.GROUP_BY_SITE);
		}

		if (checkRelatedData) {
			executionMode.add(ExecutionMode.CHECK_RELATED_DATA);
		}

		if (queryBySite) {
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

		if (dumpAllObjectsToLog) {
			executionMode.add(ExecutionMode.DUMP_ALL_OBJECTS_TO_LOG);
		}

		return executionMode;
	}

	public static Log getLogger() {
		return _log;
	}

	public static List<Model> getModelList(
		ModelFactory modelFactory, List<String> classNames) {

		List<Model> modelList = new ArrayList<>();

		for (String className : classNames) {
			Model model = modelFactory.getModelObject(className);

			if (model != null) {
				modelList.add(model);
			}
		}

		return modelList;
	}

	public static boolean hasIndexerEnabled(String className) {
		Object indexer = IndexerRegistryUtil.getIndexer(className);

		if (indexer == null) {
			return false;
		}

		if (indexer instanceof Proxy) {
			try {
				ClassLoaderBeanHandler classLoaderBeanHandler =
					(ClassLoaderBeanHandler)Proxy.getInvocationHandler(indexer);

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

		try {
			return (Boolean)ReflectionUtil.getWrappedObject(
				indexer, "isIndexerEnabled");
		}
		catch (Exception e) {
			if (_log.isDebugEnabled()) {
				_log.debug(e, e);
			}
		}

		return false;
	}

	public static Map<Data, String> reindex(Comparison comparison) {
		Model model = comparison.getModel();

		if (model == null) {
			return null;
		}

		Set<Data> objectsToReindex = new HashSet<>();

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

		PortletConfig portletConfig = (PortletConfig)renderRequest.getAttribute(
			JavaConstants.JAVAX_PORTLET_CONFIG);

		renderRequest.setAttribute(
			"updateMessage", getUpdateMessage(portletConfig));

		List<String> outputList = IndexCheckerOutput.generateCSVOutput(
			portletConfig, renderRequest);

		String portletId = portletConfig.getPortletName();

		String outputContent = OutputUtils.listStringToString(outputList);

		FileEntry exportCsvFileEntry = OutputUtils.addPortletOutputFileEntry(
			portletId, PortalUtil.getUserId(renderRequest), outputContent);

		if (exportCsvFileEntry != null) {
			ResourceURL exportCsvResourceURL =
				renderResponse.createResourceURL();

			exportCsvResourceURL.setResourceID(exportCsvFileEntry.getTitle());

			renderRequest.setAttribute(
				"exportCsvResourceURL", exportCsvResourceURL.toString());
		}

		try {
			List<Long> siteGroupIds = getSiteGroupIds();

			renderRequest.setAttribute("groupIdList", siteGroupIds);

			List<String> groupDescriptionList = getSiteGroupDescriptions(
				siteGroupIds, renderRequest.getLocale());

			renderRequest.setAttribute(
				"groupDescriptionList", groupDescriptionList);
		}
		catch (Exception e) {
			throw new PortletException(e);
		}

		try {
			renderRequest.setAttribute("modelList", getModelList());
		}
		catch (SystemException se) {
			throw new PortletException(se);
		}

		long filterModifiedDate = ParamUtil.getLong(
			renderRequest, "filterModifiedDate");

		renderRequest.setAttribute("filterModifiedDate", filterModifiedDate);

		super.doView(renderRequest, renderResponse);
	}

	public void executeCheck(ActionRequest request, ActionResponse response)
		throws Exception {

		PortalUtil.copyRequestParameters(request, response);

		EnumSet<ExecutionMode> executionMode = getExecutionMode(request);

		String[] filterClassNameArr = ParamUtil.getParameterValues(
			request, "filterClassName");

		response.setRenderParameter("filterClassName", new String[0]);

		request.setAttribute(
			"filterClassNameSelected", SetUtil.fromArray(filterClassNameArr));

		String[] filterGroupIdArr = ParamUtil.getParameterValues(
			request, "filterGroupId");

		response.setRenderParameter("filterGroupId", new String[0]);

		request.setAttribute(
			"filterGroupIdSelected", SetUtil.fromArray(filterGroupIdArr));

		List<String> classNames = getClassNames(filterClassNameArr);

		Date startModifiedDate = null;
		Date endModifiedDate = null;

		long filterModifiedDate = ParamUtil.getLong(
			request, "filterModifiedDate");

		if (filterModifiedDate > 0) {
			long now = System.currentTimeMillis();

			startModifiedDate = getStartDate(now, filterModifiedDate);
			endModifiedDate = getTomorrowDate(now);
		}

		Map<Company, Map<Long, List<Comparison>>> companyResultDataMap =
			new LinkedHashMap<>();

		Map<Company, Long> companyProcessTime = new LinkedHashMap<>();

		Map<Company, String> companyError = new LinkedHashMap<>();

		for (Company company : getCompanyList()) {
			try {
				CompanyThreadLocal.setCompanyId(company.getCompanyId());

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

				Map<Long, List<Comparison>> resultDataMap = executeCheck(
					company, groupIds, classNames, startModifiedDate,
					endModifiedDate, executionMode,
					getNumberOfThreads(request));

				boolean groupBySite = executionMode.contains(
					ExecutionMode.GROUP_BY_SITE);

				Set<Long> groupIdsSet = resultDataMap.keySet();

				long numberOfGroupIds = groupIdsSet.size();

				if (groupBySite && (numberOfGroupIds == 1)) {
					Collection<List<Comparison>> values =
						resultDataMap.values();

					List<Comparison> listComparison =
						(List<Comparison>)values.toArray()[0];

					resultDataMap = new TreeMap<>();

					for (Comparison c : listComparison) {
						Map<Long, Comparison> map = c.splitByAttribute(
							"groupId");

						for (Map.Entry<Long, Comparison> e : map.entrySet()) {
							List<Comparison> list = resultDataMap.get(
								e.getKey());

							if (list == null) {
								list = new ArrayList<>();

								resultDataMap.put(e.getKey(), list);
							}

							list.add(e.getValue());
						}
					}
				}

				if (!groupBySite && (numberOfGroupIds > 1)) {
					List<Comparison> tempComparisonList = new ArrayList<>();

					for (List<Comparison> auxList : resultDataMap.values()) {
						tempComparisonList.addAll(auxList);
					}

					List<Comparison> resultComparisonLisn =
						ComparisonUtil.mergeComparisons(tempComparisonList);

					resultDataMap = new TreeMap<>();

					resultDataMap.put(0L, resultComparisonLisn);
				}

				long endTime = System.currentTimeMillis();

				if (_log.isInfoEnabled() &&
					executionMode.contains(
						ExecutionMode.DUMP_ALL_OBJECTS_TO_LOG)) {

					_log.info("COMPANY: " + company);

					dumpToLog(groupBySite, resultDataMap, request.getLocale());
				}

				companyResultDataMap.put(company, resultDataMap);

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
		}

		request.setAttribute("companyError", companyError);
		request.setAttribute("companyProcessTime", companyProcessTime);
		request.setAttribute("companyResultDataMap", companyResultDataMap);
		request.setAttribute("executionMode", executionMode);
		request.setAttribute("title", "Check Index");
	}

	public void executeReindex(ActionRequest request, ActionResponse response)
		throws Exception {

		PortalUtil.copyRequestParameters(request, response);

		EnumSet<ExecutionMode> executionMode = getExecutionMode(request);

		String[] filterClassNameArr = ParamUtil.getParameterValues(
			request, "filterClassName");

		response.setRenderParameter("filterClassName", new String[0]);

		request.setAttribute(
			"filterClassNameSelected", SetUtil.fromArray(filterClassNameArr));

		String[] filterGroupIdArr = ParamUtil.getParameterValues(
			request, "filterGroupId");

		response.setRenderParameter("filterGroupId", new String[0]);

		request.setAttribute(
			"filterGroupIdSelected", SetUtil.fromArray(filterGroupIdArr));

		List<String> classNames = getClassNames(filterClassNameArr);

		Date startModifiedDate = null;
		Date endModifiedDate = null;

		long filterModifiedDate = ParamUtil.getLong(
			request, "filterModifiedDate");

		if (filterModifiedDate > 0) {
			long now = System.currentTimeMillis();

			startModifiedDate = getStartDate(now, filterModifiedDate);
			endModifiedDate = getTomorrowDate(now);
		}

		Map<Company, Long> companyProcessTime = new LinkedHashMap<>();

		Map<Company, String> companyError = new LinkedHashMap<>();

		for (Company company : getCompanyList()) {
			StringWriter sw = new StringWriter();

			PrintWriter pw = new PrintWriter(sw);

			try {
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

				Map<Long, List<Comparison>> resultDataMap = executeCheck(
					company, groupIds, classNames, startModifiedDate,
					endModifiedDate, executionMode,
					getNumberOfThreads(request));

				for (Map.Entry<Long, List<Comparison>> entry :
						resultDataMap.entrySet()) {

					List<Comparison> resultList = entry.getValue();

					for (Comparison result : resultList) {
						Map<Data, String> errors = reindex(result);
						/* TODO Mover todo esto al JSP */
						if (((errors != null) && !errors.isEmpty()) ||
							(result.getError() != null)) {

							pw.println();
							pw.println("----");
							pw.println(result.getModelName());
							pw.println("----");

							for (Map.Entry<Data, String> e :
									errors.entrySet()) {

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

		request.setAttribute("companyError", companyError);
		request.setAttribute("companyProcessTime", companyProcessTime);
		request.setAttribute("executionMode", executionMode);
		request.setAttribute("title", "Reindex");
	}

	public void executeRemoveOrphans(
			ActionRequest request, ActionResponse response)
		throws Exception {

		PortalUtil.copyRequestParameters(request, response);

		EnumSet<ExecutionMode> executionMode = getExecutionMode(request);

		String[] filterClassNameArr = ParamUtil.getParameterValues(
			request, "filterClassName");

		response.setRenderParameter("filterClassName", new String[0]);

		request.setAttribute(
			"filterClassNameSelected", SetUtil.fromArray(filterClassNameArr));

		String[] filterGroupIdArr = ParamUtil.getParameterValues(
			request, "filterGroupId");

		response.setRenderParameter("filterGroupId", new String[0]);

		request.setAttribute(
			"filterGroupIdSelected", SetUtil.fromArray(filterGroupIdArr));

		List<String> classNames = getClassNames(filterClassNameArr);

		Map<Company, Long> companyProcessTime = new LinkedHashMap<>();

		Map<Company, String> companyError = new LinkedHashMap<>();

		for (Company company : getCompanyList()) {
			StringWriter sw = new StringWriter();

			PrintWriter pw = new PrintWriter(sw);

			try {
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

				Map<Long, List<Comparison>> resultDataMap = executeCheck(
					company, groupIds, classNames, null, null, executionMode,
					getNumberOfThreads(request));

				for (Map.Entry<Long, List<Comparison>> entry :
						resultDataMap.entrySet()) {

					List<Comparison> resultList = entry.getValue();

					for (Comparison result : resultList) {
						Map<Data, String> errors = removeIndexOrphans(result);
						/* TODO Mover todo esto al JSP */
						if (((errors != null) && !errors.isEmpty()) ||
							(result.getError() != null)) {

							pw.println();
							pw.println("----");
							pw.println(result.getModelName());
							pw.println("----");

							for (Map.Entry<Data, String> e :
									errors.entrySet()) {

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

		request.setAttribute("companyError", companyError);
		request.setAttribute("companyProcessTime", companyProcessTime);
		request.setAttribute("executionMode", executionMode);
		request.setAttribute("title", "Remove index orphan");
	}

	public List<String> getClassNames() {
		return getClassNames(null);
	}

	public List<String> getClassNames(String[] filterClassNameArr) {
		if ((filterClassNameArr == null) || (filterClassNameArr.length == 0) ||
			((filterClassNameArr.length == 1) &&
			 Validator.isNull(filterClassNameArr[0]))) {

			filterClassNameArr = null;
		}

		List<String> allClassName = ModelUtil.getClassNameValues(
			ClassNameLocalServiceUtil.getClassNames(
				QueryUtil.ALL_POS, QueryUtil.ALL_POS));

		List<String> classNames = new ArrayList<>();

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

		return (List<Company>)companyModel.executeDynamicQuery(
			null, OrderFactoryUtil.asc("companyId"));
	}

	public List<Long> getGroupIds(
		Company company, Set<ExecutionMode> executionMode,
		String[] filterGroupIdArr) {

		if ((filterGroupIdArr != null) && (filterGroupIdArr.length == 1) &&
			filterGroupIdArr[0].equals("-1000")) {

			filterGroupIdArr = null;
		}

		boolean queryBySite = executionMode.contains(
			ExecutionMode.QUERY_BY_SITE);

		if (!queryBySite && (filterGroupIdArr == null)) {
			return null;
		}

		List<Group> groups = GroupLocalServiceUtil.getCompanyGroups(
			company.getCompanyId(), QueryUtil.ALL_POS, QueryUtil.ALL_POS);

		List<Long> groupIds = new ArrayList<>();

		boolean allSites = false;
		boolean userSites = false;

		if (filterGroupIdArr != null) {
			for (String filterGroupId : filterGroupIdArr) {
				if (filterGroupId.equals("0")) {
					groupIds.add(0L);
				}

				if (filterGroupId.equals("-1")) {
					allSites = true;
				}

				if (filterGroupId.equals("-2")) {
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

			if (allSites &&
				(group.isSite() || group.isStagingGroup() ||
				 group.isCompany())) {

				groupIds.add(group.getGroupId());

				continue;
			}

			if (userSites && (group.isUser() || group.isUserGroup())) {
				groupIds.add(group.getGroupId());

				continue;
			}

			String groupIdStr = "" + group.getGroupId();

			for (String filterGroupId : filterGroupIdArr) {
				if (groupIdStr.equals(filterGroupId)) {
					groupIds.add(group.getGroupId());

					break;
				}
			}
		}

		return groupIds;
	}

	public List<Model> getModelList() {
		return getModelList(null);
	}

	public List<Model> getModelList(String[] filterClassNameArr) {
		List<String> classNames = getClassNames(filterClassNameArr);

		ModelFactory modelFactory = new IndexCheckerModelFactory();

		List<Model> modelList = new ArrayList<>();

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
		PortletPreferences portletPreferences = actionRequest.getPreferences();

		int numberOfThreads = GetterUtil.getInteger(
			portletPreferences.getValue("numberOfThreads", StringPool.BLANK));

		if (numberOfThreads != 0) {
			return numberOfThreads;
		}

		return ConfigurationUtil.getDefaultNumberThreads();
	}

	public List<String> getSiteGroupDescriptions(
		List<Long> siteGroupIds, Locale locale) {

		List<String> groupDescriptionList = new ArrayList<>();

		for (Long siteGroupId : siteGroupIds) {
			Group group = GroupLocalServiceUtil.fetchGroup(siteGroupId);

			String groupDescription = group.getName(locale);

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

		Property classNameIdProperty = groupModel.getProperty("classNameId");
		Property liveGroupIdProperty = groupModel.getProperty("liveGroupId");

		conjuntion.add(classNameIdProperty.eq(companyClassNameId));
		conjuntion.add(liveGroupIdProperty.eq(0L));

		/* Get groupIds of live global groups */
		List<Long> liveGlobalGroupIds =
			(List<Long>)groupModel.executeDynamicQuery(conjuntion, projection);

		/* Get groupIds of staging and live global groups */
		Disjunction disjunctionGlobal = RestrictionsFactoryUtil.disjunction();

		classNameIdProperty = groupModel.getProperty("classNameId");

		disjunctionGlobal.add(classNameIdProperty.eq(companyClassNameId));

		disjunctionGlobal.add(
			groupModel.getAttributeCriterion(
				"liveGroupId", liveGlobalGroupIds));

		List<Order> orders = new ArrayList<>();

		orders.add(OrderFactoryUtil.asc("companyId"));
		orders.add(OrderFactoryUtil.asc("friendlyURL"));

		List<Long> globalSitesGroupIds =
			(List<Long>)groupModel.executeDynamicQuery(
				disjunctionGlobal, projection, orders);

		/* Get groupIds of staging and live normal groups */
		Conjunction stagingSites = RestrictionsFactoryUtil.conjunction();

		Property siteProperty = groupModel.getProperty("site");

		liveGroupIdProperty = groupModel.getProperty("liveGroupId");

		stagingSites.add(siteProperty.eq(false));
		stagingSites.add(liveGroupIdProperty.ne(0L));
		stagingSites.add(
			RestrictionsFactoryUtil.not(
				groupModel.getAttributeCriterion(
					"liveGroupId", liveGlobalGroupIds)));

		Conjunction normalSites = RestrictionsFactoryUtil.conjunction();

		classNameIdProperty = groupModel.getProperty("classNameId");

		siteProperty = groupModel.getProperty("site");

		normalSites.add(siteProperty.eq(true));

		normalSites.add(classNameIdProperty.ne(companyClassNameId));

		Disjunction disjunction = RestrictionsFactoryUtil.disjunction();

		disjunction.add(stagingSites);
		disjunction.add(normalSites);

		orders = Collections.singletonList(OrderFactoryUtil.asc("name"));

		List<Long> normalSitesGroupIds =
			(List<Long>)groupModel.executeDynamicQuery(
				disjunction, projection, orders);

		List<Long> result = new ArrayList<>();

		result.addAll(globalSitesGroupIds);
		result.addAll(normalSitesGroupIds);

		return result;
	}

	public String getUpdateMessage(PortletConfig portletConfig) {
		@SuppressWarnings("unchecked")
		Collection<String> lastAvalibleVersion =
			(Collection<String>)RemoteConfigurationUtil.getConfigurationEntry(
				"lastAvalibleVersion");

		if ((lastAvalibleVersion == null) || lastAvalibleVersion.isEmpty()) {
			return getUpdateMessageOffline(portletConfig);
		}

		Bundle bundle = FrameworkUtil.getBundle(getClass());

		Version version = bundle.getVersion();

		if (lastAvalibleVersion.contains(version.toString())) {
			return null;
		}

		return (String)RemoteConfigurationUtil.getConfigurationEntry(
			"updateMessage");
	}

	public String getUpdateMessageOffline(PortletConfig portletConfig) {
		LiferayPortletContext context =
			(LiferayPortletContext)portletConfig.getPortletContext();

		com.liferay.portal.kernel.model.Portlet portlet = context.getPortlet();

		long installationTimestamp = portlet.getTimestamp();

		if (installationTimestamp == 0L) {
			return null;
		}

		long offlineUpdateTimeoutMilis =
			(Long)ConfigurationUtil.getConfigurationEntry(
				"offlineUpdateTimeoutMilis");

		long offlineUpdateTimestamp =
			installationTimestamp + offlineUpdateTimeoutMilis;

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

		PortletConfig portletConfig = (PortletConfig)request.getAttribute(
			JavaConstants.JAVAX_PORTLET_CONFIG);

		String portletId = portletConfig.getPortletName();

		OutputUtils.servePortletFileEntry(
			portletId, request.getResourceID(), request, response);
	}

	@Activate
	protected void activate(Map<String, Object> properties) {
		try {
			_bundleBlacklistManager.addToBlacklistAndUninstall(
				"index-checker-portlet");
		}
		catch (IOException ioException) {
			_log.error(ioException, ioException);
		}
	}

	@Deactivate
	protected void deactivate() {
		try {
			_bundleBlacklistManager.removeFromBlacklistAndInstall(
				"index-checker-portlet");
		}
		catch (IOException ioException) {
			_log.error(ioException, ioException);
		}
	}

	protected Date getStartDate(long timeInMillis, long hoursToSubstract) {
		CalendarFactory calendarFactory =
			CalendarFactoryUtil.getCalendarFactory();

		long start = timeInMillis - (hoursToSubstract * 60 * 60 * 1000);

		Calendar startCalendar = calendarFactory.getCalendar(start);

		return startCalendar.getTime();
	}

	protected Date getTomorrowDate(long timeInMillis) {
		CalendarFactory calendarFactory =
			CalendarFactoryUtil.getCalendarFactory();

		Calendar tomorrowCalendar = calendarFactory.getCalendar(timeInMillis);

		tomorrowCalendar.add(Calendar.DATE, 1);
		tomorrowCalendar.set(Calendar.HOUR_OF_DAY, 0);
		tomorrowCalendar.set(Calendar.MINUTE, 0);
		tomorrowCalendar.set(Calendar.SECOND, 0);

		return tomorrowCalendar.getTime();
	}

	private static Log _log = LogFactoryUtil.getLog(IndexCheckerPortlet.class);

	@Reference
	private BundleBlacklistManager _bundleBlacklistManager;

}