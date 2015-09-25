package com.jorgediaz.util.model;

import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.DynamicQueryFactoryUtil;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.MethodKey;
import com.liferay.portal.model.ClassedModel;
import com.liferay.portal.service.GroupLocalServiceUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
public class ModelFactory {

	public ModelFactory() {
		this(null, null);
	}

	public ModelFactory(Class<? extends Model> defaultModelClass) {
		this(defaultModelClass, null);
	}

	public ModelFactory(
		Class<? extends Model> defaultModelClass,
		Map<String, Class<? extends Model>> modelClassMap) {

		this.classLoader = ModelUtil.getClassLoader();

		if (defaultModelClass == null) {
			this.defaultModelClass = DefaultModel.class;
		}
		else {
			this.defaultModelClass = defaultModelClass;
		}

		this.modelClassMap = modelClassMap;
	}

	public Model getModelObject(Class<? extends ClassedModel> clazz) {

		if ((clazz == null) || !ClassedModel.class.isAssignableFrom(clazz)) {
			return null;
		}

		String className = clazz.getName();
		Class<? extends Model> modelClass = this.defaultModelClass;

		if ((this.modelClassMap != null) &&
			this.modelClassMap.containsKey(className)) {

			modelClass = this.modelClassMap.get(className);
		}

		Model model = null;
		try {
			model = (Model)modelClass.newInstance();

			model.init(this, clazz);
		}
		catch (Exception e) {
			_log.error(
				"getModelObject(" + clazz.getName() + ") ERROR " +
				e.getClass().getName() + ": " + e.getMessage());
			throw new RuntimeException(e);
		}

		return model;
	}

	/**
	 * primaries keys can be at following ways:
	 *
	 * - single => create table
	 * UserGroupGroupRole (userGroupId LONG not null,groupId LONG not
	 * null,roleId LONG not null,primary key (userGroupId, groupId, roleId))";
	 *
	 * - multi => create table JournalArticle (uuid_ VARCHAR(75) null,id_ LONG
	 * not null primary key,resourcePrimKey LONG,groupId LONG,companyId LONG,
	 * userId LONG,userName VARCHAR(75) null,createDate DATE null,modifiedDate
	 * DATE  null,folderId LONG,classNameId LONG,classPK LONG,treePath STRING
	 * null,articleId VARCHAR(75) null,version DOUBLE,title STRING null,urlTitle
	 * VARCHAR(150) null,description TEXT null,content TEXT null,type_
	 * VARCHAR(75) null,structureId VARCHAR(75) null,templateId VARCHAR(75)
	 * null,layoutUuid VARCHAR(75) null,displayDate DATE null,expirationDate
	 * DATE null,reviewDate DATE null,indexable BOOLEAN,smallImage
	 * BOOLEAN,smallImageId LONG,smallImageURL STRING null,status
	 * INTEGER,statusByUserId LONG,statusByUserName VARCHAR(75) null,statusDate
	 * DATE null)
	 */

	@SuppressWarnings("unchecked")
	public final Model getModelObject(String className) {

		Class<? extends ClassedModel> clazz;
		try {
			clazz =
				(Class<? extends ClassedModel>)ModelUtil.getJavaClass(
					javaClasses, classLoader, className);
		}
		catch (ClassNotFoundException e) {
			_log.error("Class not found: " + className);
			throw new RuntimeException(e);
		}

		return getModelObject(clazz);
	}

	protected List<?> executeDynamicQuery(
		Class<? extends ClassedModel> clazz, DynamicQuery dynamicQuery) {

		try {
			Method method = getExecuteDynamicQueryMethod(clazz);

			if (method == null) {
				return null;
			}

			return (List<?>)method.invoke(null, dynamicQuery);
		}
		catch (ClassNotFoundException | NoSuchMethodException e) {
			if (_log.isWarnEnabled()) {
				_log.warn(
					"executeDynamicQuery: dynamicQuery method not found for " +
					clazz.getName() + " - " + e.getMessage());
			}

			try {
				return (List<?>)GroupLocalServiceUtil.dynamicQuery(
					dynamicQuery);
			}
			catch (SystemException se) {
				throw new RuntimeException(
					"executeDynamicQuery: error executing " +
						"GroupLocalServiceUtil.dynamicQuery for " +
						clazz.getName(), se);
			}
		}
		catch (IllegalAccessException | IllegalArgumentException
			| InvocationTargetException e) {

			String cause = "";
			Throwable rootException = e.getCause();

			if (rootException != null) {
				cause = " (root cause: " + rootException.getMessage() + ")";
			}

			throw new RuntimeException(
				"executeDynamicQuery: error invoking dynamicQuery method for " +
					clazz.getName() + cause, e);
		}
	}

	protected ClassedModel fetchObject(
		Class<? extends ClassedModel> clazz, long primaryKey) {

		try {
			Method method = getFetchObjectMethod(clazz);

			if (method == null) {
				return null;
			}

			return (ClassedModel)method.invoke(null, primaryKey);
		}
		catch (NoSuchMethodException | ClassNotFoundException
			| SecurityException e) {

			throw new RuntimeException(
				"fetchObject: fetch" + clazz.getSimpleName() +
				" method not found for " + clazz.getName(), e);
		}
		catch (IllegalAccessException | IllegalArgumentException
			| InvocationTargetException e) {

			String cause = "";
			Throwable rootException = e.getCause();

			if (rootException != null) {
				cause = " (root cause: " + rootException.getMessage() + ")";
			}

			throw new RuntimeException(
				"fetchObject: fetch" + clazz.getSimpleName() + " method for " +
				clazz.getName() + cause, e);
		}
	}

	protected Object[][] getDatabaseAttributesArr(
		Class<? extends ClassedModel> clazz) {

		String liferayModelImpl = ModelUtil.getLiferayModelImplClassName(clazz);
		Class<?> classLiferayModelImpl;
		try {
			classLiferayModelImpl =
				ModelUtil.getJavaClass(
					javaClasses, classLoader, liferayModelImpl);
		}
		catch (ClassNotFoundException e) {
			_log.error("Class not found: " + liferayModelImpl);

			throw new RuntimeException(e);
		}

		if (classLiferayModelImpl == null) {
			_log.error("Class not found: " + liferayModelImpl);

			throw new RuntimeException("Class not found: " + liferayModelImpl);
		}

		Object[][] tableColumns =
			(Object[][])ModelUtil.getLiferayModelImplField(
				classLiferayModelImpl, "TABLE_COLUMNS");

		if (_log.isDebugEnabled()) {
			_log.debug(
				"Database attributes array of " + clazz.getName() +
				": " + Arrays.toString(tableColumns));
		}

		return tableColumns;
	}

	protected String getDatabaseAttributesStr(
		Class<? extends ClassedModel> clazz) {

		String liferayModelImpl = ModelUtil.getLiferayModelImplClassName(clazz);
		Class<?> classLiferayModelImpl;
		try {
			classLiferayModelImpl =
				ModelUtil.getJavaClass(
					javaClasses, classLoader, liferayModelImpl);
		}
		catch (ClassNotFoundException e) {
			_log.error("Class not found: " + liferayModelImpl);
			throw new RuntimeException(e);
		}

		if (classLiferayModelImpl == null) {
			_log.error("Class not found: " + liferayModelImpl);
			throw new RuntimeException("Class not found: " + liferayModelImpl);
		}

		String tableName =
			(String)ModelUtil.getLiferayModelImplField(
				classLiferayModelImpl, "TABLE_NAME");
		String tableSqlCreate =
			(String)ModelUtil.getLiferayModelImplField(
				classLiferayModelImpl, "TABLE_SQL_CREATE");

		int posTableName = tableSqlCreate.indexOf(tableName);

		if (posTableName <= 0) {
			_log.error("Error, TABLE_NAME not found at TABLE_SQL_CREATE");
			return null;
		}

		posTableName = posTableName + tableName.length() + 2;

		String tableAttributes = tableSqlCreate.substring(
			posTableName, tableSqlCreate.length() - 1);

		int posPrimaryKeyMultiAttr = tableAttributes.indexOf(",primary key (");

		if (posPrimaryKeyMultiAttr > 0) {
			tableAttributes = tableAttributes.replaceAll(
				",primary key \\(", "#");
			tableAttributes = tableAttributes.substring(
				0, tableAttributes.length() - 1);
		}

		if (_log.isDebugEnabled()) {
			_log.debug(
				"Database attributes of " + clazz.getName() + ": " +
				tableAttributes);
		}

		return tableAttributes;
	}

	protected Method getExecuteDynamicQueryMethod(
		Class<? extends ClassedModel> clazz)
			throws ClassNotFoundException, NoSuchMethodException,
			SecurityException {

		Method method = null;

		if (executeDynamicQueryMethods.containsKey(clazz.getName())) {
			try {
				method = executeDynamicQueryMethods.get(
					clazz.getName()).getMethod();
			}
			catch (NoSuchMethodException e) {
			}
		}

		if (method == null) {
			String localServiceUtil =
				ModelUtil.getLiferayLocalServiceUtilClassName(clazz);
			Class<?> classLocalServiceUtil =
				ModelUtil.getJavaClass(
					javaClasses, classLoader, localServiceUtil);

			if (localServiceUtil != null) {
				method =
					classLocalServiceUtil.getMethod(
						"dynamicQuery", DynamicQuery.class);
			}

			if (method == null) {
				executeDynamicQueryMethods.put(
					clazz.getName(), new MethodKey());
			}
			else {
				executeDynamicQueryMethods.put(clazz.getName(), new MethodKey(
					method));
			}
		}

		return method;
	}

	protected Method getFetchObjectMethod(Class<? extends ClassedModel> clazz)
		throws ClassNotFoundException, NoSuchMethodException,
		SecurityException {

		Method method = null;

		if (fetchObjectMethods.containsKey(clazz.getName())) {
			try {
				method = fetchObjectMethods.get(clazz.getName()).getMethod();
			}
			catch (NoSuchMethodException e) {
			}
		}

		if (method == null) {
			String localServiceUtil =
				ModelUtil.getLiferayLocalServiceUtilClassName(clazz);
			Class<?> classLocalServiceUtil =
				ModelUtil.getJavaClass(
					javaClasses, classLoader, localServiceUtil);

			if (localServiceUtil != null) {
				method =
					classLocalServiceUtil.getMethod(
						"fetch" + clazz.getSimpleName(), long.class);
			}

			if (method == null) {
				fetchObjectMethods.put(clazz.getName(), new MethodKey());
			}
			else {
				fetchObjectMethods.put(clazz.getName(), new MethodKey(method));
			}
		}

		return method;
	}

	protected DynamicQuery newDynamicQuery(
		Class<? extends ClassedModel> clazz, String alias) {

		return DynamicQueryFactoryUtil.forClass(clazz, alias, classLoader);
	}

	protected ClassLoader classLoader = null;
	protected Class<? extends Model> defaultModelClass = null;
	protected Map<String, Class<? extends Model>> modelClassMap = null;

	private static Log _log = LogFactoryUtil.getLog(ModelFactory.class);

	private Map<String, MethodKey> executeDynamicQueryMethods =
		new ConcurrentHashMap<String, MethodKey>();
	private Map<String, MethodKey> fetchObjectMethods =
		new ConcurrentHashMap<String, MethodKey>();
	private Map<String, Class<?>> javaClasses =
		new ConcurrentHashMap<String, Class<?>>();

}