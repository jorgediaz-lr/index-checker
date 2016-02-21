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

package jorgediazest.indexchecker.model;

import com.liferay.portal.kernel.dao.orm.Criterion;
import com.liferay.portal.kernel.dao.orm.Junction;
import com.liferay.portal.kernel.dao.orm.Property;
import com.liferay.portal.kernel.dao.orm.PropertyFactoryUtil;
import com.liferay.portal.kernel.dao.orm.RestrictionsFactoryUtil;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portlet.journal.model.JournalArticleConstants;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import jorgediazest.util.data.Data;

/**
 * @author Jorge Díaz
 */
public class JournalArticle extends IndexCheckerModel {

	@Override
	public Criterion generateQueryFilter() {
		Junction junction = RestrictionsFactoryUtil.disjunction();

		Junction approvedArticlesJunction = RestrictionsFactoryUtil.conjunction();

		Property displayDateProperty = PropertyFactoryUtil.forName("displayDate");

		approvedArticlesJunction.add(displayDateProperty.lt(new Date()));

		Property statusProperty = PropertyFactoryUtil.forName("status");

		approvedArticlesJunction.add(statusProperty.eq(WorkflowConstants.STATUS_APPROVED));

		junction.add(approvedArticlesJunction);

		Junction draftArticlesJunction = RestrictionsFactoryUtil.conjunction();

		Property versionProperty = PropertyFactoryUtil.forName("version");

		draftArticlesJunction.add(versionProperty.eq(JournalArticleConstants.VERSION_DEFAULT));

		draftArticlesJunction.add(statusProperty.eq(WorkflowConstants.STATUS_DRAFT));

		junction.add(draftArticlesJunction);

		Junction expiredArticlesJunction = RestrictionsFactoryUtil.conjunction();

		expiredArticlesJunction.add(statusProperty.eq(WorkflowConstants.STATUS_EXPIRED));

		junction.add(expiredArticlesJunction);

		return junction;
	}

	@Override
	public Map<Data, String> reindex(Collection<Data> dataCollection) {

		Map<Long, Data> articles = new HashMap<Long, Data>();

		for (Data data : dataCollection) {
			articles.put(data.getResourcePrimKey(), data);
		}

		return super.reindex(articles.values());
	}

}