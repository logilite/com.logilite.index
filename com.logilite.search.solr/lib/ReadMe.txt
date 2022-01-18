Readme note:

[ Plugin compatible to iDempiere 9 version ]

--> For Java-11 and tika-app-2.2.1.jar libs contains same packages and creating issue
	
	Plugin		: 	com.logilite.search.solr
	Same Package: 	org.xml.sax.*
	Issue		: 	The package org.xml.sax is accessible from more than one module: <unnamed>, java.xml
	Fix			: 	Deleted that package from tika-app-2.2.1.jar file.

	Plugin		: 	org.idempiere.dms
	Same Package: 	javax.xml.datatype.*
					javax.xml.namespace.*
					javax.xml.parsers.*
					javax.xml.validation.*
					javax.xml.transform.*
					javax.xml.stream.*
					javax.xml.xpath.*
					javax.xml.XMLConstants.class
					org.w3c.*
	Issue 		: 	The package (*) is accessible from more than one module: <unnamed>, java.xml
	Fix			: 	Deleted that package from tika-app-2.2.1.jar file.
