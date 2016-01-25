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
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.portlet.PortletConfig;
public class OutputUtils {

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

}