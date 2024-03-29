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

package jorgediazest.indexchecker.output;

import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.dao.search.ResultRow;
import com.liferay.portal.kernel.dao.search.SearchContainer;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.search.Field;
import com.liferay.portal.kernel.service.GroupLocalServiceUtil;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.Validator;

import java.lang.reflect.Method;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.portlet.PortletConfig;
import javax.portlet.PortletURL;
import javax.portlet.RenderRequest;

import jorgediazest.indexchecker.ExecutionMode;

import jorgediazest.util.data.Comparison;
import jorgediazest.util.output.OutputUtils;
import jorgediazest.util.reflection.ReflectionUtil;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * @author Jorge Díaz
 */
public class IndexCheckerOutput {

	@SuppressWarnings("unchecked")
	public static List<String> generateCSVOutput(
		PortletConfig portletConfig, RenderRequest renderRequest) {

		EnumSet<ExecutionMode> executionMode =
			(EnumSet<ExecutionMode>)renderRequest.getAttribute("executionMode");
		Map<Company, Long> companyProcessTime =
			(Map<Company, Long>)renderRequest.getAttribute(
				"companyProcessTime");
		Map<Company, Map<Long, List<Comparison>>> companyResultDataMap =
			(Map<Company, Map<Long, List<Comparison>>>)
				renderRequest.getAttribute("companyResultDataMap");

		if ((executionMode == null) || (companyProcessTime == null) ||
			(companyResultDataMap == null)) {

			return null;
		}

		String title = (String)renderRequest.getAttribute("title");
		Map<Company, String> companyError =
			(Map<Company, String>)renderRequest.getAttribute("companyError");

		return generateCSVOutput(
			portletConfig, title, renderRequest.getLocale(),
			executionMode.contains(ExecutionMode.GROUP_BY_SITE),
			companyProcessTime, companyResultDataMap, companyError);
	}

	public static List<String> generateCSVOutput(
		PortletConfig portletConfig, String title, Locale locale,
		boolean groupBySite, Map<Company, Long> companyProcessTime,
		Map<Company, Map<Long, List<Comparison>>> companyResultDataMap,
		Map<Company, String> companyError) {

		List<String> out = new ArrayList<>();

		ResourceBundle resourceBundle = portletConfig.getResourceBundle(locale);

		if (companyResultDataMap != null) {
			String[] headerKeys;

			if (groupBySite) {
				headerKeys = new String[] {
					"output.company", "output.groupid", "output.groupname",
					"output.entityclass", "output.entityname",
					"output.errortype", "output.count", "output.primarykeys"
				};
			}
			else {
				headerKeys = new String[] {
					"output.company", "output.entityclass", "output.entityname",
					"output.errortype", "output.count", "output.primarykeys"
				};
			}

			List<String> headers = OutputUtils.getHeaders(
				portletConfig, locale, headerKeys);

			out.add(OutputUtils.getCSVRow(headers));
		}

		for (Map.Entry<Company, Long> companyEntry :
				companyProcessTime.entrySet()) {

			Long processTime = companyEntry.getValue();

			Company company = companyEntry.getKey();

			String companyOutput =
				company.getCompanyId() + " - " + company.getWebId();

			if (companyResultDataMap != null) {
				Map<Long, List<Comparison>> resultDataMap =
					companyResultDataMap.get(company);

				int numberOfRows = 0;

				for (Map.Entry<Long, List<Comparison>> entry :
						resultDataMap.entrySet()) {

					String groupIdOutput = null;
					String groupNameOutput = null;

					if (groupBySite) {
						try {
							Group group = GroupLocalServiceUtil.fetchGroup(
								entry.getKey());

							if (group == null) {
								groupIdOutput = LanguageUtil.get(
									resourceBundle,
									"output.not-applicable-groupid");
								groupNameOutput = LanguageUtil.get(
									resourceBundle,
									"output.not-applicable-groupname");
							}
							else {
								groupIdOutput = "" + group.getGroupId();
								groupNameOutput = group.getName(locale);
							}
						}
						catch (Exception e) {
							groupIdOutput = "" + entry.getKey();
						}
					}

					for (Comparison comp : entry.getValue()) {
						String lineError = OutputUtils.generateCSVRow(
							resourceBundle, comp, companyOutput, groupIdOutput,
							groupNameOutput, "error", locale, comp.getError(),
							-1);

						if (lineError != null) {
							numberOfRows++;
							out.add(lineError);
						}

						for (String type : comp.getOutputTypes()) {
							String attribute = "pk";

							if (type.contains("right")) {
								attribute = Field.UID;
							}

							String line = OutputUtils.generateCSVRow(
								resourceBundle, comp, companyOutput,
								groupIdOutput, groupNameOutput, type, attribute,
								locale);

							if (line != null) {
								numberOfRows++;
								out.add(line);
							}
						}
					}
				}

				if (numberOfRows == 0) {
					out.add(StringPool.BLANK);
					out.add(
						"No results found: your system is ok or perhaps you " +
							"have to change some filters");
				}
			}

			String errorMessage = companyError.get(company);

			if (Validator.isNotNull(errorMessage)) {
				out.add(
					"Company: " + company.getCompanyId() + " - " +
						company.getWebId());
				out.add(errorMessage);
			}

			out.add(StringPool.BLANK);
			out.add(
				"Executed " + title + " for company " + company.getCompanyId() +
					" in " + processTime + " ms");

			out.add(StringPool.BLANK);
		}

		Bundle bundle = FrameworkUtil.getBundle(IndexCheckerOutput.class);

		out.add("Version: " + bundle.getVersion());

		out.add(StringPool.BLANK);

		return out;
	}

	public static SearchContainer<Comparison> generateSearchContainer(
		PortletConfig portletConfig, RenderRequest renderRequest,
		boolean groupBySite, Map<Long, List<Comparison>> resultDataMap,
		PortletURL serverURL) {

		Locale locale = renderRequest.getLocale();

		ResourceBundle resourceBundle = portletConfig.getResourceBundle(locale);

		String[] headerKeys;

		if (groupBySite) {
			headerKeys = new String[] {
				"output.groupid", "output.groupname", "output.entityclass",
				"output.entityname", "output.errortype", "output.count",
				"output.primarykeys"
			};
		}
		else {
			headerKeys = new String[] {
				"output.entityclass", "output.entityname", "output.errortype",
				"output.count", "output.primarykeys"
			};
		}

		List<String> headerNames = OutputUtils.getHeaders(
			portletConfig, locale, headerKeys);

		SearchContainer<Comparison> searchContainer = new SearchContainer<>(
			renderRequest, null, null, SearchContainer.DEFAULT_CUR_PARAM,
			SearchContainer.MAX_DELTA, serverURL, headerNames, null);

		int numberOfRows = 0;

		Method setResults = null;
		Method setTotal = null;

		try {
			setResults = ReflectionUtil.getMethod(
				searchContainer, Arrays.asList("_setResults", "setResults"),
				List.class);

			setTotal = ReflectionUtil.getMethod(
				searchContainer, Arrays.asList("_setTotal", "setTotal"),
				Integer.TYPE);
		}
		catch (NoSuchMethodException noSuchMethodException) {
			throw new RuntimeException(noSuchMethodException);
		}

		for (Map.Entry<Long, List<Comparison>> entry :
				resultDataMap.entrySet()) {

			String groupIdOutput = null;
			String groupNameOutput = null;

			if (groupBySite) {
				Group group = GroupLocalServiceUtil.fetchGroup(entry.getKey());

				if (group == null) {
					groupIdOutput = LanguageUtil.get(
						resourceBundle, "output.not-applicable-groupid");
					groupNameOutput = LanguageUtil.get(
						resourceBundle, "output.not-applicable-groupname");
				}
				else {
					groupIdOutput = "" + group.getGroupId();
					groupNameOutput = group.getName(locale);
				}
			}

			List<Comparison> results = searchContainer.getResults();

			if ((results == null) || results.isEmpty()) {
				results = new ArrayList<>();
			}

			results.addAll(entry.getValue());

			results = ListUtil.subList(
				results, searchContainer.getStart(), searchContainer.getEnd());

			try {
				setResults.invoke(searchContainer, results);
			}
			catch (Exception exception) {
				throw new RuntimeException(exception);
			}

			List<ResultRow> resultRows = searchContainer.getResultRows();

			for (Comparison comp : entry.getValue()) {
				ResultRow rowError = OutputUtils.generateSearchContainerRow(
					resourceBundle, comp, groupIdOutput, groupNameOutput,
					"error", locale, numberOfRows, comp.getError());

				if (rowError != null) {
					numberOfRows++;
					resultRows.add(rowError);
				}

				for (String type : comp.getOutputTypes()) {
					String attribute = "pk";

					if (type.contains("right")) {
						attribute = Field.UID;
					}

					int maxSize = 0;

					ResultRow row = OutputUtils.generateSearchContainerRow(
						resourceBundle, comp, groupIdOutput, groupNameOutput,
						type, attribute, locale, numberOfRows, maxSize);

					if (row != null) {
						numberOfRows++;
						resultRows.add(row);
					}
				}
			}
		}

		try {
			setTotal.invoke(searchContainer, numberOfRows);
		}
		catch (Exception exception) {
			throw new RuntimeException(exception);
		}

		return searchContainer;
	}

}