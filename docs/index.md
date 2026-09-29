# cy3sbml

cy3sbml is a [Cytoscape](https://cytoscape.org) app that imports models in the
Systems Biology Markup Language ([SBML](https://sbml.org)) as Cytoscape networks.
It reads SBML with [JSBML](https://github.com/sbmlteam/jsbml) and shows the SBML
information and the annotations of the selected object in a panel next to the network.

![cy3sbml in Cytoscape: the base network of the fbc model mini_textbook with the information of the reaction R_PFK](images/screenshots/main-window-fbc-model.png)

## Features

- Import of all SBML levels and versions, including the packages `qual`, `fbc`
  (versions 1 to 3), `comp`, `groups`, `distrib` and `layout`. See
  [Supported SBML packages](guide/packages.md).
- Three networks per model: the reaction network, a kinetic network with parameters,
  rules and kinetic laws, and a network with all SBML objects, plus one network per layout
  of the `layout` package. See [Network model](guide/network.md).
- A panel with the SBML attributes, the annotations, the history and the notes of the
  selected object. Annotations are resolved with the identifiers.org registry, the
  Ontology Lookup Service, UniProt and ChEBI. See
  [Info panel and annotations](guide/info-panel.md).
- Visual styles for SBML networks, a light one and a dark one, each with a variant for the
  layout networks. See [Styles](guide/styles.md).
- Example models, and search and import of models from
  [BioModels](https://www.biomodels.org). See [Importing SBML](guide/import.md).
- Automation from Python, R and other languages: commands for the import, the SBML of the
  networks and the mapping of data onto the nodes, in the Cytoscape command line and the
  REST API (CyREST), with Python examples. See
  [Automation and REST API](guide/automation.md).
- The layouts of the SBML `layout` package as networks with the drawn positions, and
  saving and loading of the node positions of a network. See [Layouts](guide/layouts.md).
- Splitting of cofactor nodes into one node per edge, and merging them back.
  See [Cofactor nodes](guide/cofactors.md).
- Import of the SBML models of COMBINE archives (OMEX). See
  [Importing SBML](guide/import.md#combine-archives).

cy3sbml has no built-in SBML validator, see [Validation](guide/validation.md). The SBML
packages that are not converted are listed in [Supported SBML packages](guide/packages.md).

## Getting started

1. Install cy3sbml from the Cytoscape App Store, see [Installation](installation.md).
2. Click the **SBML examples** button in the Cytoscape toolbar and load one of the
   example models, or import your own SBML file with **Import SBML**.
3. After the import, the base network of the model is shown. The **Network** panel lists
   it with the other networks of the model (`<name>__kinetic`, `<name>__all`) in the
   collection of the model.
4. Select nodes in the network. The **cy3sbml** panel on the right shows the
   information of the selected object. **Help** in the toolbar shows the help page with
   all toolbar buttons of cy3sbml.

## Links

- Cytoscape App Store: [apps.cytoscape.org/apps/cy3sbml](https://apps.cytoscape.org/apps/cy3sbml)
- Source code: [github.com/matthiaskoenig/cy3sbml](https://github.com/matthiaskoenig/cy3sbml)
- Bug tracker: [GitHub issues](https://github.com/matthiaskoenig/cy3sbml/issues)
- Changes of every version: [Release notes](release-notes.md)
- How to cite cy3sbml: [Citation](citation.md)
