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
<%@ page import="com.liferay.portal.kernel.util.ParamUtil" %>
<%@ page import="com.liferay.portal.kernel.util.Validator" %>
<%@ page import="com.liferay.portal.model.Company" %>

<%@ page import="java.util.EnumSet" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Map.Entry" %>

<%@ page import="javax.portlet.PortletURL" %>

<%@ page import="jorgediazest.indexchecker.ExecutionMode" %>
<%@ page import="jorgediazest.util.data.Comparison" %>
<%@ page import="jorgediazest.indexchecker.output.IndexCheckerOutput" %>
<%@ page import="jorgediazest.indexchecker.portlet.IndexCheckerPortlet" %>

<%@ page import="jorgediazest.util.output.OutputUtils" %>

<portlet:defineObjects />

<portlet:renderURL var="viewURL" />

<portlet:actionURL name="executeCheck" var="executeCheckURL" windowState="normal" />
<portlet:actionURL name="executeReindex" var="executeReindexURL" windowState="normal" />
<portlet:actionURL name="executeRemoveOrphans" var="executeRemoveOrphansURL" windowState="normal" />

<liferay-ui:header
	backURL="<%= viewURL %>"
	title="index-checker"
/>

<%
	Log _log = IndexCheckerPortlet.getLogger();
	EnumSet<ExecutionMode> executionMode = (EnumSet<ExecutionMode>) request.getAttribute("executionMode");
	Map<Company, Long> companyProcessTime = (Map<Company, Long>) request.getAttribute("companyProcessTime");
	Map<Company, Map<Long, List<Comparison>>> companyResultDataMap = (Map<Company, Map<Long, List<Comparison>>>) request.getAttribute("companyResultDataMap");
	Map<Company, String> companyError = (Map<Company, String>) request.getAttribute("companyError");
%>

<aui:form action="<%= executeCheckURL %>" method="POST" name="fm">
	<aui:fieldset>
		<aui:column>
			<aui:select name="outputFormat">
				<aui:option selected="true" value="Table"><liferay-ui:message key="output-format-table" /></aui:option>
				<aui:option value="CSV"><liferay-ui:message key="output-format-csv" /></aui:option>
			</aui:select>
			<aui:input helpMessage="filter-class-name-help" name="filterClassName" onClick='<%= renderResponse.getNamespace() + "disableReindexAndRemoveOrphansButtons(this);" %>' type="text" value="" />
		</aui:column>
		<aui:column>
			<aui:input helpMessage="output-both-exact-help" name="outputBothExact" onClick='<%= renderResponse.getNamespace() + "disableReindexAndRemoveOrphansButtons(this);" %>' type="checkbox" value="false" />
			<aui:input helpMessage="output-both-not-exact-help" name="outputBothNotExact" onClick='<%= renderResponse.getNamespace() + "disableReindexAndRemoveOrphansButtons(this);" %>' type="checkbox" value="true" />
			<aui:input helpMessage="output-liferay-help" name="outputLiferay" onClick='<%= renderResponse.getNamespace() + "disableReindexAndRemoveOrphansButtons(this);" %>' type="checkbox" value="true" />
			<aui:input helpMessage="output-index-help" name="outputIndex" onClick='<%= renderResponse.getNamespace() + "disableReindexAndRemoveOrphansButtons(this);" %>' type="checkbox" value="false" />
		</aui:column>
		<aui:column>
			<aui:input name="outputGroupBySite" onClick='<%= renderResponse.getNamespace() + "toogleFilterGroupId(this);" %>' type="checkbox" value="false" />
			<span class="<%= (ParamUtil.getBoolean(request, "outputGroupBySite") ? "" : "hide") %>" id="filterGroupIdSpan">
			<aui:input helpMessage="filter-group-id-help" name="filterGroupId" onClick='<%= renderResponse.getNamespace() + "disableReindexAndRemoveOrphansButtons(this);" %>' type="text" value="" />
			</span>
			<aui:input name="dumpAllObjectsToLog" type="checkbox" value="false" />
		</aui:column>
	</aui:fieldset>

	<aui:button-row>
		<aui:button type="submit" value="check-index" />

<%
	if ((companyResultDataMap != null) && (companyResultDataMap.size() > 0)) {
		if (executionMode.contains(ExecutionMode.SHOW_BOTH_EXACT) ||
			executionMode.contains(ExecutionMode.SHOW_BOTH_NOTEXACT) ||
			executionMode.contains(ExecutionMode.SHOW_LIFERAY)) {
%>

			<span id="reindexButtonSpan">
			<aui:button onClick='<%= renderResponse.getNamespace() + "reindex();" %>' type="button" value="reindex" />
			</span>
<% }

		if (executionMode.contains(ExecutionMode.SHOW_INDEX)) {
%>

			<span id="removeOrphansSpan">
			<aui:button onClick='<%= renderResponse.getNamespace() + "removeOrphans();" %>' type="button" value="remove-orphan-data" />
			</span>

<%
		}
	}
%>

		<aui:button onClick="<%= viewURL %>" type="cancel" value="clean" />
	</aui:button-row>
</aui:form>

<%
	if ((companyProcessTime != null) && (companyError != null)) {

		String outputFormat = request.getParameter("outputFormat");

		if (Validator.isNotNull(outputFormat)) {
			if (outputFormat.equals("CSV")) {
%>

	<%@ include file="/html/indexchecker/output/result_csv.jspf" %>

<%
			}
			else if (outputFormat.equals("Table")) {
%>

	<%@ include file="/html/indexchecker/output/result_table.jspf" %>

<%
			}
			else {
%>

	<%@ include file="/html/indexchecker/output/result_error.jspf" %>

<%
			}
		}
	}
%>

<aui:script>
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

	function <portlet:namespace />toogleFilterGroupId(event) {
		var filterGroupId = document.getElementById("filterGroupIdSpan");

		if (event.checked) {
			filterGroupId.className = '';
		}
		else {
			filterGroupId.className = 'hide';
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
</aui:script>