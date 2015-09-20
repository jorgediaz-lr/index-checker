<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet" %>

<%@ page import="com.liferay.portal.util.PortalUtil" %>
<%@ page import="com.jorgediaz.indexchecker.IndexChecker" %>
<%@ page import="com.jorgediaz.indexchecker.ExecutionMode" %>
<%@ page import="java.io.PrintWriter" %>
<%@ page import="java.util.EnumSet" %>
<%@ page import="java.lang.Boolean" %>

<portlet:defineObjects />

<%
	boolean outputGroupBySite = Boolean.valueOf(PortalUtil.getOriginalServletRequest(request).getParameter("outputGroupBySite"));
	boolean outputBothExact = Boolean.valueOf(PortalUtil.getOriginalServletRequest(request).getParameter("outputBothExact"));
	boolean outputBothNotExact = Boolean.valueOf(PortalUtil.getOriginalServletRequest(request).getParameter("outputBothNotExact"));
	boolean outputLiferay = Boolean.valueOf(PortalUtil.getOriginalServletRequest(request).getParameter("outputLiferay"));
	boolean outputIndex = Boolean.valueOf(PortalUtil.getOriginalServletRequest(request).getParameter("outputIndex"));
	int outputMaxLength = 120;
	try {outputMaxLength = Integer.valueOf(PortalUtil.getOriginalServletRequest(request).getParameter("outputMaxLength"));} catch (Exception e){}
	boolean reindex = Boolean.valueOf(PortalUtil.getOriginalServletRequest(request).getParameter("reindex"));
	boolean removeOrphan = Boolean.valueOf(PortalUtil.getOriginalServletRequest(request).getParameter("removeOrphan"));
	String filterClassName = PortalUtil.getOriginalServletRequest(request).getParameter("filterClassName");
%>

This is the <b>Index Checker</b> portlet<br/>
<br/>
<i>Parameters</i>
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
</pre>

<i>Output</i>
<pre>
<%
	PrintWriter pw = new PrintWriter(out, true);
	EnumSet<ExecutionMode> executionMode = EnumSet.noneOf(ExecutionMode.class);
	if(outputGroupBySite) {
		executionMode.add(ExecutionMode.GROUP_BY_SITE);
	}
	if(outputBothExact) {
		executionMode.add(ExecutionMode.SHOW_BOTH_EXACT);
	}
	if(outputBothNotExact) {
		executionMode.add(ExecutionMode.SHOW_BOTH_NOTEXACT);
	}
	if(outputLiferay) {
		executionMode.add(ExecutionMode.SHOW_LIFERAY);
	}
	if(outputIndex) {
		executionMode.add(ExecutionMode.SHOW_INDEX);
	}
	if(reindex) {
		executionMode.add(ExecutionMode.REINDEX);
	}
	if(removeOrphan) {
		executionMode.add(ExecutionMode.REMOVE_ORPHAN);
	}
	IndexChecker ic = new IndexChecker(pw);
	ic.dumpData(outputMaxLength, filterClassName, executionMode);
%>
</pre>