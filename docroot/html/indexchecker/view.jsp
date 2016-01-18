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

<%@ page import="com.liferay.portal.kernel.log.Log" %>
<%@ page import="com.liferay.portal.kernel.util.Validator" %>
<%@ page import="com.liferay.portal.model.Company" %>
<%@ page import="com.liferay.portal.model.Group" %>
<%@ page import="com.liferay.portal.service.GroupLocalServiceUtil" %>

<%@ page import="java.util.Arrays" %>
<%@ page import="java.util.EnumSet" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Map.Entry" %>
<%@ page import="java.util.Set" %>

<%@ page import="jorgediazest.indexchecker.ExecutionMode" %>
<%@ page import="jorgediazest.indexchecker.IndexCheckerResult" %>
<%@ page import="jorgediazest.indexchecker.IndexCheckerUtil" %>
<%@ page import="jorgediazest.indexchecker.data.Data" %>
<%@ page import="jorgediazest.indexchecker.model.IndexCheckerModel" %>
<%@ page import="jorgediazest.indexchecker.portlet.IndexCheckerPortlet" %>

<portlet:defineObjects />

<portlet:renderURL var="viewURL" />

<portlet:actionURL name="executeScript" var="executeScriptURL" windowState="normal" />
<portlet:actionURL name="executeReindex" var="executeReindexURL" windowState="normal" />
<portlet:actionURL name="executeRemoveOrphans" var="executeRemoveOrphansURL" windowState="normal" />

<liferay-ui:header
	backURL="<%= viewURL %>"
	title="index-checker"
/>

<aui:form action="<%= executeScriptURL %>" method="POST" name="fm">
	<aui:fieldset>
		<aui:column>
			<aui:select inlineLabel="left"  name="outputFormat">
				<aui:option selected="true" value="HumanReadable"><liferay-ui:message key="output-format-human-readable" /></aui:option>
				<aui:option value="CSV"><liferay-ui:message key="output-format-csv" /></aui:option>
			</aui:select>
			<aui:input inlineLabel="left" name="filterClassName" type="text" value="" />
		</aui:column>
		<aui:column>
			<aui:input name="outputBothExact" type="checkbox" value="false" />
			<aui:input name="outputBothNotExact" type="checkbox" value="true" />
			<aui:input name="outputLiferay" type="checkbox" value="true" />
			<aui:input name="outputIndex" type="checkbox" value="false" />
		</aui:column>
		<aui:column>
			<aui:input name="outputGroupBySite" type="checkbox" value="false" />
			<aui:input name="dumpAllObjectsToLog" type="checkbox" value="false" />
		</aui:column>
	</aui:fieldset>

	<aui:button-row>
		<aui:button type="submit" value="execute" />
		<aui:button onClick='<%= renderResponse.getNamespace() + "reindex();" %>' type="button" value="reindex" />
		<aui:button onClick='<%= renderResponse.getNamespace() + "removeOrphans();" %>' type="button" value="remove-orphan-data" />
		<aui:button onClick="<%= viewURL %>" type="cancel" value="clean" />
	</aui:button-row>
</aui:form>

<%
	Log _log = IndexCheckerPortlet.getLogger();
	EnumSet<ExecutionMode> executionMode = (EnumSet<ExecutionMode>) request.getAttribute("executionMode");
	Map<Company, Long> companyProcessTime = (Map<Company, Long>) request.getAttribute("companyProcessTime");
	Map<Company, Map<Long, List<IndexCheckerResult>>> companyResultDataMap = (Map<Company, Map<Long, List<IndexCheckerResult>>>) request.getAttribute("companyResultDataMap");
	Map<Company, String> companyError = (Map<Company, String>) request.getAttribute("companyError");

	if ((companyProcessTime != null) && (companyError != null)) {

		String outputFormat = request.getParameter("outputFormat");

		if (Validator.isNotNull(outputFormat)) {
			if (outputFormat.equals("CSV")) {
%>

	<%@ include file="/html/indexchecker/output/result_csv.jspf" %>

<%
			}
			else if (outputFormat.equals("HumanReadable")) {
%>

	<%@ include file="/html/indexchecker/output/result_human.jspf" %>

<%
			}
			else {
%>

	<%@ include file="/html/indexchecker/output/result_error.jspf" %>

<%
			}
		}
%>

<aui:script>
	function <portlet:namespace />reindex() {
		document.<portlet:namespace />fm.action = "<%= executeReindexURL %>";

		submitForm(document.<portlet:namespace />fm);
	}

	function <portlet:namespace />removeOrphans() {
		document.<portlet:namespace />fm.action = "<%= executeRemoveOrphansURL %>";

		submitForm(document.<portlet:namespace />fm);
	}
</aui:script>