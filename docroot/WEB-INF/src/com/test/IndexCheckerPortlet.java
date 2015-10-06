package com.test;

import com.jorgediaz.indexchecker.ExecutionMode;
import com.jorgediaz.indexchecker.IndexChecker;
import com.jorgediaz.indexchecker.index.IndexWrapper;
import com.jorgediaz.indexchecker.index.IndexWrapperLuceneJar;
import com.jorgediaz.indexchecker.index.IndexWrapperLuceneReflection;
import com.jorgediaz.indexchecker.index.IndexWrapperSearch;
import com.jorgediaz.util.model.ModelUtil;

import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.model.Company;
import com.liferay.portal.service.ClassNameLocalServiceUtil;
import com.liferay.portal.service.CompanyLocalServiceUtil;
import com.liferay.portal.util.PortalUtil;
import com.liferay.util.bridges.mvc.MVCPortlet;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;

/**
 * Portlet implementation class IndexCheckerPortlet
 */
public class IndexCheckerPortlet extends MVCPortlet {

	public void executeScript(ActionRequest request, ActionResponse response)
		throws Exception {

		PortalUtil.copyRequestParameters(request, response);

		EnumSet<ExecutionMode> executionMode = EnumSet.noneOf(
			ExecutionMode.class);

		if (ParamUtil.getBoolean(request, "outputGroupBySite")) {
			executionMode.add(ExecutionMode.GROUP_BY_SITE);
		}

		if (ParamUtil.getBoolean(request, "outputBothExact")) {
			executionMode.add(ExecutionMode.SHOW_BOTH_EXACT);
		}

		if (ParamUtil.getBoolean(request, "outputBothNotExact")) {
			executionMode.add(ExecutionMode.SHOW_BOTH_NOTEXACT);
		}

		if (ParamUtil.getBoolean(request, "outputLiferay")) {
			executionMode.add(ExecutionMode.SHOW_LIFERAY);
		}

		if (ParamUtil.getBoolean(request, "outputIndex")) {
			executionMode.add(ExecutionMode.SHOW_INDEX);
		}

		if (ParamUtil.getBoolean(request, "reindex")) {
			executionMode.add(ExecutionMode.REINDEX);
		}

		if (ParamUtil.getBoolean(request, "removeOrphan")) {
			executionMode.add(ExecutionMode.REMOVE_ORPHAN);
		}

		if (ParamUtil.getBoolean(request, "dumpAllObjectsToLog")) {
			executionMode.add(ExecutionMode.DUMP_ALL_OBJECTS_TO_LOG);
		}

		String indexWrapperClassName = ParamUtil.getString(
			request, "indexWrapperClassName");

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

		int outputMaxLength = ParamUtil.getInteger(request, "outputMaxLength");

		String filterClassName = ParamUtil.getString(
			request, "filterClassName");

		IndexChecker ic = new IndexChecker();
		List<String> outputScript = new ArrayList<String>();

		List<Company> companies = CompanyLocalServiceUtil.getCompanies();

		for (Company company : companies) {
			List<String> allClassName =
				ModelUtil.getClassNameValues(
					ClassNameLocalServiceUtil.getClassNames(
						QueryUtil.ALL_POS, QueryUtil.ALL_POS));

			List<String> classNames = new ArrayList<String>();

			for (String className : allClassName) {
				if ((className != null) &&
					((filterClassName == null) ||
					 className.contains(filterClassName))) {

					classNames.add(className);
				}
			}

			outputScript.addAll(
				ic.executeScript(
					outputMaxLength, classNames, executionMode,
					indexWrapperClass, company));
		}

		response.setRenderParameter(
			"outputScript",IndexChecker.listStringToString(outputScript));
	}

}