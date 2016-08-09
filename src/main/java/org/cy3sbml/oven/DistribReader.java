package org.cy3sbml.oven;

import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.NamedSBase;
import org.sbml.jsbml.SBase;
import org.sbml.jsbml.ext.distrib.DistribConstants;
import org.sbml.jsbml.ext.distrib.DistribSBasePlugin;
import org.sbml.jsbml.ext.distrib.Uncertainty;
import org.sbml.jsbml.xml.XMLNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Reading distrib information.
 * This is experimental and not yet supported.
 */
public class DistribReader {
    Logger logger = LoggerFactory.getLogger(getClass());

    ////////////////////////////////////////////////////////////////////////////
    // SBML DISTRIB
    ////////////////////////////////////////////////////////////////////////////
    /**
     * Create uncertainty information from distrib package.
     */
    private void readDistrib(Model model){
        // TODO: necessary to display for all the SBase elements
        // TODO: write the string as attribute to the respective node

        logger.debug("<distrib>");
        // Compartments
        readUncertainties(model.getListOfCompartments());

        // Species
        readUncertainties(model.getListOfSpecies());

        // Parameters
        readUncertainties(model.getListOfParameters());

    }

    private void readUncertainties(ListOf<?> listOfSBase){
        for (SBase sbase: listOfSBase){
            DistribSBasePlugin dSBase = (DistribSBasePlugin) sbase.getExtension(DistribConstants.namespaceURI);
            if (dSBase != null && dSBase.isSetUncertainty()){
                Uncertainty uc = dSBase.getUncertainty();
                if (uc.isSetUncertML()){
                    readUncertainty(sbase, uc);
                }
            }
        }
    }

    /* Parse the uncertainty information.
     * Use the respective Java API.
     * https://github.com/52North/uncertml-api.git
     */
    private void readUncertainty(SBase sbase, Uncertainty uc){
        String id = null;
        String name = null;
        if (uc.isSetId()){
            id = uc.getId();
        }
        if (uc.isSetName()){
            name = uc.getName();
        }

        // XML node
        XMLNode uncertML = uc.getUncertML();
        //XMLParser ucParser = new XMLParser();

        // TODO: parse the uncertainty XML
        // Problems with the library
        // String xmlString = uncertML.toXMLString();
        // IUncertainty iuc = ucParser.parse(xmlString);
        // logger.info(iuc.toString());

        if (sbase instanceof NamedSBase){
            logger.info(String.format("UncertML <%s|%s> for %s: %s", name, id, ((NamedSBase) sbase).getId(), uncertML.toString()));
        } else {
            logger.info(String.format("UncertML <%s|%s>: %s", name, id, uncertML.toString()));
        }
    }

}
