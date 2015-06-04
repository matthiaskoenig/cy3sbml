# cy3sbml - SBML for Cytoscape 3

cy3sbml is a Cytoscape plugin for the import and work with SBML files in Cytoscape providing the 
visualisation of SBML network annotations within the network context. 

SBML models can be imported from BioModels.net or via file or urls. One click access to the annotation
resources is provided. SBML validation information about the imported files is available.

This repository provides the App for Cytoscape 3.

## Status
alpha

## Support
https://groups.google.com/forum/#!forum/cysbml-cyfluxviz

## Features to come
* Java based SBML parser for Cytoscape based on JSBML 
  ( http://sourceforge.net/projects/jsbml/ )
* access to models and annotations via BioModel 
  ( http://www.biomodels.org/ ) and MIRIAM WebServices
  ( http://www.ebi.ac.uk/miriam/main/ )
* supports all versions of SBML
* SBML validation (SBML warnings and errors accessible)
* Standard network layout based on the species/reaction model
* Provides access to RDF based annotation information within
  the network context
* Navigation menu based on the SBML structure linked to layout 
  and annotation information
* succesfully tested with all SBML.org and Biomodels.org test
  cases (sbml-test-cases-2.0.2, BioModels_Database-r21-sbml_files)

## Installation
* Download and install the latest version of Cytoscape 3 from http://www.cytoscape.org/.  
* Download the latest stable version of the cy3sbml jar from `https://github.com/matthiaskoenig/cy3sbml/tree/master/cy3sbml/target` (https://github.com/matthiaskoenig/cy3sbml/blob/master/cy3sbml/target/cy3sbml-0.1.jar?raw=true).
* For the latest development version use `https://github.com/matthiaskoenig/cy3sbml/tree/develop/cy3sbml/target` (https://github.com/matthiaskoenig/cy3sbml/blob/develop/cy3sbml/target/cy3sbml-0.1.1.jar?raw=true).
* To install cy3sbml as app within Cytoscape, go to the menu bar and choose `Apps → App Manager`. At the top of the App Manager window, 
make sure you have the Install tab selected. Now install cy3sbml by clicking the `Install from File` button on the bottom-left with the downloaded jar. Make sure that the full jar file was downloaded from github, otherwise the installation will complain
* After installation cy3sbml will be listed in the ´Currently Installed´ apps tab.

https://github.com/matthiaskoenig/cy3sbml/blob/master/cy3sbml/target/cy3sbml-0.1.jar?raw=true

## Uninstall
* To uninstall or disable cy3sbml, go to the menu bar and choose `Apps → App Manager → Currently Installed`. Select cy3sbml and click `Uninstall` or `Disable`.
