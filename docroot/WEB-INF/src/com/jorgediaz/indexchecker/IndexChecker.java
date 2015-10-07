package com.jorgediaz.indexchecker;

import com.jorgediaz.indexchecker.data.Data;
import com.jorgediaz.indexchecker.index.IndexWrapper;
import com.jorgediaz.indexchecker.model.IndexCheckerModel;
import com.jorgediaz.indexchecker.model.IndexCheckerModelFactory;
import com.jorgediaz.util.model.Model;
import com.jorgediaz.util.model.ModelFactory;

import com.liferay.portal.kernel.dao.orm.Criterion;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.dao.shard.ShardUtil;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Tuple;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.Group;
import com.liferay.portal.service.GroupLocalServiceUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
public class IndexChecker {

	public List<String> executeScript(
			Class<? extends IndexWrapper> indexWrapperClass, Company company,
			List<String> classNames, int maxLength,
			Set<ExecutionMode> executionMode)
		throws SystemException {

		List<String> out = new ArrayList<String>();

		long startTime = System.currentTimeMillis();
		out.add("COMPANY: "+company);

		if (executionMode.contains(ExecutionMode.DUMP_ALL_OBJECTS_TO_LOG)) {
			System.out.println("COMPANY: "+company);
		}

		try {
			ShardUtil.pushCompanyService(company.getCompanyId());

			IndexWrapper indexWrapper = null;
			ModelFactory modelFactory = null;

			try {
				indexWrapper =
					indexWrapperClass.getDeclaredConstructor(
						long.class).newInstance(company.getCompanyId());

				out.add("IndexWrapper: "+indexWrapper);
				out.add("num documents: "+indexWrapper.numDocs());

				modelFactory = new IndexCheckerModelFactory();
			}
			catch (Exception e) {
				out.add(
					"\t" + "EXCEPTION: " + e.getClass() + " - " +
						e.getMessage());
				e.printStackTrace();
				return out;
			}

			List<Group> groups =
				GroupLocalServiceUtil.getCompanyGroups(
					company.getCompanyId(), QueryUtil.ALL_POS,
					QueryUtil.ALL_POS);

			Map<String, Model> modelMap = modelFactory.getModelMap(classNames);

			Set<String> classNamesNotAvailable =
				indexWrapper.getMissingClassNamesAtLiferay(modelMap);

			Map<IndexCheckerModel, Map<Long, Tuple>> resultDataMap =
				getData(
					out, indexWrapper, company.getCompanyId(), groups, modelMap,
					executionMode);

			if (classNamesNotAvailable.size() > 0) {
				out.add("");
				out.add("---------------");
				out.add(
					"classNames at Index, that we are not going to check!!");
				out.add("---------------");

				for (String className : classNamesNotAvailable) {
					out.add(className);
				}
			}

			out.addAll(dumpData(maxLength, executionMode, resultDataMap));
		}
		finally {
			ShardUtil.popCompanyService();
		}

		long endTime = System.currentTimeMillis();
		out.add(
			"\nProcessed company "+company.getCompanyId()+" in "+
				(endTime-startTime)+" ms");
		out.add(StringPool.BLANK);
		return out;
	}

	protected List<String> dumpData(
		IndexCheckerModel model, Tuple data, int maxLength,
		Set<ExecutionMode> executionMode) {

		List<String> out = new ArrayList<String>();

		@SuppressWarnings("unchecked")
		Set<Data> exactDataSetIndex = (Set<Data>)data.getObject(0);
		@SuppressWarnings("unchecked")
		Set<Data> exactDataSetLiferay = (Set<Data>)data.getObject(1);
		@SuppressWarnings("unchecked")
		Set<Data> notExactDataSetIndex = (Set<Data>)data.getObject(2);
		@SuppressWarnings("unchecked")
		Set<Data> notExactDataSetLiferay = (Set<Data>)data.getObject(3);
		@SuppressWarnings("unchecked")
		Set<Data> liferayOnlyData = (Set<Data>)data.getObject(4);
		@SuppressWarnings("unchecked")
		Set<Data> indexOnlyData = (Set<Data>)data.getObject(5);

		boolean reindex = executionMode.contains(ExecutionMode.REINDEX);
		boolean removeOrphan = executionMode.contains(
			ExecutionMode.REMOVE_ORPHAN);

		if ((exactDataSetIndex != null) && !exactDataSetIndex.isEmpty()) {
			out.add("==both-exact==");
			out.addAll(
				dumpData(model.getName(), exactDataSetLiferay, maxLength));

			if (executionMode.contains(ExecutionMode.DUMP_ALL_OBJECTS_TO_LOG)) {
				System.out.println("==both-exact(index)==");

				for (Data d : exactDataSetIndex) {
					System.out.println(d.getAllData(","));
				}

				System.out.println("==both-exact(liferay)==");

				for (Data d : exactDataSetLiferay) {
					System.out.println(d.getAllData(","));
				}
			}
		}

		if ((notExactDataSetIndex != null) && !notExactDataSetIndex.isEmpty()) {
			out.add("==both-notexact==");
			out.addAll(
				dumpData(model.getName(), notExactDataSetIndex, maxLength));

			if (executionMode.contains(ExecutionMode.DUMP_ALL_OBJECTS_TO_LOG)) {
				System.out.println("==both-notexact(index)==");

				for (Data d : notExactDataSetIndex) {
					System.out.println(d.getAllData(","));
				}

				System.out.println("==both-notexact(liferay)==");

				for (Data d : notExactDataSetLiferay) {
					System.out.println(d.getAllData(","));
				}
			}

			if (reindex) {
				Map<Data, String> errors = model.reindex(notExactDataSetIndex);

				for (Entry<Data, String> error : errors.entrySet()) {
					out.add(
						"\t" + "ERROR reindexing " + error.getKey() +
						"EXCEPTION" + error.getValue());
				}
			}
		}

		if ((liferayOnlyData != null) && !liferayOnlyData.isEmpty()) {
			out.add("==only liferay==");
			out.addAll(dumpData(model.getName(), liferayOnlyData, maxLength));

			if (executionMode.contains(ExecutionMode.DUMP_ALL_OBJECTS_TO_LOG)) {
				System.out.println("==only liferay==");

				for (Data d : liferayOnlyData) {
					System.out.println(d.getAllData(","));
				}
			}

			if (reindex) {
				Map<Data, String> errors = model.reindex(liferayOnlyData);

				for (Entry<Data, String> error : errors.entrySet()) {
					out.add(
						"\t" + "ERROR reindexing " + error.getKey() +
						"EXCEPTION" + error.getValue());
				}
			}
		}

		if ((indexOnlyData != null) && !indexOnlyData.isEmpty()) {
			out.add("==only index==");
			out.addAll(dumpData(model.getName(), indexOnlyData, maxLength));

			if (executionMode.contains(ExecutionMode.DUMP_ALL_OBJECTS_TO_LOG)) {
				System.out.println("==only index==");

				for (Data d : indexOnlyData) {
					System.out.println(d.getAllData(","));
				}
			}

			if (removeOrphan) {
				Map<Data, String> errors = model.deleteAndCheck(indexOnlyData);

				for (Entry<Data, String> error : errors.entrySet()) {
					out.add(
						"\tERROR deleting from index " + error.getKey() +
						"EXCEPTION" + error.getValue());
				}
			}
		}

		return out;
	}

	protected List<String> dumpData(
			int maxLength, Set<ExecutionMode> executionMode,
			Map<IndexCheckerModel, Map<Long, Tuple>> resultDataMap)
		throws SystemException {

		List<String> out = new ArrayList<String>();

		int i = 0;

		for (Entry<IndexCheckerModel, Map<Long, Tuple>> entryResultDataMap :
				resultDataMap.entrySet()) {

			IndexCheckerModel model = entryResultDataMap.getKey();

			out.add("\n---------------");
			out.add("ClassName["+ i +"]: "+ model.getName());
			out.add("---------------");

			if (executionMode.contains(ExecutionMode.DUMP_ALL_OBJECTS_TO_LOG)) {
				System.out.println("\n---------------");
				System.out.println("ClassName["+ i +"]: "+ model.getName());
				System.out.println("---------------");
			}

			i++;

			Map<Long, Tuple> modelDataMap = entryResultDataMap.getValue();

			for (Entry<Long, Tuple> entry : modelDataMap.entrySet()) {
				String groupTitle = null;
				Group group = GroupLocalServiceUtil.fetchGroup(entry.getKey());

				if ((group == null) &&
					executionMode.contains(ExecutionMode.GROUP_BY_SITE)) {

					groupTitle = "N/A";
				}
				else if (group != null) {
					groupTitle = group.getGroupId() + " - " + group.getName();
				}

				if (groupTitle != null) {
					out.add("***GROUP: " + groupTitle);

					if (executionMode.contains(
							ExecutionMode.DUMP_ALL_OBJECTS_TO_LOG)) {

						System.out.println("***GROUP: " + groupTitle);
					}
				}

				Tuple data = entry.getValue();

				out.addAll(dumpData(model, data, maxLength, executionMode));
			}
		}

		return out;
	}

	protected List<String> dumpData(
		String entryClassName, Collection<Data> liferayData, int maxLength) {

		List<String> out = new ArrayList<String>();

		List<Long> valuesPK = new ArrayList<Long>();
		List<Long> valuesRPK = new ArrayList<Long>();

		for (Data value : liferayData) {
			valuesPK.add(value.getPrimaryKey());

			if (value.getResourcePrimKey() != -1) {
				valuesRPK.add(value.getResourcePrimKey());
			}
		}

		String listPK = IndexCheckerUtil.getListValues(valuesPK, maxLength);
		out.add(
			entryClassName+"\n\tnumber of primary keys: "+valuesPK.size()+
			"\n\tprimary keys values: ["+listPK+"]");

		Set<Long> valuesRPKset = new HashSet<Long>(valuesRPK);

		if (valuesRPKset.size()>0) {
			String listRPK = IndexCheckerUtil.getListValues(
				valuesRPKset, maxLength);
			out.add(
				entryClassName+"\n\tnumber of resource primary keys: "+
				valuesRPKset.size()+"\n\tresource primary keys values: ["+
				listRPK+"]");
		}

		return out;
	}

	protected Map<Long, Tuple> getData(
			IndexWrapper indexWrapper, long companyId, List<Group> groups,
			IndexCheckerModel icModel, Set<ExecutionMode> executionMode)
		throws Exception {

		Map<Long, Tuple> dataMap = new LinkedHashMap<Long, Tuple>();

		if (icModel.hasGroupId() &&
			executionMode.contains(ExecutionMode.GROUP_BY_SITE)) {

			Map<Long, Set<Data>> indexDataMap =
				indexWrapper.getClassNameDataByGroupId(icModel);

			for (Group group : groups) {
				Set<Data> indexData = indexDataMap.get(group.getGroupId());

				if (indexData == null) {
					indexData = new HashSet<Data>();
				}

				List<Group> listGroupAux = new ArrayList<>();
				listGroupAux.add(group);

				Tuple data =
					getData(
						companyId, listGroupAux, icModel, indexData,
						executionMode);

				if (data != null) {
					dataMap.put(group.getGroupId(), data);
				}
			}
		}
		else {
			Set<Data> indexData = indexWrapper.getClassNameData(icModel);

			Tuple data = getData(
				companyId, groups, icModel, indexData, executionMode);

			if (data != null) {
				dataMap.put(0L, data);
			}
		}

		return dataMap;
	}

	protected Map<IndexCheckerModel, Map<Long, Tuple>> getData(
		List<String> out, IndexWrapper indexWrapper, long companyId,
		List<Group> groups, Map<String, Model> modelMap,
		Set<ExecutionMode> executionMode) {

		Map<IndexCheckerModel, Map<Long, Tuple>> resultDataMap =
			new LinkedHashMap<>();

		for (Model model : modelMap.values()) {
			try {
				IndexCheckerModel icModel;
				try {
					icModel = (IndexCheckerModel)model;
				}
				catch (Exception e) {
					/* TODO DEBUG */
					System.err.println(
						"Model: " + model.getName() + " EXCEPTION: " +
							e.getClass() + " - " + e.getMessage());

					e.printStackTrace();
					icModel = null;
				}

				if (icModel == null) {
					continue;
				}

				Map<Long, Tuple> modelDataMap =
					getData(
						indexWrapper, companyId, groups, icModel,
						executionMode);

				resultDataMap.put(icModel, modelDataMap);
			}
			catch (Exception e) {
				out.add(
					"Model: " + model.getName() + " EXCEPTION: " +
						e.getClass() + " - " + e.getMessage());
				System.err.println(
					"Model: " + model.getName() + " EXCEPTION: " +
						e.getClass() + " - " + e.getMessage());
				e.printStackTrace();
			}
		}

		return resultDataMap;
	}

	protected Tuple getData(
			long companyId, List<Group> groups, IndexCheckerModel icModel,
			Set<Data> indexData, Set<ExecutionMode> executionMode)
		throws Exception {

		List<Long> listGroupId = new ArrayList<>();

		for (Group group : groups) {
			listGroupId.add(group.getGroupId());
		}

		Criterion filter = icModel.getCompanyGroupFilter(
			companyId, listGroupId);

		Set<Data> liferayData = new HashSet<Data>(
			icModel.getLiferayData(filter).values());

		Tuple data = null;

		if ((indexData.size() > 0) || (liferayData.size() > 0)) {
			data = getData(liferayData, indexData, executionMode);
		}

		return data;
	}

	protected Tuple getData(
		Set<Data> liferayData, Set<Data> indexData,
		Set<ExecutionMode> executionMode) {

		boolean reindex = executionMode.contains(ExecutionMode.REINDEX);
		boolean removeOrphan = executionMode.contains(
			ExecutionMode.REMOVE_ORPHAN);

		Data[] bothArrSetLiferay = IndexCheckerUtil.getBothDataArray(
			liferayData, indexData);
		Data[] bothArrSetIndex = IndexCheckerUtil.getBothDataArray(
			indexData, liferayData);

		Set<Data> exactDataSetIndex = new HashSet<Data>();
		Set<Data> exactDataSetLiferay = new HashSet<Data>();
		Set<Data> notExactDataSetIndex = new HashSet<Data>();
		Set<Data> notExactDataSetLiferay = new HashSet<Data>();

		if ((executionMode.contains(ExecutionMode.SHOW_BOTH_EXACT) ||
			 executionMode.contains(ExecutionMode.SHOW_BOTH_NOTEXACT) ||
			 reindex) && (bothArrSetIndex.length > 0) &&
			(bothArrSetLiferay.length > 0)) {

			for (int i = 0; i<bothArrSetIndex.length; i++) {
				Data dataIndex = bothArrSetIndex[i];
				Data dataLiferay = bothArrSetLiferay[i];

				if (!dataIndex.equals(dataLiferay)) {
					throw new RuntimeException("Inconsistent data");
				}
				else if (dataIndex.exact(dataLiferay)) {
					if (executionMode.contains(ExecutionMode.SHOW_BOTH_EXACT)) {
						exactDataSetIndex.add(dataIndex);
						exactDataSetLiferay.add(dataLiferay);
					}
				}
				else if (executionMode.contains(
							ExecutionMode.SHOW_BOTH_NOTEXACT)) {

					notExactDataSetIndex.add(dataIndex);
					notExactDataSetLiferay.add(dataLiferay);
				}
			}
		}

		Set<Data> liferayOnlyData = liferayData;
		Set<Data> indexOnlyData = indexData;
		Set<Data> bothDataSet = new HashSet<Data>(indexData);
		bothDataSet.retainAll(liferayData);

		if (executionMode.contains(ExecutionMode.SHOW_LIFERAY) || reindex) {
			liferayOnlyData.removeAll(bothDataSet);
		}
		else {
			liferayOnlyData = new HashSet<Data>();
		}

		if (executionMode.contains(ExecutionMode.SHOW_INDEX) || removeOrphan) {
			indexOnlyData.removeAll(bothDataSet);
		}
		else {
			indexOnlyData = new HashSet<Data>();
		}

		return new Tuple(
			exactDataSetIndex, exactDataSetLiferay, notExactDataSetIndex,
			notExactDataSetLiferay, liferayOnlyData, indexOnlyData);
	}

}