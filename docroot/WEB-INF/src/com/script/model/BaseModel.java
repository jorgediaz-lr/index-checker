package com.script.model;

import com.liferay.portal.kernel.dao.jdbc.DataAccess;
import com.liferay.portal.kernel.search.Indexer;
import com.liferay.portal.kernel.search.SearchException;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portal.model.Company;
import com.liferay.portal.util.PortalUtil;
import com.script.data.Data;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

/*

com.liferay.portal.model.Contact			NO => user.isDefaultUser() || (user.getStatus() != WorkflowConstants.STATUS_APPROVED)
com.liferay.portal.model.Organization
com.liferay.portal.model.User				NO => user.isDefaultUser()
com.liferay.portal.model.UserGroup
com.liferay.portlet.journal.model.JournalArticle	NO => !article.isIndexable() || (PortalUtil.getClassNameId(DDMStructure.class) == article.getClassNameId())
com.liferay.portlet.journal.model.JournalFolder
com.liferay.portlet.dynamicdatalists.model.DDLRecord	NO => !recordVersion.isApproved()
com.liferay.portlet.messageboards.model.MBMessage		NO => (!message.isApproved() && !message.isInTrash()) OR (message.isDiscussion() && message.isRoot())
com.liferay.portlet.messageboards.model.MBThread
com.liferay.portlet.bookmarks.model.BookmarksEntry
com.liferay.portlet.bookmarks.model.BookmarksFolder		NO => (!folder.isApproved() && !folder.isInTrash())
com.liferay.portlet.blogs.model.BlogsEntry				NO => (!entry.isApproved() && !entry.isInTrash())
com.liferay.portlet.wiki.model.WikiNode
com.liferay.portlet.wiki.model.WikiPage					NO => (!page.isHead() || (!page.isApproved() && !page.isInTrash())) OR (Validator.isNotNull(page.getRedirectTitle()))
com.liferay.portlet.documentlibrary.model.DLFileEntry	NO => (!dlFileVersion.isApproved() && !dlFileEntry.isInTrash())
com.liferay.portlet.documentlibrary.model.DLFolder		NO => (!dlFolder.isApproved() && !dlFolder.isInTrash())

Contact_ t where t.companyId  = ?  and userId not in (select userId from User_ where defaultuser = true or status != 0)
Organization_ t where t.companyId  = ? 
User_ t where t.companyId  = ? and defaultuser = false
UserGroup t where t.companyId  = ? 
JournalArticle t where t.companyId  = ? and indexable = true and classnameid = 0
JournalFolder t where t.companyId  = ?  
DDLRecordVersion v, DDLRecord t where t.companyId  = ?  and t.recordId = v.recordId and t.version = v.version and v.status = 0
MBMessage t where t.companyId  = ?  and (status = 8 or  status = 0) and not (categoryid = -1 and parentmessageid = 0)
MBThread t where t.companyId  = ?  
BookmarksEntry t where t.companyId  = ?  
BookmarksFolder t where t.companyId  = ?   and (status = 8 or  status = 0)
BlogsEntry t where t.companyId  = ?   and (status = 8 or  status = 0)
WikiNode t where t.companyId  = ? 
WikiPage t where t.companyId  = ?  and head = true and (status = 8 or status = 0) and redirecttitle = ''
DLFileVersion v, DLFileEntry t where t.companyId  = ?  and (status = 8 or  status = 0) and t.fileentryid = v.fileentryid and t.version = v.version
DLFolder t where t.companyId  = ?  and (status = 8 or status = 0)

	 */
	public abstract class BaseModel {

		public void init(String fullClassName, String tableName, String[] arrAttributes) {

			this.fullClassName = fullClassName;
			this.name = tableName;
			this.tableName = tableName;
			this.attributes = new HashSet<String>();
			this.conditions = new HashSet<String>();

			for(String attr : arrAttributes) {
				String[] aux = attr.split(" ");
				if (aux.length<2) {
					continue;
				}
				String col = aux[0];
				if(attr.endsWith("not null primary key")) {
					this.primaryKey=col;
					attributes.add(col);
				}
				else if("createDate".equals(col)) {
					attributes.add(col); /* AuditedModel */
				}
				else if("modifiedDate".equals(col)) {
					attributes.add(col); /* AuditedModel */
				}
				else if("status".equals(col)) {
					attributes.add(col); /* WorkflowedModel */
					conditions.add(col);
				}
				else if("indexable".equals(col)) {
					conditions.add(col); /* ? */
				}
				else if("head".equals(col)) {
					conditions.add(col); /* ? */
				}
				else if("resourcePrimKey".equals(col)) {
					attributes.add(col); /* ResourcedModel */
				}
				else if("companyId".equals(col)) {
					attributes.add(col); /* ? */
				}
				else if("groupId".equals(col)) {
					attributes.add(col); /* ? */
				}
			}
		}

		public Indexer getIndexer() {
			return indexer;
		}

		public void setIndexer(Indexer indexer) {
			this.indexer = indexer;
		}

		public void reindex(Data value) throws SearchException {
			this.indexer.reindex(fullClassName, value.primaryKey);
		}

		public void delete(Data value) throws SearchException {
			this.indexer.delete(value.companyId, value.uid);
		}

		protected String getSQLWhere() {
			String where = "";

			if(conditions.contains("indexable")) {
				where = where + " and indexable = [$TRUE$]";
			}
			if(conditions.contains("head")) {
				where = where + " and head = [$TRUE$]";
			}
			if(conditions.contains("status")) {
				where = where + " and status in (" + WorkflowConstants.STATUS_APPROVED + "," + WorkflowConstants.STATUS_IN_TRASH + ")";
			}
			return where;
		}

		public String getSQLAttributes() {
			String selectAttr = "";
			for(String attrib : attributes) {
				if(selectAttr.length() > 0) {
					selectAttr += ",";
				}
				selectAttr = selectAttr + "t."+attrib;
			}
			return selectAttr;
		}

		public String getSQLOfObjectType(Long groupId) {
			String attr = this.getSQLAttributes();
			
			String where = " t where ";
			if(attributes.contains("companyId")) {
				where += "t.companyId  = ? ";
			}
			if(attributes.contains("groupId") && groupId == null) {
				where += "and t.groupid in (select groupid from group_) ";
			}
			else if(attributes.contains("groupId") && groupId != null) {
				where += "and t.groupid in (select groupid from group_ where groupId = "+groupId+") ";
			}
			where += this.getSQLWhere();

			return "select " + attr + " from " + this.tableName + where;
		}

		public Data getDataResult(ResultSet rs) throws SQLException {

			Data data = new Data();
			data.primaryKey = rs.getLong(this.primaryKey);
			if(attributes.contains("resourcePrimKey")) {
				data.resourcePrimaryKey = rs.getLong("resourcePrimKey");
			}
			data.entryClassName = this.fullClassName;
			if(attributes.contains("createDate")) {
				data.createDate = rs.getString("createDate");
			}
			if(attributes.contains("modifiedDate")) {
				data.modifyDate = rs.getString("modifiedDate");
			}
			if(attributes.contains("status")) {
				data.status = rs.getLong("status");
			}
			if(attributes.contains("companyId")) {
				data.companyId = rs.getLong("companyId");
			}
			if(attributes.contains("groupId")) {
				data.groupId = rs.getLong("groupId");
			}
			return data;
		}

		public String toString() {
			String toString = name+": "+primaryKey;
			for(String attr : this.attributes) {
				toString += "," + attr;
			}
			for(String cond : this.conditions) {
				toString += "," + cond;
			}
			return toString;
		}

		public String getFullClassName() {
			return fullClassName;
		}

		public boolean hasGroupId() {
			return attributes.contains("groupId");
		}

		public boolean isIndexedModel() {
			return indexedModel;
		}

		public Set<Data> getLiferayData(PrintWriter out, Long companyId, Long groupId) throws SQLException {
		
			Set<Data> dataSet = new HashSet<Data>();
			
			Connection con = null;
			PreparedStatement ps = null;
			ResultSet rs = null;
			
			try {
				con = DataAccess.getConnection();
				
				String sql = getSQLOfObjectType(groupId);
		
				sql = PortalUtil.transformSQL(sql);
		
				out.println("SQL: "+ sql);
			
				ps = con.prepareStatement(sql);
			
				ps.setLong(1, companyId);
			
				rs = ps.executeQuery();
			
				while (rs.next()) {
			
					dataSet.add(getDataResult(rs));
			
				}
			
			}
			finally {
				DataAccess.cleanUp(con, ps, rs);
			}
			
			return dataSet;
		
		}

		protected Indexer indexer = null;
		protected Set<String> attributes = null;
		protected Set<String> conditions = null;
		protected String fullClassName = null;
		protected String name = null;
		protected String tableName = null;
		protected String primaryKey = null;
		protected boolean indexedModel = true;
	}