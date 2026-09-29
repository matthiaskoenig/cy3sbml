# distrib support (#281)

## Goal

Read and display the information of the SBML Level 3 `distrib` package (version 1, the
final specification) for model browsing: the uncertainties of SBML elements in the network
tables and the info panel, and the distribution functions in math.

## Current state

- The pinned JSBML (fork `matthiaskoenig/jsbml`, branch `cy3sbml`) implements distrib
  version 1: `DistribSBasePlugin` (`listOfUncertainties` on any SBase), `Uncertainty`,
  `UncertParameter` (type, `value`, `var`, `units`, `definitionURL`, math, nested
  `listOfUncertParameters`) and `UncertSpan` (`valueLower`/`varLower`,
  `valueUpper`/`varUpper`). Reading and writing a spec-conformant distrib model works: a
  JSBML round trip of a model written by libSBML (`python-libsbml-experimental`) validates
  without errors in libSBML, nested uncertainty parameters and math included.
- Distribution functions in math (`<csymbol definitionURL="http://www.sbml.org/sbml/symbols/distrib/normal">`)
  parse as `FUNCTION_CSYMBOL` and render as `normal(mean, sd)`. `MathGraphBuilder` creates
  the edges from the referenced objects as for any other math.
- cy3sbml ignores distrib: no package reader, no info panel content.
- The test models in `src/test/resources/models/distrib/` use the obsolete 2015 UncertML
  draft of distrib (`<distrib:uncertainty><UncertML ...>`), which JSBML does not read. The
  corpus has 61 SBML test suite `stochastic` models with distribution functions in math and
  no uncertainties.

## JSBML (upstream)

Changes on a branch `distrib-fixes` of the fork, merged into the fork branch `cy3sbml`, each with a
JSBML test, and a pull request to `sbmlteam/jsbml`. cy3sbml re-pins with
`scripts/update_jsbml.py`.

1. **Uncertainty ids.** Reading any model with an uncertainty id logs the WARN
   `registerIds: the object org.sbml.jsbml.ext.distrib.Uncertainty is neither a
   UniqueNamedSBase, ...`. The ids of distrib objects (`DistribBase`) are not in the SId
   namespace of the model: libSBML accepts an uncertainty with the id of a parameter. JSBML
   must not try to register them, so no warning is logged.
2. **Validation of distrib math.** `SBMLDocument.checkConsistencyOffline()` reports three
   MathML errors (`definitionURL`/`encoding` not permitted on the element, `definitionURL`
   value not permitted on a `csymbol`) for the distribution `csymbol`s of valid models
   (libSBML: no errors, for example SBML test suite `stochastic/00091`). With the distrib
   package enabled on the document, the `csymbol`s of the distrib definition URLs are valid.

The read/write behaviour needs no change; a round-trip test of all distrib elements is added
in JSBML to keep it that way.

## cy3sbml

### DistribReader

A new `PackageReader`, `reader.DistribReader`, runs after `CoreReader` (registered in
`SBMLReaderTask` after `GroupsReader`, before `LayoutReader`).

- It visits every SBase of the model (the `SBase` tree, not only the core lists, so that
  uncertainties of package elements are found as well) whose `DistribSBasePlugin` has at
  least one uncertainty.
- The element of the SBase is its node (`ConversionContext.nodeByMetaId`; every node
  created by `ConversionContext.createNode` has a metaId). Species references are edges:
  `ConversionContext` gets an edge lookup by metaId, and `CoreReader` registers the
  reactant and product edges of the species references (the modifier edges as well, for
  consistency). SBases with neither a node nor an edge (for example a list) are shown in the
  info panel only.
- Columns (constants in `SBML`), on nodes and edges:
  - `distrib_uncertainty` (String): the summary of the uncertainties of the element. One
    uncertainty is written as its parameters separated by `; `: `type=value` with the
    `value`, else the `var`, followed by the units if set (`standardDeviation=0.3 mole`,
    `mean=p2`); a span as `type=[lower, upper]` (`confidenceInterval=[1.0, 5.0]`, lower
    and upper each the value or the var); a parameter with math as `type=<formula>`
    (`distribution=normal(3, 0.5)`); an `externalParameter` with its name if set, else its
    `definitionURL`. Nested parameters are appended in parentheses after their parent
    (`distribution=normal(3, 0.5) (skew=0.1)`). Several uncertainties are separated by
    ` | `, prefixed with their id if set (`u1: standardDeviation=0.3`).
  - `distrib_uncertaintyCount` (Integer): the number of uncertainties, for filters and
    styles.
- The obsolete UncertML draft: JSBML does not read it, so the reader cannot see it. The
  reader does not guess; the draft files are removed from the tests (see below).
- The flattened comp model (`Flat__<name>`) is read by the same readers, so its
  uncertainties are handled the same way.

### Info panel

- `SBaseHTMLFactory` renders an "Uncertainties" section for every SBase with
  uncertainties, after the class-specific table and before the annotations. It is built in
  `SBMLUtil` (as the fbc and comp maps) and a template fragment.
- Per uncertainty: the id and name if set, then a table with one row per uncertainty
  parameter or span: type, value (the `value`, or the `var` as a link to the referenced
  element through the existing select link mechanism, or `[lower, upper]` for a span),
  units, `definitionURL` (as an external link), and the math as formula. Nested parameters
  follow their parent row, indented.
- An `Uncertainty`, `UncertParameter` and `UncertSpan` have no nodes, so they are not
  selected on their own.

### Distribution functions in math

No change in the conversion. A golden snapshot of an SBML test suite `stochastic` model
pins the `math` column and the math edges of the distribution functions.

## Tests

- Test models (`src/test/resources/models/distrib/`), written with libSBML
  (`python-libsbml-experimental`) by a script in `tools/pycysbml` and validated by it:
  - `distrib_uncertainties.xml`: uncertainties on a compartment, species, parameter,
    initial assignment, rules, reaction, species reference, kinetic law and local
    parameter; every uncertainty parameter type, spans with values and vars, units, `var`
    references, a distribution with math and nested external parameters, an element with
    two uncertainties, uncertainty ids and names.
  - `distrib_math.xml`: a copy of SBML test suite `stochastic/00091` (distribution
    functions in math).
- The UncertML draft files (`distrib_all_elements.xml`, `distrib_exchange_values.xml`,
  `distrib_exchange_variables.xml`, `distrib_stochastic_tests.zip`) and the golden
  `distrib__distrib_all_elements.json` are removed; `GoldenModelsTest` pins the two new
  models.
- `DistribReaderTest`: the columns of nodes and edges for the test model, the summary
  format, and a model without distrib (no columns written).
- `SBMLUtil`/`SBaseHTMLFactory` test: the uncertainty table for a parameter with a nested
  distribution.
- Session round trip: `SessionData` writes and reads the document with the uncertainties
  unchanged.
- No new warnings in the test output (the JSBML id warning is gone after the re-pin).
- Manual check in Cytoscape: import `distrib_uncertainties.xml`, check the columns in the
  node and edge tables and the info panel of a parameter and a species reference.

## Documentation

- `docs/guide/packages.md`: `distrib` becomes supported, with a section describing the
  columns and the info panel; `docs/guide/network.md` lists the columns if it lists
  package columns.
- `docs/index.md`, `CLAUDE.md`: distrib in the list of supported packages.
- `docs/development/dependencies.md`: the JSBML fork branch includes the distrib fixes.
- `release-notes/0.7.0.md`: distrib support (#281).

## Out of scope

- Uncertainty nodes or edges from the `var` references (decided: columns and info panel
  only).
- The UncertML draft format.
- Sampling or simulation of distributions.
