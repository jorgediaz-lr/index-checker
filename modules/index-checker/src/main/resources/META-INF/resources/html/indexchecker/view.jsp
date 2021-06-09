<%--
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
--%>

<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/sql" prefix="sql" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/xml" prefix="x" %>

<%@ taglib uri="http://liferay.com/tld/aui" prefix="aui" %>
<%@ taglib uri="http://liferay.com/tld/portlet" prefix="liferay-portlet" %>
<%@ taglib uri="http://liferay.com/tld/security" prefix="liferay-security" %>
<%@ taglib uri="http://liferay.com/tld/theme" prefix="liferay-theme" %>
<%@ taglib uri="http://liferay.com/tld/ui" prefix="liferay-ui" %>
<%@ taglib uri="http://liferay.com/tld/util" prefix="liferay-util" %>

<%@ page contentType="text/html; charset=UTF-8" %>

<%@ page import="com.liferay.portal.kernel.dao.search.SearchContainer" %>
<%@ page import="com.liferay.portal.kernel.log.Log" %>
<%@ page import="com.liferay.portal.kernel.model.Company" %>
<%@ page import="com.liferay.portal.kernel.theme.PortletDisplay" %>
<%@ page import="com.liferay.portal.kernel.theme.ThemeDisplay" %>
<%@ page import="com.liferay.portal.kernel.util.GetterUtil" %>
<%@ page import="com.liferay.portal.kernel.util.ReleaseInfo" %>
<%@ page import="com.liferay.portal.kernel.util.Validator" %>
<%@ page import="com.liferay.portal.kernel.util.WebKeys" %>

<%@ page import="java.util.EnumSet" %>
<%@ page import="java.util.HashSet" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Locale" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Map.Entry" %>
<%@ page import="java.util.Set" %>

<%@ page import="javax.portlet.PortletURL" %>

<%@ page import="jorgediazest.indexchecker.ExecutionMode" %>
<%@ page import="jorgediazest.indexchecker.output.IndexCheckerOutput" %>
<%@ page import="jorgediazest.indexchecker.portlet.IndexCheckerPortlet" %>

<%@ page import="jorgediazest.util.data.Comparison" %>
<%@ page import="jorgediazest.util.model.Model" %>

<portlet:defineObjects />

<portlet:renderURL var="viewURL" />

<%
	ThemeDisplay themeDisplay = (ThemeDisplay) request.getAttribute(WebKeys.THEME_DISPLAY);

	PortletDisplay portletDisplay = themeDisplay.getPortletDisplay();

	String configurationURL = portletDisplay.getURLConfigurationJS();
%>

<portlet:actionURL name="executeCheck" var="executeCheckURL" windowState="normal" />
<portlet:actionURL name="executeReindex" var="executeReindexURL" windowState="normal" />
<portlet:actionURL name="executeRemoveOrphans" var="executeRemoveOrphansURL" windowState="normal" />

<script type="text/javascript">
	function showHide(shID) {
		if (document.getElementById(shID)) {
			if (document.getElementById(shID+'-show').style.display != 'none') {
				document.getElementById(shID+'-show').style.display = 'none';
				document.getElementById(shID).style.display = 'block';
			}
			else {
				document.getElementById(shID+'-show').style.display = 'inline';
				document.getElementById(shID).style.display = 'none';
			}
		}
	}
</script>

<div class="container-fluid-1280"><div class="card-horizontal main-content-card"><div class="panel-body">

<%
	Log _log = IndexCheckerPortlet.getLogger();
	EnumSet<ExecutionMode> executionMode = (EnumSet<ExecutionMode>) request.getAttribute("executionMode");
	Map<Company, Long> companyProcessTime = (Map<Company, Long>) request.getAttribute("companyProcessTime");
	Map<Company, Map<Long, List<Comparison>>> companyResultDataMap = (Map<Company, Map<Long, List<Comparison>>>) request.getAttribute("companyResultDataMap");
	Map<Company, String> companyError = (Map<Company, String>) request.getAttribute("companyError");
	List<Model> modelList = (List<Model>) request.getAttribute("modelList");
	Set<String> filterClassNameSelected = (Set<String>) request.getAttribute("filterClassNameSelected");
	if (filterClassNameSelected == null) {
		filterClassNameSelected = new HashSet<String>();
	}
	List<Long> groupIdList = (List<Long>) request.getAttribute("groupIdList");
	List<String> groupDescriptionList = (List<String>) request.getAttribute("groupDescriptionList");
	Set<String> filterGroupIdSelected = (Set<String>) request.getAttribute("filterGroupIdSelected");
	if (filterGroupIdSelected == null) {
		filterGroupIdSelected = new HashSet<String>();
	}
	Long filterModifiedDate = GetterUtil.getLong(request.getAttribute("filterModifiedDate"));
	Locale locale = renderRequest.getLocale();
	String updateMessage = (String) request.getAttribute("updateMessage");
%>

<%
	if (Validator.isNotNull(updateMessage)) {
%>

<div class="alert alert-warning"><%= updateMessage %></div>

<%
	}
%>

<aui:form action="<%= executeCheckURL %>" method="POST" name="fm">
	<aui:row>
		<aui:col width="33">
			<aui:input helpMessage="output-both-exact-help" name="outputBothExact" onClick='<%= renderResponse.getNamespace() + "disableReindexAndRemoveOrphansButtons(this);" %>' type="checkbox" value="false" />
			<aui:input helpMessage="output-both-not-exact-help" name="outputBothNotExact" onClick='<%= renderResponse.getNamespace() + "disableReindexAndRemoveOrphansButtons(this);" %>' type="checkbox" value="true" />
			<aui:input helpMessage="output-liferay-help" name="outputLiferay" onClick='<%= renderResponse.getNamespace() + "disableReindexAndRemoveOrphansButtons(this);" %>' type="checkbox" value="true" />
			<aui:input disabled="<%= (filterModifiedDate > 0) %>" helpMessage="output-index-help" name="outputIndex" onClick='<%= renderResponse.getNamespace() + "disableReindexAndRemoveOrphansButtons(this);" %>' type="checkbox" value="false" />
			<aui:fieldset>
				<aui:select helpMessage="filter-modified-date-help"  inlineLabel="left" name="filterModifiedDate" onClick='<%= renderResponse.getNamespace() + "disableReindexAndRemoveOrphansButtons(this);" + renderResponse.getNamespace() + "disableOutputOnlyInIndex(this);" %>' >
					<aui:option selected="true" value="0"><liferay-ui:message key="filter-group-id-no-filter" /></aui:option>
					<aui:option value="1"><liferay-ui:message key="1-hour" /></aui:option>
					<aui:option value="3"><liferay-ui:message key="3-hours" /></aui:option>
					<aui:option value="6"><liferay-ui:message key="6-hours" /></aui:option>
					<aui:option value="12"><liferay-ui:message key="12-hours" /></aui:option>
					<aui:option value="24"><liferay-ui:message key="1-day" /></aui:option>
					<aui:option value="72"><liferay-ui:message key="3-days" /></aui:option>
					<aui:option value="168"><liferay-ui:message key="1-week" /></aui:option>
					<aui:option value="336"><liferay-ui:message key="2-weeks" /></aui:option>
					<aui:option value="729"><liferay-ui:message key="1-month" /></aui:option>
					<aui:option value="2190"><liferay-ui:message key="3-months" /></aui:option>
					<aui:option value="4380"><liferay-ui:message key="6-months" /></aui:option>
					<aui:option value="8760"><liferay-ui:message key="1-year" /></aui:option>
					<aui:option value="26280"><liferay-ui:message key="3-years" /></aui:option>
					<aui:option value="43800"><liferay-ui:message key="5-years" /></aui:option>
				</aui:select>
			</aui:fieldset>
		</aui:col>
		<aui:col width="33">
			<aui:select helpMessage="filter-class-name-help" multiple="true" name="filterClassName" onChange='<%= renderResponse.getNamespace() + "disableReindexAndRemoveOrphansButtons(this);" %>' onClick='<%= renderResponse.getNamespace() + "disableReindexAndRemoveOrphansButtons(this);" %>' style="height: 240px; min-width: 120px; max-width: 340px;">

<%
				String selectedAllModels = "";
				if (filterClassNameSelected.isEmpty()) {
					selectedAllModels = "selected";
				}
%>

				<option <%= selectedAllModels %> value=""><liferay-ui:message key="all" /></option>
				<option disabled="true" value="-">--------</option>

<%
				for (Model model : modelList) {
					String className = model.getClassName();
					String displayName = model.getDisplayName(locale);
					if (Validator.isNull(displayName)) {
						displayName = className;
					}

					String selectedModel = "";
					if (filterClassNameSelected.contains(className)) {
						selectedModel = "selected";
					}
%>

					<option <%= selectedModel %> value="<%= className %>"><%= displayName %></option>

<%
				}
%>

			</aui:select>
		</aui:col>
		<aui:col width="33">
			<aui:select helpMessage="filter-group-id-help" multiple="true" name="filterGroupId" onChange='<%= renderResponse.getNamespace() + "disableReindexAndRemoveOrphansButtons(this);" %>' onClick='<%= renderResponse.getNamespace() + "disableReindexAndRemoveOrphansButtons(this);" %>' style="height: 240px; min-width: 120px; max-width: 340px;">

<%
String selectedNoFilter = "";
String selectedWithoutGroupId = "";
String selectedAllSites = "";
String selectedUserSites = "";
if (filterGroupIdSelected.contains("-1000")) {
	selectedNoFilter = "selected";
}
if (filterGroupIdSelected.isEmpty() || filterGroupIdSelected.contains("0")) {
	selectedWithoutGroupId = "selected";
}
if (filterGroupIdSelected.isEmpty() || filterGroupIdSelected.contains("-1")) {
	selectedAllSites = "selected";
}
if (filterGroupIdSelected.contains("-2")) {
	selectedUserSites = "selected";
}
%>

				<option <%= selectedNoFilter %> value="-1000"><liferay-ui:message key="filter-group-id-no-filter" /></option>
				<option disabled="true" value="-">--------</option>
				<option <%= selectedWithoutGroupId %> value="0"><liferay-ui:message key="filter-group-id-entities-without-groupId" /></option>
				<option <%= selectedAllSites %> value="-1"><liferay-ui:message key="all-sites" /></option>
				<option disabled="true" value="-">--------</option>

<%
				for (int i=0;i<groupIdList.size();i++) {
					String groupIdStr = "" + groupIdList.get(i);

					String selectedGroup = "";
					if (filterGroupIdSelected.contains(groupIdStr)) {
						selectedGroup = "selected";
					}
%>

					<option <%= selectedGroup %> value="<%= groupIdStr %>"><%= groupDescriptionList.get(i) %></option>

<%
				}
%>

				<option disabled="true" value="-">--------</option>
				<option <%= selectedUserSites %> value="-2"><liferay-ui:message key="filter-group-id-user-sites" /></option>
			</aui:select>
		</aui:col>
	</aui:row>

	<aui:button-row style="margin-top: 0px;">
		<aui:button type="submit" value="check-index" />

<%
	String exportCsvResourceURL = (String)request.getAttribute("exportCsvResourceURL");
	if (exportCsvResourceURL != null) {
		exportCsvResourceURL = "window.open('" + exportCsvResourceURL + "');";
%>

		<aui:button onClick="<%= exportCsvResourceURL %>" value="export-to-csv" />

<%
	}
%>

<%
	if ((companyResultDataMap != null) && (companyResultDataMap.size() > 0)) {
		if (executionMode.contains(ExecutionMode.SHOW_BOTH_EXACT) ||
			executionMode.contains(ExecutionMode.SHOW_BOTH_NOTEXACT) ||
			executionMode.contains(ExecutionMode.SHOW_LIFERAY)) {
%>

			<span id="reindexButtonSpan">
			<aui:button onClick='<%= renderResponse.getNamespace() + "reindex();" %>' value="reindex" />
			</span>
<% }

		if (executionMode.contains(ExecutionMode.SHOW_INDEX)) {
%>

			<span id="removeOrphansSpan">
			<aui:button onClick='<%= renderResponse.getNamespace() + "removeOrphans();" %>' value="remove-orphan-data" />
			</span>

<%
		}
	}
%>

		<aui:button onClick="<%= viewURL %>" value="clean" />
		<aui:button onClick="<%= configurationURL %>" value="configuration" />

	</aui:button-row>
</aui:form>

<%
	if ((companyProcessTime != null) && (companyError != null)) {
%>

<h2><b><%= request.getAttribute("title") %></b></h2>

<%
		for (Entry<Company, Long> companyEntry : companyProcessTime.entrySet()) {
			Long processTime = companyEntry.getValue();
			%>

			<h3>Company: <%= companyEntry.getKey().getCompanyId() %> - <%= companyEntry.getKey().getWebId() %></h3>

			<%
			if (companyResultDataMap != null) {
				Map<Long, List<Comparison>> resultDataMap =
					companyResultDataMap.get(companyEntry.getKey());

				PortletURL serverURL = renderResponse.createRenderURL();

				SearchContainer searchContainer = IndexCheckerOutput.generateSearchContainer(portletConfig, renderRequest, executionMode.contains(ExecutionMode.GROUP_BY_SITE), resultDataMap, serverURL);

				if (searchContainer.getTotal() > 0) {
				%>

				<liferay-ui:search-iterator paginate="false" searchContainer="<%= searchContainer %>" />

				<%
				}
				else {
				%>

				<b>No results found:</b> your system is ok or perhaps you have to change some filters<br /><br />

				<%
				}
			}
			String errorMessage = companyError.get(companyEntry.getKey());
%>

<c:if test="<%= Validator.isNotNull(errorMessage) %>">
	<aui:input cssClass="lfr-textarea-container" name="output" resizable="<%= true %>" type="textarea" value="<%= errorMessage %>" />
</c:if>

<i>Executed <b><%= request.getAttribute("title") %></b> for company <%= companyEntry.getKey().getCompanyId() %> in <%=processTime %> ms</i><br />

<%
		}
%>

<%
	}
%>

</div></div></div>

<aui:script>
	function <portlet:namespace />disableOutputOnlyInIndex(event) {
		<portlet:namespace />outputIndex.checked=false;
		<portlet:namespace />outputIndex.disabled=true;
		<portlet:namespace />outputIndex.value=false;
	}
	function <portlet:namespace />disableReindexAndRemoveOrphansButtons(event) {
		var reindexButton = document.getElementById("reindexButtonSpan");
		var removeOrphansButton = document.getElementById("removeOrphansSpan");

		if (reindexButton != null) {
			reindexButton.className = 'hide';
		}

		if (removeOrphansButton != null) {
			removeOrphansButton.className = 'hide';
		}
	}

	function <portlet:namespace />reindex() {
		document.<portlet:namespace />fm.action = "<%= executeReindexURL %>";

		submitForm(document.<portlet:namespace />fm);
	}

	function <portlet:namespace />removeOrphans() {
		document.<portlet:namespace />fm.action = "<%= executeRemoveOrphansURL %>";

		submitForm(document.<portlet:namespace />fm);
	}

<%
	if (ReleaseInfo.getBuildNumber() < 7300) {
%>
	Liferay.provide(window,'closePopupWindow', function(dialogId) {
			var dialog = Liferay.Util.Window.getById(dialogId);
			dialog.destroy();
		},
		['liferay-util-window']
	);
<%
	}
	else
	{
%>
	window['closePopupWindow'] = function(dialogId) {
		Liferay.fire('closeModal', {
			id: dialogId,
		});
	};
<%
	}
%>
</aui:script>