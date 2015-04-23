# cy3sbml - CySBML for Cytoscape 3

CySBML is a Cytoscape plugin for the import and work with SBML files in Cytoscape providing the 
visualisation of SBML network annotations within the network context. 

SBML models can be imported from BioModels.net or via file or urls. One click access to the annotation
resources is provided. SBML validation information about the imported files is available.

This repository provides the App for Cytoscape 3.

**Status**
alpha, untested

**Support**

https://groups.google.com/forum/#!forum/cysbml-cyfluxviz

**Features**

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

**Installation**

Download via the Cytoscape App store.

**Uninstall**

Via the app store.
