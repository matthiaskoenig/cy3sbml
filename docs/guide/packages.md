# Supported SBML packages

cy3sbml reads SBML Level 1, 2 and 3 in all versions with JSBML. The SBML core and four
Level 3 packages are converted into the network. The node and edge types of each package
are listed in [Network model](network.md).

| Package | Status |
|---|---|
| core | supported |
| `qual` (qualitative models) | supported |
| `fbc` (flux balance constraints), versions 1 and 2 | supported |
| `comp` (hierarchical model composition) | supported, with the limits below |
| `groups` | supported |
| `layout` | not yet supported |
| other packages, for example `distrib` | read by JSBML, not converted |

## core

All core objects become nodes: compartments, species, reactions, parameters, kinetic laws
and local parameters, rules, initial assignments, function definitions, events and event
assignments, constraints, and unit definitions with their units. The math of kinetic
laws, rules, assignments and event triggers and priorities becomes edges from every
referenced object to the object with the math.

## qual

Qualitative species and transitions become nodes, inputs and outputs become edges. The
levels, signs, thresholds and transition effects are stored as columns with the prefix
`qual_`.

## fbc

- Species get the columns `fbc_charge` and `fbc_chemicalFormula`.
- Reactions get the columns `fbc_lowerFluxBound` and `fbc_upperFluxBound` with the ids of
  the bound parameters, and an edge from each bound parameter. The flux bounds of fbc
  version 1 are read as well.
- Every objective becomes a column `fbc_objective-<objective id>` with the coefficients
  of its reactions. The active objective is not marked.
- Gene products become nodes. Gene product associations become a tree of `AND` and `OR`
  nodes that ends in the reaction. The gene associations of fbc version 1 are not read.
- The COBRA key value pairs in the notes, for example `GENE_ASSOCIATION`, become columns.

## comp (hierarchical model composition)

- Submodels, ports, deletions, replaced elements and replaced-by elements become nodes,
  with edges to the elements they reference.
- Every model definition in the file gets its own network collection, in addition to the
  main model.
- External model definitions are not loaded. Import the referenced files one by one.
- The flattened model is not created, and the edges from deletions to the deleted
  elements are not created yet
  ([issue #401](https://github.com/matthiaskoenig/cy3sbml/issues/401)).

![The All network of a comp test model with submodels, deletions and replaced elements](../images/screenshots/comp-model.png)

## groups

Every group becomes a Cytoscape group of the nodes of its members in the base and the
kinetic network. The SBO term, notes and annotation of a list of members are applied to
the members that do not have their own.

## layout

The layout package is not imported yet
([issue #71](https://github.com/matthiaskoenig/cy3sbml/issues/71)). If a model has
layouts, a warning is written to the log file, and the force-directed layout is used.
See [Layouts](layouts.md).

## COMBINE archives

COMBINE archives (OMEX) are not supported yet. See
[Importing SBML](import.md#combine-archives).
