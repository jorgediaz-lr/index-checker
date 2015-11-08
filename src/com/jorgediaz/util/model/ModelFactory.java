package com.jorgediaz.util.model;

import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.DynamicQueryFactoryUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.MethodKey;
import com.liferay.portal.model.ClassedModel;
import com.liferay.portal.model.Group;

import java.lang.reflect.Method;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
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

	@SuppressWarnings("unchecked")
	public ClassedModel addObject(ClassedModel object) {

		return executeOperation(
			(Class<? extends ClassedModel>)object.getModelClass(), "add",
			object.getModelClass(), object);
	}

	public ClassedModel createObject(
		Class<? extends ClassedModel> clazz, long primaryKey) {

		return executeOperation(clazz, "create", long.class, primaryKey);
	}

	public ClassedModel deleteObject(
		Class<? extends ClassedModel> clazz, long primaryKey) {

		return executeOperation(clazz, "delete", long.class, primaryKey);
	}

	@SuppressWarnings("unchecked")
	public ClassedModel deleteObject(ClassedModel object) {

		return executeOperation(
			(Class<? extends ClassedModel>)object.getModelClass(), "delete",
			object.getModelClass(), object);
	}

	public Map<String, Model> getModelMap(Collection<String> classNames) {

		Map<String, Model> modelMap = new LinkedHashMap<String, Model>();

		for (String classname : classNames) {
			Model model = null;
			String[] attributes = null;
			try {
				model = this.getModelObject(classname);

				if (model != null) {
					attributes = model.getAttributesName();
				}
			}
			catch (Exception e) {
				if (_log.isInfoEnabled()) {
					_log.info(
						"Cannot get model object of " + classname +
						" EXCEPTION: " + e.getClass().getName() + ": " +
						e.getMessage());
				}
			}

			if ((model != null) && (attributes != null)) {
				modelMap.put(model.getName(), model);
			}
		}

		return modelMap;
	}

	public Model getModelObject(Class<? extends ClassedModel> clazz) {

		if ((clazz == null) || !ClassedModel.class.isAssignableFrom(clazz)) {
			if (_log.isDebugEnabled()) {
				_log.debug(
					"Class: " + clazz.getName() + "is null or does not "+
					"implements ClassedModel, returning null");
			}

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

	@SuppressWarnings("unchecked")
	public ClassedModel updateObject(ClassedModel object) {

		return executeOperation(
			(Class<? extends ClassedModel>)object.getModelClass(), "update",
			object.getModelClass(), object);
	}

	protected List<?> executeDynamicQuery(
		Class<? extends ClassedModel> clazz, DynamicQuery dynamicQuery) {

		try {
			Method method = getLocalServiceUtilMethod(
				clazz, "dynamicQuery", DynamicQuery.class);

			if (method == null) {
				return null;
			}

			return (List<?>)method.invoke(null, dynamicQuery);
		}
		catch (Exception e) {
			if (!clazz.equals(Group.class) &&
				(e instanceof ClassNotFoundException ||
				 e instanceof NoSuchMethodException)) {

				if (_log.isWarnEnabled()) {
					_log.warn(
						"executeDynamicQuery: dynamicQuery method not found " +
						"for " + clazz.getName() + " - " + e.getMessage() +
						" trying with GroupLocalServiceUtil.dynamicQuery");
				}

				return executeDynamicQuery(Group.class, dynamicQuery);
			}

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

	protected ClassedModel executeOperation(
		Class<? extends ClassedModel> clazz, String operation,
		Class<?> parameterType, Object arg) {

		try {
			Method method = getLocalServiceUtilMethod(
				clazz, operation + clazz.getSimpleName(), parameterType);

			if (method == null) {
				return null;
			}

			return (ClassedModel)method.invoke(null, arg);
		}
		catch (ClassNotFoundException e) {
			throw new RuntimeException(
				"executeOperation: class not found exception calling " +
				operation + clazz.getSimpleName() + " for " + clazz.getName(),
				e);
		}
		catch (NoSuchMethodException e) {
			throw new RuntimeException(
				"executeOperation: " + operation + clazz.getSimpleName() +
				" method not found for " + clazz.getName(), e);
		}
		catch (Exception e) {
			String cause = "";
			Throwable rootException = e.getCause();

			if (rootException != null) {
				cause = " (root cause: " + rootException.getMessage() + ")";
			}

			throw new RuntimeException(
				"executeOperation: " + operation + clazz.getSimpleName() +
				" method for " + clazz.getName() + cause, e);
		}
	}

	protected ClassedModel fetchObject(
		Class<? extends ClassedModel> clazz, long primaryKey) {

		return executeOperation(clazz, "fetch", long.class, primaryKey);
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

	protected Method getLocalServiceUtilMethod(
		Class<? extends ClassedModel> clazz, String methodName,
		Class <?> parameterType)
			throws ClassNotFoundException, NoSuchMethodException,
			SecurityException {

		String key =
			clazz.getName() + "#" + methodName + "#" + parameterType.getName();

		Method method = null;

		if (localServiceUtilMethods.containsKey(key)) {
			try {
				method = localServiceUtilMethods.get(key).getMethod();
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
				method = classLocalServiceUtil.getMethod(
					methodName, parameterType);
			}

			if (method == null) {
				localServiceUtilMethods.put(key, new MethodKey());
			}
			else {
				localServiceUtilMethods.put(key, new MethodKey(method));
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

	private Map<String, Class<?>> javaClasses =
		new ConcurrentHashMap<String, Class<?>>();
	private Map<String, MethodKey> localServiceUtilMethods =
		new ConcurrentHashMap<String, MethodKey>();

}