/******************************************************************************
 * Copyright (C) 2016 Logilite Technologies LLP * This program is free software;
 * you can redistribute it and/or modify it * under the terms version 2 of the
 * GNU General Public License as published * by the Free Software Foundation.
 * This program is distributed in the hope * that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied * warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. * See the GNU General Public License for
 * more details. * You should have received a copy of the GNU General Public
 * License along * with this program; if not, write to the Free Software
 * Foundation, Inc., * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA. *
 *****************************************************************************/

package com.logilite.search.solr.factoryimpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.impl.conn.SchemeRegistryFactory;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.BinaryRequestWriter;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.impl.NoOpResponseParser;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.NamedList;
import org.compiere.model.MSysConfig;
import org.compiere.util.CLogger;

import com.logilite.search.factory.IIndexSearcher;
import com.logilite.search.model.MIndexingConfig;

public class SolrIndexSearcher implements IIndexSearcher
{

	public static CLogger	log				= CLogger.getCLogger(SolrIndexSearcher.class);

	@SuppressWarnings("deprecation")
	private HttpSolrServer	server			= null;
	private MIndexingConfig	indexingConfig	= null;

	@SuppressWarnings("deprecation")
	@Override
	/**
	 * 	Initialize solr server
	 * 	@param MIndexingConfig
	 */
	public void init(MIndexingConfig indexingConfig)
	{
		try
		{
			this.indexingConfig = indexingConfig;
			PoolingClientConnectionManager cxMgr = new PoolingClientConnectionManager(SchemeRegistryFactory.createDefault());
			cxMgr.setMaxTotal(100);
			cxMgr.setDefaultMaxPerRoute(20);

			DefaultHttpClient httpclient = new DefaultHttpClient(cxMgr);
			httpclient.addRequestInterceptor(new PreemptiveAuthInterceptor(), 0);
			httpclient.getCredentialsProvider().setCredentials(AuthScope.ANY,
					new UsernamePasswordCredentials(indexingConfig.getUserName(), indexingConfig.getPassword()));

			server = new HttpSolrServer(indexingConfig.getIndexServerUrl(), httpclient);
			server.setRequestWriter(new BinaryRequestWriter());
			server.setAllowCompression(true);

			server.ping();
		}
		catch (SolrServerException e)
		{
			log.log(Level.SEVERE, "Solr server is not started ", e);
			throw new AdempiereException("Solr server is not started: " + e.getLocalizedMessage());
		}
		catch (IOException e)
		{
			log.log(Level.SEVERE, "Fail to ping solr Server ", e);
			throw new AdempiereException("Fail to ping solr Server: " + e.getLocalizedMessage());
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Fail to initialize solr Server OR Invalid Username or Password ", e);
			throw new AdempiereException("Fail to initialize solr Server OR Invalid Username or Password ");
		}
	} // init

	@Override
	public void indexContent(Map<String, Object> indexValue)
	{
		try
		{
			try
			{
				if (server.ping() == null)
					init(indexingConfig);
			}
			catch (SolrServerException | IOException e)
			{
				log.log(Level.SEVERE, "Fail to ping solr Server ", e);
				throw new AdempiereException("Fail to ping solr Server: " + e.getLocalizedMessage());
			}

			SolrInputDocument document = new SolrInputDocument();

			for (Entry<String, Object> row : indexValue.entrySet())
			{
				if (row.getKey() != null && row.getValue() != null)
				{
					document.addField(row.getKey(), row.getValue());
				}
			}
			server.add(document);
			server.commit();
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Indexing failure: ", e);
			throw new AdempiereException("Indexing failure: " + e.getLocalizedMessage());
		}
	} // indexContent

	/**
	 * Delete by Id
	 * 
	 * @param Solr ID name
	 * @param ID
	 */
	@Override
	public void deleteIndexByID(String solrStr, String index_ID)
	{
		try
		{
			try
			{
				if (server.ping() == null)
					init(indexingConfig);
			}
			catch (SolrServerException | IOException e)
			{
				log.log(Level.SEVERE, "Fail to ping solr Server ", e);
				throw new AdempiereException("Fail to ping solr Server: " + e.getLocalizedMessage());
			}

			server.deleteByQuery(solrStr + index_ID);
			server.commit();
		}
		catch (SolrServerException e)
		{
			log.log(Level.SEVERE, "Solr server connection failure: ", e);
			throw new AdempiereException("Solr server connection failure:  " + e.getLocalizedMessage());
		}
		catch (IOException e)
		{
			log.log(Level.SEVERE, "Solr Document delete failure: ", e);
			throw new AdempiereException("Solr Document delete failure:  " + e.getLocalizedMessage());
		}

	} // deleteIndexByID

	private class PreemptiveAuthInterceptor implements HttpRequestInterceptor
	{

		@SuppressWarnings("deprecation")
		public void process(final HttpRequest request, final HttpContext context) throws HttpException, IOException
		{
			AuthState authState = (AuthState) context.getAttribute(ClientContext.TARGET_AUTH_STATE);

			// If no auth scheme avaialble yet, try to initialize it
			// preemptively
			if (authState.getAuthScheme() == null)
			{
				CredentialsProvider credsProvider = (CredentialsProvider) context.getAttribute(ClientContext.CREDS_PROVIDER);
				HttpHost targetHost = (HttpHost) context.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
				Credentials creds = credsProvider.getCredentials(new AuthScope(targetHost.getHostName(), targetHost.getPort()));
				if (creds == null)
					throw new HttpException("No credentials for preemptive authentication");
				authState.setAuthScheme(new BasicScheme());
				authState.setCredentials(creds);
			}

		} // process
	}

	/**
	 * Delete solr Index
	 */
	@Override
	public void deleteAllIndex()
	{
		try
		{
			try
			{
				if (server.ping() == null)
					init(indexingConfig);
			}
			catch (SolrServerException | IOException e)
			{
				log.log(Level.SEVERE, "Fail to ping solr Server ", e);
				throw new AdempiereException("Fail to ping solr Server: " + e.getLocalizedMessage());
			}

			server.deleteByQuery("*:*");
			server.commit();
		}
		catch (SolrServerException e)
		{
			log.log(Level.SEVERE, "Solr server connection failure: ", e);
			throw new AdempiereException("Solr server connection failure:  " + e.getLocalizedMessage());
		}
		catch (IOException e)
		{
			log.log(Level.SEVERE, "Solr Documents delete failure: ", e);
			throw new AdempiereException("Solr Documents delete failure:  " + e.getLocalizedMessage());
		}
	} // deleteAllIndex

	/**
	 * Get json String from Solr
	 * 
	 * @param Query String
	 */
	@Override
	public String searchIndexJson(String queryString)
	{
		int maxRows = MSysConfig.getIntValue("SOLR_MAXROWS", 100);
		return searchIndexJson(queryString, maxRows);
	} // searchIndexJson

	/**
	 * Get json String from Solr
	 * 
	 * @param Query String
	 * @param max rows allow
	 */
	@Override
	public String searchIndexJson(String queryString, int maxRows)
	{
		int startFrom = MSysConfig.getIntValue("SOLR_STARTFROM", 0);
		return searchIndexJson(queryString, maxRows, startFrom);
	} // searchIndexJson

	/**
	 * Get json String from Solr
	 * 
	 * @param Query String
	 * @param max rows allow
	 * @param Startfrom
	 */
	@Override
	public String searchIndexJson(String queryString, int maxRows, int startFrom)
	{
		int fragsize = MSysConfig.getIntValue("SOLR_FRAGMENTSIZE", 10000);
		return searchIndexJson(queryString, maxRows, startFrom, fragsize);
	} // searchIndexJson

	/**
	 * Get json String from Solr
	 * 
	 * @param Query String
	 * @param max rows allow
	 * @param Startfrom
	 * @param fragment size
	 */
	@Override
	public String searchIndexJson(String queryString, int maxRows, int startFrom, int fragsize)
	{
		queryString = escapeMetaCharacters(queryString);

		try
		{
			if (server.ping() == null)
				init(indexingConfig);
		}
		catch (SolrServerException | IOException e)
		{
			log.log(Level.SEVERE, "Fail to ping solr Server ", e);
			throw new AdempiereException("Fail to ping solr Server: " + e.getLocalizedMessage());
		}

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
	 * Escape meta characters from query string
	 * 
	 * @param input string
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
	 * Build Solr serach Query
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
				if (value.get(0) instanceof String || value.get(1) instanceof String)
				{
					query.append(" AND (").append(key + ":[" + value.get(0) + " TO " + value.get(1) + " ])");
				}
				else if (value.get(1).equals("*"))
					query.append(" AND (").append(key + ":[\"" + value.get(0) + "\" TO " + value.get(1) + " ])");
				else
					query.append(" AND (").append(key + ":[\"" + value.get(0) + "\" TO \"" + value.get(1) + "\" ])");
			}
			else
			{
				if (value.get(0) instanceof String)
					query.append(" AND (").append(key + ":*" + value.get(0) + "*)");
				else
					query.append(" AND (").append(key + ":\"" + value.get(0) + "\")");
			}
		}

		if (query.length() > 0)
			query.delete(0, 5);
		else
			query.append("*:*");

		return query.toString();
	} // buildSolrSearchQuery

	/**
	 * Search for solr Document
	 * 
	 * @param query
	 */
	public List<SolrDocument> searchIndexDocument(String query)
	{
		int maxRow = MSysConfig.getIntValue("SOLR_MAXROWS", 100);
		return searchIndexDocument(query, maxRow);
	} // searchIndexDocument

	/**
	 * Search for solr Document
	 * 
	 * @param Query
	 * @param Max rows allow
	 */
	public List<SolrDocument> searchIndexDocument(String query, int maxRow)
	{
		try
		{
			if (server.ping() == null)
				init(indexingConfig);
		}
		catch (SolrServerException | IOException e)
		{
			log.log(Level.SEVERE, "Fail to ping solr Server ", e);
			throw new AdempiereException("Fail to ping solr Server: " + e.getLocalizedMessage());
		}

		SolrQuery solrQuery = new SolrQuery();
		SolrIndexDataSet dataset = null;
		List<SolrDocument> solrDocList = new ArrayList<SolrDocument>();
		Object solrDoc = null;
		try
		{
			solrQuery.setQuery(query);
			dataset = new SolrIndexDataSet(solrQuery, server, maxRow);
			dataset = dataset.execute();
			while (dataset.hasMore())
			{
				solrDoc = dataset.next();
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
}
