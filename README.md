# ![cy3sbml logo](https://github.com/matthiaskoenig/cy3sbml/raw/develop/docs/images/logo100.png) cy3sbml - SBML for Cytoscape

[![CI](https://github.com/matthiaskoenig/cy3sbml/actions/workflows/ci.yml/badge.svg)](https://github.com/matthiaskoenig/cy3sbml/actions/workflows/ci.yml)
[![Documentation](https://img.shields.io/badge/docs-matthiaskoenig.github.io%2Fcy3sbml-teal)](https://matthiaskoenig.github.io/cy3sbml/)
[![DOI](https://zenodo.org/badge/5066/matthiaskoenig/cy3sbml.svg)](https://zenodo.org/badge/latestdoi/5066/matthiaskoenig/cy3sbml)
[![Cytoscape App Store](https://img.shields.io/badge/Cytoscape-App%20Store-blue)](https://apps.cytoscape.org/apps/cy3sbml)
[![MIT License](https://img.shields.io/badge/license-MIT-blue.svg)](https://opensource.org/licenses/MIT)

`cy3sbml` is a [Cytoscape 3](https://cytoscape.org) app that imports SBML models as networks. It
uses [JSBML](https://github.com/sbmlteam/jsbml) to read all SBML levels and versions, with the
`qual`, `fbc` (versions 1 to 3), `comp`, `groups`, `distrib` and `layout` packages. Every SBML object becomes a node,
the relations between the objects become edges, and every layout of the `layout` package becomes a
network with the drawn positions. A panel next to the network shows the SBML information and the
annotations of the selected object, with links to [BioModels](https://www.biomodels.org),
[identifiers.org](https://identifiers.org/) and the [Ontology Lookup Service](https://www.ebi.ac.uk/ols4/index).
Models are imported from SBML files, from COMBINE archives (OMEX), or searched and imported from
the BioModels database. Commands in the Cytoscape REST API (CyREST) automate the import, the access
to the SBML of the networks and the mapping of data onto the nodes, for example from Python
([Automation and REST API](https://matthiaskoenig.github.io/cy3sbml/guide/automation/)).

![cy3sbml in Cytoscape: the base network of the fbc model mini_textbook with the information of the reaction R_PFK](https://github.com/matthiaskoenig/cy3sbml/raw/develop/docs/images/screenshots/main-window-fbc-model.png)

## Documentation

The full documentation is at <https://matthiaskoenig.github.io/cy3sbml/>:

- [Installation](https://matthiaskoenig.github.io/cy3sbml/installation/)
- [User guide](https://matthiaskoenig.github.io/cy3sbml/guide/import/)
- [Development](https://matthiaskoenig.github.io/cy3sbml/development/building/)

## Citation

If you use `cy3sbml`, please cite:

**Matthias König, Andreas Dräger, and Hermann-Georg Holzhütter**
*CySBML: a Cytoscape plugin for SBML*
Bioinformatics, 28(18):2402-2403, 2012.
[doi:10.1093/bioinformatics/bts432](https://doi.org/10.1093/bioinformatics/bts432)

To cite a specific version of the software, use its archive on Zenodo:
[![DOI](https://zenodo.org/badge/5066/matthiaskoenig/cy3sbml.svg)](https://zenodo.org/badge/latestdoi/5066/matthiaskoenig/cy3sbml)

## License

- Source code: [MIT](https://opensource.org/license/MIT)
- Documentation: [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/)

## Funding

Matthias König was supported by the Federal Ministry of Education and Research (BMBF, Germany) within LiSyM by grant number 031L0054 and ATLAS by grant number 031L0304B and by the German Research Foundation (DFG) within the Research Unit Program FOR 5151 QuaLiPerF (Quantifying Liver Perfusion-Function Relationship in Complex Resection - A Systems Medicine Approach) by grant number 436883643 and by grant number 465194077 (Priority Programme SPP 2311, Subproject SimLivA). This work was supported by the BMBF-funded de.NBI Cloud within the German Network for Bioinformatics Infrastructure (de.NBI) (031A537B, 031A533A, 031A538A, 031A533B, 031A535A, 031A537C, 031A534A, 031A532B). MK was supported by the National Resource for Network Biology [NRNB](https://nrnb.org) within the NRNB Academy Summer Session 2015. The project received support from [Google Summer of Code](https://summerofcode.withgoogle.com/).

&copy; 2012-2026 Matthias König, [Systems Medicine of the Liver](https://livermetabolism.com)
