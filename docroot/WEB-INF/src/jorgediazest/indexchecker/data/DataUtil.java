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

package jorgediazest.indexchecker.data;

import com.liferay.portal.kernel.util.PortalClassLoaderUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;

import java.lang.reflect.Method;

import java.sql.Timestamp;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.TimeZone;

/**
 * @author Jorge Díaz
 */
public class DataUtil {

	public static Double castDouble(Object value) {
		if (value == null) {
			return null;
		}

		if (value instanceof Double) {
			return (Double)value;
		}

		if (value instanceof Number) {
			return ((Number)value).doubleValue();
		}

		if (value instanceof String) {
			try {
				return Double.parseDouble((String)value);
			}
			catch (Exception e) {
			}
		}

		return null;
	}

	public static Integer castInt(Object value) {
		if (value == null) {
			return null;
		}

		if (value instanceof Integer) {
			return (Integer)value;
		}

		if (value instanceof Number) {
			return ((Number)value).intValue();
		}

		if (value instanceof String) {
			try {
				return Integer.parseInt((String)value);
			}
			catch (Exception e) {
			}
		}

		return null;
	}

	public static Long castLong(Object value) {
		if (value == null) {
			return null;
		}

		if (value instanceof Long) {
			return (Long)value;
		}

		if (value instanceof Number) {
			return ((Number)value).longValue();
		}

		if (value instanceof String) {
			try {
				return Long.parseLong((String)value);
			}
			catch (Exception e) {
			}
		}

		return null;
	}

	public static String castString(Object value) {
		if (value == null) {
			return null;
		}

		if (value.getClass() == byte[].class) {
			byte[] valueArray = (byte[])value;
			return new String(valueArray, 0, valueArray.length);
		}

		return value.toString();
	}

	public static int compareLongs(long x, long y) {
		return (x < y) ? -1 : ((x == y) ? 0 : 1);
	}

	public static boolean exactIntegers(Integer i1, Integer i2) {
		if (i1 == null) {
			return (i2 == null);
		}

		return i1.equals(i2);
	}

	public static boolean exactLongs(Long l1, Long l2) {
		if (l1 == null) {
			return (l2 == null);
		}

		return l1.equals(l2);
	}

	public static boolean exactStrings(String s1, String s2) {
		if (s1 == null) {
			return (s2 == null);
		}

		return s1.equals(s2);
	}

	public static long getIdFromUID(String strValue) {
		long id = -1;
		String[] uidArr = strValue.split("_");

		if ((uidArr != null) && (uidArr.length >= 3)) {
			int pos = uidArr.length-2;
			while ((pos > 0) && !"PORTLET".equals(uidArr[pos])) {
				pos = pos - 2;
			}

			if ((pos > 0) && "PORTLET".equals(uidArr[pos])) {
				id = DataUtil.castLong(uidArr[pos+1]);
			}
		}

		return id;
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

	public static String getValuesPKText(String type, Set<Data> data) {
		String valuesPK = null;

		if (type.contains("index")) {
			valuesPK = Arrays.toString(getListUid(data));
		}
		else {
			valuesPK = Arrays.toString(getListPK(data));
		}

		if (valuesPK.length() <= 1) {
			valuesPK = StringPool.BLANK;
		}
		else {
			valuesPK = valuesPK.substring(1, valuesPK.length()-1);
		}

		return valuesPK;
	}

	public static Long stringToTime(String dateString) {
		if (Validator.isNull(dateString)) {
			return null;
		}

		try {
			return ((Long)stringToTimeMethod.invoke(null, dateString))/1000;
		}
		catch (Exception e) {}

		try {
			return (Long.valueOf(dateString))/1000;
		}
		catch (Exception e) {}

		try {
			long rawOffset = TimeZone.getDefault().getRawOffset() / 1000L;

			return ((Timestamp.valueOf(dateString).getTime())/1000) + rawOffset;
		}
		catch (Exception e) {}

		return null;
	}

	protected static Method stringToTimeMethod = null;

	static {
		try {
			Class<?> dateTools =
				PortalClassLoaderUtil.getClassLoader().loadClass(
				"org.apache.lucene.document.DateTools");

			stringToTimeMethod = dateTools.getMethod(
				"stringToTime", String.class);
		}
		catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

}