# ![cy3sbml logo](https://github.com/matthiaskoenig/cy3sbml/raw/develop/docs/images/logo100.png) cy3sbml - SBML for Cytoscape 3

[![DOI](https://zenodo.org/badge/5066/matthiaskoenig/cy3sbml.svg)](https://zenodo.org/badge/latestdoi/5066/matthiaskoenig/cy3sbml)
<a href="https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&amp;hosted_button_id=RYHNRJFBMWD5N" title="Donate to this project using Paypal"><img src="https://img.shields.io/badge/paypal-donate-yellow.svg" alt="PayPal donate button" /></a>
[![Build Status](https://travis-ci.org/matthiaskoenig/cy3sbml.svg?branch=develop)](https://travis-ci.org/matthiaskoenig/cy3sbml)
[![codecov](https://codecov.io/gh/matthiaskoenig/cy3sbml/branch/develop/graph/badge.svg)](https://codecov.io/gh/matthiaskoenig/cy3sbml)
[![GitHub version](https://badge.fury.io/gh/matthiaskoenig%2Fcy3sbml.svg)](https://badge.fury.io/gh/matthiaskoenig%2Fcy3sbml)
[![License (LGPL version 3)](https://img.shields.io/badge/license-LGPLv3.0-blue.svg?style=flat-square)](http://opensource.org/licenses/LGPL-3.0)
[![Dependency Status](https://www.versioneye.com/user/projects/57a9a0cdf27cc2004c87e56d/badge.svg?style=flat-square)](https://www.versioneye.com/user/projects/57a9a0cdf27cc2004c87e56d)

* [Overview](https://github.com/matthiaskoenig/annotatedb#overview)
* [Overview](https://github.com/matthiaskoenig/annotatedb#citation)
* [Overview](https://github.com/matthiaskoenig/annotatedb#funding)
* [Installation](https://github.com/matthiaskoenig/annotatedb#installation)
* [Changelog](https://github.com/matthiaskoenig/annotatedb#changelog)


## Overview
[[^]](https://github.com/matthiaskoenig/cy3sbml#-cy3sbml---sbml-for-cytoscape-3)
**cy3sbml** is a [Cytoscape 3](http://www.cytoscape.org) app for the Systems Biology Markup Language [SBML](http://www.sbml.org).  
Our mission is the visualization of SBML information within the network context. 
Our vision is a visualization tool for computational models and simulations which seamlessly integrates with computational modeling 
frameworks and workflows.

**App store**: http://apps.cytoscape.org/apps/cy3sbml  
**Latest release**: https://github.com/matthiaskoenig/cy3sbml/releases/latest  
**Support & Forum**: https://groups.google.com/forum/#!forum/cysbml-cyfluxviz  
**Bug Tracker**: https://github.com/matthiaskoenig/cy3sbml/issues  

### Features
**cy3sbml** provides advanced functionality for the import and work with models encoded in SBML, amongst others the 
visualization of SBML network annotations within the network context, direct import of models from repositories 
like [BioModels](http://www.biomodels.org) and one-click access to annotation resources and SBML model information and SBML validation.

* Java based SBML parser for Cytoscape based on [JSBML](https://github.com/sbmlteam/jsbml)
* access to models and annotations via [BioModels](http://www.biomodels.org/), [MIRIAM](http://www.ebi.ac.uk/miriam/main/), and [Ontology Lookup Service](http://www.ebi.ac.uk/ols/index)
* supports all versions of SBML
* SBML validation (SBML warnings and errors accessible)
* Network graph based on the species/reaction model
* Graph of SBML objects (Kinetics, FunctionDefinitions, Parameters, ...)
* Support of qual, comp and fbc extensions
* Provides access to RDF based annotation information within
  the network context (and non-RDF annotations)
* Tested with all models from sbml-test-suite (3.2.0), Biomodels (release 30) and BiGG models (v1.3)

For Cytoscape 2 use [cy2sbml](https://github.com/matthiaskoenig/cy2sbml) with documentation available from http://matthiaskoenig.github.io/cy2sbml/

### Screenshots
![cy3sbml screenshot](https://github.com/matthiaskoenig/cy3sbml/raw/develop/docs/images/screenshot-cy3sbml-0.1.7_01.png)

### Documentation
* [Build instructions](./docs/build.md)

### License
* Source Code: [LGPLv3.0](http://opensource.org/licenses/LGPL-3.0)
* Documentation: [CC BY-SA 4.0](http://creativecommons.org/licenses/by-sa/4.0/)

## Citation
[[^]](https://github.com/matthiaskoenig/cy3sbml#-cy3sbml---sbml-for-cytoscape-3)
Matthias König, Andreas Dräger and Hermann-Georg Holzhütter  
*CySBML: a Cytoscape plugin for SBML*  
Bioinformatics. 2012 Jul 5. [PubMed](http://www.ncbi.nlm.nih.gov/pubmed/22772946) 

## Funding
[[^]](https://github.com/matthiaskoenig/cy3sbml#-cy3sbml---sbml-for-cytoscape-3)
Matthias König is supported by the Federal Ministry of Education and Research (BMBF, Germany) within the research network Systems Medicine of the Liver (**LiSyM**, grant number 031L0054) and Virtual Liver Network (VLN, grant number 0315756), and by the National Resource for Network Biology [NRNB](http://nrnb.org) within the [NRNB Academy Summer Session 2015](http://nrnb.org/gsoc.html).

## Installation
[[^]](https://github.com/matthiaskoenig/cy3sbml#-cy3sbml---sbml-for-cytoscape-3)
### Install
cy3sbml is available via the [Cytoscape App Store](http://apps.cytoscape.org/apps/cy3sbml).  
* install the latest version of [Cytoscape](http://www.cytoscape.org/) (>=3.5.1) 

* In Cytoscape open `Apps → App Manager → Install Apps` and search for `cy3sbml`. 
* Select `cy3sbml` and click install.

After installation cy3sbml is listed in the `Currently Installed` apps tab.  

Alternatively the latests release jars are available from
https://github.com/matthiaskoenig/cy3sbml/releases/latest 
to install manually.

### Uninstall
To uninstall or disable cy3sbml, go to the menu bar and choose `Apps → App Manager → Currently Installed`. Select cy3sbml and click `Uninstall` or `Disable`.

### Build instructions
The develop release contains all features implemented since the latest release.
To work with the latest **develop release** clone the repository and build with `maven`
```
git clone https://github.com/matthiaskoenig/cy3sbml.git cy3sbml
cd cy3sbml
mvn install -DskipTests
```
The test suite takes some minutes to finish. If you want to build with tests use
```
mvn install
```
The `cy3sbml-vx.x.x.jar` if available in the `target` folder.

More detailed build instructions are available from https://goo.gl/4xMgff.

## Changelog
[[^]](https://github.com/matthiaskoenig/cy3sbml#-cy3sbml---sbml-for-cytoscape-3)
**v0.3.0** [2019-09-19]
* major bugfix release
* updated dependencies

**v0.2.7** [2017/11/12]

Bugfix and dependency release
* bugfixes
* updated dependencies

**v0.2.6** [2017/10/03]

Major bugfix release to handle EBI https and Uniprot https.
* bugfixes
* updated dependencies
* cleanup of unused functionality and dependencies

**v0.2.5** [2017/06/19]

* Bug fixes
* Updated UniProt Information
* Updated HTML display
* Updated VisualStyles
* Tested with latest bigg models (v1.3)

**v0.2.2** [2016/08/10]

* Complete redesign of information pane (JavaFx instead of Swing with CSS, JS and HTML)
* HTML export of information
* Ontology information retrieved via Ontology Lookup Service
* Proper formating and display of raw xml in annotations
* Support of KineticLaws
* Offline MIRIAM with latest MIRIAM registry (faster access & reduced webservice calls)
* Improved visual styles (distinguish reversible & irreversible reactions)
* Fixed name attributes
* Improved compatibility to SBMLCore reader (identical attributes)
* Updated icons
* UniProt secondary information for RDF
* Caching of webservice information from OLS and UniProt
* Support of *.sbml files
* Multitude of bug fixes
* Improved session saving & loading (now with layout information)
* Unittest coverage increased to 37%
* Simplified maven dependencies
* Testing of serialization & deserialization
* Updated and tested sbml-test-suite 3.2.0 
* Licence update to LGPL v3.0
* Dependencies updated
* Improved logging

**v0.2.1** [2016/07/11]

* bug fixes (autofocus)

**v0.2.0** [2016/07/01]

* bug fixes

**v0.1.9** [2016/06/28]

* updated test models to BiGG v1.2
* updated test BioModels to release 30
* improved unit tests against test models
* bug fixes related to new test models, SBOTerms, LocalParameters & annotations
* additional support for cy3sabiork
* updated JSBML dependencies and pom files

**v0.1.8** [2016/06/07]

* visualStyles for new and reopened sessions
* display of RDF & non-RDF annotation information
* kineticLaws as first class objects (with annotation display)
* FunctionDefinitions are now parsed correctly
* refactoring of CyNode to SBase mapping (performance increase)
* multitude of smaller bugfixes

**v0.1.7** [2016/03/24]

* position saving and restoring ported from cy2sbml (saving & restoring layouts)
* dark VisualStyle implemented (cy3sbml-dark)
* improved visual styles & dynamic compartment colors
* saving and restoring of full session with SBML files
* COBRA information parsed into attributes
* multiple bugfixes
* first version of cofactor nodes
* tested with Cy3.3 and Cy3.4-milestone-2

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

## Third-party software 
[[^]](https://github.com/matthiaskoenig/cy3sbml#-cy3sbml---sbml-for-cytoscape-3)
cy3sbml uses the following third-party software libraries; these
are distributed along with the bundled cy3sbml app. 
The license statements for these third-party
software libraries can be found at the web addresses noted
below. 

[JSBML](https://github.com/sbmlteam/jsbml) [![License (LGPL 2.1)](https://img.shields.io/badge/license-LGPL2.1-blue.svg?style=flat-square)](https://opensource.org/licenses/LGPL-2.1) [checked 2016-08-03]

[Cytoscape](http://www.cytoscape.org/download.php) [![License (LGPL 2.1)](https://img.shields.io/badge/license-LGPL2.1-blue.svg?style=flat-square)](https://opensource.org/licenses/LGPL-2.1) [checked 2016-08-03]

[org.osgi.core](https://mvnrepository.com/artifact/org.osgi/org.osgi.core) [![License (Apache 2)](https://img.shields.io/badge/license-Apache2-blue.svg?style=flat-square)](https://opensource.org/licenses/Apache-2.0) [checked 2016-08-03]

[Ehcache](https://mvnrepository.com/artifact/net.sf.ehcache/ehcache) [![License (Apache 2)](https://img.shields.io/badge/license-Apache2-blue.svg?style=flat-square)](https://opensource.org/licenses/Apache-2.0) [checked 2016-08-03]

[Apache Commons Lang](https://mvnrepository.com/artifact/org.apache.commons/commons-lang3) [![License (Apache 2)](https://img.shields.io/badge/license-Apache2-blue.svg?style=flat-square)](https://opensource.org/licenses/Apache-2.0) [checked 2016-08-03]

[Apache Commons IO](https://mvnrepository.com/artifact/commons-io/commons-io) [![License (Apache 2)](https://img.shields.io/badge/license-Apache2-blue.svg?style=flat-square)](https://opensource.org/licenses/Apache-2.0) [checked 2016-08-03]

[Apache Velocity](https://mvnrepository.com/artifact/org.apache.velocity/velocity) [![License (Apache 2)](https://img.shields.io/badge/license-Apache2-blue.svg?style=flat-square)](https://opensource.org/licenses/Apache-2.0) [checked 2016-08-03]

[Unirest Java](https://mvnrepository.com/artifact/com.mashape.unirest/unirest-java) [![License (MIT)](https://img.shields.io/badge/license-MIT-blue.svg?style=flat-square)](https://opensource.org/licenses/MIT) [checked 2016-08-03]

[Apache HttpClient](https://mvnrepository.com/artifact/org.apache.httpcomponents/httpclient) [![License (Apache 2)](https://img.shields.io/badge/license-Apache2-blue.svg?style=flat-square)](https://opensource.org/licenses/Apache-2.0) [checked 2016-08-03]

[Apache HttpAsyncClient](https://mvnrepository.com/artifact/org.apache.httpcomponents/httpasyncclient) [![License (Apache 2.0)](https://img.shields.io/badge/license-Apache2-blue.svg?style=flat-square)](https://opensource.org/licenses/Apache-2.0) [checked 2016-08-03]

[Apache HttpClient Mime](https://mvnrepository.com/artifact/org.apache.httpcomponents/httpmime) [![License (Apache 2)](https://img.shields.io/badge/license-Apache2-blue.svg?style=flat-square)](https://opensource.org/licenses/Apache-2.0) [checked 2016-08-03]

[JSON in Java](https://mvnrepository.com/artifact/org.json/json) [![License (JSON)](https://img.shields.io/badge/license-JSON-blue.svg?style=flat-square)](http://www.json.org/license.html) [checked 2016-08-03]

[LibFX](https://mvnrepository.com/artifact/org.codefx.libfx/LibFX) [![License (LGPL 3.0)](https://img.shields.io/badge/license-LGPL3.0-blue.svg?style=flat-square)](https://opensource.org/licenses/LGPL-3.0) [checked 2016-08-03]

[uk.ac.ebi.miriam.registry-lib](https://sourceforge.net/projects/identifiers-org/) [![License (LGPL 2.1)](https://img.shields.io/badge/license-LGPL2.1-blue.svg?style=flat-square)](https://opensource.org/licenses/LGPL-2.1) [checked 2016-08-03]

[uk.ac.ebi.miriam.mirian-lib](https://sourceforge.net/projects/identifiers-org/) [![License (LGPL 2.1)](https://img.shields.io/badge/license-LGPL2.1-blue.svg?style=flat-square)](https://opensource.org/licenses/LGPL-2.1) [checked 2016-08-03]

[OLS client](https://github.com/EBISPOT/OLS) [![License (Apache 2.0)](https://img.shields.io/badge/license-Apache2-blue.svg?style=flat-square)](https://opensource.org/licenses/Apache-2.0) [checked 2016-08-03]

[Jackson Databind](https://mvnrepository.com/artifact/com.fasterxml.jackson.core/jackson-databind) [![License (Apache 2.0)](https://img.shields.io/badge/license-Apache2-blue.svg?style=flat-square)](https://opensource.org/licenses/Apache-2.0) [checked 2016-08-03]

[Uniprot Japi](https://www.ebi.ac.uk/uniprot/japi/license.html) [![License (Apache 2.0)](https://img.shields.io/badge/license-Apache2-blue.svg?style=flat-square)](https://opensource.org/licenses/Apache-2.0) [checked 2016-08-03]

[chebiWS-client](https://www.ebi.ac.uk/chebi/webServices.do) [![License (Apache 2.0)](https://img.shields.io/badge/license-Apache2-blue.svg?style=flat-square)](https://opensource.org/licenses/Apache-2.0) [checked 2016-08-03]

[SLF4J API Module](https://mvnrepository.com/artifact/org.slf4j/slf4j-api) [![License (MIT)](https://img.shields.io/badge/license-MIT-blue.svg?style=flat-square)](https://opensource.org/licenses/MIT) [checked 2016-08-03]

[SLF4J LOG4J 12 Binding](https://mvnrepository.com/artifact/org.slf4j/slf4j-log4j12) [![License (MIT)](https://img.shields.io/badge/license-MIT-blue.svg?style=flat-square)](https://opensource.org/licenses/MIT) [checked 2016-08-03]

[Junit](https://mvnrepository.com/artifact/junit/junit) [![License (EPL-1.0)](https://img.shields.io/badge/license-EPL1.0-blue.svg?style=flat-square)](https://opensource.org/licenses/EPL-1.0) [checked 2016-08-03]

[Mockito](https://mvnrepository.com/artifact/org.mockito/mockito-all) [![License (MIT)](https://img.shields.io/badge/license-MIT-blue.svg?style=flat-square)](https://opensource.org/licenses/MIT) [checked 2016-08-03]

[Apache Taverna Language](https://github.com/apache/incubator-taverna-language) [![License (Apache 2.0)](https://img.shields.io/badge/license-Apache2-blue.svg?style=flat-square)](https://opensource.org/licenses/Apache-2.0) [checked 2016-08-16]

----
&copy; 2012-2019 Matthias König. Developed and maintained by Matthias König, Andreas Dräger and Nicolas Rodriguez.
