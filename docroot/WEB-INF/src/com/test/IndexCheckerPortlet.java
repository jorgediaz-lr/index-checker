package com.test;

import com.jorgediaz.indexchecker.ExecutionMode;
import com.jorgediaz.indexchecker.IndexChecker;
import com.jorgediaz.indexchecker.index.IndexWrapper;

import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.util.bridges.mvc.MVCPortlet;

import java.util.Set;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;

/**
 * Portlet implementation class IndexCheckerPortlet
 */
public class IndexCheckerPortlet extends MVCPortlet {

	public void executeScript(ActionRequest request, ActionResponse response)
		throws Exception {

		String dummy = ParamUtil.getString(request, "dummy");
		System.out.println(dummy);
		response.setRenderParameter("dummy", dummy);
		/* TODO pendiente recuperar parametros:


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



		*/
		int maxLength = 120;
		String filter = "test";
		Set<ExecutionMode> executionMode = null;
		Class<? extends IndexWrapper> indexWrapperClass = null;
		String outputScript = IndexChecker.execute(
			maxLength, filter, executionMode, indexWrapperClass);
		response.setRenderParameter("outputScript", outputScript);
	}

}