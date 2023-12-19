package com.logilite.search.exception;

import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Msg;

/**
 * Any exception that occurs inside the indexing server
 * 
 * @author Sachin Bhimani
 * @author Bhautik
 */

public class IndexException extends RuntimeException
{
	private static final long serialVersionUID = -2301877502491120553L;

	/**
	 * Default Constructor (saved logger error will be used as message)
	 */
	public IndexException()
	{
		this(getMessageFromLogger());
	}

	/**
	 * @param message
	 */
	public IndexException(String message)
	{
		super(message);
	}

	/**
	 * @param cause
	 */
	public IndexException(Throwable cause)
	{
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public IndexException(String message, Throwable cause)
	{
		super(message, cause);
	}

	@Override
	public String getLocalizedMessage()
	{
		String msg = super.getLocalizedMessage();
		msg = Msg.parseTranslation(Env.getCtx(), msg);
		return msg;
	}

	private static String getMessageFromLogger()
	{
		org.compiere.util.ValueNamePair err = CLogger.retrieveError();
		String msg = null;
		if (err != null)
			msg = err.getName();
		if (msg == null)
			msg = "UnknownError";
		return msg;
	}

}
