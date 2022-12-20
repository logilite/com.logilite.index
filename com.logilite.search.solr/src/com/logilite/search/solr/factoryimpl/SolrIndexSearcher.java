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

package com.logilite.search.solr.factoryimpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.NoOpResponseParser;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.request.schema.FieldTypeDefinition;
import org.apache.solr.client.solrj.request.schema.SchemaRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.schema.FieldTypeRepresentation;
import org.apache.solr.client.solrj.response.schema.SchemaResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.NamedList;
import org.compiere.model.MSysConfig;
import org.compiere.util.CLogger;
import org.compiere.util.Util;

import com.logilite.search.factory.IIndexSearcher;
import com.logilite.search.model.MIndexingConfig;

public class SolrIndexSearcher implements IIndexSearcher
{
	public static CLogger		log							= CLogger.getCLogger(SolrIndexSearcher.class);

	public static final String	SYSCONFIG_SOLR_MAXROWS		= "SOLR_MAXROWS";
	public static final String	SYSCONFIG_SOLR_STARTFROM	= "SOLR_STARTFROM";
	public static final String	SYSCONFIG_SOLR_FRAGMENTSIZE	= "SOLR_FRAGMENTSIZE";

	public HashSet<String>		fieldSet					= new HashSet<String>();
	public HashSet<String>		fieldTypeSet				= new HashSet<String>();

	private SolrClient			server						= null;
	private MIndexingConfig		indexingConfig				= null;

	/**
	 * Initialize solr server
	 * 
	 * @param MIndexingConfig
	 */
	@Override
	public void init(MIndexingConfig indexingConfig)
	{
		try
		{
			this.indexingConfig = indexingConfig;

			PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
			connManager.setMaxTotal(100);
			connManager.setDefaultMaxPerRoute(20);

			CredentialsProvider credsProvider = new BasicCredentialsProvider();
			credsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(indexingConfig.getUserName(), indexingConfig.getPassword()));

			HttpClient httpClient = HttpClientBuilder	.create()
														.setConnectionManager(connManager)
														.setDefaultCredentialsProvider(credsProvider)
														.addInterceptorFirst(new PreemptiveAuthInterceptor())
														.build();

			server = new HttpSolrClient.Builder(indexingConfig.getIndexServerUrl())
																					.withHttpClient(httpClient)
																					.build();
			server.ping();

			//
			try
			{
				// Build Schema Fields Type set
				buildSolrSchemaFieldTypesSet();

				// Build Schema Fields set
				buildSolrSchemaFieldsSet();
			}
			catch (Exception e)
			{
				log.log(Level.SEVERE, "Issue while build/create fieldtype/field in solr schema: ", e.getLocalizedMessage());
				throw new AdempiereException("Issue while build/create fieldtype/field in solr schema: " + e.getLocalizedMessage(), e);
			}
		}
		catch (SolrServerException e)
		{
			log.log(Level.SEVERE, "Solr server is not started: ", e);
			throw new AdempiereException("Solr server is not started: " + e.getLocalizedMessage(), e);
		}
		catch (IOException e)
		{
			log.log(Level.SEVERE, "Fail to ping solr Server ", e);
			throw new AdempiereException("Fail to ping solr Server: " + e.getLocalizedMessage(), e);
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Fail to initialize solr Server OR Invalid Username or Password ", e);
			throw new AdempiereException("Fail to initialize solr Server OR Invalid Username or Password.", e);
		}
	} // init

	/**
	 * Build Solr schema for Fields
	 */
	public void buildSolrSchemaFieldsSet()
	{
		final SchemaRequest.Fields fieldsSchemaRequest = new SchemaRequest.Fields();
		fieldSet.clear();
		try
		{
			SchemaResponse.FieldsResponse fieldsResponse = fieldsSchemaRequest.process(server);
			List<Map<String, Object>> fields = fieldsResponse.getFields();
			for (Map<String, Object> map : fields)
			{
				fieldSet.add((String) map.get("name"));
			}
		}
		catch (SolrServerException e)
		{
			log.log(Level.SEVERE, "Solr server is not started: ", e);
			throw new AdempiereException("Solr server is not started: " + e.getLocalizedMessage(), e);
		}
		catch (IOException e)
		{
			log.log(Level.SEVERE, "Fail to retried fields set: ", e);
			throw new AdempiereException("Fail to retried fields set: " + e.getLocalizedMessage(), e);
		}

	} // buildSolrSchemaFieldsSet

	/**
	 * Build Solr schema for FieldTypes
	 */
	public void buildSolrSchemaFieldTypesSet()
	{
		SchemaRequest.FieldTypes fTypes = new SchemaRequest.FieldTypes();
		fieldTypeSet.clear();
		try
		{
			SchemaResponse.FieldTypesResponse ftRes = fTypes.process(server);
			List<FieldTypeRepresentation> fieldTypes = ftRes.getFieldTypes();
			for (FieldTypeRepresentation ftRepr : fieldTypes)
			{
				Map<String, Object> ftAttrib = ftRepr.getAttributes();
				fieldTypeSet.add(ftAttrib.get("name").toString());
			}
		}
		catch (SolrServerException e)
		{
			log.log(Level.SEVERE, "Solr server is not started: ", e);
			throw new AdempiereException("Solr server is not started: " + e.getLocalizedMessage(), e);
		}
		catch (IOException e)
		{
			log.log(Level.SEVERE, "Fail to retried field types: ", e);
			throw new AdempiereException("Fail to retried field type: " + e.getLocalizedMessage(), e);
		}
	} // buildSolrSchemaFieldTypesSet

	/**
	 * Create FieldType in solr schema
	 * 
	 * @param fieldTypeAttribute - new fields type attribute
	 */
	public void createFieldTypeInIndexSchema(Map<String, Object> fieldTypeAttribute)
	{
		if (fieldTypeAttribute != null && fieldTypeAttribute.size() > 0)
		{
			FieldTypeDefinition ftDefinition = new FieldTypeDefinition();
			ftDefinition.setAttributes(fieldTypeAttribute);

			SchemaRequest.AddFieldType ft = new SchemaRequest.AddFieldType(ftDefinition);
			try
			{
				ft.process(server);
			}
			catch (SolrServerException e)
			{
				log.log(Level.SEVERE, "Solr server is not started: ", e);
				throw new AdempiereException("Solr server is not started: " + e.getLocalizedMessage(), e);
			}
			catch (IOException e)
			{
				log.log(Level.SEVERE, "Fail to add field type for : " + fieldTypeAttribute.toString(), e);
				throw new AdempiereException("Fail to add field type for : " + fieldTypeAttribute.toString() + ", Error: " + e.getLocalizedMessage(), e);
			}

			//
			buildSolrSchemaFieldTypesSet();
		}
	} // createFieldTypeInIndexSchema

	/**
	 * Create Field in solr schema
	 * 
	 * @param fieldAttribute - new field attribute
	 */
	public void createFieldsInIndexSchema(Map<String, Object> fieldAttribute)
	{
		if (fieldAttribute.size() > 0)
		{
			SchemaRequest.AddField schemaRequest = new SchemaRequest.AddField(fieldAttribute);
			try
			{
				schemaRequest.process(server);
			}
			catch (SolrServerException e)
			{
				log.log(Level.SEVERE, "Solr server is not started: ", e);
				throw new AdempiereException("Solr server is not started: " + e.getLocalizedMessage(), e);
			}
			catch (IOException e)
			{
				log.log(Level.SEVERE, "Fail to add field in schema for : " + fieldAttribute.toString(), e);
				throw new AdempiereException("Fail to add field in schema for : " + fieldAttribute.toString() + ", Error: " + e.getLocalizedMessage(), e);
			}

			//
			buildSolrSchemaFieldsSet();
		}
	} // createFieldsInIndexSchema

	/**
	 * Check Server is Up and running
	 * 
	 * @throws AdempiereException
	 */
	public void checkServerIsUp() throws AdempiereException
	{
		try
		{
			if (server.ping() == null)
				init(indexingConfig);
		}
		catch (SolrServerException | IOException e)
		{
			log.log(Level.SEVERE, "Fail to ping solr Server ", e);
			throw new AdempiereException("Fail to ping solr Server: " + e.getLocalizedMessage(), e);
		}
	} // checkServerIsUp

	/******************************************************
	 * Create index from content data
	 ******************************************************/
	@Override
	public void indexContent(Map<String, Object> indexValue)
	{
		checkServerIsUp();

		log.log(Level.INFO, "Solr index creation value = " + indexValue.toString());

		boolean isReBuildFieldSet = false;

		SolrInputDocument document = new SolrInputDocument();
		for (Entry<String, Object> row : indexValue.entrySet())
		{
			Object value = row.getValue();
			if (row.getKey() != null && value != null)
			{
				if (!fieldSet.contains(row.getKey()) && value instanceof String)
				{
					// In solr add a new field and having numeric value in string object then solr
					// side it should create as text_general type field instead of tlongs.
					String strValue = (String) value;
					try
					{
						strValue = " " + Integer.parseInt(value.toString());
					}
					catch (NumberFormatException e)
					{
						// Do nothing
					}
					document.addField(row.getKey(), strValue);
					isReBuildFieldSet = true;
				}
				else
				{
					document.addField(row.getKey(), value);
				}
			}
		}

		try
		{
			log.log(Level.INFO, "Solr index creation value = " + document.toString());

			server.add(document);
			server.commit();
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Fail to create Indexing: ", e);
			throw new AdempiereException("Fail to create Indexing: " + e.getLocalizedMessage(), e);
		}

		if (isReBuildFieldSet)
			buildSolrSchemaFieldsSet();
	} // indexContent

	/******************************************************
	 * Delete index from indexing server
	 ******************************************************/
	@Override
	public void deleteAllIndex()
	{
		deleteIndexByQuery("*:*");
	} // deleteAllIndex

	@Override
	public void deleteIndexByField(String fieldName, String fieldValue)
	{
		deleteIndexByQuery(fieldName + ":" + fieldValue);
	} // deleteIndexByField

	@Override
	public void deleteIndexByQuery(String query)
	{
		checkServerIsUp();

		try
		{
			server.deleteByQuery(query);
			server.commit();
		}
		catch (SolrServerException e)
		{
			log.log(Level.SEVERE, "Solr server connection failure, Query=" + query, e);
			throw new AdempiereException("Solr server connection failure, Query=" + query + " Error:" + e.getLocalizedMessage(), e);
		}
		catch (IOException e)
		{
			log.log(Level.SEVERE, "Solr document delete failure, Query=" + query, e);
			throw new AdempiereException("Solr document delete failure, Query=" + query + " Error:" + e.getLocalizedMessage(), e);
		}
	} // deleteIndexByQuery

	/******************************************************
	 * Search content from index based on query definition
	 ******************************************************/
	@Override
	public Object searchIndexNoRestriction(String query)
	{
		checkServerIsUp();

		log.log(Level.INFO, "Solr search query = " + query);

		SolrDocumentList documents = null;
		try
		{
			SolrQuery solrQuery = new SolrQuery(query);
			solrQuery.setRows(Integer.MAX_VALUE);
			QueryResponse response = server.query(solrQuery);
			documents = response.getResults();
		}
		catch (SolrServerException e)
		{
			log.log(Level.SEVERE, "Solr server connection failure, Query=" + query, e);
			throw new AdempiereException("Solr server connection failure, Query=" + query + " Error:" + e.getLocalizedMessage(), e);
		}
		catch (IOException e)
		{
			log.log(Level.SEVERE, "Solr document searching failed, Query=" + query, e);
			throw new AdempiereException("Solr document searching failed, Query=" + query + " Error:" + e.getLocalizedMessage(), e);
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Solr searching failed, Query=" + query, e);
			throw new AdempiereException("Solr searching failed, Query=" + query + " Error:" + e.getLocalizedMessage(), e);
		}

		return documents;
	}

	/**
	 * Get json String from Solr
	 * 
	 * @param queryString
	 */
	@Override
	public String searchIndexJson(String queryString)
	{
		int maxRows = MSysConfig.getIntValue(SYSCONFIG_SOLR_MAXROWS, 100);
		return searchIndexJson(queryString, maxRows);
	} // searchIndexJson

	/**
	 * Get json String from Solr
	 * 
	 * @param queryString
	 * @param maxRows     max rows allow
	 */
	@Override
	public String searchIndexJson(String queryString, int maxRows)
	{
		int startFrom = MSysConfig.getIntValue(SYSCONFIG_SOLR_STARTFROM, 0);
		return searchIndexJson(queryString, maxRows, startFrom);
	} // searchIndexJson

	/**
	 * Get json String from Solr
	 * 
	 * @param queryString
	 * @param maxRows     max rows allow
	 * @param startFrom   index start from
	 */
	@Override
	public String searchIndexJson(String queryString, int maxRows, int startFrom)
	{
		int fragsize = MSysConfig.getIntValue(SYSCONFIG_SOLR_FRAGMENTSIZE, 10000);
		return searchIndexJson(queryString, maxRows, startFrom, fragsize);
	} // searchIndexJson

	/**
	 * Get json String from Solr
	 * 
	 * @param queryString
	 * @param maxRows     max rows allow
	 * @param Startfrom   index start from
	 * @param fragsize    fragment size
	 */
	@Override
	public String searchIndexJson(String queryString, int maxRows, int startFrom, int fragsize)
	{
		checkServerIsUp();

		//
		queryString = escapeMetaCharacters(queryString);
		log.log(Level.INFO, "Solr search json = " + queryString);

		SolrQuery query = new SolrQuery(queryString);
		query.setRows(maxRows);
		query.setStart(startFrom);
		query.setHighlight(true);
		query.setHighlightFragsize(fragsize);
		QueryRequest req = new QueryRequest(query);

		NoOpResponseParser rawJsonResponseParser = new NoOpResponseParser();
		rawJsonResponseParser.setWriterType("json");
		req.setResponseParser(rawJsonResponseParser);

		NamedList<Object> resp = null;
		try
		{
			log.log(Level.INFO, "Solr search json QueryRequest = " + req.toString());
			resp = server.request(req);
		}
		catch (SolrServerException e)
		{
			log.log(Level.SEVERE, "Solr server connection failure: ", e);
			throw new AdempiereException("Solr server connection failure:  " + e.getLocalizedMessage());
		}
		catch (IOException e)
		{
			log.log(Level.SEVERE, "Solr Document(s) fetching failure: ", e);
			throw new AdempiereException("Solr Document(s) fetching failure:  " + e.getLocalizedMessage());
		}
		return (String) resp.get("response");
	} // searchIndexJson

	/**
	 * Get solr document from Solr
	 * 
	 * @param  queryString
	 * @return             List of object as {@link SolrDocument}
	 */
	@Override
	public List<Object> searchIndexDocument(String queryString)
	{
		int maxRow = MSysConfig.getIntValue(SYSCONFIG_SOLR_MAXROWS, 100);
		return searchIndexDocument(queryString, maxRow);
	} // searchIndexDocument

	/**
	 * Get solr document from Solr with Max Row
	 * 
	 * @param  queryString
	 * @param  maxRow      rows allow
	 * @return             List of object as {@link SolrDocument}
	 */
	public List<Object> searchIndexDocument(String queryString, int maxRow)
	{
		checkServerIsUp();

		log.log(Level.INFO, "Solr search document = " + queryString);

		//
		SolrQuery solrQuery = new SolrQuery();
		solrQuery.setQuery(queryString);

		List<Object> solrDocList = new ArrayList<Object>();
		SolrIndexDataSet dataset = new SolrIndexDataSet(solrQuery, server, maxRow).execute();
		try
		{
			while (dataset.hasMore())
			{
				Object solrDoc = dataset.next();
				if (solrDoc != null)
					solrDocList.add((SolrDocument) solrDoc);
			}
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Searching content failure:", e);
		}
		finally
		{
			dataset.clear();
		}
		return solrDocList;
	} // searchIndexDocument

	/**
	 * Escape meta characters from query string
	 * 
	 * @param inputString
	 */
	public String escapeMetaCharacters(String inputString)
	{
		final String[] metaCharacters = { "\\", "^", "$", "{", "}", "[", "]", "(", ")", ".", "+", "?", "|", "<", ">", "-", "%", "#", "@", "!", "?", ",", "/" };
		String outputString = "";
		for (int i = 0; i < metaCharacters.length; i++)
		{
			if (inputString.contains(metaCharacters[i]))
			{
				outputString = inputString.replace(metaCharacters[i], "\\" + metaCharacters[i]);
				inputString = outputString;
			}
		}
		return inputString;
	} // escapeMetaCharacters

	/**
	 * Build Solr search Query
	 * 
	 * @param List of Parameters
	 */
	@Override
	public String buildSolrSearchQuery(HashMap<String, List<Object>> params)
	{
		StringBuffer query = new StringBuffer();

		for (Entry<String, List<Object>> row : params.entrySet())
		{
			String key = row.getKey();
			List<Object> value = row.getValue();

			if (value.size() == 2)
			{
				if (value.get(0) instanceof String && value.get(1) instanceof Boolean && value.get(1) == Boolean.TRUE)
				{
					query.append(" AND (").append(key + ":" + value.get(0) + ")");
				}
				else if (value.get(0) instanceof String || value.get(1) instanceof String)
				{
					query.append(" AND (").append(key + ":[" + value.get(0) + " TO " + value.get(1) + " ])");
				} // Handle condition when two boolean value passed.
				else if (value.get(0) instanceof Boolean || value.get(1) instanceof Boolean)
				{
					query.append(" AND (").append(key + ":" + value.get(0) + " OR ").append(key + ":" + value.get(1) + ")");
				}
				else if (value.get(1).equals("*"))
				{
					query.append(" AND (").append(key + ":[\"" + value.get(0) + "\" TO " + value.get(1) + " ])");
				}
				else
				{
					query.append(" AND (").append(key + ":[\"" + value.get(0) + "\" TO \"" + value.get(1) + "\" ])");
				}
			}
			else
			{
				if (value.get(0) instanceof String)
				{
					if (Util.isEmpty((String) value.get(0), true))
						query.append(" AND (").append(key + ":*)");
					else
						query.append(" AND (").append(key + ":*" + value.get(0) + "*)");
				}
				else
				{
					query.append(" AND (").append(key + ":\"" + value.get(0) + "\")");
				}
			}
		}

		if (query.length() > 0)
			query.delete(0, 5);
		else
			query.append("*:*");

		return query.toString();
	} // buildSolrSearchQuery

	/**
	 * @param  query
	 * @param  columnName
	 * @return            value of column in index server
	 */
	public String getColumnValue(String query, String columnName)
	{
		checkServerIsUp();

		String content = "";
		try
		{
			SolrQuery solrQuery = new SolrQuery();
			QueryResponse response = new QueryResponse();

			solrQuery.setQuery(query);
			response = server.query(solrQuery);
			SolrDocumentList documentList = response.getResults();
			ListIterator<SolrDocument> iterator = documentList.listIterator();
			while (iterator.hasNext())
			{
				SolrDocument document = iterator.next();
				Map<String, Collection<Object>> searchedContent = document.getFieldValuesMap();
				Iterator<String> fields = document.getFieldNames().iterator();
				while (fields.hasNext())
				{
					String field = fields.next();
					if (field.equalsIgnoreCase(columnName))
					{
						Collection<Object> values = searchedContent.get(field);
						Iterator<Object> value = values.iterator();

						while (value.hasNext())
						{
							String obj = (String) value.next();
							content = obj.toString();
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Searching content failure:", e);
		}

		return content;
	} // getColumnValue

	public HashSet<String> getFieldSet()
	{
		return fieldSet;
	}

	public HashSet<String> getFieldTypeSet()
	{
		return fieldTypeSet;
	}

	/**
	 * Class PreemptiveAuthInterceptor
	 */
	private class PreemptiveAuthInterceptor implements HttpRequestInterceptor
	{
		public void process(final HttpRequest request, final HttpContext context) throws HttpException, IOException
		{
			AuthState authState = (AuthState) context.getAttribute(HttpClientContext.TARGET_AUTH_STATE);

			// If no auth scheme available yet, try to initialize it preemptively
			if (authState.getAuthScheme() == null)
			{
				CredentialsProvider credsProvider = (CredentialsProvider) context.getAttribute(HttpClientContext.CREDS_PROVIDER);
				HttpHost targetHost = (HttpHost) context.getAttribute(HttpCoreContext.HTTP_TARGET_HOST);
				Credentials creds = credsProvider.getCredentials(new AuthScope(targetHost.getHostName(), targetHost.getPort()));
				if (creds == null)
					throw new HttpException("No credentials for preemptive authentication");
				authState.update(new BasicScheme(), creds);
			}
		} // process

	} // PreemptiveAuthInterceptor class

}
