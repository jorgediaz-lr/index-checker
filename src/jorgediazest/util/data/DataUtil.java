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

import com.liferay.portal.kernel.util.DateFormatFactoryUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;

import java.math.BigDecimal;

import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;

import java.text.DateFormat;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Set;
import java.util.TreeSet;

import jorgediazest.util.model.Model;
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
			return ((Number)value).byteValue();
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

	public static Long castDateToEpoch(Object value) {
		if (value == null) {
			return null;
		}

		Date date = null;

		if (value instanceof Date) {
			date = (Date)value;
		}
		else if (value instanceof String) {
			date = stringToDate((String)value);
		}

		if (Validator.isNull(date)) {
			return castLong(value);
		}

		return date.getTime() / 1000L;
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
				result = castDateToEpoch(value);
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
			return ((Integer)value).shortValue();
		}

		if (value instanceof Number) {
			return ((Number)value).shortValue();
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

	public static boolean equalsAttributes(
		Model model1, Model model2, String attr1, String attr2, Object o1,
		Object o2) {

		if (o1 == null) {
			return (o1 == o2);
		}

		int type1 = model1.getAttributeType(attr1);
		int type2 = model2.getAttributeType(attr2);

		if ((type1 != type2) || (type1 == 0) || (type2 == 0)) {
			o1 = o1.toString();
			o2 = o2.toString();
		}

		return o1.equals(o2);
	}

	public static Data[] getArrayCommonData(Set<Data> set1, Set<Data> set2) {
		Set<Data> both = new TreeSet<Data>(set1);
		both.retainAll(set2);
		return both.toArray(new Data[0]);
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

	public static String[] getListAttr(Collection<Data> data, String attr) {
		String[] values = new String[data.size()];

		int i = 0;

		for (Data value : data) {
			values[i++] = value.get(attr).toString();
		}

		return values;
	}

	public static String getListAttrAsString(Set<Data> data, String attr) {
		String valuesPK = Arrays.toString(getListAttr(data, attr));

		if (valuesPK.length() <= 1) {
			valuesPK = StringPool.BLANK;
		}
		else {
			valuesPK = valuesPK.substring(1, valuesPK.length()-1);
		}

		return valuesPK;
	}

	public static Date stringToDate(String dateString) {

		Date date = null;

		try {
			date = dateFormatyyyyMMddHHmmss.get().parse(dateString);
		}
		catch (Exception e) {}

		if (date != null) {
			return date;
		}

		try {
			date = dateFormatyyyyMMddHHmmssSSS.get().parse(dateString);
		}
		catch (Exception e) {}

		if (date != null) {
			return date;
		}

		try {
			date = Timestamp.valueOf(dateString);
		}
		catch (Exception e) {}

		if (date != null) {
			return date;
		}

		try {
			date = Time.valueOf(dateString);
		}
		catch (Exception e) {}

		return date;
	}

	private static final ThreadLocal<DateFormat> dateFormatyyyyMMddHHmmss =
			new ThreadLocal<DateFormat>() {

		@Override
		protected DateFormat initialValue()
		{
			return DateFormatFactoryUtil.getSimpleDateFormat("yyyyMMddHHmmss");
		}
	};

	private static final ThreadLocal<DateFormat> dateFormatyyyyMMddHHmmssSSS =
			new ThreadLocal<DateFormat>() {

		@Override
		protected DateFormat initialValue()
		{
			return DateFormatFactoryUtil.getSimpleDateFormat(
				"yyyyMMddHHmmssSSS");
		}
	};

}