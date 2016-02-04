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

import com.liferay.portal.kernel.dao.orm.Criterion;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.dao.shard.ShardUtil;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.Validator;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;

import jorgediazest.indexchecker.ExecutionMode;
import jorgediazest.indexchecker.data.Data;
import jorgediazest.indexchecker.data.Results;
import jorgediazest.indexchecker.model.IndexCheckerModel;
import jorgediazest.indexchecker.model.IndexCheckerModelFactory;

import jorgediazest.util.model.Model;
import jorgediazest.util.model.ModelFactory;
import jorgediazest.util.model.ModelUtil;

/**
 * Portlet implementation class IndexCheckerPortlet
 *
 * @author Jorge Díaz
 */
public class IndexCheckerPortlet extends MVCPortlet {

	public static IndexCheckerModel castModel(Model model) {
		IndexCheckerModel icModel;
		try {
			icModel = (IndexCheckerModel)model;
		}
		catch (Exception e) {
			if (_log.isWarnEnabled()) {
				_log.warn(
					"Model: " + model.getName() + " EXCEPTION: " +
						e.getClass() + " - " + e.getMessage(), e);
			}

			icModel = null;
		}

		return icModel;
	}

	public static Map<Long, List<Results>>
		executeCheck(
			Company company, List<Long> groupIds, List<String> classNames,
			Set<ExecutionMode> executionMode)
		throws SystemException {

		ModelFactory modelFactory = new IndexCheckerModelFactory();

		Map<String, Model> modelMap = modelFactory.getModelMap(classNames);

		List<IndexCheckerModel> modelList = new ArrayList<IndexCheckerModel>();

		for (Model model : modelMap.values()) {
			IndexCheckerModel icModel = castModel(model);
			modelList.add(icModel);
		}

		Map<Long, List<Results>> resultDataMap =
			new LinkedHashMap<Long, List<Results>>();

		long companyId = company.getCompanyId();

		List<Long> groupIdsFor = new ArrayList<Long>();
		groupIdsFor.add(0L);

		if (executionMode.contains(ExecutionMode.GROUP_BY_SITE)) {
			groupIdsFor.addAll(groupIds);
		}

		for (long groupId : groupIdsFor) {
			List<Results> resultList = new ArrayList<Results>();

			for (IndexCheckerModel icModel : modelList) {
				try {
					if ((groupId == 0L) &&
						icModel.hasAttribute("groupId") &&
						executionMode.contains(ExecutionMode.GROUP_BY_SITE)) {

						continue;
					}

					if ((groupId != 0L) &&
						!icModel.hasAttribute("groupId") &&
						executionMode.contains(ExecutionMode.GROUP_BY_SITE)) {

						continue;
					}

					Criterion filter = icModel.getCompanyGroupFilter(
						companyId, groupId);

					Set<Data> liferayData = new HashSet<Data>(
						icModel.getLiferayData(filter).values());

					Set<Data> indexData;

					if (executionMode.contains(ExecutionMode.SHOW_INDEX) ||
						!liferayData.isEmpty()) {

						indexData = icModel.getIndexData(companyId, groupId);
					}
					else {
						indexData = new HashSet<Data>();
					}

					Results result =
						Results.getIndexCheckResult(
							icModel, liferayData, indexData, executionMode);

					resultList.add(result);
				}
				catch (Exception e) {
					_log.error(
						"Model: " + icModel.getName() + " EXCEPTION: " +
							e.getClass() + " - " + e.getMessage(),e);
				}
			}

			resultDataMap.put(groupId, resultList);
		}

		return resultDataMap;
	}

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

	public void executeCheck(ActionRequest request, ActionResponse response)
		throws Exception {

		PortalUtil.copyRequestParameters(request, response);

		EnumSet<ExecutionMode> executionMode = getExecutionMode(request);

		String[] filterClassNameArr = null;
		String filterClassName = ParamUtil.getString(
			request, "filterClassName");

		if (Validator.isNotNull(filterClassName)) {
			filterClassNameArr = filterClassName.split(",");
		}

		String[] filterGroupIdArr = null;
		String filterGroupId = ParamUtil.getString(request, "filterGroupId");

		if (Validator.isNotNull(filterGroupId)) {
			filterGroupIdArr = filterGroupId.split(",");
		}

		List<Company> companies = CompanyLocalServiceUtil.getCompanies();

		Map<Company, Map<Long, List<Results>>> companyResultDataMap =
			new HashMap<Company, Map<Long, List<Results>>>();

		Map<Company, Long> companyProcessTime = new HashMap<Company, Long>();

		Map<Company, String> companyError = new HashMap<Company, String>();

		for (Company company : companies) {
			try {
				ShardUtil.pushCompanyService(company.getCompanyId());

				List<String> classNames = getClassNames(filterClassNameArr);

				List<Long> groupIds = getGroupIds(company, filterGroupIdArr);

				long startTime = System.currentTimeMillis();

				Map<Long, List<Results>> resultDataMap =
					IndexCheckerPortlet.executeCheck(
						company, groupIds, classNames, executionMode);

				long endTime = System.currentTimeMillis();

				if (_log.isInfoEnabled() &&
					executionMode.contains(
							ExecutionMode.DUMP_ALL_OBJECTS_TO_LOG)) {

					_log.info("COMPANY: " + company);

					boolean groupBySite = executionMode.contains(
						ExecutionMode.GROUP_BY_SITE);

					Results.dumpToLog(groupBySite, resultDataMap);
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

	public void executeReindex(ActionRequest request, ActionResponse response)
		throws Exception {

		PortalUtil.copyRequestParameters(request, response);

		EnumSet<ExecutionMode> executionMode = getExecutionMode(request);

		String[] filterClassNameArr = null;
		String filterClassName = ParamUtil.getString(
			request, "filterClassName");

		if (Validator.isNotNull(filterClassName)) {
			filterClassNameArr = filterClassName.split(",");
		}

		String[] filterGroupIdArr = null;
		String filterGroupId = ParamUtil.getString(request, "filterGroupId");

		if (Validator.isNotNull(filterGroupId)) {
			filterGroupIdArr = filterGroupId.split(",");
		}

		List<Company> companies = CompanyLocalServiceUtil.getCompanies();

		Map<Company, Long> companyProcessTime = new HashMap<Company, Long>();

		Map<Company, String> companyError = new HashMap<Company, String>();

		for (Company company : companies) {
			StringWriter sw = new StringWriter();

			PrintWriter pw = new PrintWriter(sw);

			try {
				ShardUtil.pushCompanyService(company.getCompanyId());

				List<String> classNames = getClassNames(filterClassNameArr);

				List<Long> groupIds = getGroupIds(company, filterGroupIdArr);

				long startTime = System.currentTimeMillis();

				Map<Long, List<Results>> resultDataMap =
					IndexCheckerPortlet.executeCheck(
						company, groupIds, classNames, executionMode);

				for (
					Entry<Long, List<Results>> entry :
						resultDataMap.entrySet()) {

					List<Results> resultList = entry.getValue();

					for (Results result : resultList) {
						Map<Data, String> errors = result.reindex();
/* TODO Mover todo esto al JSP */
						if ((errors!= null) && (errors.size() > 0)) {
							pw.println();
							pw.println("----");
							pw.println(result.getModel().getName());
							pw.println("----");

							for (Entry<Data, String> e : errors.entrySet()) {
								pw.println(
									" * " + e.getKey() + " - Exception: " +
										e.getValue());
							}
						}
					}
				}

				long endTime = System.currentTimeMillis();

				companyProcessTime.put(company, endTime-startTime);
			}
			catch (Exception e) {
				pw.println("Error during script execution: " + e.getMessage());
				e.printStackTrace(pw);
				_log.error(e, e);
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

		String[] filterClassNameArr = null;
		String filterClassName = ParamUtil.getString(
			request, "filterClassName");

		if (Validator.isNotNull(filterClassName)) {
			filterClassNameArr = filterClassName.split(",");
		}

		String[] filterGroupIdArr = null;
		String filterGroupId = ParamUtil.getString(request, "filterGroupId");

		if (Validator.isNotNull(filterGroupId)) {
			filterGroupIdArr = filterGroupId.split(",");
		}

		List<Company> companies = CompanyLocalServiceUtil.getCompanies();

		Map<Company, Long> companyProcessTime = new HashMap<Company, Long>();

		Map<Company, String> companyError = new HashMap<Company, String>();

		for (Company company : companies) {
			StringWriter sw = new StringWriter();

			PrintWriter pw = new PrintWriter(sw);

			try {
				ShardUtil.pushCompanyService(company.getCompanyId());

				List<String> classNames = getClassNames(filterClassNameArr);

				List<Long> groupIds = getGroupIds(company, filterGroupIdArr);

				long startTime = System.currentTimeMillis();

				Map<Long, List<Results>> resultDataMap =
					IndexCheckerPortlet.executeCheck(
						company, groupIds, classNames, executionMode);

				for (
					Entry<Long, List<Results>> entry :
						resultDataMap.entrySet()) {

					List<Results> resultList = entry.getValue();

					for (Results result : resultList) {
						Map<Data, String> errors = result.removeIndexOrphans();
/* TODO Mover todo esto al JSP */
						if ((errors!= null) && (errors.size() > 0)) {
							pw.println();
							pw.println("----");
							pw.println(result.getModel().getName());
							pw.println("----");

							for (Entry<Data, String> e : errors.entrySet()) {
								pw.println(
									" * " + e.getKey() + " - Exception: " +
										e.getValue());
							}
						}
					}
				}

				long endTime = System.currentTimeMillis();

				companyProcessTime.put(company, endTime-startTime);
			}
			catch (Exception e) {
				pw.println("Error during script execution: " + e.getMessage());
				e.printStackTrace(pw);
				_log.error(e, e);
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

	public List<String> getClassNames(String[] filterClassNameArr)
		throws SystemException {

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

		return classNames;
	}

	public List<Long> getGroupIds(Company company, String[] filterGroupIdArr)
		throws SystemException {

		List<Group> groups =
			GroupLocalServiceUtil.getCompanyGroups(
				company.getCompanyId(), QueryUtil.ALL_POS, QueryUtil.ALL_POS);

		List<Long> groupIds = new ArrayList<Long>();

		for (Group group : groups) {
			if (filterGroupIdArr == null) {
				groupIds.add(group.getGroupId());
				continue;
			}

			String groupIdStr = "" + group.getGroupId();

			for (int i = 0; i<filterGroupIdArr.length; i++) {
				if (groupIdStr.equals(filterGroupIdArr[i])) {
					groupIds.add(group.getGroupId());
					break;
				}
			}
		}

		return groupIds;
	}

	private static Log _log = LogFactoryUtil.getLog(IndexCheckerPortlet.class);

}