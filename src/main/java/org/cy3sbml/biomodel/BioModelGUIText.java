package org.cy3sbml.biomodel;

public class BioModelGUIText {
	public static String getHeaderString(){
		String imgsrc = 
				BioModelGUIText.class.getClassLoader().getResource("http://www.biomodels.org/images/biomodels_new_logo.png").toString();
		String info = "<a href=\"http://www.biomodels.org/\"><img src=\""+imgsrc+"\"alt=\"BioModel.net Logo\" height=75 width=508 border=0></img></a>";
		return info;
	}
	
	public static String getInfo(){
		String info = getHeaderString();
		info += "<h2>Import of BioModels</h2>" +
				"<p>Load SBML models from <a href=\"http://www.biomodels.org/\">BioModels.net</a> by " +
				"(1) searching the database or (2) via given BioModel Ids.</p>" +
				"<p>(1) The BioModel database can be searched by " +
				"<ul>" +
				"<li>Name</li>" +
				"<li>Person</li>" +
				"<li>Publication (PMID or Abstract)</li>" +
				"<li>ChEBI (name or identifier)</li>" +
				"<li>UniProt (name or identifier)</li>" +
				"</ul>" +
				"Search terms can be connected via AND or OR.</p>" +
				"<p>Search results are shown in list format. To load one or multiple models select the model ids in the list and click " +
				"<span color=\"gray\"><b>Load Selected</b></span>.</p>" +
				"<p>(2) For direct access to specific models type the BioModel Ids in the text field.<br>" +
				"Click <span color=\"gray\"><b>Parse Ids</b></span>" +
				" to get information about the models or <span color=\"gray\"><b>Load Ids</b></span> to load the models for the given ids.<br>" +
				"Arbitrary text containing BioModel Ids can be used as input for the parsing and loading.</p>" +
				"<p>The access via the BioModel WebService can take a few seconds depending on the search query.</p>";
		return info;
	}
	
	public static String getWebserviceError(){
		String info = getHeaderString();
		info += "<p>BioModel WebService could not be accessed.</p>" +
				"<p>Test your internet connection and set the proxy information for Cytoscape" +
				"(Edit -> Preferences -> ProxyServer) for your connection in Cytoscape.</p>" +
				"<p>Possibly, the BioModel WebService is temporary not available.\n" +
				"In this case download the model of interest as SBML from " +
				"<a href=\"http://www.ebi.ac.uk/biomodels-main/\">BioModels.net</a> and " +
				"import manually via (File -> Import -> Network (Multiple File Types)." +
				"Sorry for the inconvinience.</p>";
		return info;
	}
	public static String performBioModelSearch(){
		String info = getHeaderString();
		info += "<p>Searching BioModels ...</p>" +
				"<p>... WebService request can take a few seconds.</p>";
		return info;
	}
		
	public static String getString(String msg){
		String info = getHeaderString();
		info += msg;
		return info;
	}
	
	public static String getWebserviceSBMLRequest(){
		String info = getHeaderString();
		info += "<p>Getting SBML information via Webservice ... </p>" +
				"... Webservice request can take a few seconds.</p>";
		return info;
	}
}
