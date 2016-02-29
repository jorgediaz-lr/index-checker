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

package jorgediazest.util.data;

import com.liferay.portal.kernel.util.PortalClassLoaderUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;

import java.lang.reflect.Method;

import java.math.BigDecimal;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.TimeZone;

import jorgediazest.util.reflection.ReflectionUtil;

/**
 * @author Jorge Díaz
 */
public class DataUtil {

	public static BigDecimal castBigDecimal(Object value) {
		if (value == null) {
			return null;
		}

		if (value instanceof BigDecimal) {
			return (BigDecimal)value;
		}

		if (value instanceof Number) {
			return new java.math.BigDecimal(((Number)value).toString());
		}

		if (value instanceof String) {
			try {
				return new java.math.BigDecimal((String)value);
			}
			catch (Exception e) {
			}
		}

		return null;
	}

	public static Boolean castBoolean(Object value) {
		if (value == null) {
			return null;
		}

		if (value instanceof Boolean) {
			return (Boolean)value;
		}

		if (value instanceof Number) {
			return ((Boolean)value).booleanValue();
		}

		if (value instanceof String) {
			try {
				return Boolean.parseBoolean((String)value);
			}
			catch (Exception e) {
			}
		}

		return null;
	}

	public static Byte castByte(Object value) {
		if (value == null) {
			return null;
		}

		if (value instanceof Byte) {
			return (Byte)value;
		}

		if (value instanceof Number) {
			return ((Byte)value).byteValue();
		}

		if (value instanceof String) {
			try {
				return Byte.parseByte((String)value);
			}
			catch (Exception e) {
			}
		}

		return null;
	}

	public static byte[] castBytes(Object value) {
		if (value == null) {
			return null;
		}

		if (value instanceof byte[]) {
			return (byte[])value;
		}

		if (value instanceof String) {
			try {
				return ((String)value).getBytes();
			}
			catch (Exception e) {
			}
		}

		return null;
	}

	public static Long castDate(Object value) {
		if (value == null) {
			return null;
		}

		if (value instanceof Long) {
			return (Long)value;
		}

		if (value instanceof Number) {
			return ((Number)value).longValue();
		}

		if (value instanceof Time) {
			return ((Time)value).getTime();
		}

		if (value instanceof Date) {
			return ((Date)value).getTime();
		}

		if (value instanceof Timestamp) {
			return ((Timestamp)value).getTime();
		}

		if (value instanceof String) {
			try {
				return stringToDate((String)value);
			}
			catch (Exception e) {
			}
		}

		return null;
	}

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

	public static Float castFloat(Object value) {
		if (value == null) {
			return null;
		}

		if (value instanceof Float) {
			return (Float)value;
		}

		if (value instanceof Number) {
			return ((Number)value).floatValue();
		}

		if (value instanceof String) {
			try {
				return Float.parseFloat((String)value);
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

	public static Object castObjectToJdbcTypeObject(int type, Object value) {
		Object result = null;

		switch (type) {
			case Types.CHAR:
			case Types.VARCHAR:
			case Types.LONGVARCHAR:
			case Types.CLOB:
				result = castString(value);
				break;

			case Types.NUMERIC:
			case Types.DECIMAL:
				result = castBigDecimal(value);
				break;

			case Types.BIT:
			case Types.BOOLEAN:
				result = castBoolean(value);
				break;

			case Types.TINYINT:
				result = castByte(value);
				break;

			case Types.SMALLINT:
				result = castShort(value);
				break;

			case Types.INTEGER:
				result = castInt(value);
				break;

			case Types.BIGINT:
				result = castLong(value);
				break;

			case Types.REAL:
			case Types.FLOAT:
				result = castFloat(value);
				break;

			case Types.DOUBLE:
				result = castDouble(value);
				break;

			case Types.BINARY:
			case Types.VARBINARY:
			case Types.LONGVARBINARY:
				result = castBytes(value);
				break;

			case Types.DATE:
			case Types.TIME:
			case Types.TIMESTAMP:
				result = castDate(value);
				break;

			default:
				throw new RuntimeException(
					"Unsupported conversion for " +
						ReflectionUtil.getJdbcTypeNames().get(type));
		}

		return result;
	}

	public static Short castShort(Object value) {
		if (value == null) {
			return null;
		}

		if (value instanceof Integer) {
			return (Short)value;
		}

		if (value instanceof Number) {
			return ((Short)value).shortValue();
		}

		if (value instanceof String) {
			try {
				return Short.parseShort((String)value);
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

	/* Long.compare()is not available at java 1.6 */

	public static int compareLongs(long x, long y) {
		return (x < y) ? -1 : ((x == y) ? 0 : 1);
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

	public static String[] getListUuid(Collection<Data> data) {
		String[] valuesPK = new String[data.size()];

		int i = 0;

		for (Data value : data) {
			valuesPK[i++] = value.getUuid();
		}

		return valuesPK;
	}

	public static String getValuesPKText(String type, Set<Data> data) {
		String valuesPK = Arrays.toString(getListUuid(data));

		if (valuesPK.length() <= 1) {
			valuesPK = StringPool.BLANK;
		}
		else {
			valuesPK = valuesPK.substring(1, valuesPK.length()-1);
		}

		return valuesPK;
	}

	public static Long stringToDate(String dateString) {
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