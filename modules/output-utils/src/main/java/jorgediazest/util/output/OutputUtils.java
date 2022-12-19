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

package jorgediazest.util.output;

import com.liferay.document.library.kernel.exception.NoSuchFileEntryException;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.dao.search.ResultRow;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.Repository;
import com.liferay.portal.kernel.portlet.PortletResponseUtil;
import com.liferay.portal.kernel.portletfilerepository.PortletFileRepositoryUtil;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.service.CompanyLocalServiceUtil;
import com.liferay.portal.kernel.service.GroupServiceUtil;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.util.HtmlUtil;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;

import java.io.IOException;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;

import javax.portlet.PortletConfig;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;

import javax.servlet.http.HttpServletResponse;

import jorgediazest.util.data.Comparison;
import jorgediazest.util.data.Data;

/**
 * @author Jorge Díaz
 */
public class OutputUtils {

	public static FileEntry addPortletFileEntry(
			Repository repository, byte[] bytes, long userId, String title,
			String mimeType)
		throws PortalException {

		return PortletFileRepositoryUtil.addPortletFileEntry(
			repository.getGroupId(), userId, StringPool.BLANK, 0,
			repository.getPortletId(), repository.getDlFolderId(), bytes, title,
			mimeType, true);
	}

	public static FileEntry addPortletOutputFileEntry(
		String portletId, long userId, String outputContent) {

		if (Validator.isNull(outputContent)) {
			return null;
		}

		try {
			Repository repository = getPortletRepository(portletId);

			cleanupPortletFileEntries(repository, 8 * 60);

			String fileName =
				portletId + "_output_" + userId + "_" +
					System.currentTimeMillis() + ".csv";

			return addPortletFileEntry(
				repository, outputContent.getBytes(StringPool.UTF8), userId,
				fileName, "text/plain");
		}
		catch (Exception e) {
			_log.error(e, e);

			return null;
		}
	}

	public static void cleanupPortletFileEntries(
			Repository repository, long minutes)
		throws PortalException {

		if (repository == null) {
			return;
		}

		List<FileEntry> fileEntries =
			PortletFileRepositoryUtil.getPortletFileEntries(
				repository.getGroupId(), repository.getDlFolderId());

		for (FileEntry fileEntry : fileEntries) {
			Date createDate = fileEntry.getCreateDate();

			long fileEntryDate = createDate.getTime();

			long delta = minutes * 60 * 1000;

			if ((fileEntryDate + delta) < System.currentTimeMillis()) {
				PortletFileRepositoryUtil.deletePortletFileEntry(
					fileEntry.getFileEntryId());
			}
		}
	}

	public static String generateCSVRow(
		ResourceBundle resourceBundle, Comparison comp, String companyOutput,
		String groupIdOutput, String groupNameOutput, String type,
		List<String> attributeList, Locale locale) {

		Set<Data> data = comp.getData(type);

		if ((data == null) || data.isEmpty()) {
			return null;
		}

		String[] output = DataUtil.getListAttr(data, attributeList);

		String outputString = stringArrayToString(output);

		return generateCSVRow(
			resourceBundle, comp, companyOutput, groupIdOutput, groupNameOutput,
			type, locale, outputString, data.size());
	}

	public static String generateCSVRow(
		ResourceBundle resourceBundle, Comparison comp, String companyOutput,
		String groupIdOutput, String groupNameOutput, String type,
		Locale locale, String output, int outputSize) {

		if (Validator.isNull(output)) {
			return null;
		}

		List<String> line = new ArrayList<>();

		line.add(companyOutput);

		if (groupIdOutput != null) {
			line.add(groupIdOutput);
			line.add(groupNameOutput);
		}

		line.add(comp.getModelName());
		line.add(comp.getModelDisplayName(locale));
		line.add(LanguageUtil.get(resourceBundle, "output." + type));

		if (outputSize < 0) {
			line.add(StringPool.BLANK);
		}
		else {
			line.add(StringPool.BLANK + outputSize);
		}

		line.add(output);

		return getCSVRow(line);
	}

	public static String generateCSVRow(
		ResourceBundle resourceBundle, Comparison comp, String companyOutput,
		String groupIdOutput, String groupNameOutput, String type,
		String attribute, Locale locale) {

		return generateCSVRow(
			resourceBundle, comp, companyOutput, groupIdOutput, groupNameOutput,
			type, Collections.singletonList(attribute), locale);
	}

	public static ResultRow generateSearchContainerRow(
		ResourceBundle resourceBundle, Comparison comp, String groupIdOutput,
		String groupNameOutput, String type, List<String> attributeList,
		Locale locale, int numberOfRows, int maxSize) {

		Set<Data> data = comp.getData(type);

		if ((data == null) || data.isEmpty()) {
			return null;
		}

		String[] output = DataUtil.getListAttr(data, attributeList);

		String outputString = stringArrayToString(output);

		outputString = HtmlUtil.escape(outputString);

		int overflow = data.size() - maxSize;

		if (overflow > 0) {
			String[] outputTrimmed = DataUtil.getListAttr(
				data, attributeList, maxSize);

			String outputStringTrimmed = stringArrayToString(outputTrimmed);

			outputStringTrimmed = HtmlUtil.escape(outputStringTrimmed);

			String tagId = StringUtil.randomString() + "_" + numberOfRows;

			String onClick =
				"onclick=\"showHide('" + tagId + "');return false;\"";

			String linkStartTag = "<a href=\"#\"" + onClick + " >";

			String linkMoreEndTag;

			if (maxSize == 0) {
				linkMoreEndTag = "click to display</a>";
			}
			else {
				outputStringTrimmed = outputStringTrimmed + "... ";

				linkMoreEndTag = "(" + overflow + " more)</a>";
			}

			String linkMore = linkStartTag + linkMoreEndTag;

			String linkCollapse = linkStartTag + "(collapse)</a>";

			outputString =
				"<span id=\"" + tagId + "-show\" >" + outputStringTrimmed +
					linkMore + "</span><span id=\"" + tagId +
						"\" style=\"display: none;\" >" + outputString + " " +
							linkCollapse + "</span>";
		}

		return generateSearchContainerRow(
			resourceBundle, comp, groupIdOutput, groupNameOutput, type, locale,
			numberOfRows, outputString, data.size());
	}

	public static ResultRow generateSearchContainerRow(
		ResourceBundle resourceBundle, Comparison comp, String groupIdOutput,
		String groupNameOutput, String type, Locale locale, int numberOfRows,
		String errorOutput) {

		return generateSearchContainerRow(
			resourceBundle, comp, groupIdOutput, groupNameOutput, type, locale,
			numberOfRows, HtmlUtil.escape(errorOutput), -1);
	}

	public static ResultRow generateSearchContainerRow(
		ResourceBundle resourceBundle, Comparison comp, String groupIdOutput,
		String groupNameOutput, String type, Locale locale, int numberOfRows,
		String htmlOutput, int outputSize) {

		if (Validator.isNull(htmlOutput)) {
			return null;
		}

		ResultRow row = new com.liferay.taglib.search.ResultRow(
			comp, type, numberOfRows);

		if ((groupIdOutput != null) && (groupNameOutput != null)) {
			row.addText(groupIdOutput);
			row.addText(groupNameOutput);
		}

		String output = HtmlUtil.escape(
			LanguageUtil.get(resourceBundle, "output." + type));

		row.addText(HtmlUtil.escape(comp.getModelName()));
		row.addText(HtmlUtil.escape(comp.getModelDisplayName(locale)));
		row.addText(output.replace(" ", "&nbsp;"));

		if (outputSize < 0) {
			row.addText(StringPool.BLANK);
		}
		else {
			row.addText(StringPool.BLANK + outputSize);
		}

		row.addText(htmlOutput);

		return row;
	}

	public static ResultRow generateSearchContainerRow(
		ResourceBundle resourceBundle, Comparison comp, String groupIdOutput,
		String groupNameOutput, String type, String attribute, Locale locale,
		int numberOfRows, int maxSize) {

		return generateSearchContainerRow(
			resourceBundle, comp, groupIdOutput, groupNameOutput, type,
			Collections.singletonList(attribute), locale, numberOfRows,
			maxSize);
	}

	public static String getCSVRow(List<String> rowData) {
		return getCSVRow(rowData, StringPool.COMMA);
	}

	public static String getCSVRow(List<String> rowData, String sep) {
		String row = StringPool.BLANK;

		for (String aux : rowData) {
			row = addCell(row, aux, sep);
		}

		return row;
	}

	public static List<String> getHeaders(
		PortletConfig portletConfig, Locale locale, String[] headerKeys) {

		List<String> headers = new ArrayList<>();

		for (String headerKey : headerKeys) {
			ResourceBundle resourceBundle = portletConfig.getResourceBundle(
				locale);

			headers.add(LanguageUtil.get(resourceBundle, headerKey));
		}

		return headers;
	}

	public static FileEntry getPortletFileEntry(
			Repository repository, String title)
		throws PortalException {

		return PortletFileRepositoryUtil.getPortletFileEntry(
			repository.getGroupId(), repository.getDlFolderId(), title);
	}

	public static Repository getPortletRepository(String portletId)
		throws PortalException {

		List<Company> companies = CompanyLocalServiceUtil.getCompanies();

		Company company = companies.get(0);

		Group group = GroupServiceUtil.getCompanyGroup(company.getCompanyId());

		Repository repository =
			PortletFileRepositoryUtil.fetchPortletRepository(
				group.getGroupId(), portletId);

		if (repository == null) {
			repository = PortletFileRepositoryUtil.addPortletRepository(
				group.getGroupId(), portletId, new ServiceContext());
		}

		return repository;
	}

	public static String listStringToString(List<String> out) {
		if (out == null) {
			return null;
		}

		StringBundler sb = new StringBundler(out.size() * 2);

		for (String s : out) {
			sb.append(s);
			sb.append(StringPool.NEW_LINE);
		}

		return sb.toString();
	}

	public static void servePortletFileEntry(
			String portletId, String title, ResourceRequest request,
			ResourceResponse response)
		throws IOException {

		try {
			Repository repository = getPortletRepository(portletId);

			FileEntry fileEntry = getPortletFileEntry(repository, title);

			InputStream inputStream = fileEntry.getContentStream();

			PortletResponseUtil.sendFile(
				request, response, title, inputStream, -1,
				fileEntry.getMimeType(), "attachment");
		}
		catch (NoSuchFileEntryException nsfe) {
			if (_log.isWarnEnabled()) {
				_log.warn(nsfe.getMessage());
			}

			response.setProperty(
				ResourceResponse.HTTP_STATUS_CODE,
				String.valueOf(HttpServletResponse.SC_NOT_FOUND));
		}
		catch (Exception e) {
			_log.error(e, e);

			response.setProperty(
				ResourceResponse.HTTP_STATUS_CODE,
				String.valueOf(HttpServletResponse.SC_INTERNAL_SERVER_ERROR));
		}
	}

	public static String stringArrayToString(String[] stringArray) {
		String string = Arrays.toString(stringArray);

		if (string.length() <= 1) {
			return StringPool.BLANK;
		}

		return string.substring(1, string.length() - 1);
	}

	protected static String addCell(String line, String cell, String sep) {
		if (cell.contains(StringPool.SPACE) || cell.contains(sep)) {
			cell = StringPool.QUOTE + cell + StringPool.QUOTE;
		}

		if (Validator.isNull(line)) {
			line = cell;
		}
		else {
			line += sep + cell;
		}

		return line;
	}

	private static Log _log = LogFactoryUtil.getLog(OutputUtils.class);

}