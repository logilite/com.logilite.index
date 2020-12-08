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

import java.util.logging.Level;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.compiere.util.CLogger;

import com.logilite.search.factory.IndexDataSet;

public class SolrIndexDataSet implements IndexDataSet
{
	public static CLogger		log				= CLogger.getCLogger(SolrIndexDataSet.class);

	// get total count from query result
	private long				totalCount		= 0;
	// Current index is for local iteration
	private int					currentIndex	= 0;
	// Cursor index is for iterate starting to total count
	private long				cursorIndex		= 0;
	// max rows is for get total number of row in one execution
	private int					maxSolrRow		= 0;

	private SolrDocumentList	documentList	= null;
	private SolrQuery			solrDataQuery	= null;
	private SolrClient			solrServer		= null;
	private SolrDocument		solrDoc			= null;

	public SolrIndexDataSet(SolrQuery solrQuery, SolrClient server, int maxRow)
	{
		solrDataQuery = solrQuery;
		solrServer = server;
		maxSolrRow = maxRow;
	}

	@Override
	public long getCount()
	{
		return totalCount;
	}

	@Override
	public Object next()
	{
		if (documentList == null)
			return null;

		if (currentIndex == documentList.size())
		{
			try
			{
				QueryResponse response = new QueryResponse();
				solrDataQuery.setRows(maxSolrRow);
				solrDataQuery.setStart((int) cursorIndex);
				response = solrServer.query(solrDataQuery);
				documentList = response.getResults();
				currentIndex = 0;
				if (documentList.size() > 0)
					solrDoc = documentList.get(currentIndex);
				else
					solrDoc = null;
			}
			catch (Exception e)
			{
				log.log(Level.SEVERE, "Searching content failure:", e);
			}
		}
		else
		{
			solrDoc = documentList.get(currentIndex);
		}
		currentIndex++;
		cursorIndex++;
		return solrDoc;
	} // next

	@Override
	public boolean clear()
	{
		documentList = null;
		cursorIndex = 0;
		currentIndex = 0;
		totalCount = 0;
		return true;
	} // clear

	@Override
	public boolean hasMore()
	{
		return (currentIndex <= documentList.size() && cursorIndex < totalCount);
	} // hasMore

	public SolrIndexDataSet execute()
	{
		if (solrServer != null)
		{
			QueryResponse response = new QueryResponse();

			try
			{
				solrDataQuery.setRows(maxSolrRow);
				response = solrServer.query(solrDataQuery);
				documentList = response.getResults();
				totalCount = documentList.getNumFound();

			}
			catch (Exception e)
			{
				log.log(Level.SEVERE, "Searching content failure:", e);
			}
		}
		return this;
	} // execute

}
