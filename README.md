# ![cy3sbml logo](https://github.com/matthiaskoenig/cy3sbml/raw/develop/docs/images/logo100.png) cy3sbml - SBML for Cytoscape

[![DOI](https://zenodo.org/badge/5066/matthiaskoenig/cy3sbml.svg)](https://zenodo.org/badge/latestdoi/5066/matthiaskoenig/cy3sbml)
[![GitHub version](https://badge.fury.io/gh/matthiaskoenig%2Fcy3sbml.svg)](https://badge.fury.io/gh/matthiaskoenig%2Fcy3sbml)
[![MIT License](https://img.shields.io/pypi/l/pymetadata.svg)](https://opensource.org/licenses/MIT)

`cy3sbml` is a [Cytoscape 3](http://www.cytoscape.org) app designed for seamless integration with the Systems Biology Markup Language ([SBML](http://www.sbml.org)).

### Mission
Our mission is to provide intuitive visualization of SBML information within a network context.

### Vision
We aim to create a powerful visualization tool for computational models and simulations that integrates effortlessly with computational modeling frameworks and workflows.

### Resources
- **App Store**: [Cytoscape App Store](http://apps.cytoscape.org/apps/cy3sbml)
- **Latest Release**: [GitHub Releases](https://github.com/matthiaskoenig/cy3sbml/releases/latest)
- **Bug Tracker**: [GitHub Issues](https://github.com/matthiaskoenig/cy3sbml/issues)

## Features

`cy3sbml` provides advanced functionalities for working with SBML models, including direct imports, network visualization, and annotation access. Key features include:

- **SBML Parsing**: Java-based SBML parser for Cytoscape built on [JSBML](https://github.com/sbmlteam/jsbml).
- **Model Import**: Direct import of SBML models from repositories such as [BioModels](https://www.ebi.ac.uk/biomodels/).
- **Annotation Access**: One-click access to SBML model annotations via [BioModels](https://www.ebi.ac.uk/biomodels/), [identifiers.org](https://identifiers.org/), and the [Ontology Lookup Service](https://www.ebi.ac.uk/ols4/index).
- **SBML Validation**: Built-in validation with accessible warnings and errors.
- **Comprehensive Visualization**:
  - Network graph representation based on species-reaction relationships.
  - Graph visualization of SBML objects (e.g., Kinetics, FunctionDefinitions, Parameters).
  - Supports RDF-based annotation information (as well as non-RDF annotations).
- **Extensive Compatibility**:
  - Supports all versions of SBML.
  - Includes support for `qual`, `comp`, and `fbc` SBML extensions.
- **Robust Testing**:
  - Validated using SBML models from the [SBML Test Suite](https://github.com/sbmlteam/sbml-test-suite) (3.2.0), [BioModels](https://www.ebi.ac.uk/biomodels/) (Release 30), and [BiGG Models](http://bigg.ucsd.edu) (v1.3).

## Screenshots
![cy3sbml screenshot](https://github.com/matthiaskoenig/cy3sbml/raw/develop/docs/images/screenshot-cy3sbml-0.1.7_01.png)


# Citation
If you use `cy3sbml`, please cite the following publication:

**Matthias König, Andreas Dräger, and Hermann-Georg Holzhütter**  
*"CySBML: a Cytoscape plugin for SBML"*  
Bioinformatics, 2012 Jul 5. [PubMed](http://www.ncbi.nlm.nih.gov/pubmed/22772946)

[![DOI](https://zenodo.org/badge/5066/matthiaskoenig/cy3sbml.svg)](https://zenodo.org/badge/latestdoi/5066/matthiaskoenig/cy3sbml)

# License
* Source Code: [MIT](https://opensource.org/license/MIT)
* Documentation: [CC BY-SA 4.0](http://creativecommons.org/licenses/by-sa/4.0/)

# Funding
Matthias König was supported by the Federal Ministry of Education and Research (BMBF, Germany) within LiSyM by grant number 031L0054 and ATLAS by grant number 031L0304B and by the German Research Foundation (DFG) within the Research Unit Program FOR 5151 QuaLiPerF (Quantifying Liver Perfusion-Function Relationship in Complex Resection - A Systems Medicine Approach) by grant number 436883643 and by grant number 465194077 (Priority Programme SPP 2311, Subproject SimLivA). This work was supported by the BMBF-funded de.NBI Cloud within the German Network for Bioinformatics Infrastructure (de.NBI) (031A537B, 031A533A, 031A538A, 031A533B, 031A535A, 031A537C, 031A534A, 031A532B). MK was supported by the National Resource for Network Biology [NRNB](http://nrnb.org) within the [NRNB Academy Summer Session 2015](http://nrnb.org/gsoc.html). The project received support from [Google Summer of Code](https://summerofcode.withgoogle.com/).

# Installation
This section provides an overview of how to install, uninstall, and build `cy3sbml`.

## Installation

`cy3sbml` is available from the [Cytoscape App Store](http://apps.cytoscape.org/apps/cy3sbml) and can be installed using one of the following methods after installing the latest version of Install the latest version of [Cytoscape](http://www.cytoscape.org/).

### Method 1: Installing via the Cytoscape App Store

1. Open Cytoscape.
2. Visit [cy3sbml in the Cytoscape App Store](http://apps.cytoscape.org/apps/cy3sbml) using a web browser.
3. Click **Install**.

### Method 2: Installing via the App Manager in Cytoscape

1. Open Cytoscape.
2. Navigate to `Apps → App Manager → Install Apps`.
3. Search for `cy3sbml`.
4. Select `cy3sbml` and click **Install**.

Once installed, `cy3sbml` will appear under the **Currently Installed** apps tab.

## Uninstallation

To uninstall or disable `cy3sbml`:

1. Open Cytoscape.
2. Navigate to `Apps → App Manager → Currently Installed`.
3. Locate `cy3sbml` in the list.
4. Click **Uninstall** to remove the app or **Disable** to deactivate it without uninstalling.

# Documentation
Documentation is available in the [`./docs/`](./docs/) folder. Information on how to contribute can be found here [`contributing.md`](./docs/contributing.md), information for developers can be found here: [`develo.md`](./docs/develop.md)

&copy; 2012-2025 Matthias König, [Systems Medicine of the Liver](https://livermetabolism.com)