package org.cy3sbml.biomodel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.cy3sbml.biomodelrest.BiomodelsQueryResult;
import org.cy3sbml.biomodelrest.rest.Biomodel;
import uk.ac.ebi.biomodels.ws.SimpleModel;

/**
 * Tools to interact with BioModels.
 */
public class BioModelWSInterfaceTools {
	// private static final Logger logger = LoggerFactory.getLogger(BioModelWSInterfaceTools.class);
	
	// string and html representations
	public static String getHTMLInformationForSimpleModels(List<String>modelIds,
                                                           List<String> selectedSimpleModels, ArrayList<Biomodel> biomodelArrayList) throws IOException, InterruptedException {
		String info = "";
		for (int i =0; i<modelIds.size(); i++){
			String modelId = modelIds.get(i);
			Biomodel model = biomodelArrayList.get(i);
			boolean modelIsSelected = false;
			for (int j=0; j<selectedSimpleModels.size(); ++j){
				String selectedId = selectedSimpleModels.get(j);
				if (modelId.equals(selectedId)){
					modelIsSelected = true;
				}
			}
			info += getHTMLInformationForSimpleModel(model, modelIsSelected);
			info += "<hr>";
		}
		return info;
	}
	
	public static String getHTMLInformationForSimpleModel(Biomodel simpleModel){
		boolean selected = false;
		return getHTMLInformationForSimpleModel(simpleModel, selected);
	}
	
	public static String getHTMLInformationForSimpleModel(Biomodel simpleModel, boolean selected){

		String name = simpleModel.getName();
		String publicationId = simpleModel.getPublicationIdentifier();
		//String dateModified = simpleModel.getLastModificationDateStr();
		String description = simpleModel.getDescription();

		String info;
		if (selected){
			info = "<table><tr><td bgcolor=\"#339933\">&nbsp;&nbsp;&nbsp;<td><td>";
		} else {
			info = "<table><tr><td>&nbsp;&nbsp;&nbsp;<td><td>";
		}
		info += createHTMLTableHeader(selected)+

					createHTMLTableRow("name", name) +
					createHTMLTableRow("description", description.toString()) +
					createHTMLTableRow("pubmed", createPubmedHTMLLink(publicationId)) +
					//createHTMLTableRow("modified", dateModified) +

				"</table>" +
				"</td></tr></table>";
		return info;
	}
	
	private static String createHTMLTableHeader(boolean selected){		
		String border = "0";
		String header = String.format("<table border=%s>", border); 
		return header;
	}
	
	private static String createHTMLTableRow(String attribute, String value){
		return String.format(
				"<tr>" +
				"	<td><b><font size=\"-1\">%s</font></b></td> " +
				"	<td><font size=\"-1\">%s</font></b></td></tr>",
				attribute, value);
	}
	
	public static String createBioModelHTMLLink(String bioModelId){
		return String.format(
				"<a href=\"http://www.ebi.ac.uk/biomodels-main/%s\" target=\"_blank\">%s</a>",
				bioModelId, bioModelId);
	}
	
	private static String createPubmedHTMLLink(String pubmedId){
		return String.format(
				"<a href=\"http://www.ncbi.nlm.nih.gov/pubmed?term=%s\" target=\"_blank\">%s</a>",
				pubmedId, pubmedId);
	}
	
	public static String getTextInformationForSimpleModel(SimpleModel simpleModel){
		String smId = simpleModel.getId();
		String smName = simpleModel.getName();
		String smPublicationId = simpleModel.getPublicationId();
		String smDateModified = simpleModel.getLastModificationDateStr();
		String info = String.format(
					"ID:\t %s \n" +
					"NAME:\t %s \n" +
					"PUBID:\t %s \n" +
					"MODIFIED:\t %s \n",
						smId, smName, smPublicationId, smDateModified
				);
		return info;
	}
}
