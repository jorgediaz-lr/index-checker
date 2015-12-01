package com.jorgediaz.util.model;

import com.liferay.portal.kernel.dao.orm.Conjunction;
import com.liferay.portal.kernel.dao.orm.Criterion;
import com.liferay.portal.kernel.dao.orm.RestrictionsFactoryUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.servlet.ServletContextPool;
import com.liferay.portal.kernel.util.AggregateClassLoader;
import com.liferay.portal.kernel.util.PortalClassLoaderUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.model.ClassName;
import com.liferay.portal.model.ClassedModel;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import java.sql.Types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
public class ModelUtil {

	public static Object castStringToJdbcTypeObject(int type, String value) {
		Object result = null;

		switch (type) {
			case Types.CHAR:
			case Types.VARCHAR:
			case Types.LONGVARCHAR:
				result = value;
				break;

			case Types.NUMERIC:
			case Types.DECIMAL:
				result = new java.math.BigDecimal(value);
				break;

			case Types.BIT:
			case Types.BOOLEAN:
				result = Boolean.parseBoolean(value);
				break;

			case Types.TINYINT:
				result = Byte.parseByte(value);
				break;

			case Types.SMALLINT:
				result = Short.parseShort(value);
				break;

			case Types.INTEGER:
				result = Integer.parseInt(value);
				break;

			case Types.BIGINT:
				result = Long.parseLong(value);
				break;

			case Types.REAL:
			case Types.FLOAT:
				result = Float.parseFloat(value);
				break;

			case Types.DOUBLE:
				result = Double.parseDouble(value);
				break;

			case Types.BINARY:
			case Types.VARBINARY:
			case Types.LONGVARBINARY:
				result = value.getBytes();
				break;

			case Types.DATE:
				result = java.sql.Date.valueOf(value);
				break;

			case Types.TIME:
				result = java.sql.Time.valueOf(value);
				break;

			case Types.TIMESTAMP:
				result = java.sql.Timestamp.valueOf(value);
				break;

			default:
				throw new RuntimeException("Unsupported conversion for " +
					ModelUtil.getJdbcTypeNames().get(type));
		}

		return result;
	}

	public static Criterion generateCriterionFilter(
		Model model, String stringFilter) {

		String[] allFiltersArr = stringFilter.split(",");
		Conjunction conjuntion = RestrictionsFactoryUtil.conjunction();

		for (String filter : allFiltersArr) {
			String[] ops = {"=", "<>", " like ", ">", "<" ,"<=", ">="};

			Criterion criterion = null;

			for (String op : ops) {
				boolean dummyValue = false;

				if (filter.endsWith(op)) {
					filter = filter + "DUMMY_TEXT";
					dummyValue = true;
				}

				String[] filterArr = filter.split(op);

				if ((filterArr != null) && (filterArr.length == 2)) {
					String attrName = filterArr[0];
					String attrValue = filterArr[1];

					if (dummyValue) {
						attrValue = attrValue.replaceAll(
							"DUMMY_TEXT", StringPool.BLANK);
					}

					if (!model.hasAttribute(attrName)) {
						return null;
					}

					try {
						if (model.hasAttribute(attrValue)) {
							criterion =
								(Criterion)contructorCriterionImpl.newInstance(
									contructorPropertyExpression.newInstance(
								new Object[] { attrName, attrValue, op}));
						}
						else {
							Object value =
								ModelUtil.castStringToJdbcTypeObject(
									model.getAttributeType(
										attrName), attrValue);

							criterion =
								(Criterion)contructorCriterionImpl.newInstance(
									contructorSimpleExpression.newInstance(
								new Object[] { attrName, value, op}));
						}
					}
					catch (Exception e) {
						throw new RuntimeException(e.getMessage(), e);
					}

					break;
				}
			}

			if (criterion == null) {
				return null;
			}

			conjuntion.add(criterion);
		}

		return conjuntion;
	}

	public static ClassLoader getClassLoader() {

		ClassLoader portalClassLoader = PortalClassLoaderUtil.getClassLoader();

		AggregateClassLoader aggregateClassLoader = new AggregateClassLoader(
			portalClassLoader);

		if (_log.isDebugEnabled()) {
			_log.debug("Adding " + portalClassLoader);
		}

		aggregateClassLoader.addClassLoader(portalClassLoader);

		for (String servletContextName : ServletContextPool.keySet()) {
			try {
				ServletContext servletContext = ServletContextPool.get(
					servletContextName);

				ClassLoader classLoader = servletContext.getClassLoader();

				_log.debug(
					"Adding " + classLoader + " for " + servletContextName);

				aggregateClassLoader.addClassLoader(classLoader);
			}
			catch (Exception e) {
				if (_log.isWarnEnabled()) {
					_log.warn(
						"Error adding classLoader for " + servletContextName +
						": " + e.getMessage(), e);
				}
			}
		}

		return aggregateClassLoader;
	}

	public static List<String> getClassNameValues(
			Collection<ClassName> classNames) {

		List<String> classNameStr = new ArrayList<String>();

		for (ClassName className : classNames) {
			classNameStr.add(className.getValue());
		}

		return classNameStr;
	}

	public static Class<?> getJavaClass(
		Map<String, Class<?>> javaClassesCache, ClassLoader classloader,
		String className) throws ClassNotFoundException {

		Class<?> clazz = javaClassesCache.get(className);

		if (clazz != null) {
			return clazz;
		}

		try {
			clazz = PortalClassLoaderUtil.getClassLoader().loadClass(className);
		}
		catch (ClassNotFoundException e) {
		}

		if ((clazz == null) && (classloader != null)) {
			clazz = classloader.loadClass(className);
		}

		if (_log.isDebugEnabled()) {
			_log.debug(
				"loaded class: " + clazz + " from classloader :" + classloader);
		}

		javaClassesCache.put(className, clazz);
		return clazz;
	}

	public static Class<?> getJdbcTypeClass(int type) {
		Class<?> result = Object.class;

		switch (type) {
			case Types.CHAR:
			case Types.VARCHAR:
			case Types.LONGVARCHAR:
				result = String.class;
				break;

			case Types.NUMERIC:
			case Types.DECIMAL:
				result = java.math.BigDecimal.class;
				break;

			case Types.BIT:
				result = Boolean.class;
				break;

			case Types.TINYINT:
				result = Byte.class;
				break;

			case Types.SMALLINT:
				result = Short.class;
				break;

			case Types.INTEGER:
				result = Integer.class;
				break;

			case Types.BIGINT:
				result = Long.class;
				break;

			case Types.REAL:
			case Types.FLOAT:
				result = Float.class;
				break;

			case Types.DOUBLE:
				result = Double.class;
				break;

			case Types.BINARY:
			case Types.VARBINARY:
			case Types.LONGVARBINARY:
				result = Byte[].class;
				break;

			case Types.DATE:
				result = java.sql.Date.class;
				break;

			case Types.TIME:
				result = java.sql.Time.class;
				break;

			case Types.TIMESTAMP:
				result = java.sql.Timestamp.class;
				break;
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

	public static String getLiferayLocalServiceUtilClassName(
		Class<? extends ClassedModel> clazz) {

		Package pkg = clazz.getPackage();

		String packageName = pkg.getName();
		int pos = packageName.lastIndexOf(".model");

		if (pos > 0) {
			packageName = packageName.substring(0, pos);
		}

		String className =
			packageName + ".service." + clazz.getSimpleName() +
				"LocalServiceUtil";

		if (_log.isDebugEnabled()) {
			_log.debug(
				"LocalServiceUtil of " + clazz.getName() + ": " + className);
		}

		return className;
	}

	public static String getLiferayModelImplClassName(
		Class<? extends ClassedModel> clazz) {

		Package pkg = clazz.getPackage();

		String className =
			pkg.getName() + ".impl." + clazz.getSimpleName() + "ModelImpl";

		if (_log.isDebugEnabled()) {
			_log.debug("ModelImpl of " + clazz.getName() + ": " + className);
		}

		return className;
	}

	public static Object getLiferayModelImplField(
		Class<?> classLiferayModelImpl, String liferayModelImplField) {

		Object data = null;
		try {
			Field field = classLiferayModelImpl.getDeclaredField(
				liferayModelImplField);
			data = field.get(null);
		}
		catch (Exception e) {
			throw new RuntimeException(
				"Error accessing to " +
				classLiferayModelImpl.getName() + "#" +
				liferayModelImplField, e);
		}

		return data;
	}

	public static void makeFieldModifiable(Field nameField)
		throws NoSuchFieldException, SecurityException,
			IllegalArgumentException, IllegalAccessException {

		nameField.setAccessible(true);
		int modifiers = nameField.getModifiers();
		Field modifierField =
			nameField.getClass().getDeclaredField("modifiers");
		modifiers = modifiers & ~Modifier.FINAL;
		modifierField.setAccessible(true);
		modifierField.setInt(nameField, modifiers);
	}

	public static void setFieldValue(Object owner, Field field, Object value)
		throws IllegalArgumentException, IllegalAccessException,
			NoSuchFieldException, SecurityException {

		makeFieldModifiable(field);
		field.set(owner, value);
	}

	private static Log _log = LogFactoryUtil.getLog(ModelUtil.class);

	private static Constructor<?> contructorCriterionImpl;
	private static Constructor<?> contructorPropertyExpression;
	private static Constructor<?> contructorSimpleExpression;
	private static Map<Integer, String> jdbcTypeNames = null;

	static {
		try {
			Class<?> criterion =
				PortalClassLoaderUtil.getClassLoader().loadClass(
				"org.hibernate.criterion.Criterion");

			Class<?> simpleExpression =
				PortalClassLoaderUtil.getClassLoader().loadClass(
				"org.hibernate.criterion.SimpleExpression");
			contructorSimpleExpression =
				simpleExpression.getDeclaredConstructor(
					String.class, Object.class, String.class);
			contructorSimpleExpression.setAccessible(true);

			Class<?> propertyExpression =
				PortalClassLoaderUtil.getClassLoader().loadClass(
				"org.hibernate.criterion.PropertyExpression");
			contructorPropertyExpression =
				propertyExpression.getDeclaredConstructor(
					String.class, String.class, String.class);
			contructorPropertyExpression.setAccessible(true);

			Class<?> criterionImpl =
				PortalClassLoaderUtil.getClassLoader().loadClass(
				"com.liferay.portal.dao.orm.hibernate.CriterionImpl");
			contructorCriterionImpl = criterionImpl.getDeclaredConstructor(
				criterion);
		}
		catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

}