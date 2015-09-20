package com.jorgediaz.indexchecker.model;

import com.jorgediaz.indexchecker.data.Data;
import com.liferay.portal.kernel.dao.jdbc.DataAccess;
import com.liferay.portal.kernel.search.Indexer;
import com.liferay.portal.kernel.search.SearchException;
import com.liferay.portal.util.PortalUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/*
 
http://localhost:7080/group/control_panel?refererPlid=10175&p_p_id=indexchecker_WAR_index_checkerportlet&outputBothExact=true&outputBothNotExact=true&outputLiferay=true&outputIndex=true&outputGroupBySite=true

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

		public static Set<String> defaultAttributes =
				new HashSet<String>(Arrays.asList("createDate", "modifiedDate","status","resourcePrimKey","companyId","groupId"));

		public void init(String fullClassName, String tableName, String[] arrAttributes) {

			this.fullClassName = fullClassName;
			this.name = tableName;
			this.tableName = tableName;
			this.attributes = new HashSet<String>();
			this.conditions = new HashSet<String>();

			if(arrAttributes == null) {
				arrAttributes = new String[0];
				return;
			}

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
				else if(defaultAttributes.contains(col)) {
					attributes.add(col);
				}
			}
		}

		public long getCompanyId() {
			return companyId;
		}

		public void setCompanyId(long companyId) {
			this.companyId = companyId;
		}

		public Indexer getIndexer() {
			return indexer;
		}

		public void setIndexer(Indexer indexer) {
			this.indexer = indexer;
		}

		public void reindex(Data value) throws SearchException {
			this.indexer.reindex(fullClassName, value.getPrimaryKey());
		}

		public void delete(Data value) throws SearchException {
			this.indexer.delete(value.getCompanyId(), value.getUid());
		}

		protected String getSQLWhere() {
			return "";
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

		public String getSQLOfObjectType(Long companyId, Long groupId) {
			String attr = this.getSQLAttributes();
			
			String where = " t where ";
			if(attributes.contains("companyId") && companyId != null) {
				where += "t.companyId  = " + companyId + " ";
			}
			if(attributes.contains("groupId") && groupId == null) {
				where += "and t.groupid in (select groupid from group_) ";
			}
			else if(attributes.contains("groupId") && groupId != null) {
				where += "and t.groupid in (select groupid from group_ where groupId = " + groupId + ") ";
			}
			where += this.getSQLWhere();

			return "select " + attr + " from " + this.tableName + where;
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

		public Set<String> getAttributes() {
			return attributes;
		}

		public String getPrimaryKey() {
			return primaryKey;
		}

		public Set<Data> getLiferayData() throws SQLException {
			return getLiferayData(null);
		}
		public Set<Data> getLiferayData(Long groupId) throws SQLException {
		
			Set<Data> dataSet = new HashSet<Data>();
			
			Connection con = null;
			PreparedStatement ps = null;
			ResultSet rs = null;
			
			try {
				con = DataAccess.getConnection();
				
				String sql = getSQLOfObjectType(companyId, groupId);
		
				sql = PortalUtil.transformSQL(sql);
		
				System.out.println("SQL: "+ sql);
			
				ps = con.prepareStatement(sql);
			
				rs = ps.executeQuery();
			
				while (rs.next()) {

					Data data = new Data(this);
					data.init(rs);
					dataSet.add(data);
			
				}
			
			}
			finally {
				DataAccess.cleanUp(con, ps, rs);
			}
			
			return dataSet;
		
		}

		protected long companyId = 0;
		protected Indexer indexer = null;
		protected Set<String> attributes = null;
		protected Set<String> conditions = null;
		protected String fullClassName = null;
		protected String name = null;
		protected String tableName = null;
		protected String primaryKey = null;
		protected boolean indexedModel = true;
	}