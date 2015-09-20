<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet" %>

<%@ page import="com.liferay.portal.util.PortalUtil" %>
<%@ page import="com.script.IndexChecker" %>
<%@ page import="com.script.OutputMode" %>
<%@ page import="java.io.PrintWriter" %>
<%@ page import="java.util.EnumSet" %>
<%@ page import="java.lang.Boolean" %>

<portlet:defineObjects />

<%
	boolean outputGroupBySite = Boolean.valueOf(PortalUtil.getOriginalServletRequest(request).getParameter("outputGroupBySite"));
	boolean outputBoth = Boolean.valueOf(PortalUtil.getOriginalServletRequest(request).getParameter("outputBoth"));
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
outputBoth: <%= outputBoth %>
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
	EnumSet<OutputMode> outputMode = EnumSet.noneOf(OutputMode.class);
	if(outputGroupBySite) {
		outputMode.add(OutputMode.GROUP_BY_SITE);
	}
	if(outputBoth) {
		outputMode.add(OutputMode.BOTH);
	}
	if(outputLiferay) {
		outputMode.add(OutputMode.LIFERAY);
	}
	if(outputIndex) {
		outputMode.add(OutputMode.INDEX);
	}
	IndexChecker ic = new IndexChecker(pw);
	ic.dumpData(outputMaxLength, filterClassName, outputMode, reindex, removeOrphan);
%>
</pre>