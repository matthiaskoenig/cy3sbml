# cy3sbml
## open issues
* TODO: create unit tests
* TODO: create proper logging
* TODO: release - create jar for git release (see cyREST)
* TODO: VisualStyle : definition, loading, applying after network read
* TODO: Layout : apply force layout after read
* TODO/INFO: API - how to provide API functions in a plugin ?
* TODO: JWS online webservice ? how, is it possible?
* TODO: display annotation information


## cy2sbml bugs
<BUG>
if node is not in SBML, "NamedSBase for node not found" information should be displayed;

<BUG> 
Visual style can not be changed for GRN Style.
  
<FIXME> 
Bugs Biomodel loading and associating with networks (test the interface with BioModels.

<FIXME>  
problems if offline (generate offline mode -> preload all the annotation information
available in the annotation files (offline mode) 

<IMPLEMENT> 
additional reaction information (kinetics and if available the parameters)
-> preprocessing of the SBML file for availability of referred information in 
kineticLaws and the Parameters (represent kinetics and parameters in proper view)

<IMPLEMENT> save SBML layouts & export SBML
<IMPLEMENT> socket connection, REST interaction

<FIXME> 
if no network view and network selected in NetworkPanel the SBML information in the
Navigation Panel is not updated, reset after session loaded
<FIX>
BioModelDialog -> some hacks in the scrollbar to make it work
SearchResults handled as List and not as Set