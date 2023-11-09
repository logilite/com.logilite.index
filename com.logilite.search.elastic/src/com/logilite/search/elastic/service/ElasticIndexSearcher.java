package com.logilite.search.elastic.service;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHeader;
import org.compiere.util.CLogger;
import org.elasticsearch.client.RestClient;
import org.json.JSONObject;

import com.logilite.search.factory.IIndexSearcher;
import com.logilite.search.model.MIndexingConfig;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import co.elastic.clients.transport.rest_client.RestClientTransport;

public class ElasticIndexSearcher implements IIndexSearcher
{

	public static CLogger		log				= CLogger.getCLogger(ElasticIndexSearcher.class);

	private MIndexingConfig		indexingConfig	= null;
	private ElasticsearchClient	esClient		= null;

	@Override
	public void init(MIndexingConfig indexingConfig)
	{
		this.indexingConfig = indexingConfig;

		String serverUrl = indexingConfig.getURL();
		String apiKey = indexingConfig.getPassword();
		// "ak1reGdJc0IwcURiX3JjS18zdE06UWZYYkdvVzhUaWF0cmR4bXdXaFlPUQ==";

		// final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
		// credentialsProvider.setCredentials(AuthScope.ANY,
		// new UsernamePasswordCredentials("elastic", "Logilite@803"));

		RestClient restClient = RestClient	.builder(HttpHost.create(serverUrl))
											.setDefaultHeaders(new Header[] { new BasicHeader("Authorization", "ApiKey " + apiKey) }).build();

		ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
		esClient = new ElasticsearchClient(transport);
		System.out.println(esClient);
		try
		{
			BooleanResponse result = esClient.ping();
			System.out.println("test " + result);
			// TODO
			// esclient.execute(null, null);
			// esclient.ping();
		}
		catch (ElasticsearchException e)
		{
			log.log(Level.SEVERE, "Elastic server is not started: ", e);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public void createFieldTypeInIndexSchema(Map<String, Object> fieldTypeAttribute)
	{

	}

	@Override
	public void createFieldsInIndexSchema(Map<String, Object> fieldAttribute)
	{

	}

	/**
	 * Check Server is Up and running
	 * 
	 * @throws AdempiereException
	 */
	public void checkServerIsUp() throws AdempiereException
	{
		try
		{
			if (esClient.ping() == null)
				init(indexingConfig);
		}
		catch (IOException e)
		{
			log.log(Level.SEVERE, "Fail to ping solr Server ", e);
			throw new AdempiereException("Fail to ping solr Server: " + e.getLocalizedMessage(), e);
		}
	} // checkServerIsUp

	@Override
	public void deleteAllIndex()
	{

	}

	@Override
	public void deleteIndexByField(String fieldName, String fieldValue)
	{

	}

	@Override
	public void deleteIndexByQuery(String query)
	{

	}

	@Override
	public Object searchIndexNoRestriction(String queryString)
	{
		return null;
	}

	@Override
	public String searchIndexJson(String queryString)
	{
		return null;
	}

	@Override
	public String searchIndexJson(String queryString, int maxRows)
	{
		return null;
	}

	@Override
	public String searchIndexJson(String queryString, int maxRows, int startFrom)
	{
		return null;
	}

	@Override
	public String searchIndexJson(String queryString, int maxRows, int startFrom, int fragsize)
	{
		return null;
	}

	@Override
	public List<Object> searchIndexDocument(String queryString)
	{
		return null;
	}

	@Override
	public List<Object> searchIndexDocument(String queryString, int maxRow)
	{
		return null;
	}

	@Override
	public void indexContent(Map<String, Object> indexData)
	{
		checkServerIsUp();

		log.log(Level.INFO, "Elastic index creation value = " + indexData.toString());

		// Map data convert to JSon format
		JSONObject jsonObject = new JSONObject(indexData);
		String orgJsonData = jsonObject.toString();

		// System.out.println(orgJsonData);

		try
		{
			Reader input = new StringReader(orgJsonData.replace('\'', '"'));
			IndexRequest<JsonData> req = IndexRequest.of(i -> i	.index(indexingConfig.getLTX_Indexing_Core())
																.withJson(input));
			IndexResponse response = esClient.index(req);

			// System.out.println("result : " + response);
		}
		catch (co.elastic.clients.elasticsearch._types.ElasticsearchException | IOException e)
		{
			log.log(Level.SEVERE, "Fail to create Indexing: ", e);
			throw new AdempiereException("Fail to create Indexing: " + e.getLocalizedMessage(), e);

		}
	}

	@Override
	public Object getColumnValue(String query, String columnName)
	{
		return null;
	}

	@Override
	public HashSet<String> getFieldTypeSet()
	{
		return new HashSet<String>();
	}

	@Override
	public HashSet<String> getFieldSet()
	{
		return new HashSet<String>();
	}

	@Override
	public String getParseDocumentContent(File file)
	{
		return null;
	}

	@Override
	public HashSet<Integer> searchIndex(String query, String searchFieldName)
	{
		return null;
	}

}
