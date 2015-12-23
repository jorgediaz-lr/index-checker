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

package jorgediazest.indexchecker.index;

/**
 * @author Jorge Díaz
 */
public abstract class DocumentWrapper {

	public DocumentWrapper(Object document) {
		this.document = document;
	}

	public String getEntryClassName() {
		return get("entryClassName");
	}

	abstract public String get(String key);

	protected Object document = null;

}