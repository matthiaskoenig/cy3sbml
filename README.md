# ![alt tag](./docs/images/logo100.png) cy3sbml - SBML for Cytoscape 3

<a href="https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&amp;hosted_button_id=RYHNRJFBMWD5N" title="Donate to this project using Paypal"><img src="https://img.shields.io/badge/paypal-donate-yellow.svg" alt="PayPal donate button" /></a>

**cy3sbml** is a [Cytoscape 3](http://www.cytoscape.org) app for the Systems Biology Markup Language [SBML](http://www.sbml.org).  
For Cytoscape 2 use [cy2sbml](https://github.com/matthiaskoenig/cy2sbml).

[![Download](docs/images/icon-download.png) Download](https://github.com/matthiaskoenig/cy3sbml/releases/latest)  
**Support & Forum** : https://groups.google.com/forum/#!forum/cysbml-cyfluxviz  
**Bug Tracker** : https://github.com/matthiaskoenig/cy3sbml/issues  

## Features
**cy3sbml** provides advanced functionality for the import and work with models encoded in SBML, amongst others the 
visualization of SBML network annotations within the network context, direct import of models from repositories like [biomodels](http://www.biomodels.org) and one-click access to annotation resources and SBML model information and SBML validation.

* Java based SBML parser for Cytoscape based on JSBML (http://sourceforge.net/projects/jsbml/)
* access to models and annotations via BioModel 
  (http://www.biomodels.org/) and MIRIAM WebServices (http://www.ebi.ac.uk/miriam/main/)
* supports all versions of SBML
* SBML validation (SBML warnings and errors accessible)
* Network graph based on the species/reaction model
* Network graph of full SBML information
* Support of qual, comp and fbc extensions
* Provides access to RDF based annotation information within
  the network context
* Annotation information 
* succesfully tested against all SBML.org and Biomodels.org test
  cases (BioModels_Database-r29)

We are currently porting missing features from the Cytoscape 2 plugin to Cytoscape 3. Documentation of most of the features is available from http://matthiaskoenig.github.io/cy2sbml/

## License
* Source Code: [GPLv3](http://opensource.org/licenses/GPL-3.0)
* Documentation: [CC BY-SA 4.0](http://creativecommons.org/licenses/by-sa/4.0/)

## Funding
**cy3sbml** was funded by 
* [NRNB](http://nrnb.org) (National Resource for Network Biology) within the [NRNB Academy Summer Session 2015](http://nrnb.org/gsoc.html)
* Virtual Liver Network (VLN)

## Citation
Matthias König, Andreas Dräger and Hermann-Georg Holzhütter  
*CySBML: a Cytoscape plugin for SBML*  
Bioinformatics. 2012 Jul 5. [PubMed](http://www.ncbi.nlm.nih.gov/pubmed/22772946) 

## Installation
### Install
* cy3sbml is available via the [Cytoscape App Store](http://apps.cytoscape.org/apps/cy3sbml). Download and install the latest version of [Cytoscape](http://www.cytoscape.org/) (>=3.2).  Within Cytoscape open `Apps → App Manager → Install Apps` and search for `cy3sbml`. Select `cy3sbml` click install. After installation cy3sbml will be listed in the `Currently Installed` apps tab.  

### Uninstall
To uninstall or disable cy3sbml, go to the menu bar and choose `Apps → App Manager → Currently Installed`. Select cy3sbml and click `Uninstall` or `Disable`.

### Build instructions
For working with the latest **development release** follow the [build instructions] (https://goo.gl/4xMgff). This includes recently developed features not yet released.

Clone the repository and build with `mvn`
```
git clone https://github.com/matthiaskoenig/cy3sbml.git cy3sbml
cd cy3sbml
mvn clean install
mvn clean install -DskipTests
```
Development is done in `develop` branch, documentation in `README.md`
```
git checkout -b develop --track origin/develop
```

## Changelog
**v0.1.7** [?]

**v0.1.6** [2015/08/27]
* Testing, bug fixes and documentation
* sbml-test-cases models successfully parsed
* BIGG models successfully parsed
* cobrapy test models parsed
* full SBML FBC v2 support
* basic SBML distrib support

**v0.1.5** [2015/07/04]
* SBO Name and definitions for information
* Parsing of rateRules, InitialAssignments and kineticLaws
* Display of NamedSBase attributes in information
* Example loader (menu) & examples added
* FBC support
* NetworkReader tests (passed all BioModels r29 curated models)
* master SBML graph (compartments, kinetics, parameters, ...)
* File selection & multiple file import ported
* SBML validator ported

**v0.1.4** [2015/06/24]
* support of multiple networks, views and subnetworks
* qualitative model support
* biomodel webservice search & retrieval
* redesign & bug fixes

**v0.1.3** [2015/06/14]
* cy3sbml VisualStyles
* Support of multiple networks and views (subnetworks)
* Model information is now displayed 
* RDF annotations displayed (MIRIAM) 
* proxy support
* first unit tests created and integrated with maven
* logging with log4j and slf4j implemented (cy3sbml.log)
* support of cy3sbml properties for general settings like preferred VisualStyle
* node EventListener for updating annotation information
* application of layout after generating views

**v0.1.2** [2015/06/01]
* documentation update (build instructions, installation instructions)

**v0.1** [2015/05]
* first app release (OSGI build with JSBML integration)


----
&copy; 2015 Matthias König. Developed and maintained by Matthias König, Andreas Dräger and Nicolas Rodriguez.
