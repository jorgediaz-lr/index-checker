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

package jorgediazest.indexchecker.index;

import com.liferay.petra.reflect.ReflectionUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.search.BooleanClauseOccur;
import com.liferay.portal.kernel.search.BooleanQuery;
import com.liferay.portal.kernel.search.ParseException;
import com.liferay.portal.kernel.search.Query;
import com.liferay.portal.kernel.search.QueryConfig;
import com.liferay.portal.kernel.search.TermRangeQuery;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Builds the search queries without binding the bundle to one Liferay release.
 *
 * <p>
 * Up to 2026.Q1 the queries are interfaces implemented by the classes of
 * com.liferay.portal.kernel.search.generic. LPD-87417 merged those classes into
 * their interfaces, so from 2026.Q2 onwards BooleanQuery and TermRangeQuery are
 * classes and the whole generic package is gone.
 * </p>
 *
 * <p>
 * The compiler emits invokeinterface for the old releases and invokevirtual for
 * the new ones, so a call written against either release fails in the other one
 * with an IncompatibleClassChangeError. Reflection dispatches dynamically, so it
 * works in both. Only the calls on these receivers need it: the types can still
 * be used to declare variables and to call the Liferay APIs that receive them.
 * </p>
 *
 * <p>
 * Never call a method on a BooleanQuery, a TermRangeQuery or a Query directly,
 * add it here instead.
 * </p>
 *
 * @author Jorge Díaz
 */
public class SearchQueryFactory {

	public static void addMust(BooleanQuery booleanQuery, Query query)
		throws ParseException {

		_invoke(_addMethod, booleanQuery, query, BooleanClauseOccur.MUST);
	}

	public static void addRequiredTerm(
		BooleanQuery booleanQuery, String field, String value) {

		_invoke(_addRequiredTermMethod, booleanQuery, field, value);
	}

	public static void addTerm(
			BooleanQuery booleanQuery, String field, long value)
		throws ParseException {

		_invoke(_addTermMethod, booleanQuery, field, value);
	}

	public static String getField(TermRangeQuery termRangeQuery) {
		return (String)_invoke(_getFieldMethod, termRangeQuery);
	}

	public static String getLowerTerm(TermRangeQuery termRangeQuery) {
		return (String)_invoke(_getLowerTermMethod, termRangeQuery);
	}

	public static BooleanQuery newBooleanQuery() {
		return (BooleanQuery)_newInstance(_booleanQueryConstructor);
	}

	public static TermRangeQuery newTermRangeQuery(
		String field, boolean includesLower, String lowerTerm,
		String upperTerm) {

		return (TermRangeQuery)_newInstance(
			_termRangeQueryConstructor, field, lowerTerm, upperTerm,
			includesLower, true);
	}

	public static void setQueryConfig(Query query, QueryConfig queryConfig) {
		_invoke(_setQueryConfigMethod, query, queryConfig);
	}

	private static Class<?> _getQueryClass(
		Class<?> queryClass, String genericClassName) {

		try {
			ClassLoader classLoader = SearchQueryFactory.class.getClassLoader();

			return Class.forName(genericClassName, false, classLoader);
		}
		catch (ClassNotFoundException classNotFoundException) {
			if (queryClass.isInterface()) {
				_log.warn(
					"Unable to load " + genericClassName + " and " +
						queryClass.getName() + " is an interface. Check the " +
							"optional Import-Package of its package in bnd.bnd",
					classNotFoundException);
			}
			else if (_log.isDebugEnabled()) {
				_log.debug(
					genericClassName + " is not available, this release " +
						"provides " + queryClass.getName() + " as a class");
			}

			return queryClass;
		}
	}

	private static Object _invoke(
		Method method, Object target, Object... arguments) {

		if (method == null) {
			throw new IllegalStateException(
				"Unable to obtain the search query methods", _initFailure);
		}

		try {
			return method.invoke(target, arguments);
		}
		catch (IllegalAccessException illegalAccessException) {
			return ReflectionUtil.throwException(illegalAccessException);
		}
		catch (InvocationTargetException invocationTargetException) {
			return ReflectionUtil.throwException(
				invocationTargetException.getCause());
		}
	}

	private static Object _newInstance(
		Constructor<?> constructor, Object... arguments) {

		if (constructor == null) {
			throw new IllegalStateException(
				"Unable to obtain the search query constructors", _initFailure);
		}

		try {
			return constructor.newInstance(arguments);
		}
		catch (ReflectiveOperationException reflectiveOperationException) {
			return ReflectionUtil.throwException(reflectiveOperationException);
		}
	}

	private static final Log _log = LogFactoryUtil.getLog(
		SearchQueryFactory.class);

	private static Method _addMethod;
	private static Method _addRequiredTermMethod;
	private static Method _addTermMethod;
	private static Constructor<?> _booleanQueryConstructor;
	private static Method _getFieldMethod;
	private static Method _getLowerTermMethod;
	private static ReflectiveOperationException _initFailure;
	private static Method _setQueryConfigMethod;
	private static Constructor<?> _termRangeQueryConstructor;

	static {
		try {
			Class<?> booleanQueryClass = _getQueryClass(
				BooleanQuery.class,
				"com.liferay.portal.kernel.search.generic.BooleanQueryImpl");

			_booleanQueryConstructor = booleanQueryClass.getConstructor();

			Class<?> termRangeQueryClass = _getQueryClass(
				TermRangeQuery.class,
				"com.liferay.portal.kernel.search.generic.TermRangeQueryImpl");

			_termRangeQueryConstructor = termRangeQueryClass.getConstructor(
				String.class, String.class, String.class, boolean.class,
				boolean.class);

			_addMethod = BooleanQuery.class.getMethod(
				"add", Query.class, BooleanClauseOccur.class);
			_addRequiredTermMethod = BooleanQuery.class.getMethod(
				"addRequiredTerm", String.class, String.class);
			_addTermMethod = BooleanQuery.class.getMethod(
				"addTerm", String.class, long.class);
			_getFieldMethod = TermRangeQuery.class.getMethod("getField");
			_getLowerTermMethod = TermRangeQuery.class.getMethod(
				"getLowerTerm");
			_setQueryConfigMethod = Query.class.getMethod(
				"setQueryConfig", QueryConfig.class);
		}
		catch (ReflectiveOperationException reflectiveOperationException) {
			_initFailure = reflectiveOperationException;

			_log.error(
				"Unable to obtain the search query members",
				reflectiveOperationException);
		}
	}

}