package com.jorgediaz.indexchecker.model;

import com.jorgediaz.indexchecker.data.Data;
import com.jorgediaz.util.model.Model;
import com.jorgediaz.util.model.ModelFactory;
import com.jorgediaz.util.model.ModelImpl;
import com.liferay.portal.kernel.dao.orm.Conjunction;
import com.liferay.portal.kernel.dao.orm.Criterion;
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.ProjectionFactoryUtil;
import com.liferay.portal.kernel.dao.orm.ProjectionList;
import com.liferay.portal.kernel.dao.orm.RestrictionsFactoryUtil;
import com.liferay.portal.kernel.search.SearchException;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portal.model.AuditedModel;
import com.liferay.portal.model.ClassedModel;
import com.liferay.portal.model.GroupedModel;
import com.liferay.portal.model.ResourcedModel;
import com.liferay.portal.model.StagedModel;
import com.liferay.portal.model.WorkflowedModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

public abstract class IndexCheckerModel extends ModelImpl {


	public static Map<Class<?>, String[]> modelInterfaceAttributesMap = new HashMap<Class<?>, String[]>();
	
	static {
		String[] auditedModelAttributes = new String[] { "companyId", "createDate", "modifiedDate"};
		String[] groupedModelAttributes = new String[] { "groupId" };
		String[] resourcedModelAttributes = new String[] { "resourcePrimKey" };
		String[] stagedModelAttributes = new String[] { "companyId", "createDate", "modifiedDate" };
		String[] workflowModelAttributes = new String[] { "status" };

		modelInterfaceAttributesMap.put(AuditedModel.class, auditedModelAttributes);
		modelInterfaceAttributesMap.put(GroupedModel.class, groupedModelAttributes);
		modelInterfaceAttributesMap.put(ResourcedModel.class, resourcedModelAttributes);
		modelInterfaceAttributesMap.put(StagedModel.class, stagedModelAttributes);
		modelInterfaceAttributesMap.put(WorkflowedModel.class, workflowModelAttributes);
	}

	@Override
	public void init(ModelFactory modelFactory, Class<? extends ClassedModel> modelClass) throws Exception {

		super.init(modelFactory, modelClass);

		this.indexedAttributes = new ArrayList<String>();

		String primaryKey =  this.getPrimaryKeyAttribute();

		this.setIndexPrimaryKey(primaryKey);

		if(primaryKey == null || primaryKey.isEmpty()) {
			throw new RuntimeException("Missing primary key!!");
		}

		if(this.hasAttribute("companyId")) {
			this.addIndexedAttribute("companyId");
		}

		for(Class<?> modelInterface : modelInterfaceAttributesMap.keySet()) {
			if(this.modelExtendsClass(modelInterface)) {
				String[] modelInterfaceAttributes = modelInterfaceAttributesMap.get(modelInterface);
				for(int i = 0; i<modelInterfaceAttributes.length; i++)
				{
					this.addIndexedAttribute((modelInterfaceAttributes[i]));
				}
			}
		}

		this.setFilter(this.generateQueryFilter());
	}

	public String toString() {
		String toString = this.modelClass.getSimpleName()+":";
		for(String attr : this.indexedAttributes) {
			toString += " " + attr;
		}
		return toString;
	}

	public void reindex(Data value) throws SearchException {
		getIndexer().reindex(getClassName(), value.getPrimaryKey());
	}

	public void delete(Data value) throws SearchException {
		getIndexer().delete(value.getCompanyId(), value.getUid());
	}

	@Override
	public int[] getValidStatuses() {
		if (!this.modelExtendsClass(WorkflowedModel.class)) {
			return null;
		}
	
		int[] statuses = {
				WorkflowConstants.STATUS_APPROVED,
				WorkflowConstants.STATUS_IN_TRASH
			};
		return statuses;
	}

	public List<String> getIndexAttributes() {
		return indexedAttributes;
	}

	public Map<Long,Data> getLiferayData(Criterion filter) throws Exception {

		Map<Long,Data> dataMap = new HashMap<Long,Data>();

		DynamicQuery query = newDynamicQuery();

		ProjectionList projectionList = ProjectionFactoryUtil.projectionList();

		for (String attrib : indexedAttributes) {
			projectionList.add(ProjectionFactoryUtil.property(attrib));
		}

		query.setProjection(ProjectionFactoryUtil.distinct(projectionList));

		query.add(filter);
	
		@SuppressWarnings("unchecked")
		List<Object[]> results = (List<Object[]>) executeDynamicQuery(query);
	
		for(Object[] result : results) {
			Data data = new Data(this);
			data.init(result);
			dataMap.put(data.getPrimaryKey(), data);
		}
		
		return dataMap;
	
	}

	public Conjunction generateQueryFilter() {
		return RestrictionsFactoryUtil.conjunction();
	}

	protected void addIndexedAttribute(String col) {
		if(!indexedAttributes.contains(col)) {
			indexedAttributes.add(col);
		}
	}

	protected void removeIndexedAttribute(String col) {
		while (indexedAttributes.contains(col)) {
			indexedAttributes.remove(col);
		}
	}

	protected void setIndexPrimaryKey(String col) {
		if(indexedAttributes.contains(col)) {
			indexedAttributes.remove(col);
		}
		indexedAttributes.add(0, col);
	}

	@Override
	public Model clone() {
		IndexCheckerModel model;
		try {
			model = (IndexCheckerModel) super.clone();
			model.indexedAttributes = ListUtil.copy(this.indexedAttributes);
		}
		catch (Exception e) {
			System.err.println("Error executing clone");
			throw new RuntimeException(e);
		}
		return model;
	}

	private List<String> indexedAttributes = null;

}
