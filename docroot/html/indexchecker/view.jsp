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

<%@ page import="com.jorgediaz.indexchecker.ExecutionMode" %>
<%@ page import="com.jorgediaz.indexchecker.IndexCheckerResult" %>
<%@ page import="com.jorgediaz.indexchecker.IndexCheckerUtil" %>
<%@ page import="com.jorgediaz.indexchecker.portlet.IndexCheckerPortlet" %>

<%@ page import="com.liferay.portal.kernel.log.Log" %>
<%@ page import="com.liferay.portal.kernel.util.ParamUtil" %>
<%@ page import="com.liferay.portal.kernel.util.StringPool" %>
<%@ page import="com.liferay.portal.kernel.util.Validator" %>
<%@ page import="com.liferay.portal.model.Company" %>

<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.EnumSet" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Map.Entry" %>

<portlet:defineObjects />

<portlet:renderURL var="viewURL" />

<portlet:actionURL name="executeScript" var="executeScriptURL" windowState="normal" />
<portlet:actionURL name="executeGetIndexMissingClassNames" var="executeGetIndexMissingClassNamesURL" windowState="normal" />
<portlet:actionURL name="executeReindex" var="executeReindexURL" windowState="normal" />
<portlet:actionURL name="executeRemoveOrphans" var="executeRemoveOrphansURL" windowState="normal" />

<liferay-ui:header
	backURL="<%= viewURL %>"
	title="index-checker"
/>

<aui:form action="<%= executeScriptURL %>" method="post" name="fm">
	<aui:fieldset>
		<aui:column>
			<aui:input inlineLabel="left" name="outputMaxLength" type="text" value="160" />
			<aui:input inlineLabel="left" name="filterClassName" type="text" value="" />
			<aui:select inlineLabel="left"  name="indexWrapperClassName">
				<aui:option value="Search"><liferay-ui:message key="index-wrapper-class-name-search" /></aui:option>
				<aui:option selected="true" value="Lucene"><liferay-ui:message key="index-wrapper-class-name-lucene" /></aui:option>
				<aui:option value="LuceneJar"><liferay-ui:message key="index-wrapper-class-name-lucene-jar" /></aui:option>
			</aui:select>
		</aui:column>
		<aui:column>
			<aui:input name="outputBothExact" type="checkbox" value="false" />
			<aui:input name="outputBothNotExact" type="checkbox" value="true" />
			<aui:input name="outputLiferay" type="checkbox" value="true" />
			<aui:input name="outputIndex" type="checkbox" value="true" />
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
		<aui:button onClick='<%= renderResponse.getNamespace() + "removeGetIndexMissingClassNames();" %>' type="button" value="get-index-missing-classnames" />
		<aui:button onClick="<%= viewURL %>" type="cancel" value="clean" />
	</aui:button-row>
</aui:form>

<%
	String outputScript = StringPool.BLANK;

	Log _log = IndexCheckerPortlet.getLogger();
	EnumSet<ExecutionMode> executionMode = (EnumSet<ExecutionMode>) request.getAttribute("executionMode");
	Map<Company, Long> companyProcessTime = (Map<Company, Long>) request.getAttribute("companyProcessTime");
	Map<Company, Map<Long, List<IndexCheckerResult>>> companyResultDataMap = (Map<Company, Map<Long, List<IndexCheckerResult>>>) request.getAttribute("companyResultDataMap");
	Map<Company, String> companyError = (Map<Company, String>) request.getAttribute("companyError");

	if ((companyProcessTime != null) && (companyError != null)) {
		List<String> outList = new ArrayList<String>();

		int outputMaxLength = ParamUtil.getInteger(request, "outputMaxLength");

		for (Entry<Company, Long> entry : companyProcessTime.entrySet()) {
			Long processTime = entry.getValue();

			outList.add("COMPANY: "+entry.getKey());

			if (_log.isInfoEnabled() &&
				executionMode.contains(
					ExecutionMode.DUMP_ALL_OBJECTS_TO_LOG)) {

				_log.info("COMPANY: "+entry.getKey());
			}

			outList.add(StringPool.BLANK);

			if (companyResultDataMap != null) {
				Map<Long, List<IndexCheckerResult>> resultDataMap =
					companyResultDataMap.get(entry.getKey());

				outList.addAll(
					IndexCheckerUtil.generateOutput(
						outputMaxLength, executionMode, resultDataMap));
			}

			outList.add(
				"\nProcessed company "+entry.getKey().getCompanyId()+" in "+
					processTime +" ms");
			outList.add(StringPool.BLANK);
		}

		for (Entry<Company, String> entry : companyError.entrySet()) {
			if ((companyResultDataMap != null) && companyResultDataMap.containsKey(entry.getKey())) {
				continue;
			}

			Long processTime = companyProcessTime.get(entry.getKey());

			outList.add("COMPANY: "+entry.getKey());

			outList.add(StringPool.BLANK);

			outList.add(entry.getValue());

			outList.add(
				"\nProcessed company "+entry.getKey().getCompanyId()+" in "+
					processTime +" ms");
			outList.add(StringPool.BLANK);
		}

		outputScript = IndexCheckerUtil.listStringToString(outList);
	}
%>

<c:if test="<%= Validator.isNotNull(outputScript) %>">
	<aui:input cssClass="lfr-textarea-container" name="output" resizable="<%= true %>" type="textarea" value="<%= outputScript %>" />
</c:if>

<aui:script>
	function <portlet:namespace />reindex() {
		document.<portlet:namespace />fm.action = "<%= executeReindexURL %>";

		submitForm(document.<portlet:namespace />fm);
	}

	function <portlet:namespace />removeOrphans() {
		document.<portlet:namespace />fm.action = "<%= executeRemoveOrphansURL %>";

		submitForm(document.<portlet:namespace />fm);
	}

	function <portlet:namespace />removeGetIndexMissingClassNames() {
		document.<portlet:namespace />fm.action = "<%= executeGetIndexMissingClassNamesURL %>";

		submitForm(document.<portlet:namespace />fm);
	}
</aui:script>