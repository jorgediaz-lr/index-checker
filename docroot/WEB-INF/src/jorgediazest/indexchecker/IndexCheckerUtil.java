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

import jorgediazest.indexchecker.data.Data;
import jorgediazest.indexchecker.model.IndexCheckerModel;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
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
import java.util.Set;

/**
 * @author Jorge Díaz
 */
public class IndexCheckerUtil {

	public static List<String> generateOutputCSV(
		String title, Locale locale, EnumSet<ExecutionMode> executionMode,
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

				for (Map.Entry<Long, List<IndexCheckerResult>> entry :
					resultDataMap.entrySet()) {

					String groupIdOutput = null;
					String groupNameOutput = null;

					if (executionMode.contains(ExecutionMode.GROUP_BY_SITE)) {
						try {
							Group group =
								GroupLocalServiceUtil.fetchGroup(
									entry.getKey());

							if (group == null) {
								groupIdOutput = "N/A";
								groupNameOutput = "(Not Applicable)";
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
						Set<Data> exactDataSetIndex =
							result.getIndexExactData();
						Set<Data> exactDataSetLiferay =
							result.getLiferayExactData();
						Set<Data> notExactDataSetIndex =
							result.getIndexNotExactData();
						Set<Data> liferayOnlyData = result.getLiferayOnlyData();
						Set<Data> indexOnlyData = result.getIndexOnlyData();

						if (((exactDataSetIndex == null) ||
							 exactDataSetIndex.isEmpty()) &&
							((notExactDataSetIndex == null) ||
							 notExactDataSetIndex.isEmpty()) &&
							((liferayOnlyData == null) ||
							 liferayOnlyData.isEmpty()) &&
							((indexOnlyData == null) ||
							 indexOnlyData.isEmpty())) {

							continue;
						}

						IndexCheckerModel model = result.getModel();
						String modelOutput = model.getName();
						String modelDisplayNameOutput = model.getDisplayName(
							locale);

						if ((exactDataSetIndex != null) &&
							!exactDataSetIndex.isEmpty()) {

							String valuesPK = Arrays.toString(
								IndexCheckerUtil.getListPK(
									exactDataSetLiferay));

							String line =
								generateLine(
									companyOutput, groupIdOutput,
									groupNameOutput, modelOutput,
									modelDisplayNameOutput, "both-exact",
									exactDataSetLiferay.size(), valuesPK);

							out.add(line);
						}

						if ((notExactDataSetIndex != null) &&
							!notExactDataSetIndex.isEmpty()) {

							String valuesPK = Arrays.toString(
								IndexCheckerUtil.getListPK(
									notExactDataSetIndex));

							String line =
								generateLine(
									companyOutput, groupIdOutput,
									groupNameOutput, modelOutput,
									modelDisplayNameOutput, "both-notexact",
									notExactDataSetIndex.size(), valuesPK);

							out.add(line);
						}

						if ((liferayOnlyData != null) &&
							!liferayOnlyData.isEmpty()) {

							String valuesPK = Arrays.toString(
								IndexCheckerUtil.getListPK(liferayOnlyData));

							String line =
								generateLine(
									companyOutput, groupIdOutput,
									groupNameOutput, modelOutput,
									modelDisplayNameOutput, "only liferay",
									liferayOnlyData.size(), valuesPK);

							out.add(line);
						}

						if ((indexOnlyData != null) &&
							!indexOnlyData.isEmpty()) {

							String valuesPK = Arrays.toString(
								IndexCheckerUtil.getListPK(indexOnlyData));

							String line =
								generateLine(
									companyOutput, groupIdOutput,
									groupNameOutput, modelOutput,
									modelDisplayNameOutput, "only liferay",
									indexOnlyData.size(), valuesPK);

							out.add(line);
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

	public static Long[] getListPK(Collection<Data> data) {
		Long[] valuesPK = new Long[data.size()];

		int i = 0;

		for (Data value : data) {
			valuesPK[i++] = value.getPrimaryKey();
		}

		return valuesPK;
	}

	static Log _log = LogFactoryUtil.getLog(IndexCheckerUtil.class);

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
		String companyOutput, String groupIdOutput, String groupNameOutput,
		String modelOutput, String modelDisplayNameOutput, String type,
		long size, String valuesPK) {

		String line = StringPool.BLANK;
		line = IndexCheckerUtil.addCell(line, companyOutput);

		if (groupIdOutput != null) {
			line = IndexCheckerUtil.addCell(line, groupIdOutput);
			line = IndexCheckerUtil.addCell(line, groupNameOutput);
		}

		line = IndexCheckerUtil.addCell(line, modelOutput);
		line = IndexCheckerUtil.addCell(line, modelDisplayNameOutput);
		line = IndexCheckerUtil.addCell(line, type);
		line = IndexCheckerUtil.addCell(line, "" + size);
		line = IndexCheckerUtil.addCell(line, valuesPK);
		return line;
	}

}