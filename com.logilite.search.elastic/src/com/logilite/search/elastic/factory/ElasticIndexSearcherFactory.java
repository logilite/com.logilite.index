package com.logilite.search.elastic.factory;

import com.logilite.search.elastic.service.ElasticIndexSearcher;
import com.logilite.search.factory.IIndexSearcher;
import com.logilite.search.factory.IIndexSearcherFactory;


public class ElasticIndexSearcherFactory implements IIndexSearcherFactory {

	@Override
	public IIndexSearcher getIndexSearcher(String indexEngine) {
		if (indexEngine.equalsIgnoreCase("ELS"))
		{
			return new ElasticIndexSearcher();
		}
		else
			return null;
	}
	}


