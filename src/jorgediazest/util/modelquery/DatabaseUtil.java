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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jorgediazest.util.data.Data;
import jorgediazest.util.data.DataComparator;
import jorgediazest.util.data.DataComparatorMap;
import jorgediazest.util.data.DataUtil;
import jorgediazest.util.model.Model;

/**
 * @author Jorge Díaz
 */
public class DatabaseUtil {

	public static Set<Data> queryTable(
			Model model, String name, String[] attributesName)
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

			String sql = "SELECT " + attributes + " FROM " + name;

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
					model, dataComparatorMap, attributesName, result);
				dataSet.add(data);
			}
		}
		finally {
			DataAccess.cleanUp(con, ps, rs);
		}

		return dataSet;
	}

	public static Set<Data> queryTableWithCache(
			Map<String, Set<Data>> dataSetCacheMap, Model model, String name,
			String[] attributes)
		throws SQLException {

		Set<Data> dataSetCache = dataSetCacheMap.get(name);

		if (dataSetCache == null) {
			dataSetCache = queryTable(model, name, attributes);

			dataSetCacheMap.put(name, dataSetCache);
		}

		return dataSetCache;
	}

	private static Log _log = LogFactoryUtil.getLog(DatabaseUtil.class);

	private static DataComparator dataComparatorMap = new DataComparatorMap();

}