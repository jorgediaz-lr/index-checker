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

package jorgediazest.util.service;

import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.ClassedModel;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.SAXReaderUtil;

import java.io.InputStream;

import java.util.ArrayList;
import java.util.List;

import jorgediazest.util.reflection.ReflectionUtil;
import jorgediazest.util.table.TableInfo;

/**
 * @author Jorge Díaz
 */
public abstract class ServiceImpl implements Service {

	public ClassedModel addObject(ClassedModel object) {
		throw new UnsupportedOperationException();
	}

	public ClassedModel createObject(long primaryKey) {
		throw new UnsupportedOperationException();
	}

	public ClassedModel deleteObject(ClassedModel object) {
		throw new UnsupportedOperationException();
	}

	public ClassedModel deleteObject(long primaryKey) {
		throw new UnsupportedOperationException();
	}

	public ClassedModel fetchObject(long primaryKey) {
		throw new UnsupportedOperationException();
	}

	public abstract ClassLoader getClassLoader();

	public String getClassName() {
		return className;
	}

	public String getClassSimpleName() {
		return classSimpleName;
	}

	public Class<?> getLiferayModelImplClass() {
		if (liferayModelImplClassIsNull) {
			return null;
		}

		if (liferayModelImplClass == null) {
			String liferayModelImpl = ServiceUtil.getLiferayModelImplClassName(
				this);

			if (liferayModelImpl == null) {
				liferayModelImplClassIsNull = true;

				return null;
			}

			liferayModelImplClass = ServiceUtil.getLiferayModelImplClass(
				getClassLoader(), liferayModelImpl);

			if (liferayModelImplClass == null) {
				liferayModelImplClassIsNull = true;

				return null;
			}
		}

		return liferayModelImplClass;
	}

	public List<String> getMappingTables() {
		List<String> mappingTablesFields =
			ReflectionUtil.getLiferayModelImplMappingTablesFields(
				getLiferayModelImplClass());

		List<String> mappingTables = new ArrayList<>(
			mappingTablesFields.size());

		for (String mappingTablesField : mappingTablesFields) {
			String mappingTable = StringUtil.replaceFirst(
				mappingTablesField, "MAPPING_TABLE_", StringPool.BLANK);

			mappingTable = StringUtil.replace(
				mappingTable, "_NAME", StringPool.BLANK);

			mappingTable = StringUtil.toLowerCase(mappingTable);

			mappingTables.add(mappingTable);
		}

		return mappingTables;
	}

	public TableInfo getTableInfo() {
		return getTableInfo(null);
	}

	public TableInfo getTableInfo(String mappingTable) {
		Class<?> classLiferayModelImpl = getLiferayModelImplClass();

		String fieldPrefix = "TABLE";

		if (mappingTable != null) {
			fieldPrefix =
				"MAPPING_TABLE_" + StringUtil.toUpperCase(mappingTable);
		}

		Object[][] attributesArr =
			(Object[][])ReflectionUtil.getStaticFieldValue(
				classLiferayModelImpl, fieldPrefix + "_COLUMNS");
		String name = (String)ReflectionUtil.getStaticFieldValue(
			classLiferayModelImpl, fieldPrefix + "_NAME");
		String sqlCreate = (String)ReflectionUtil.getStaticFieldValue(
			classLiferayModelImpl, fieldPrefix + "_SQL_CREATE");

		if (mappingTable == null) {
			return new TableInfo(
				name, attributesArr, sqlCreate,
				getTableInfoFromHbmXml(classLiferayModelImpl));
		}

		return new TableInfo(name, attributesArr, sqlCreate, null);
	}

	public ClassedModel updateObject(ClassedModel object) {
		throw new UnsupportedOperationException();
	}

	protected Element getTableInfoFromHbmXml(Class<?> classLiferayModelImpl) {
		ClassLoader classLoader = classLiferayModelImpl.getClassLoader();

		InputStream inputStream = classLoader.getResourceAsStream(
			"/META-INF/module-hbm.xml");

		if (inputStream == null) {
			inputStream = classLoader.getResourceAsStream(
				"/META-INF/portal-hbm.xml");
		}

		Document document;

		try {
			document = SAXReaderUtil.read(inputStream);
		}
		catch (DocumentException e) {
			_log.error(e, e);

			return null;
		}

		String hibernateClassImpl = classLiferayModelImpl.getName();

		hibernateClassImpl = StringUtil.replaceLast(
			hibernateClassImpl, "ModelImpl", "Impl");

		Element rootElement = document.getRootElement();

		for (Element element : rootElement.elements("class")) {
			if (hibernateClassImpl.equals(element.attributeValue("name"))) {
				return element;
			};
		}

		return null;
	}

	protected String className = null;
	protected String classSimpleName = null;
	protected Class<?> liferayModelImplClass = null;
	protected boolean liferayModelImplClassIsNull = false;

	private static Log _log = LogFactoryUtil.getLog(ServiceImpl.class);

}