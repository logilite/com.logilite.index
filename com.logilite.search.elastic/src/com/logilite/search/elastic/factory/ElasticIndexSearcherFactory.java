package com.logilite.search.elastic.factory;

import com.logilite.search.elastic.service.ElasticIndexSearcher;
import com.logilite.search.factory.IIndexSearcher;
import com.logilite.search.factory.IIndexSearcherFactory;
import com.logilite.search.model.MIndexingConfig;

/**
 * Elastic Index Searcher Factory
 * 
 * @author Sachin Bhimani
 */
public class ElasticIndexSearcherFactory implements IIndexSearcherFactory
{

	@Override
	public IIndexSearcher getIndexSearcher(String indexingType)
	{
		if (MIndexingConfig.LTX_INDEXING_TYPE_Elastic.equalsIgnoreCase(indexingType))
		{
			return new ElasticIndexSearcher();
		}
		else
			return null;
	}
}
