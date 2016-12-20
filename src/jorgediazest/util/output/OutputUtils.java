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
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.servlet.HttpHeaders;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.Repository;
import com.liferay.portal.portletfilerepository.PortletFileRepositoryUtil;
import com.liferay.portal.service.CompanyLocalServiceUtil;
import com.liferay.portal.service.GroupServiceUtil;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portlet.documentlibrary.NoSuchFileEntryException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.portlet.PortletConfig;
import javax.portlet.ResourceResponse;

import javax.servlet.http.HttpServletResponse;
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
			headers.add(LanguageUtil.get(portletConfig, locale, headerKeys[i]));
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