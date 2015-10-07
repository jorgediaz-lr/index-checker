package com.jorgediaz.indexchecker;

import com.jorgediaz.indexchecker.data.Data;

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;
public class IndexCheckerUtil {

	protected static Data[] getBothDataArray(Set<Data> set1, Set<Data> set2) {
		Set<Data> both = new TreeSet<Data>(set1);
		both.retainAll(set2);
		return both.toArray(new Data[0]);
	}

	protected static String getListValues(
		Collection<Long> values, int maxLength) {

		String list = "";

		for (Long value : values) {
			if ("".equals(list)) {
				list = "" + value;
			}
			else {
				list = list + "," + value;
			}
		}

		if (list.length()>maxLength && (maxLength > 3)) {
			list = list.substring(0, maxLength-3) + "...";
		}

		return list;
	}

}