package com.logilite.search.solr.tika;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.compiere.util.CLogger;
import org.xml.sax.helpers.DefaultHandler;

/**
 * File content parsing through Apache Tika
 * 
 * @author Sachin Bhimani @ Logilite Technologies
 * @since  2020 December 05
 */
public class FileContentParsingThroughTika
{

	public static CLogger	log	= CLogger.getCLogger(FileContentParsingThroughTika.class);

	private File			file;

	public FileContentParsingThroughTika(File file)
	{
		this.file = file;
	}

	public String getParsedDocumentContent()
	{
		try
		{
			Metadata metadata = new Metadata();
			DefaultHandler handler = new BodyContentHandler(-1);
			TikaConfig tikaConfig = TikaConfig.getDefaultConfig();
			AutoDetectParser parser = new AutoDetectParser(tikaConfig);
			TikaInputStream stream = TikaInputStream.get(new FileInputStream(file));

			parser.parse(stream, handler, metadata, new ParseContext());
			stream.close();

			return handler.toString();
		}
		catch (FileNotFoundException e)
		{
			log.log(Level.SEVERE, "File Not Found: ", e);
			throw new AdempiereException("File Not Found: " + e.getLocalizedMessage(), e);
		}
		catch (IOException e)
		{
			log.log(Level.SEVERE, "Fail to read file: ", e);
			throw new AdempiereException("Fail to read file: " + e.getLocalizedMessage(), e);
		}
		catch (TikaException e)
		{
			log.log(Level.SEVERE, "Fail to parse file content: ", e);
			throw new AdempiereException("Fail to parse file content: " + e.getLocalizedMessage(), e);
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Fail to parsing file: ", e);
			throw new AdempiereException("Fail to parsing file: " + e.getLocalizedMessage(), e);
		}
	} // getParsedDocumentContent

}
