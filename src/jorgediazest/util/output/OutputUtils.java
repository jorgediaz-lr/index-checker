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

/**
 * @author Jorge Díaz
 */
import com.liferay.portal.kernel.dao.search.ResultRow;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.servlet.HttpHeaders;
import com.liferay.portal.kernel.util.HtmlUtil;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.Repository;
import com.liferay.portal.portletfilerepository.PortletFileRepositoryUtil;
import com.liferay.portal.service.CompanyLocalServiceUtil;
import com.liferay.portal.service.GroupServiceUtil;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portlet.documentlibrary.NoSuchFileEntryException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.portlet.PortletConfig;
import javax.portlet.ResourceResponse;

import javax.servlet.http.HttpServletResponse;

import jorgediazest.util.data.Comparison;
import jorgediazest.util.data.Data;
public class OutputUtils {

	public static FileEntry addPortletFileEntry(
			Repository repository, InputStream inputStream, long userId,
			String title, String mimeType)
		throws PortalException, SystemException {

		if (Validator.isNull(inputStream)) {
			return null;
		}

		return PortletFileRepositoryUtil.addPortletFileEntry(
			repository.getGroupId(), userId, StringPool.BLANK, 0,
			repository.getPortletId(), repository.getDlFolderId(), inputStream,
			title, mimeType, true);
	}

	public static FileEntry addPortletOutputFileEntry(
		String portletId, long userId, String outputContent) {

		if (Validator.isNull(outputContent)) {
			return null;
		}

		try {
			InputStream inputStream = new ByteArrayInputStream(
				outputContent.getBytes(StringPool.UTF8));

			Repository repository = OutputUtils.getPortletRepository(portletId);

			OutputUtils.cleanupPortletFileEntries(repository, 8 * 60);

			String fileName =
				portletId + "_output_" + userId + "_" +
				System.currentTimeMillis() + ".csv";

			return OutputUtils.addPortletFileEntry(
				repository, inputStream, userId, fileName, "text/plain");
		}
		catch (Exception e) {
			_log.error(e, e);

			return null;
		}
	}

	public static void cleanupPortletFileEntries(
			Repository repository, long minutes)
		throws PortalException, SystemException {

		if (repository == null) {
			return;
		}

		List<FileEntry> fileEntries =
			PortletFileRepositoryUtil.getPortletFileEntries(
				repository.getGroupId(), repository.getDlFolderId());

		for (FileEntry fileEntry : fileEntries) {
			long fileEntryDate = fileEntry.getCreateDate().getTime();
			long delta = minutes * 60 *1000;

			if ((fileEntryDate + delta) < System.currentTimeMillis()) {
				PortletFileRepositoryUtil.deletePortletFileEntry(
					fileEntry.getFileEntryId());
			}
		}
	}

	public static String generateCSVRow(
		PortletConfig portletConfig, Comparison comp, String companyOutput,
		String groupIdOutput, String groupNameOutput, String type,
		List<String> attributeList, Locale locale) {

		Set<Data> data = comp.getData(type);

		if ((data == null) || data.isEmpty()) {
			return null;
		}

		String[] output = DataUtil.getListAttr(data, attributeList);

		String outputString = OutputUtils.stringArrayToString(output);

		return OutputUtils.generateCSVRow(
			portletConfig, comp, companyOutput, groupIdOutput, groupNameOutput,
			type, locale, outputString, data.size());
	}

	public static String generateCSVRow(
		PortletConfig portletConfig, Comparison comp, String companyOutput,
		String groupIdOutput, String groupNameOutput, String type,
		Locale locale, String output, int outputSize) {

		if (Validator.isNull(output)) {
			return null;
		}

		List<String> line = new ArrayList<String>();
		line.add(companyOutput);

		if (groupIdOutput != null) {
			line.add(groupIdOutput);
			line.add(groupNameOutput);
		}

		line.add(comp.getModelName());
		line.add(comp.getModelDisplayName(locale));
		line.add(LanguageUtil.get(portletConfig, locale, "output." + type));

		if (outputSize < 0) {
			line.add(StringPool.BLANK);
		}
		else {
			line.add(StringPool.BLANK + outputSize);
		}

		line.add(output);
		return OutputUtils.getCSVRow(line);
	}

	public static String generateCSVRow(
		PortletConfig portletConfig, Comparison comp, String companyOutput,
		String groupIdOutput, String groupNameOutput, String type,
		String attribute, Locale locale) {

		return generateCSVRow(
			portletConfig, comp, companyOutput, groupIdOutput, groupNameOutput,
			type, Collections.singletonList(attribute), locale);
	}

	public static ResultRow generateSearchContainerRow(
		PortletConfig portletConfig, Comparison comp, String groupIdOutput,
		String groupNameOutput, String type, List<String> attributeList,
		Locale locale, int numberOfRows, int maxSize) {

		Set<Data> data = comp.getData(type);

		if ((data == null) || data.isEmpty()) {
			return null;
		}

		String[] output = DataUtil.getListAttr(data, attributeList);

		String outputString = OutputUtils.stringArrayToString(output);

		outputString = HtmlUtil.escape(outputString);

		String outputStringTrimmed = null;

		int overflow = data.size() - maxSize;

		if (overflow > 0) {
			String[] outputTrimmed = DataUtil.getListAttr(
				data, attributeList, maxSize);

			outputStringTrimmed = OutputUtils.stringArrayToString(
				outputTrimmed);

			outputStringTrimmed = HtmlUtil.escape(outputStringTrimmed);

			String tagId = StringUtil.randomString() + "_" + numberOfRows;
			String onClick =
				"onclick=\"showHide('" + tagId + "');return false;\"";
			String linkMore =
				"<a href=\"#\"" + onClick + " >(" + overflow + " more)</a>";
			String linkCollapse =
				"<a href=\"#\"" + onClick + " >(collapse)</a>";

			outputString =
				"<span id=\"" + tagId + "-show\" >" + outputStringTrimmed +
				"... " + linkMore + "</span><span id=\"" + tagId +
				"\" style=\"display: none;\" >" + outputString + " " +
				linkCollapse + "</span>";
		}

		return OutputUtils.generateSearchContainerRow(
			portletConfig, comp, groupIdOutput, groupNameOutput, type, locale,
			numberOfRows, outputString, data.size());
	}

	public static ResultRow generateSearchContainerRow(
		PortletConfig portletConfig, Comparison comp, String groupIdOutput,
		String groupNameOutput, String type, Locale locale, int numberOfRows,
		String errorOutput) {

		return generateSearchContainerRow(
			portletConfig, comp, groupIdOutput, groupNameOutput, type, locale,
			numberOfRows, HtmlUtil.escape(errorOutput), -1);
	}

	public static ResultRow generateSearchContainerRow(
		PortletConfig portletConfig, Comparison comp, String groupIdOutput,
		String groupNameOutput, String type, Locale locale, int numberOfRows,
		String htmlOutput, int outputSize) {

		if (Validator.isNull(htmlOutput)) {
			return null;
		}

		ResultRow row = new ResultRow(comp, type, numberOfRows);

		if ((groupIdOutput != null) && (groupNameOutput!= null)) {
			row.addText(groupIdOutput);
			row.addText(groupNameOutput);
		}

		row.addText(HtmlUtil.escape(comp.getModelName()));
		row.addText(HtmlUtil.escape(comp.getModelDisplayName(locale)));
		row.addText(
			HtmlUtil.escape(
				LanguageUtil.get(
					portletConfig, locale, "output." + type)).replace(
						" ", "&nbsp;"));

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
		PortletConfig portletConfig, Comparison comp, String groupIdOutput,
		String groupNameOutput, String type, String attribute, Locale locale,
		int numberOfRows, int maxSize) {

		return generateSearchContainerRow(
			portletConfig, comp, groupIdOutput, groupNameOutput, type,
			Collections.singletonList(attribute), locale, numberOfRows,
			maxSize);
	}

	public static String getCSVRow(List<String> rowData) {
		return getCSVRow(rowData, StringPool.COMMA);
	}

	public static String getCSVRow(List<String> rowData, String sep) {

		String row = StringPool.BLANK;

		for (String aux : rowData) {
			row = OutputUtils.addCell(row, aux, sep);
		}

		return row;
	}

	public static List<String> getHeaders(
		PortletConfig portletConfig, Locale locale, String[] headerKeys) {

		List<String> headers = new ArrayList<String>();

		for (int i = 0; i<headerKeys.length; i++) {
			ResourceBundle resourceBundle = portletConfig.getResourceBundle(
				locale);
			headers.add(LanguageUtil.get(resourceBundle, headerKeys[i]));
		}

		return headers;
	}

	public static FileEntry getPortletFileEntry(
		Repository repository, String title)
			throws PortalException, SystemException {

		return PortletFileRepositoryUtil.getPortletFileEntry(
			repository.getGroupId(), repository.getDlFolderId(), title);
	}

	public static Repository getPortletRepository(String portletId)
		throws PortalException, SystemException {

		List<Company> companies = CompanyLocalServiceUtil.getCompanies(false);

		long companyId = companies.get(0).getCompanyId();
		long groupId = GroupServiceUtil.getCompanyGroup(companyId).getGroupId();

		Repository repository =
			PortletFileRepositoryUtil.fetchPortletRepository(
				groupId, portletId);

		if (repository == null) {
			repository = PortletFileRepositoryUtil.addPortletRepository(
				groupId, portletId, new ServiceContext());
		}

		return repository;
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

	public static void servePortletFileEntry(
			String portletId, String title, ResourceResponse response)
		throws IOException {

		try {
			Repository repository = OutputUtils.getPortletRepository(portletId);

			FileEntry fileEntry = OutputUtils.getPortletFileEntry(
				repository, title);

			InputStream inputStream = fileEntry.getContentStream();
			OutputStream outputStream = response.getPortletOutputStream();

			byte[] buffer = new byte[10024];
			int bytesRead = 0;
			while ((bytesRead = inputStream.read(buffer)) != -1) {
				outputStream.write(buffer, 0, bytesRead);
			}

			response.setContentType(fileEntry.getMimeType());

			response.addProperty(
				HttpHeaders.CONTENT_DISPOSITION, "attachment; filename="+title);
		}
		catch (NoSuchFileEntryException nsfe) {
			if (_log.isWarnEnabled()) {
				_log.warn(nsfe.getMessage());
			}

			response.setProperty(
				ResourceResponse.HTTP_STATUS_CODE,
				Integer.toString(HttpServletResponse.SC_NOT_FOUND));
		}
		catch (Exception e) {
			_log.error(e, e);

			response.setProperty(
				ResourceResponse.HTTP_STATUS_CODE,
				Integer.toString(HttpServletResponse.SC_INTERNAL_SERVER_ERROR));
		}
	}

	public static String stringArrayToString(String[] stringArray) {
		String string = Arrays.toString(stringArray);

		if (string.length() <= 1) {
			return StringPool.BLANK;
		}

		return string.substring(1, string.length()-1);
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