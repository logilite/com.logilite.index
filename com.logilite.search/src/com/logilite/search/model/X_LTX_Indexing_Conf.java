/******************************************************************************
 * Product: iDempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2012 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY, without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program, if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/
/** Generated Model - DO NOT CHANGE */
package com.logilite.search.model;

import java.sql.ResultSet;
import java.util.Properties;
import org.compiere.model.*;

/** Generated Model for LTX_Indexing_Conf
 *  @author iDempiere (generated) 
 *  @version Release 7.1 - $Id$ */
public class X_LTX_Indexing_Conf extends PO implements I_LTX_Indexing_Conf, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20231109L;

    /** Standard Constructor */
    public X_LTX_Indexing_Conf (Properties ctx, int LTX_Indexing_Conf_ID, String trxName)
    {
      super (ctx, LTX_Indexing_Conf_ID, trxName);
      /** if (LTX_Indexing_Conf_ID == 0)
        {
			setLTX_Indexing_Core (null);
			setLTX_Indexing_Type (null);
			setURL (null);
        } */
    }

    /** Load Constructor */
    public X_LTX_Indexing_Conf (Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }

    /** AccessLevel
      * @return 3 - Client - Org 
      */
    protected int get_AccessLevel()
    {
      return accessLevel.intValue();
    }

    /** Load Meta Data */
    protected POInfo initPO (Properties ctx)
    {
      POInfo poi = POInfo.getPOInfo (ctx, Table_ID, get_TrxName());
      return poi;
    }

    public String toString()
    {
      StringBuffer sb = new StringBuffer ("X_LTX_Indexing_Conf[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	/** Set Indexing Configuration .
		@param LTX_Indexing_Conf_ID Indexing Configuration 	  */
	public void setLTX_Indexing_Conf_ID (int LTX_Indexing_Conf_ID)
	{
		if (LTX_Indexing_Conf_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_LTX_Indexing_Conf_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_LTX_Indexing_Conf_ID, Integer.valueOf(LTX_Indexing_Conf_ID));
	}

	/** Get Indexing Configuration .
		@return Indexing Configuration 	  */
	public int getLTX_Indexing_Conf_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_LTX_Indexing_Conf_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Indexing Core.
		@param LTX_Indexing_Core Indexing Core	  */
	public void setLTX_Indexing_Core (String LTX_Indexing_Core)
	{
		set_Value (COLUMNNAME_LTX_Indexing_Core, LTX_Indexing_Core);
	}

	/** Get Indexing Core.
		@return Indexing Core	  */
	public String getLTX_Indexing_Core () 
	{
		return (String)get_Value(COLUMNNAME_LTX_Indexing_Core);
	}

	/** Lucene = LUC */
	public static final String LTX_INDEXING_TYPE_Lucene = "LUC";
	/** Solr = SOL */
	public static final String LTX_INDEXING_TYPE_Solr = "SOL";
	/** Elastic = ELS */
	public static final String LTX_INDEXING_TYPE_Elastic = "ELS";
	/** Set Indexing Type.
		@param LTX_Indexing_Type Indexing Type	  */
	public void setLTX_Indexing_Type (String LTX_Indexing_Type)
	{

		set_Value (COLUMNNAME_LTX_Indexing_Type, LTX_Indexing_Type);
	}

	/** Get Indexing Type.
		@return Indexing Type	  */
	public String getLTX_Indexing_Type () 
	{
		return (String)get_Value(COLUMNNAME_LTX_Indexing_Type);
	}

	/** Set Password.
		@param Password 
		Password of any length (case sensitive)
	  */
	public void setPassword (String Password)
	{
		set_Value (COLUMNNAME_Password, Password);
	}

	/** Get Password.
		@return Password of any length (case sensitive)
	  */
	public String getPassword () 
	{
		return (String)get_Value(COLUMNNAME_Password);
	}

	/** Set URL.
		@param URL 
		Full URL address - e.g. http://www.idempiere.org
	  */
	public void setURL (String URL)
	{
		set_ValueNoCheck (COLUMNNAME_URL, URL);
	}

	/** Get URL.
		@return Full URL address - e.g. http://www.idempiere.org
	  */
	public String getURL () 
	{
		return (String)get_Value(COLUMNNAME_URL);
	}

	/** Set User Name.
		@param UserName User Name	  */
	public void setUserName (String UserName)
	{
		set_Value (COLUMNNAME_UserName, UserName);
	}

	/** Get User Name.
		@return User Name	  */
	public String getUserName () 
	{
		return (String)get_Value(COLUMNNAME_UserName);
	}
}