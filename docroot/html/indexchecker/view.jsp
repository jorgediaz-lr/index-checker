<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet" %>

<%@ taglib uri="http://liferay.com/tld/aui" prefix="aui" %>
<%@ taglib uri="http://liferay.com/tld/portlet" prefix="liferay-portlet" %>
<%@ taglib uri="http://liferay.com/tld/security" prefix="liferay-security" %>
<%@ taglib uri="http://liferay.com/tld/theme" prefix="liferay-theme" %>
<%@ taglib uri="http://liferay.com/tld/ui" prefix="liferay-ui" %>
<%@ taglib uri="http://liferay.com/tld/util" prefix="liferay-util" %>

<%@ page contentType="text/html; charset=UTF-8" %>

<%@ page import="com.jorgediaz.indexchecker.ExecutionMode" %>
<%@ page import="com.jorgediaz.indexchecker.IndexChecker" %>
<%@ page import="com.jorgediaz.indexchecker.index.IndexWrapper" %>
<%@ page import="com.jorgediaz.indexchecker.index.IndexWrapperLuceneJar" %>
<%@ page import="com.jorgediaz.indexchecker.index.IndexWrapperLuceneReflection" %>
<%@ page import="com.jorgediaz.indexchecker.index.IndexWrapperSearch" %>

<%@ page import="com.liferay.portal.util.PortalUtil" %>

<%@ page import="java.lang.Boolean" %>

<%@ page import="java.util.EnumSet" %>

<portlet:defineObjects />

<%
	boolean outputGroupBySite = Boolean.valueOf(PortalUtil.getOriginalServletRequest(request).getParameter("outputGroupBySite"));
	boolean outputBothExact = Boolean.valueOf(PortalUtil.getOriginalServletRequest(request).getParameter("outputBothExact"));
	boolean outputBothNotExact = Boolean.valueOf(PortalUtil.getOriginalServletRequest(request).getParameter("outputBothNotExact"));
	boolean outputLiferay = Boolean.valueOf(PortalUtil.getOriginalServletRequest(request).getParameter("outputLiferay"));
	boolean outputIndex = Boolean.valueOf(PortalUtil.getOriginalServletRequest(request).getParameter("outputIndex"));
	int outputMaxLength = 160;
	try {outputMaxLength = Integer.valueOf(PortalUtil.getOriginalServletRequest(request).getParameter("outputMaxLength"));} catch (Exception e){}
	boolean reindex = Boolean.valueOf(PortalUtil.getOriginalServletRequest(request).getParameter("reindex"));
	boolean removeOrphan = Boolean.valueOf(PortalUtil.getOriginalServletRequest(request).getParameter("removeOrphan"));
	String filterClassName = PortalUtil.getOriginalServletRequest(request).getParameter("filterClassName");
	String indexWrapperClassName = PortalUtil.getOriginalServletRequest(request).getParameter("indexWrapperClassName");
	boolean dumpAllObjectsToLog = Boolean.valueOf(PortalUtil.getOriginalServletRequest(request).getParameter("dumpAllObjectsToLog"));
%>

This is the <b>Index Checker</b> portlet<br />
<br />
Parameters
<pre>
outputGroupBySite: <%= outputGroupBySite %>
outputBothExact: <%= outputBothExact %>
outputBothNotExact: <%= outputBothNotExact %>
outputLiferay: <%= outputLiferay %>
outputIndex: <%= outputIndex %>
outputMaxLength: <%= outputMaxLength %>
reindex: <%= reindex %>
removeOrphan: <%= removeOrphan %>
filterClassName: <%= filterClassName %>
indexWrapperClassName: <%= indexWrapperClassName %>
dumpAllObjectsToLog: <%= dumpAllObjectsToLog %>
</pre>

<%
if (!outputBothExact & !outputBothNotExact & !outputLiferay & !outputIndex) {
	return;
}
%>

<%
	EnumSet<ExecutionMode> executionMode = EnumSet.noneOf(ExecutionMode.class);
	if (outputGroupBySite) {
		executionMode.add(ExecutionMode.GROUP_BY_SITE);
	}
	if (outputBothExact) {
		executionMode.add(ExecutionMode.SHOW_BOTH_EXACT);
	}
	if (outputBothNotExact) {
		executionMode.add(ExecutionMode.SHOW_BOTH_NOTEXACT);
	}
	if (outputLiferay) {
		executionMode.add(ExecutionMode.SHOW_LIFERAY);
	}
	if (outputIndex) {
		executionMode.add(ExecutionMode.SHOW_INDEX);
	}
	if (reindex) {
		executionMode.add(ExecutionMode.REINDEX);
	}
	if (removeOrphan) {
		executionMode.add(ExecutionMode.REMOVE_ORPHAN);
	}
	if (dumpAllObjectsToLog) {
		executionMode.add(ExecutionMode.DUMP_ALL_OBJECTS_TO_LOG);
	}

	Class<? extends IndexWrapper> indexWrapperClass;

	if ("LuceneJar".equals(indexWrapperClassName)) {
		indexWrapperClass = IndexWrapperLuceneJar.class;
	}
	else if ("LuceneReflection".equals(indexWrapperClassName)) {
		indexWrapperClass = IndexWrapperLuceneReflection.class;
	}
	else if ("Search".equals(indexWrapperClassName)) {
		indexWrapperClass = IndexWrapperSearch.class;
	}
	else {
		indexWrapperClass = IndexWrapperLuceneReflection.class;
	}

	String output = IndexChecker.execute(outputMaxLength, filterClassName, executionMode, indexWrapperClass);
%>

<aui:input cssClass="lfr-textarea-container" name="output" resizable="<%= true %>" type="textarea" value="<%= output %>" />