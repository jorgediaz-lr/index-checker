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

package jorgediazest.util.query;

import com.liferay.portal.kernel.dao.jdbc.DataAccess;
import com.liferay.portal.kernel.dao.orm.Criterion;
import com.liferay.portal.kernel.dao.orm.ProjectionList;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.util.PortalUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jorgediazest.util.data.Data;
import jorgediazest.util.data.DataUtil;
import jorgediazest.util.model.Model;
import jorgediazest.util.table.TableInfo;

/**
 * @author Jorge Díaz
 */
public class Query {

	public static Map<Long, Data> getData(
		Model model, String[] attributes, Criterion filter)
	throws Exception {
		return getData(
			model, attributes, model.getPrimaryKeyAttribute(), filter);
	}

	public static Map<Long, Data> getData(
			Model model, String[] attributes, String mapKeyAttribute,
			Criterion filter)
		throws Exception {

		Map<Long, Data> dataMap = new HashMap<Long, Data>();
		Map<Long, Data> dataMapByPK = new HashMap<Long, Data>();

		if (mapKeyAttribute.equals(model.getPrimaryKeyAttribute())) {
			dataMapByPK = null;
		}

		List<String> validAttributes = new ArrayList<String>();
		List<String> notValidAttributes = new ArrayList<String>();

		ProjectionList projectionList = model.getPropertyProjection(
			attributes, validAttributes, notValidAttributes);

		if ((projectionList == null) && (notValidAttributes.size() > 0)) {
			return getDataFromMappingTables(
				model, notValidAttributes, mapKeyAttribute);
		}

		@SuppressWarnings("unchecked")
		List<Object[]> results = (List<Object[]>)model.executeDynamicQuery(
			filter, projectionList);

		String[] validAttributesArr = validAttributes.toArray(
			new String[validAttributes.size()]);

		long i = -1;

		for (Object[] result : results) {
			Data data = DataUtil.createDataObject(
				model, validAttributesArr, result);

			addDataToMap(dataMap, mapKeyAttribute, data, i--);

			addDataToMap(
				dataMapByPK, model.getPrimaryKeyAttribute(), data, null);
		}

		if (dataMapByPK == null) {
			dataMapByPK = dataMap;
		}

		if (dataMap.isEmpty()) {
			return Collections.emptyMap();
		}

		for (String notValidAttribute : notValidAttributes) {
			Map<Long, List<Data>> relatedDataMap =
				getRelatedDataFromMappingTable(model, notValidAttribute);

			if (relatedDataMap == null) {
				continue;
			}

			QueryUtil.addRelatedModelData(
				dataMapByPK, relatedDataMap, new String[]{notValidAttribute});
		}

		return dataMap;
	}

	public static Map<Long, List<Data>> getDataWithDuplicates(
		Model model, String[] attributes, String mapKeyAttribute,
		Criterion filter)
	throws Exception {

		Map<Long, List<Data>> dataMap = new HashMap<Long, List<Data>>();
		Map<Long, List<Data>> dataMapByPK = new HashMap<Long, List<Data>>();

		if (mapKeyAttribute.equals(model.getPrimaryKeyAttribute())) {
			dataMapByPK = null;
		}

		if (attributes == null) {
			attributes = model.getAttributesName();
		}

		List<String> validAttributes = new ArrayList<String>();
		List<String> notValidAttributes = new ArrayList<String>();
		ProjectionList projectionList = model.getPropertyProjection(
			attributes, validAttributes, notValidAttributes);

		@SuppressWarnings("unchecked")
		List<Object[]> results = (List<Object[]>)model.executeDynamicQuery(
			filter, projectionList);

		String[] validAttributesArr = validAttributes.toArray(
			new String[validAttributes.size()]);

		long i = -1;

		for (Object[] result : results) {
			Data data = DataUtil.createDataObject(
				model, validAttributesArr, result);

			addDataToMapValueList(dataMap, mapKeyAttribute, data, i--);

			addDataToMapValueList(
				dataMapByPK, model.getPrimaryKeyAttribute(), data, null);
		}

		if (dataMapByPK == null) {
			dataMapByPK = dataMap;
		}

		for (String notValidAttribute : notValidAttributes) {
			Map<Long, List<Data>> relatedDataMap =
				getRelatedDataFromMappingTable(model, notValidAttribute);

			if (relatedDataMap == null) {
				continue;
			}

			QueryUtil.addRelatedModelData(
				dataMapByPK, relatedDataMap, new String[]{notValidAttribute});
		}

		return dataMap;
	}

	public static Set<Data> queryTable(Model model, TableInfo tableInfo)
		throws Exception {

		if (Validator.isNull(tableInfo)) {
			return Collections.emptySet();
		}

		return queryTable(model, tableInfo, tableInfo.getAttributesName());
	}

	public static Set<Data> queryTable(
			Model model, TableInfo tableInfo, String[] attributesName)
		throws SQLException {

		Set<Data> dataSet = new HashSet<Data>();

		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			con = DataAccess.getConnection();

			String attributes = cleanAttributeName(
				tableInfo.getName(), (String)attributesName[0]);

			for (int i = 1; i<attributesName.length; i++) {
				String attribute = cleanAttributeName(
					tableInfo.getName(), (String)attributesName[i]);

				attributes += ", " + attribute;
			}

			String sql =
				"SELECT " + attributes + " FROM " + tableInfo.getName();

			sql = PortalUtil.transformSQL(sql);

			if (_log.isDebugEnabled()) {
				_log.debug("SQL: " + sql);
			}

			ps = con.prepareStatement(sql);

			rs = ps.executeQuery();

			while (rs.next()) {
				Object[] result = new Object[attributesName.length];

				for (int i = 0; i<attributesName.length; i++) {
					result[i] = rs.getObject(i+1);
				}

				Data data = DataUtil.createDataObject(
					tableInfo, attributesName, result);

				dataSet.add(data);
			}
		}
		finally {
			DataAccess.cleanUp(con, ps, rs);
		}

		return dataSet;
	}

	protected static void addDataToMap(
		Map<Long, Data> dataMap, String mapKeyAttribute, Data data,
		Long defaultValue) {

		if ((dataMap == null) || (data == null)) {
			return;
		}

		Long mappingAttributeValue = DataUtil.castLong(
			data.get(mapKeyAttribute));

		if (Validator.isNull(mappingAttributeValue)) {
			if (defaultValue == null) {
				return;
			}

			mappingAttributeValue = defaultValue;
		}

		if (!dataMap.containsKey(mappingAttributeValue)) {
			dataMap.put(mappingAttributeValue, data);
		}
	}

	protected static void addDataToMapValueList(
			Map<Long, List<Data>> dataMap, String mapKeyAttribute, Data data,
			Long defaultValue) {

		if ((dataMap == null) || (data == null)) {
			return;
		}

		Long mappingAttributeValue = DataUtil.castLong(
			data.get(mapKeyAttribute));

		if (Validator.isNull(mappingAttributeValue)) {
			if (defaultValue == null) {
				return;
			}

			mappingAttributeValue = defaultValue;
		}

		if (!dataMap.containsKey(mappingAttributeValue)) {
			List<Data> list = new ArrayList<Data>();
			list.add(data);
			dataMap.put(mappingAttributeValue, list);
		}
		else {
			dataMap.get(mappingAttributeValue).add(data);
		}
	}

	protected static String cleanAttributeName(
			String tableName, String attribute) {

		if (Validator.isNull(attribute)) {
			return StringPool.BLANK;
		}

		int pos = attribute.indexOf(".");

		if (pos == -1) {
			return attribute;
		}

		String prefix = attribute.substring(0, pos);

		if (prefix.equals(tableName)) {
			return attribute.substring(pos + 1);
		}

		return attribute;
	}

	protected static Map<Long, Data> getDataFromMappingTables(
			Model model, List<String> notValidAttributes,
			String mapKeyAttribute)
		throws Exception {

		Map<Long, Data> dataMap = new HashMap<Long, Data>();

		long i = -1;

		for (String notValidAttribute : notValidAttributes) {
			TableInfo tableInfo = model.getTableInfo(notValidAttribute);

			if (tableInfo == null) {
				continue;
			}

			Set<Data> dataSet = Query.queryTable(
				model, tableInfo, new String[] {notValidAttribute});

			for (Data data : dataSet) {
				addDataToMap(dataMap, mapKeyAttribute, data, i--);
			}
		}

		return dataMap;
	}

	protected static Map<Long, List<Data>> getRelatedDataFromMappingTable(
			Model model, String attribute)
		throws Exception {

		TableInfo tableInfo = model.getTableInfo(attribute);

		if (tableInfo == null) {
			return null;
		}

		Set<Data> relateDataSet = Query.queryTable(
			model, tableInfo,
			new String[] {model.getPrimaryKeyAttribute(), attribute});

		return DataUtil.getMapFromSetData(
			relateDataSet, model.getPrimaryKeyAttribute());
	}

	private static Log _log = LogFactoryUtil.getLog(Query.class);

}