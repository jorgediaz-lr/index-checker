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

package jorgediazest.indexchecker;

import com.liferay.portal.kernel.dao.search.ResultRow;
import com.liferay.portal.kernel.dao.search.SearchContainer;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.HtmlUtil;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.Group;
import com.liferay.portal.service.GroupLocalServiceUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.portlet.PortletConfig;
import javax.portlet.PortletURL;
import javax.portlet.RenderRequest;

import jorgediazest.indexchecker.data.Data;
import jorgediazest.indexchecker.model.IndexCheckerModel;

/**
 * @author Jorge Díaz
 */
public class IndexCheckerUtil {

	public static List<String> generateOutputCSV(
		PortletConfig portletConfig, String title, Locale locale,
		EnumSet<ExecutionMode> executionMode,
		Map<Company, Long> companyProcessTime,
		Map<Company, Map<Long, List<IndexCheckerResult>>> companyResultDataMap,
		Map<Company, String> companyError) {

		List<String> out = new ArrayList<String>();

		if (companyResultDataMap != null) {
			String header = StringPool.BLANK;
			header = IndexCheckerUtil.addCell(header, "Company");

			if (executionMode.contains(ExecutionMode.GROUP_BY_SITE)) {
				header = IndexCheckerUtil.addCell(header, "GroupId");
				header = IndexCheckerUtil.addCell(header, "Group name");
			}

			header = IndexCheckerUtil.addCell(header, "Entity class");
			header = IndexCheckerUtil.addCell(header, "Entity name");
			header = IndexCheckerUtil.addCell(header, "Type");
			header = IndexCheckerUtil.addCell(header, "Count");
			header = IndexCheckerUtil.addCell(header, "Primary keys");

			out.add(header);
		}

		for (
			Map.Entry<Company, Long> companyEntry :
				companyProcessTime.entrySet()) {

			Long processTime = companyEntry.getValue();

			String companyOutput =
				companyEntry.getKey().getCompanyId() + " - " +
				companyEntry.getKey().getWebId();

			if (companyResultDataMap != null) {
				Map<Long, List<IndexCheckerResult>> resultDataMap =
					companyResultDataMap.get(companyEntry.getKey());

				for (
					Map.Entry<Long, List<IndexCheckerResult>> entry :
						resultDataMap.entrySet()) {

					String groupIdOutput = null;
					String groupNameOutput = null;

					if (executionMode.contains(ExecutionMode.GROUP_BY_SITE)) {
						try {
							Group group =
								GroupLocalServiceUtil.fetchGroup(
									entry.getKey());

							if (group == null) {
								groupIdOutput = LanguageUtil.get(
									locale, "output.not-applicable-groupid");
								groupNameOutput = LanguageUtil.get(
									locale, "output.not-applicable-groupname");
							}
							else {
								groupIdOutput = "" + group.getGroupId();
								groupNameOutput = group.getName();
							}
						}
						catch (Exception e) {
							groupIdOutput = "" + entry.getKey();
						}
					}

					for (IndexCheckerResult result : entry.getValue()) {
						for (String type : outputTypes) {
							String line = generateLine(
									portletConfig, result, companyOutput,
									groupIdOutput, groupNameOutput, type,
									locale);

							if (line != null) {
								out.add(line);
							}
						}
					}
				}
			}

			String errorMessage = companyError.get(companyEntry.getKey());

			if (Validator.isNotNull(errorMessage)) {
				out.add(
					"Company: " + companyEntry.getKey().getCompanyId() +
					" - " + companyEntry.getKey().getWebId());
				out.add(errorMessage);
			}

			out.add(
				"\nExecuted " + title + " for company " +
				companyEntry.getKey().getCompanyId() + " in " + processTime +
				" ms");
		}

		return out;
	}

	public static SearchContainer<IndexCheckerResult> generateSearchContainer(
		PortletConfig portletConfig, RenderRequest renderRequest,
		EnumSet<ExecutionMode> executionMode,
		Map<Long, List<IndexCheckerResult>> resultDataMap,
		PortletURL serverURL) throws SystemException {

		List<String> headerNames = new ArrayList<String>();

		if (executionMode.contains(ExecutionMode.GROUP_BY_SITE)) {
			headerNames.add("GroupId");
			headerNames.add("Group name");
		}

		headerNames.add("Entity class");
		headerNames.add("Entity name");
		headerNames.add("Type");
		headerNames.add("Count");
		headerNames.add("Primary keys");

		SearchContainer<IndexCheckerResult> searchContainer =
			new SearchContainer<IndexCheckerResult>(
				renderRequest, null, null, SearchContainer.DEFAULT_CUR_PARAM,
				SearchContainer.MAX_DELTA, serverURL, headerNames, null);

		for (
			Entry<Long, List<IndexCheckerResult>> entry :
				resultDataMap.entrySet()) {

			String groupIdOutput = null;
			String groupNameOutput = null;

			if (executionMode.contains(ExecutionMode.GROUP_BY_SITE)) {
				Group group = GroupLocalServiceUtil.fetchGroup(entry.getKey());

				if (group == null) {
					groupIdOutput = LanguageUtil.get(
						portletConfig, renderRequest.getLocale(),
						"output.not-applicable-groupid");
					groupNameOutput = LanguageUtil.get(
						portletConfig, renderRequest.getLocale(),
						"output.not-applicable-groupname");
				}
				else {
					groupIdOutput = "" + group.getGroupId();
					groupNameOutput = group.getName();
				}
			}

			List<IndexCheckerResult> results = entry.getValue();

			searchContainer.setTotal(results.size());

			results = ListUtil.subList(
				results, searchContainer.getStart(), searchContainer.getEnd());

			searchContainer.setResults(results);

			List<ResultRow> resultRows = searchContainer.getResultRows();

			int j = 0;

			for (IndexCheckerResult result : results) {
				for (String type : outputTypes) {
					ResultRow row = generateRow(
						portletConfig, result, groupIdOutput, groupNameOutput,
						type, renderRequest.getLocale(), j);

					if (row != null) {
						j++;
						resultRows.add(row);
					}
				}
			}
		}

		return searchContainer;
	}

	public static Long[] getListPK(Collection<Data> data) {
		Long[] valuesPK = new Long[data.size()];

		int i = 0;

		for (Data value : data) {
			valuesPK[i++] = value.getPrimaryKey();
		}

		return valuesPK;
	}

	public static String[] getListUid(Collection<Data> data) {
		String[] valuesPK = new String[data.size()];

		int i = 0;

		for (Data value : data) {
			valuesPK[i++] = value.getUid();
		}

		return valuesPK;
	}

	static Log _log = LogFactoryUtil.getLog(IndexCheckerUtil.class);

	public static String getValuesPKText(String type, Set<Data> data) {
		String valuesPK = null;

		if (type.contains("index")) {
			valuesPK = Arrays.toString(IndexCheckerUtil.getListUid(data));
		}
		else {
			valuesPK = Arrays.toString(IndexCheckerUtil.getListPK(data));
		}

		if (valuesPK.length() <= 1) {
			valuesPK = StringPool.BLANK;
		}
		else {
			valuesPK = valuesPK.substring(1, valuesPK.length()-1);
		}

		return valuesPK;
	}

	public static String listStringToString(List<String> out) {
		if (Validator.isNull(out)) {
			return null;
		}

		StringBundler stringBundler = new StringBundler(out.size()*2);

		for (String s : out) {
			stringBundler.append(s);
			stringBundler.append(StringPool.NEW_LINE);
		}

		return stringBundler.toString();
	}

	protected static String addCell(String line, String cell) {
		if (cell.contains(StringPool.SPACE) ||
			cell.contains(StringPool.COMMA)) {

			cell = StringPool.QUOTE + cell + StringPool.QUOTE;
		}

		if (Validator.isNull(line)) {
			line = cell;
		}
		else {
			line += StringPool.COMMA + cell;
		}

		return line;
	}

	protected static String generateLine(
		PortletConfig portletConfig, IndexCheckerResult result,
		String companyOutput, String groupIdOutput, String groupNameOutput,
		String type, Locale locale) {

		Set<Data> data = result.getData(type);

		if ((data == null) || data.isEmpty()) {
			return null;
		}

		IndexCheckerModel model = result.getModel();

		String modelOutput = model.getName();
		String modelDisplayNameOutput = model.getDisplayName(locale);

		String valuesPK = getValuesPKText(type, data);

		String line = StringPool.BLANK;
		line = IndexCheckerUtil.addCell(line, companyOutput);

		if (groupIdOutput != null) {
			line = IndexCheckerUtil.addCell(line, groupIdOutput);
			line = IndexCheckerUtil.addCell(line, groupNameOutput);
		}

		line = IndexCheckerUtil.addCell(line, modelOutput);
		line = IndexCheckerUtil.addCell(line, modelDisplayNameOutput);
		line = IndexCheckerUtil.addCell(
			line, LanguageUtil.get(portletConfig, locale, "output." + type));
		line = IndexCheckerUtil.addCell(line, "" + data.size());
		line = IndexCheckerUtil.addCell(line, valuesPK);
		return line;
	}

	protected static ResultRow generateRow(
		PortletConfig portletConfig, IndexCheckerResult result,
		String groupIdOutput, String groupNameOutput, String type,
		Locale locale, int j) {

		Set<Data> data = result.getData(type);

		if ((data == null) || data.isEmpty()) {
			return null;
		}

		String valuesPK = getValuesPKText(type, data);

		ResultRow row = new ResultRow(result, type, j);
		IndexCheckerModel model = result.getModel();

		String modelOutput = model.getName();
		String modelDisplayNameOutput = model.getDisplayName(locale);

		if ((groupIdOutput != null) && (groupNameOutput!= null)) {
			row.addText(groupIdOutput);
			row.addText(groupNameOutput);
		}

		row.addText(HtmlUtil.escape(modelOutput));
		row.addText(HtmlUtil.escape(modelDisplayNameOutput));
		row.addText(
			HtmlUtil.escape(
				LanguageUtil.get(portletConfig, locale, "output." + type)));
		row.addText(HtmlUtil.escape(""+data.size()));
		row.addText(HtmlUtil.escape(valuesPK));
		return row;
	}

	private static String[] outputTypes = {
		"both-exact", "both-notexact", "only-liferay", "only-index"};

}