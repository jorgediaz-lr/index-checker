package com.jorgediaz.indexchecker.model;

import com.jorgediaz.indexchecker.data.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotIndexed extends IndexCheckerModel {

	@Override
	public Map<Long,Data> getLiferayData(Long companyId, List<Long> listGroupId) throws Exception {
		/* Return empty data as it is not indexed */
		return new HashMap<Long,Data>();
	}
}