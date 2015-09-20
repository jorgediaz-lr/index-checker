package com.jorgediaz.indexchecker.model;

import com.jorgediaz.indexchecker.data.Data;
import com.liferay.portal.kernel.dao.orm.Criterion;

import java.util.HashMap;
import java.util.Map;

public class NotIndexed extends IndexCheckerModel {

	@Override
	public Map<Long,Data> getLiferayData(Criterion filter) throws Exception {
		/* Return empty data as it is not indexed */
		return new HashMap<Long,Data>();
	}
}