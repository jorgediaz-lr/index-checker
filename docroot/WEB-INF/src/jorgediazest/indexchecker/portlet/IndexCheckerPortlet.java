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

import jorgediazest.indexchecker.ExecutionMode;
import jorgediazest.indexchecker.IndexChecker;
import jorgediazest.indexchecker.IndexCheckerResult;
import jorgediazest.indexchecker.IndexCheckerUtil;
import jorgediazest.indexchecker.index.IndexWrapper;
import jorgediazest.indexchecker.index.IndexWrapperLuceneJar;
import jorgediazest.indexchecker.index.IndexWrapperLuceneReflection;
import jorgediazest.indexchecker.index.IndexWrapperSearch;
import jorgediazest.util.model.ModelUtil;

import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.dao.shard.ShardUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.Group;
import com.liferay.portal.service.ClassNameLocalServiceUtil;
import com.liferay.portal.service.CompanyLocalServiceUtil;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.util.PortalUtil;
import com.liferay.util.bridges.mvc.MVCPortlet;

import java.io.PrintWriter;
import java.io.StringWriter;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;

/**
 * Portlet implementation class IndexCheckerPortlet
 *
 * @author Jorge Díaz
 */
public class IndexCheckerPortlet extends MVCPortlet {

	public static EnumSet<ExecutionMode> getExecutionMode(
		ActionRequest request) {

		EnumSet<ExecutionMode> executionMode = EnumSet.noneOf(
			ExecutionMode.class);

		if (ParamUtil.getBoolean(request, "outputGroupBySite")) {
			executionMode.add(ExecutionMode.GROUP_BY_SITE);
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

	public void executeGetIndexMissingClassNames(
			ActionRequest request, ActionResponse response)
		throws Exception {

		PortalUtil.copyRequestParameters(request, response);

		EnumSet<ExecutionMode> executionMode = getExecutionMode(request);

		String indexWrapperClassName = ParamUtil.getString(
			request, "indexWrapperClassName");

		Class<? extends IndexWrapper> indexWrapperClass = getIndexWrapper(
			indexWrapperClassName);

		List<Company> companies = CompanyLocalServiceUtil.getCompanies();

		Map<Company, Long> companyProcessTime = new HashMap<Company, Long>();

		Map<Company, String> companyError = new HashMap<Company, String>();

		for (Company company : companies) {
			List<String> classNames =
				ModelUtil.getClassNameValues(
					ClassNameLocalServiceUtil.getClassNames(
						QueryUtil.ALL_POS, QueryUtil.ALL_POS));

			try {
				ShardUtil.pushCompanyService(company.getCompanyId());

				long startTime = System.currentTimeMillis();

				String error =
					IndexCheckerUtil.listStringToString(
						IndexChecker.executeScriptGetIndexMissingClassNames(
							indexWrapperClass, company, classNames));

				companyError.put(company, error);

				long endTime = System.currentTimeMillis();

				companyProcessTime.put(company, endTime-startTime);
			}
			finally {
				ShardUtil.popCompanyService();
			}
		}

		request.setAttribute("title", "Index missing ClassNames");
		request.setAttribute("executionMode", executionMode);
		request.setAttribute("companyProcessTime", companyProcessTime);
		request.setAttribute("companyError", companyError);
	}

	public void executeReindex(ActionRequest request, ActionResponse response)
		throws Exception {

		PortalUtil.copyRequestParameters(request, response);

		EnumSet<ExecutionMode> executionMode = getExecutionMode(request);

		String indexWrapperClassName = ParamUtil.getString(
			request, "indexWrapperClassName");

		Class<? extends IndexWrapper> indexWrapperClass = getIndexWrapper(
			indexWrapperClassName);

		String filterClassName = ParamUtil.getString(
			request, "filterClassName");

		List<Company> companies = CompanyLocalServiceUtil.getCompanies();

		Map<Company, Long> companyProcessTime = new HashMap<Company, Long>();

		Map<Company, String> companyError = new HashMap<Company, String>();

		for (Company company : companies) {
			List<String> allClassName =
				ModelUtil.getClassNameValues(
					ClassNameLocalServiceUtil.getClassNames(
						QueryUtil.ALL_POS, QueryUtil.ALL_POS));

			List<String> classNames = new ArrayList<String>();

			for (String className : allClassName) {
				if ((className != null) &&
					((filterClassName == null) ||
					 className.contains(filterClassName))) {

					classNames.add(className);
				}
			}

			try {
				ShardUtil.pushCompanyService(company.getCompanyId());

				List<Group> groups =
					GroupLocalServiceUtil.getCompanyGroups(
						company.getCompanyId(), QueryUtil.ALL_POS,
						QueryUtil.ALL_POS);

				long startTime = System.currentTimeMillis();

				Map<Long, List<IndexCheckerResult>> resultDataMap =
					IndexChecker.executeScript(
						indexWrapperClass, company, groups, classNames,
						executionMode);

				for (
					Entry<Long, List<IndexCheckerResult>> entry :
						resultDataMap.entrySet()) {

					List<IndexCheckerResult> resultList = entry.getValue();

					for (IndexCheckerResult result : resultList) {
						result.reindex();
					}
				}

				long endTime = System.currentTimeMillis();

				companyProcessTime.put(company, endTime-startTime);
			}
			catch (Exception e) {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				pw.println("Error during script execution: " + e.getMessage());
				e.printStackTrace(pw);
				companyError.put(company, sw.toString());
				_log.error(e, e);
			}
			finally {
				ShardUtil.popCompanyService();
			}
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

		String indexWrapperClassName = ParamUtil.getString(
			request, "indexWrapperClassName");

		Class<? extends IndexWrapper> indexWrapperClass = getIndexWrapper(
			indexWrapperClassName);

		String filterClassName = ParamUtil.getString(
			request, "filterClassName");

		List<Company> companies = CompanyLocalServiceUtil.getCompanies();

		Map<Company, Long> companyProcessTime = new HashMap<Company, Long>();

		Map<Company, String> companyError = new HashMap<Company, String>();

		for (Company company : companies) {
			List<String> allClassName =
				ModelUtil.getClassNameValues(
					ClassNameLocalServiceUtil.getClassNames(
						QueryUtil.ALL_POS, QueryUtil.ALL_POS));

			List<String> classNames = new ArrayList<String>();

			for (String className : allClassName) {
				if ((className != null) &&
					((filterClassName == null) ||
					 className.contains(filterClassName))) {

					classNames.add(className);
				}
			}

			try {
				ShardUtil.pushCompanyService(company.getCompanyId());

				List<Group> groups =
					GroupLocalServiceUtil.getCompanyGroups(
						company.getCompanyId(), QueryUtil.ALL_POS,
						QueryUtil.ALL_POS);

				long startTime = System.currentTimeMillis();

				Map<Long, List<IndexCheckerResult>> resultDataMap =
					IndexChecker.executeScript(
						indexWrapperClass, company, groups, classNames,
						executionMode);

				for (
					Entry<Long, List<IndexCheckerResult>> entry :
						resultDataMap.entrySet()) {

					List<IndexCheckerResult> resultList = entry.getValue();

					for (IndexCheckerResult result : resultList) {
						result.removeIndexOrphans();
					}
				}

				long endTime = System.currentTimeMillis();

				companyProcessTime.put(company, endTime-startTime);
			}
			catch (Exception e) {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				pw.println("Error during script execution: " + e.getMessage());
				e.printStackTrace(pw);
				companyError.put(company, sw.toString());
				_log.error(e, e);
			}
			finally {
				ShardUtil.popCompanyService();
			}
		}

		request.setAttribute("title", "Remove index orphan");
		request.setAttribute("executionMode", executionMode);
		request.setAttribute("companyProcessTime", companyProcessTime);
		request.setAttribute("companyError", companyError);
	}

	public void executeScript(ActionRequest request, ActionResponse response)
		throws Exception {

		PortalUtil.copyRequestParameters(request, response);

		EnumSet<ExecutionMode> executionMode = getExecutionMode(request);

		String indexWrapperClassName = ParamUtil.getString(
			request, "indexWrapperClassName");

		Class<? extends IndexWrapper> indexWrapperClass = getIndexWrapper(
			indexWrapperClassName);

		String[] filterClassNameArr = null;
		String filterClassName = ParamUtil.getString(
			request, "filterClassName");

		if (filterClassName != null) {
			filterClassNameArr = filterClassName.split(",");
		}

		List<Company> companies = CompanyLocalServiceUtil.getCompanies();

		Map<Company, Map<Long, List<IndexCheckerResult>>> companyResultDataMap =
			new HashMap<Company, Map<Long, List<IndexCheckerResult>>>();

		Map<Company, Long> companyProcessTime = new HashMap<Company, Long>();

		Map<Company, String> companyError = new HashMap<Company, String>();

		for (Company company : companies) {
			List<String> allClassName =
				ModelUtil.getClassNameValues(
					ClassNameLocalServiceUtil.getClassNames(
						QueryUtil.ALL_POS, QueryUtil.ALL_POS));

			List<String> classNames = new ArrayList<String>();

			for (String className : allClassName) {
				if (className == null) {
					continue;
				}

				if (filterClassNameArr == null) {
					classNames.add(className);
					continue;
				}

				for (int i = 0; i<filterClassNameArr.length; i++) {
					if (className.contains(filterClassNameArr[i])) {
						classNames.add(className);
						break;
					}
				}
			}

			try {
				ShardUtil.pushCompanyService(company.getCompanyId());

				List<Group> groups =
					GroupLocalServiceUtil.getCompanyGroups(
						company.getCompanyId(), QueryUtil.ALL_POS,
						QueryUtil.ALL_POS);

				long startTime = System.currentTimeMillis();

				Map<Long, List<IndexCheckerResult>> resultDataMap =
					IndexChecker.executeScript(
						indexWrapperClass, company, groups, classNames,
						executionMode);

				long endTime = System.currentTimeMillis();

				if (_log.isInfoEnabled() &&
					executionMode.contains(
							ExecutionMode.DUMP_ALL_OBJECTS_TO_LOG)) {

					_log.info("COMPANY: " + company);

					boolean groupBySite = executionMode.contains(
						ExecutionMode.GROUP_BY_SITE);

					IndexCheckerResult.dumpToLog(groupBySite, resultDataMap);
				}

				companyResultDataMap.put(company, resultDataMap);

				companyProcessTime.put(company, (endTime-startTime));
			}
			catch (Exception e) {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				pw.println("Error during script execution: " + e.getMessage());
				e.printStackTrace(pw);
				companyError.put(company, sw.toString());
				_log.error(e, e);
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

	protected Class<? extends IndexWrapper> getIndexWrapper(
		String indexWrapperClassName) {

		if ("LuceneJar".equals(indexWrapperClassName)) {
			return IndexWrapperLuceneJar.class;
		}
		else if ("LuceneReflection".equals(indexWrapperClassName)) {
			return IndexWrapperLuceneReflection.class;
		}
		else if ("Search".equals(indexWrapperClassName)) {
			return IndexWrapperSearch.class;
		}
		else {
			return IndexWrapperLuceneReflection.class;
		}
	}

	private static Log _log = LogFactoryUtil.getLog(IndexCheckerPortlet.class);

}