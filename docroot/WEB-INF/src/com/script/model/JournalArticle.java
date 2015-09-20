package com.script.model;

import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.search.SearchException;
import com.liferay.portal.kernel.util.PrefsPropsUtil;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.script.data.Data;


public class JournalArticle extends BaseModel {

	protected boolean indexAllVersions;

	public void init(String fullClassName, String tableName, String[] arrAttributes) {
		super.init(fullClassName, tableName, arrAttributes);

		conditions.remove("indexable");
		conditions.remove("status");

		try {
			indexAllVersions = PrefsPropsUtil.getBoolean("journal.articles.index.all.versions");
		} catch (SystemException e) {
			e.printStackTrace();

			throw new RuntimeException(e);
		}

		if(!indexAllVersions) {
			this.primaryKey = "resourcePrimKey";
		}
	}

	public void reindex(Data value) throws SearchException {
		if(indexAllVersions) {
			super.reindex(value);
		}
		else {
			indexer.reindex(fullClassName, value.resourcePrimaryKey);
		}
	}

	public String getSQLWhere() {
		if(indexAllVersions) {
			// Revisar esto de indexable:
			//return super.getSQLWhere() + " and classnameid = 0 and indexable = [$TRUE$]";
			return super.getSQLWhere() + " and classnameid = 0";
		}
		else {
			return super.getSQLWhere() + " and classnameid = 0 and status in (" + WorkflowConstants.STATUS_APPROVED + 
					"," + WorkflowConstants.STATUS_IN_TRASH + ")";
		}
	}
}