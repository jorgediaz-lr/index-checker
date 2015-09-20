package com.jorgediaz.util.model;

import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.search.Indexer;
import com.liferay.portal.kernel.search.IndexerRegistryUtil;
import com.liferay.portal.model.ClassedModel;
import com.liferay.portal.model.GroupedModel;

import java.util.List;

	public abstract class ModelImpl implements Model {

		public void init(ModelFactory modelFactory, Class<? extends ClassedModel> modelClass) throws Exception {
			this.modelFactory = modelFactory;
			this.modelClass = modelClass;
		}

		public String toString() {
			return getFullClassName();
		}

		public String getFullClassName() {
			return modelClass.getCanonicalName();
		}

		String attributes = null;

		public String getAttributes() {/* TODO ES necesario tratar las primary key multi-columnas */
			if(attributes == null) {
				attributes = modelFactory.getDatabaseAttributes(modelClass);
			}
			return attributes;
		}

		public String[] getAttributesArray() {/* TODO ES necesario tratar las primary key multi-columnas */
			String[] arrDatabaseAttributes = getAttributes().split(",");
			String[] result = new String[arrDatabaseAttributes.length];
			int i=0;
			for(String attr : arrDatabaseAttributes) {
				String[] aux = attr.split(" ");
				if (aux.length<2) {
					continue;
				}
				String col = aux[0];
				if(col.endsWith("_")) {
					col = col.substring(0, col.length()-1);
				}
				result[i++] = col;
			}
			return result;
		}


		String primaryKeyAttribute = null;

		public String getPrimaryKeyAttribute() {/* TODO ES necesario tratar las primary key multi-columnas */
			if(primaryKeyAttribute == null) {
				String[] arrDatabaseAttributes = getAttributes().split(",");
				for(String attr : arrDatabaseAttributes) {
					String[] aux = attr.split(" ");
					if (aux.length<2) {
						continue;
					}
					String col = aux[0];
					if(col.endsWith("_")) {
						col = col.substring(0, col.length()-1);
					}
					if(attr.endsWith("not null primary key")) {
						primaryKeyAttribute=col;
					}
				}
			}
			return primaryKeyAttribute;
		}

		public boolean hasGroupId() {
			return this.modelExtendsClass(GroupedModel.class);
		}

		public DynamicQuery newDynamicQuery() {
			return modelFactory.newDynamicQuery(this.modelClass);
		}

		public DynamicQuery newDynamicQuery(Class<? extends ClassedModel> clazz) {
			return modelFactory.newDynamicQuery(clazz);
		}

		public List<?> executeDynamicQuery(DynamicQuery dynamicQuery) throws Exception {
			return modelFactory.executeDynamicQuery(this.modelClass, dynamicQuery);
		}

		public List<?> executeDynamicQuery(Class<? extends ClassedModel> clazz, DynamicQuery dynamicQuery) throws Exception {
			return modelFactory.executeDynamicQuery(clazz, dynamicQuery);
		}

		public Class<?> getModelClass() {
			return modelClass;
		}

		public boolean modelExtendsClass(Class<?> clazz) {
			return clazz.isAssignableFrom(this.modelClass);
		}

		public boolean hasIndexer() {
			return (IndexerRegistryUtil.getIndexer(modelClass) != null);
		}

		public Indexer getIndexer() {
			return IndexerRegistryUtil.nullSafeGetIndexer(modelClass);
		}

		protected ModelFactory modelFactory = null;
		protected Class<? extends ClassedModel> modelClass = null;
	}