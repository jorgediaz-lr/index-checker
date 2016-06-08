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

import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.util.DateFormatFactoryUtil;
import com.liferay.portal.kernel.util.LocalizationUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;

import java.math.BigDecimal;

import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;

import java.text.DateFormat;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
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

		String aux;

		if (value.getClass() == byte[].class) {
			byte[] valueArray = (byte[])value;
			aux = new String(valueArray, 0, valueArray.length);
		}
		else {
			aux = value.toString();
		}

		if (DataUtil.getIgnoreCase() && !Validator.isXml(aux)) {
			aux = StringUtil.toLowerCase(aux);
		}

		return aux;
	}

	/* Long.compare()is not available at java 1.6 */

	public static int compareLongs(long x, long y) {
		return (x < y) ? -1 : ((x == y) ? 0 : 1);
	}

	public static Object convertXmlToMap(Object object) {
		if (object instanceof String) {
			String str = (String)object;

			if (Validator.isXml(str)) {
				object = convertXmlToMap(str);
			}
		}

		return object;
	}

	public static Map<Locale, String> convertXmlToMap(String xml) {
		Map<Locale, String> map = LocalizationUtil.getLocalizationMap(xml);

		Map<Locale, String> cleanMap = new HashMap<Locale, String>();

		for (Locale key : LanguageUtil.getAvailableLocales()) {
			if (!map.containsKey(key)) {
				continue;
			}

			String value = map.get(key);

			if (DataUtil.isNotNull(value)) {
				if (DataUtil.getIgnoreCase()) {
					value = StringUtil.toLowerCase(value);
				}

				cleanMap.put(key, value);
			}
		}

		return cleanMap;
	}

	public static Data createDataObject(
		Model model, DataComparator dataComparator, String[] attributes,
		Object[] result) {

		Data data = new Data(model, dataComparator);

		int i = 0;

		for (String attrib : attributes) {
			data.set(attrib, result[i++]);
		}

		return data;
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

	public static ThreadLocal<Boolean> getIgnorecase() {
		return DataUtil.ignoreCase;
	}

	public static boolean getIgnoreCase() {
		return DataUtil.ignoreCase.get();
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

	public static boolean isNotNull(Object obj) {
		return !isNull(obj);
	}

	public static boolean isNull(Object obj) {
		if (Validator.isNull(obj)) {
			return true;
		}

		if (obj instanceof Double) {
			return (((Double)obj).longValue() == 0);
		}
		else if (obj instanceof Float) {
			return (((Float)obj).longValue() == 0);
		}
		else if (obj instanceof Integer) {
			return (((Integer)obj).longValue() == 0);
		}
		else if (obj instanceof String) {
			Double d = castDouble((String)obj);

			if (d != null) {
				return (d.longValue() == 0);
			}

			Long l = castLong((String)obj);

			if (l != null) {
				return (l.longValue() == 0);
			}
		}

		return false;
	}

	public static void setIgnoreCase(boolean ignoreCase) {
		DataUtil.ignoreCase.set(ignoreCase);
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

	private static ThreadLocal<Boolean> ignoreCase =
		new ThreadLocal<Boolean>() {

		@Override
		protected Boolean initialValue()
		{
			return false;
		}
	};

}