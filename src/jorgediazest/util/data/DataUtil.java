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
import com.liferay.portal.kernel.util.CharPool;
import com.liferay.portal.kernel.util.DateFormatFactoryUtil;
import com.liferay.portal.kernel.util.LocalizationUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;

import java.lang.ref.WeakReference;

import java.math.BigDecimal;

import java.sql.Time;
import java.sql.Timestamp;

import java.text.DateFormat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.WeakHashMap;

import jorgediazest.util.comparator.DataComparator;
import jorgediazest.util.comparator.DataComparatorMap;
import jorgediazest.util.comparator.DataModelComparator;
import jorgediazest.util.model.Model;
import jorgediazest.util.table.TableInfo;

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
			if (Validator.isNull(value)) {
				return null;
			}

			try {
				return new java.math.BigDecimal((String)value);
			}
			catch (Exception e) {
			}
		}

		return null;
	}

	public static Boolean castBoolean(Object value) {
		if (Validator.isNull(value)) {
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
			if (Validator.isNull(value)) {
				return null;
			}

			try {
				return Byte.parseByte((String)value);
			}
			catch (Exception e) {
			}
		}

		return null;
	}

	public static byte[] castBytes(Object value) {
		if (Validator.isNull(value)) {
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
		if (Validator.isNull(value)) {
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
			if (Validator.isNull(value)) {
				return null;
			}

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
			if (Validator.isNull(value)) {
				return null;
			}

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
			return _parseInt((String)value);
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
			return _parseLong((String)value);
		}

		return null;
	}

	public static Object castObject(Class<?> type, Object value) {
		if (Object.class.equals(type)) {
			return value;
		}

		if (String.class.equals(type)) {
			return castString(value);
		}

		if (java.math.BigDecimal.class.equals(type)) {
			return castBigDecimal(value);
		}

		if (Boolean.class.equals(type)) {
			return castBoolean(value);
		}

		if (Byte.class.equals(type)) {
			return castByte(value);
		}

		if (Short.class.equals(type)) {
			return castShort(value);
		}

		if (Integer.class.equals(type)) {
			return castInt(value);
		}

		if (Long.class.equals(type)) {
			return castLong(value);
		}

		if (Float.class.equals(type)) {
			return castFloat(value);
		}

		if (Double.class.equals(type)) {
			return castDouble(value);
		}

		if (Byte[].class.equals(type)) {
			return castBytes(value);
		}

		if (java.sql.Date.class.equals(type) ||
			java.sql.Time.class.equals(type) ||
			java.sql.Timestamp.class.equals(type)) {

			return castDateToEpoch(value);
		}

		if (UUID.class.equals(type)) {
			return castUUID(value);
		}

		throw new RuntimeException(
			"Unsupported conversion for " + type.getCanonicalName());
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
			return _parseShort((String)value);
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

	public static UUID castUUID(Object value) {
		if (value == null) {
			return null;
		}

		if (value instanceof UUID) {
			return (UUID)value;
		}

		if (value instanceof String) {
			return UUID.fromString((String)value);
		}

		return null;
	}

	public static Data createDataObject(
		Model model, String[] attributes, Object[] array) {

		Data data = new Data(model);

		data.setArray(attributes, array);

		return data;
	}

	public static Data createDataObject(
		TableInfo tableInfo, String[] attributes, Object[] array) {

		Data data = new Data(tableInfo);

		data.setArray(attributes, array);

		return data;
	}

	public static Data[] getArrayCommonData(Set<Data> set1, Set<Data> set2) {
		Set<Data> both = new TreeSet<Data>(set1);
		both.retainAll(set2);
		return both.toArray(new Data[0]);
	}

	public static DataComparator getDataComparator(Model model) {
		if (model == null) {
			return dataComparatorMap;
		}

		DataComparator dataComparator = null;

		WeakReference<DataComparator> weakReference =
			modelDataComparatorCache.get(model);

		if (weakReference != null) {
			dataComparator = weakReference.get();
		}

		if (dataComparator == null) {
			List<String> keyAttributes = model.getKeyAttributes();

			if ((keyAttributes == null) || keyAttributes.isEmpty()) {
				dataComparator = dataComparatorMap;
			}
			else {
				dataComparator = new DataModelComparator(keyAttributes);
			}

			modelDataComparatorCache.put(
				model, new WeakReference<DataComparator>(dataComparator));
		}

		return dataComparator;
	}

	public static ThreadLocal<Boolean> getIgnorecase() {
		return DataUtil.ignoreCase;
	}

	public static boolean getIgnoreCase() {
		return DataUtil.ignoreCase.get();
	}

	public static String[] getListAttr(Collection<Data> data, String attr) {
		return getListAttr(data, attr, data.size());
	}

	public static String[] getListAttr(
		Collection<Data> data, String attr, int size) {

		if ((size > data.size()) || (size <= 0)) {
			size = data.size();
		}

		String[] values = new String[size];

		int i = 0;

		for (Data value : data) {
			values[i++] = value.get(attr).toString();

			if (i >= size) {
				break;
			}
		}

		return values;
	}

	public static Map<Long, List<Data>> getMapFromSetData(
		Set<Data> dataSet, String keyAttribute) {

		Map<Long, List<Data>> dataMap = new HashMap<Long, List<Data>>();

		for (Data data : dataSet) {
			Long key = (Long)data.get(keyAttribute);

			if (key == null) {
				continue;
			}

			if (!dataMap.containsKey(key)) {
				List<Data> list = new ArrayList<Data>();
				list.add(data);
				dataMap.put(key, list);
			}
			else {
				dataMap.get(key).add(data);
			}
		}

		return dataMap;
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

	public static Object transformArray(Class<?> type, Object[] values) {
		Set<Object> transformObjects = transformArrayToSet(type, values);

		if (transformObjects.isEmpty()) {
			return null;
		}

		if (transformObjects.size() == 1) {
			return transformObjects.toArray()[0];
		}

		return transformObjects;
	}

	public static Object transformObject(Class<?> type, Object o) {
		if (o instanceof Map) {
			return o;
		}

		Object transformObject = DataUtil.castObject(type, o);

		if (transformObject instanceof String) {
			String str = (String)transformObject.toString();

			if (Validator.isXml(str)) {
				transformObject = transformXmlToMap(str);
			}
		}

		return transformObject;
	}

	protected static Set<Object> transformArrayToSet(
		Class<?> type, Object[] values) {

		Set<Object> transformObjects = new HashSet<Object>(values.length);

		for (Object o : values) {
			Object transformObject = transformObject(type, o);

			if (transformObject != null) {
				transformObjects.add(transformObject);
			}
		}

		return transformObjects;
	}

	protected static Map<Locale, String> transformXmlToMap(String xml) {
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

	private static Integer _parseInt(String value) {
		int length = value.length();

		if (length <= 0) {
			return null;
		}

		int pos = 0;
		int limit = -Integer.MAX_VALUE;
		boolean negative = false;

		char c = value.charAt(0);

		if (c < CharPool.NUMBER_0) {
			if (c == CharPool.MINUS) {
				limit = Integer.MIN_VALUE;
				negative = true;
			}
			else if (c != CharPool.PLUS) {
				return null;
			}

			if (length == 1) {
				return null;
			}

			pos++;
		}

		int smallLimit = limit / 10;

		int result = 0;

		while (pos < length) {
			if (result < smallLimit) {
				return null;
			}

			c = value.charAt(pos++);

			if ((c < CharPool.NUMBER_0) || (c > CharPool.NUMBER_9)) {
				return null;
			}

			int number = c - CharPool.NUMBER_0;

			result *= 10;

			if (result < (limit + number)) {
				return null;
			}

			result -= number;
		}

		if (negative) {
			return result;
		}
		else {
			return -result;
		}
	}

	private static Long _parseLong(String value) {
		int length = value.length();

		if (length <= 0) {
			return null;
		}

		int pos = 0;
		long limit = -Long.MAX_VALUE;
		boolean negative = false;

		char c = value.charAt(0);

		if (c < CharPool.NUMBER_0) {
			if (c == CharPool.MINUS) {
				limit = Long.MIN_VALUE;
				negative = true;
			}
			else if (c != CharPool.PLUS) {
				return null;
			}

			if (length == 1) {
				return null;
			}

			pos++;
		}

		long smallLimit = limit / 10;

		long result = 0;

		while (pos < length) {
			if (result < smallLimit) {
				return null;
			}

			c = value.charAt(pos++);

			if ((c < CharPool.NUMBER_0) || (c > CharPool.NUMBER_9)) {
				return null;
			}

			int number = c - CharPool.NUMBER_0;

			result *= 10;

			if (result < (limit + number)) {
				return null;
			}

			result -= number;
		}

		if (negative) {
			return result;
		}
		else {
			return -result;
		}
	}

	private static Short _parseShort(String value) {
		Integer i = _parseInt(value);

		if ((i == null) || (i < Short.MIN_VALUE) || (i > Short.MAX_VALUE)) {
			return null;
		}

		return (short)i.shortValue();
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

	private static DataComparator dataComparatorMap = new DataComparatorMap();

	private static ThreadLocal<Boolean> ignoreCase =
		new ThreadLocal<Boolean>() {

		@Override
		protected Boolean initialValue()
		{
			return false;
		}
	};

	private static Map<Model, WeakReference<DataComparator>>
		modelDataComparatorCache = Collections.synchronizedMap(
			new WeakHashMap<Model, WeakReference<DataComparator>>());

}