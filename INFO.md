# cy3sbml
## Changelog

**v0.1.3** [?]
* logging with log4j and slf4j implemented (cy3sbml.log)
* support of cy3sbml properties for general settings like preferred VisualStyle
* node EventListener for updating annotation information
* application of layout after generating views

**v0.1.2** [06/2015]
* documentation update (build instructions, installation instructions)

**v0.1** [05/2015]
* first app release (OSGI build with JSBML integration)

## Open issues for v0.1.3
* *TODO* create unit tests & run unit tests within Maven
* *TODO* VisualStyle applying after network read
* *TODO* display annotation information (MIRIAM)

## Open issues
* *TODO* BioModels webservice
* *TODO* JWS online webservice? how can this be integrated?
* *TODO* additional reaction information (kinetics and if available the parameters)
-> preprocessing of the SBML file for availability of referred information in kineticLaws and the Parameters (represent kinetics and parameters in proper view)
* *TODO* save SBML layouts & export SBML
* *TODO* API implementation for other Cytoscape apps
* *TODO* REST interaction

## Bugs
* *BUG cy2sbml* if node is not in SBML, "NamedSBase for node not found" information should be displayed
* *BUG cy2sbml* Visual style can not be changed for GRN Style
* *FIXME cy2sbml* Bugs Biomodel loading and associating with networks (test the interface with BioModels
* *FIXME cy2sbml* problems if offline (generate offline mode -> preload all the annotation information 
available in the annotation files (offline mode)
* *FIXME cy2sbml* if no network view and network selected in NetworkPanel the SBML information in the
Navigation Panel is not updated, reset after session loaded
* *FIX cy2sbml* BioModelDialog -> some hacks in the scrollbar to make it work SearchResults handled as List and not as Set
