package com.test;

import com.jorgediaz.indexchecker.ExecutionMode;
import com.jorgediaz.indexchecker.IndexChecker;
import com.jorgediaz.indexchecker.IndexCheckerResult;
import com.jorgediaz.indexchecker.index.IndexWrapper;
import com.jorgediaz.indexchecker.index.IndexWrapperLuceneJar;
import com.jorgediaz.indexchecker.index.IndexWrapperLuceneReflection;
import com.jorgediaz.indexchecker.index.IndexWrapperSearch;
import com.jorgediaz.util.model.ModelUtil;

import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.dao.shard.ShardUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.Group;
import com.liferay.portal.service.ClassNameLocalServiceUtil;
import com.liferay.portal.service.CompanyLocalServiceUtil;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.util.PortalUtil;
import com.liferay.util.bridges.mvc.MVCPortlet;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;

/**
 * Portlet implementation class IndexCheckerPortlet
 */
public class IndexCheckerPortlet extends MVCPortlet {

	public static String listStringToString(List<String> out) {
		if (Validator.isNull(out)) {
			return null;
		}

		StringBundler stringBundler = new StringBundler(out.size()*2);

		for (String s : out) {
			stringBundler.append(s);
			stringBundler.append(StringPool.NEW_LINE);
		}

		return stringBundler.toString();
	}

	public void executeGetIndexMissingClassNames(
			ActionRequest request, ActionResponse response)
		throws Exception {

		PortalUtil.copyRequestParameters(request, response);

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

		List<String> outputScript = new ArrayList<String>();

		List<Company> companies = CompanyLocalServiceUtil.getCompanies();

		for (Company company : companies) {
			List<String> classNames =
				ModelUtil.getClassNameValues(
					ClassNameLocalServiceUtil.getClassNames(
						QueryUtil.ALL_POS, QueryUtil.ALL_POS));

			try {
				ShardUtil.pushCompanyService(company.getCompanyId());

				outputScript.add("COMPANY: "+company);

				outputScript.addAll(
					IndexChecker.executeScriptGetIndexMissingClassNames(
						indexWrapperClass, company, classNames));

				outputScript.add(StringPool.BLANK);
			}
			finally {
				ShardUtil.popCompanyService();
			}
		}

		response.setRenderParameter(
			"outputScript", listStringToString(outputScript));
	}

	public void executeReindex(ActionRequest request, ActionResponse response)
		throws Exception {

		PortalUtil.copyRequestParameters(request, response);

		EnumSet<ExecutionMode> executionMode = getExecutionMode(request);

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

		String filterClassName = ParamUtil.getString(
			request, "filterClassName");

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

			try {
				ShardUtil.pushCompanyService(company.getCompanyId());

				List<Group> groups =
					GroupLocalServiceUtil.getCompanyGroups(
						company.getCompanyId(), QueryUtil.ALL_POS,
						QueryUtil.ALL_POS);

				long startTime = System.currentTimeMillis();

				Map<Long, List<IndexCheckerResult>> resultDataMap =
					IndexChecker.executeScript(
						indexWrapperClass, company, groups, classNames,
						executionMode);

				outputScript.add("COMPANY: "+company);

				outputScript.add(StringPool.BLANK);

				for (
					Entry<Long, List<IndexCheckerResult>> entry :
						resultDataMap.entrySet()) {

					List<IndexCheckerResult> resultList = entry.getValue();

					for (IndexCheckerResult result : resultList) {
						result.reindex();
					}
				}

				long endTime = System.currentTimeMillis();

				outputScript.add(
					"Reindexed company "+company.getCompanyId()+" in "+
						(endTime-startTime)+" ms");
				outputScript.add(StringPool.BLANK);
			}
			finally {
				ShardUtil.popCompanyService();
			}
		}

		response.setRenderParameter(
			"outputScript", listStringToString(outputScript));
	}

	public void executeRemoveOrphans(
			ActionRequest request, ActionResponse response)
		throws Exception {

		PortalUtil.copyRequestParameters(request, response);

		EnumSet<ExecutionMode> executionMode = getExecutionMode(request);

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

		String filterClassName = ParamUtil.getString(
			request, "filterClassName");

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

			try {
				ShardUtil.pushCompanyService(company.getCompanyId());

				List<Group> groups =
					GroupLocalServiceUtil.getCompanyGroups(
						company.getCompanyId(), QueryUtil.ALL_POS,
						QueryUtil.ALL_POS);

				long startTime = System.currentTimeMillis();

				Map<Long, List<IndexCheckerResult>> resultDataMap =
					IndexChecker.executeScript(
						indexWrapperClass, company, groups, classNames,
						executionMode);

				outputScript.add("COMPANY: "+company);

				outputScript.add(StringPool.BLANK);

				for (
					Entry<Long, List<IndexCheckerResult>> entry :
						resultDataMap.entrySet()) {

					List<IndexCheckerResult> resultList = entry.getValue();

					for (IndexCheckerResult result : resultList) {
						result.removeIndexOrphans();
					}
				}

				long endTime = System.currentTimeMillis();

				outputScript.add(
					"Removed orphans of company " + company.getCompanyId() +
					" in "+ (endTime-startTime)+" ms");
				outputScript.add(StringPool.BLANK);
			}
			finally {
				ShardUtil.popCompanyService();
			}
		}

		response.setRenderParameter(
			"outputScript", listStringToString(outputScript));
	}

	public void executeScript(ActionRequest request, ActionResponse response)
		throws Exception {

		PortalUtil.copyRequestParameters(request, response);

		EnumSet<ExecutionMode> executionMode = getExecutionMode(request);

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

		String[] filterClassNameArr = null;
		String filterClassName = ParamUtil.getString(
			request, "filterClassName");

		if (filterClassName != null) {
			filterClassNameArr = filterClassName.split(",");
		}

		List<String> outputScript = new ArrayList<String>();

		List<Company> companies = CompanyLocalServiceUtil.getCompanies();

		for (Company company : companies) {
			List<String> allClassName =
				ModelUtil.getClassNameValues(
					ClassNameLocalServiceUtil.getClassNames(
						QueryUtil.ALL_POS, QueryUtil.ALL_POS));

			List<String> classNames = new ArrayList<String>();

			for (String className : allClassName) {
				if (className == null) {
					continue;
				}

				if (filterClassNameArr == null) {
					classNames.add(className);
					continue;
				}

				for (int i = 0; i<filterClassNameArr.length; i++) {
					if (className.contains(filterClassNameArr[i])) {
						classNames.add(className);
						break;
					}
				}
			}

			try {
				ShardUtil.pushCompanyService(company.getCompanyId());

				List<Group> groups =
					GroupLocalServiceUtil.getCompanyGroups(
						company.getCompanyId(), QueryUtil.ALL_POS,
						QueryUtil.ALL_POS);

				long startTime = System.currentTimeMillis();

				Map<Long, List<IndexCheckerResult>> resultDataMap =
					IndexChecker.executeScript(
						indexWrapperClass, company, groups, classNames,
						executionMode);

				long endTime = System.currentTimeMillis();

				outputScript.add("COMPANY: "+company);

				if (executionMode.contains(
						ExecutionMode.DUMP_ALL_OBJECTS_TO_LOG)) {

					System.out.println("COMPANY: "+company);
				}

				outputScript.add(StringPool.BLANK);

				/* TODO QUITAR EN EL OUTPUT EL CODIGO DE REINDEX!! */
				outputScript.addAll(
					IndexChecker.generateOutput(
						outputMaxLength, executionMode, resultDataMap));

				outputScript.add(
					"\nProcessed company "+company.getCompanyId()+" in "+
						(endTime-startTime)+" ms");
				outputScript.add(StringPool.BLANK);
			}
			finally {
				ShardUtil.popCompanyService();
			}
		}

		response.setRenderParameter(
			"outputScript", listStringToString(outputScript));
	}

	protected static EnumSet<ExecutionMode> getExecutionMode(
		ActionRequest request) {

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

		if (ParamUtil.getBoolean(request, "dumpAllObjectsToLog")) {
			executionMode.add(ExecutionMode.DUMP_ALL_OBJECTS_TO_LOG);
		}

		return executionMode;
	}

}