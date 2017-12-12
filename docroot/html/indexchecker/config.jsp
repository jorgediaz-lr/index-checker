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

<%@ page import="com.liferay.portal.kernel.util.Constants" %>
<%@ page import="com.liferay.portal.kernel.util.GetterUtil" %>
<%@ page import="com.liferay.portal.kernel.util.StringPool" %>

<%@ page import="jorgediazest.indexchecker.util.ConfigurationUtil" %>

<portlet:defineObjects />

<liferay-portlet:actionURL portletConfiguration="true" var="configurationURL" />

<%
boolean queryBySite_cfg = GetterUtil.getBoolean(portletPreferences.getValue("queryBySite", StringPool.FALSE));
boolean dumpAllObjectsToLog_cfg = GetterUtil.getBoolean(portletPreferences.getValue("dumpAllObjectsToLog", StringPool.FALSE));
int numberOfThreads_cfg = GetterUtil.getInteger(portletPreferences.getValue("numberOfThreads", StringPool.BLANK));
if (numberOfThreads_cfg == 0) {
	numberOfThreads_cfg = ConfigurationUtil.getDefaultNumberThreads();
}
%>

<aui:form action="<%= configurationURL %>" method="post" name="fm">

	<div class="portlet-configuration-body-content"><div class="container-fluid-1280"><div class="card-horizontal main-content-card"><div class="panel-body">

	<aui:input name="<%= Constants.CMD %>" type="hidden" value="<%= Constants.UPDATE %>" />

	<aui:input name="preferences--queryBySite--" type="checkbox" value="<%= queryBySite_cfg %>" />
	<aui:input name="preferences--dumpAllObjectsToLog--" type="checkbox" value="<%= dumpAllObjectsToLog_cfg %>" />
	<aui:input helpMessage="number-of-threads-help" name="preferences--numberOfThreads--" type="text" value="<%= numberOfThreads_cfg %>" />

	</div></div></div></div>

	<aui:button-row>
		<aui:button type="submit" />
		<aui:button onClick='<%= renderResponse.getNamespace() + "closeButton(this);" %>' value="close" />
	</aui:button-row>
</aui:form>

<aui:script>
	function <portlet:namespace />closeButton(event) {
		Liferay.Util.getOpener().closePopupWindow(Liferay.Util.getWindowName());
	}
</aui:script>