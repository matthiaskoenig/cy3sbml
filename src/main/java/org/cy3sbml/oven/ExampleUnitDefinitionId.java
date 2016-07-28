package org.cy3sbml.oven;

import org.sbml.jsbml.*;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;


public class ExampleUnitDefinitionId {


	/**
	 INFO (TestUtils.java:167) - org.cy3sbml.SBMLTestCaseTest : /models/sbml-test-cases/syntactic/10302/10302-pass-00-08-l1v2.xml
	 ERROR (Model.java:3953) - An element of type unitDefinition with the id "mmls" is already present in this model. The new element of type compartment will not be added to the model.
	 ERROR (IdentifierException.java:62) - An element with the id "mmls" is already present in the SBML model. The identifier of compartment will not be set to this value.
	 ERROR (SBMLCoreParser.java:179) - org.sbml.jsbml.IdentifierException: Cannot set duplicate identifier mmls for compartment.
	 WARN (SBMLCoreParser.java:207) - Could not recognize the attribute 'name' on the element compartment. Please check the specification for SBML.
     */
	public static void main(String[] args) throws XMLStreamException, IOException{
		System.setProperty("logfile.name","/tmp/test.log");

		// String path = "/home/mkoenig/git/cy3sbml/src/test/resources/models/sbml-test-cases/syntactic/10302/10302-pass-00-08-l1v2.xml";
        String path = "/home/mkoenig/git/cy3sbml/src/test/resources/models/sbml-test-cases/syntactic/10309/10309-pass-00-03-l2v1.xml";
		JSBML.readSBMLFromFile(path);
	}
	
}
