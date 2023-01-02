/******************************************************************************
 * Copyright (C) 2016 Logilite Technologies LLP								  *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 *****************************************************************************/

package com.logilite.search.factory;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.logilite.search.model.MIndexingConfig;

public interface IIndexSearcher
{
	// Initiate
	public void init(MIndexingConfig indexingConfig);

	public void createFieldTypeInIndexSchema(Map<String, Object> fieldTypeAttribute);

	public void createFieldsInIndexSchema(Map<String, Object> fieldAttribute);

	// Delete
	public void deleteAllIndex();

	public void deleteIndexByField(String fieldName, String fieldValue);

	public void deleteIndexByQuery(String query);

	// Search
	public Object searchIndexNoRestriction(String queryString);

	public String searchIndexJson(String queryString);

	public String searchIndexJson(String queryString, int maxRows);

	public String searchIndexJson(String queryString, int maxRows, int startFrom);

	public String searchIndexJson(String queryString, int maxRows, int startFrom, int fragsize);

	public List<Object> searchIndexDocument(String queryString);

	public List<Object> searchIndexDocument(String queryString, int maxRow);

	// Indexing document
	public void indexContent(Map<String, Object> solrValue);

	// Miscellaneous
	public String buildSolrSearchQuery(HashMap<String, List<Object>> params);

	public Object getColumnValue(String query, String columnName);

	public HashSet<String> getFieldTypeSet();

	public HashSet<String> getFieldSet();

	public String getParseDocumentContent(File file);

	public HashSet<Integer> searchIndex(String query, String searchFieldName);

}
