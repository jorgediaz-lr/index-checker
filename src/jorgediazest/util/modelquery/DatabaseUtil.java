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

package jorgediazest.util.modelquery;

import com.liferay.portal.kernel.dao.jdbc.DataAccess;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.util.PortalUtil;

import java.lang.reflect.Field;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jorgediazest.util.data.Data;
import jorgediazest.util.data.DataComparator;
import jorgediazest.util.data.DataComparatorMap;
import jorgediazest.util.data.DataUtil;
import jorgediazest.util.model.Model;
import jorgediazest.util.model.TableInfo;

/**
 * @author Jorge Díaz
 */
public class DatabaseUtil {

	public static Object castObjectToJdbcTypeObject(int type, Object value) {
		Object result = null;

		switch (type) {
			case Types.NULL:
				result = value;
				break;
			case Types.CHAR:
			case Types.VARCHAR:
			case Types.LONGVARCHAR:
			case Types.CLOB:
				result = DataUtil.castString(value);
				break;

			case Types.NUMERIC:
			case Types.DECIMAL:
				result = DataUtil.castBigDecimal(value);
				break;

			case Types.BIT:
			case Types.BOOLEAN:
				result = DataUtil.castBoolean(value);
				break;

			case Types.TINYINT:
				result = DataUtil.castByte(value);
				break;

			case Types.SMALLINT:
				result = DataUtil.castShort(value);
				break;

			case Types.INTEGER:
				result = DataUtil.castInt(value);
				break;

			case Types.BIGINT:
				result = DataUtil.castLong(value);
				break;

			case Types.REAL:
			case Types.FLOAT:
				result = DataUtil.castFloat(value);
				break;

			case Types.DOUBLE:
				result = DataUtil.castDouble(value);
				break;

			case Types.BINARY:
			case Types.VARBINARY:
			case Types.LONGVARBINARY:
				result = DataUtil.castBytes(value);
				break;

			case Types.DATE:
			case Types.TIME:
			case Types.TIMESTAMP:
				result = DataUtil.castDateToEpoch(value);
				break;

			default:
				throw new RuntimeException(
					"Unsupported conversion for " +
						getJdbcTypeNames().get(type));
		}

		return result;
	}

	public static Map<Integer, String> getJdbcTypeNames() {

		if (jdbcTypeNames == null) {
			Map<Integer, String> aux = new HashMap<Integer, String>();

			for (Field field : Types.class.getFields()) {
				try {
					aux.put((Integer)field.get(null), field.getName());
				}
				catch (IllegalArgumentException e) {
				}
				catch (IllegalAccessException e) {
				}
			}

			jdbcTypeNames = aux;
		}

		return jdbcTypeNames;
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

			String attributes = (String)attributesName[0];

			for (int i = 1; i<attributesName.length; i++) {
				attributes += ", " + (String) attributesName[i];
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
					model, tableInfo, dataComparatorMap, attributesName,
					result);
				dataSet.add(data);
			}
		}
		finally {
			DataAccess.cleanUp(con, ps, rs);
		}

		return dataSet;
	}

	public static Set<Data> queryTableWithCache(
			Map<String, Set<Data>> dataSetCacheMap, Model model,
			TableInfo tableInfo, String[] attributes)
		throws SQLException {

		Set<Data> dataSetCache = dataSetCacheMap.get(tableInfo.getName());

		if (dataSetCache == null) {
			dataSetCache = queryTable(model, tableInfo, attributes);

			dataSetCacheMap.put(tableInfo.getName(), dataSetCache);
		}

		return dataSetCache;
	}

	private static Log _log = LogFactoryUtil.getLog(DatabaseUtil.class);

	private static DataComparator dataComparatorMap = new DataComparatorMap();
	private static Map<Integer, String> jdbcTypeNames = null;

}