# Complete comp support - design

Issues: [#401](https://github.com/matthiaskoenig/cy3sbml/issues/401),
[#220](https://github.com/matthiaskoenig/cy3sbml/issues/220).

## Goal

cy3sbml imports every part of the SBML Hierarchical Model Composition package (comp,
version 1 release 3) into Cytoscape networks:

- External model definitions are loaded (#220).
- Deletions, replaced elements and replaced-by elements are linked to the elements they
  reference, including references into submodels, ports and nested `sBaseRef` chains (#401).
- All comp attributes are written as columns, including `conversionFactor` and `deletion`
  of replaced elements (#401).
- A network of the flattened model is created for documents with submodels (#401).
- The comp reading code is restructured into small units with clear responsibilities and
  unit tests.

Success criteria:

- `toy_top_level.xml` (4 relative external model definitions) and the Watanabe2014 models
  (`file:<name>.xml` sources) import in Cytoscape with one network collection per model,
  including the external ones, and a flattened network.
- The flattened models of the comp cases of the SBML test suite match the libSBML
  flattening (element ids per type). The SBML test suite ships no flattened models, so a
  script in `tools` writes the libSBML reference.
- Every comp class and attribute of the specification is listed in the docs with its
  conversion.
- `./mvnw -B -q verify` passes without warnings, the lint profile passes, the docs build
  stays warning-free.

## Decisions

1. Flattening and external model loading use JSBML (`CompFlatteningConverter`,
   `ExternalModelDefinition.getReferencedModel`). The JSBML defects are fixed upstream, and
   cy3sbml builds JSBML from a commit of the fork `matthiaskoenig/jsbml` until the pull
   request is merged.
2. The reader learns the location of the imported file from `SBMLFileFilter.accepts(URI)`.
3. Links from comp elements to elements of another model (another network) are stored as
   columns and shown as links in the info panel. Edges are created only inside one network.

## 1. JSBML upstream fixes

A throwaway probe flattened the comp test models with the pinned JSBML: 1 of 17 succeeded.

| Defect | Example |
|---|---|
| `getAbsoluteSourceURI` fails on opaque relative file URIs (`file:represCirc.xml`): "URI is not hierarchical" | Watanabe2014 models |
| `flatten` does not load external model definitions: NPE on `ListOf.get(String)` | `toy_top_level.xml` |
| `internaliseExternalModelDefinitions` + `flatten`: `IndexOutOfBoundsException` | `toy_top_level.xml` |
| NPE when an external document has no comp plugin | `represCirc.xml`, `repressilator.xml` |

Work:

- Branch `comp-fixes` in `matthiaskoenig/jsbml` (from `sbmlteam/jsbml` `master`), pull request
  to `sbmlteam/jsbml`. JSBML compiles for Java 7, the fixes do so too.
- Fix the defects above and every further defect the comp cases of the SBML test suite
  expose, each with a JSBML JUnit test. Baseline with the pinned JSBML against libSBML on
  124 cases: 65 identical, 20 different, 39 exceptions.
- `scripts/update_jsbml.py` gets a `--repository` option (default
  `https://github.com/sbmlteam/jsbml`); the `update-jsbml` workflow gets the same input.
  `jsbml.version` keeps the format `1.7-<commit-date>-<short-sha>`.
- `docs/development/dependencies.md` records that the pin is a fork commit, with the link to
  the pull request. After the merge, the pin moves back to upstream `master`.

## 2. Location of the imported file

Cytoscape passes a reader only the stream and the file name
(`GenericReaderManager.getReader(URI, String)` calls `CyFileFilter.accepts(URI, DataCategory)`
and then `InputStreamTaskFactory.createTaskIterator(InputStream, String)` on the same thread).

- `SBMLFileFilter.accepts(URI, DataCategory)` stores an accepted URI in a `ThreadLocal`.
- `SBMLReaderTaskFactory.createTaskIterator(InputStream, String)` takes and clears it, and uses
  it only if its file name equals the input name. It passes it to `SBMLReaderTask` as a
  nullable `URI location` (constructor parameter).
- `SBMLReaderTask` calls `document.setLocationURI(location)` after reading.
- Without a location, a relative source is not loaded; the warning names the source and says
  that the file location is unknown. Absolute `file:` and `http(s):` sources load.

## 3. Model resolution: `comp.CompModels`

New package `org.cy3sbml.comp`, JSBML only, no Cytoscape types.

`CompModels` resolves a `modelRef` in the scope of a document:

- A `ModelDefinition` of the document, or the main model.
- An `ExternalModelDefinition`: JSBML `getReferencedModel()`, recursive (an external document
  can have its own external model definitions).
- Cached per (absolute source URI, modelRef), so a file referenced several times is read once.
- A cycle of external references is detected and reported.
- If `md5` is set and does not match the source, a warning is logged, the model is still used.
- Result type: sealed `ModelResolution` with `Resolved(Model model, SBMLDocument document)` and
  `Failed(String reason)`. Nothing throws into the import.

## 4. SBaseRef resolution: `comp.SBaseRefResolver`

Resolves the element an `SBaseRef` points to, per the specification:

- Scope model: the own model for a `Port`; the model of the submodel (through `CompModels`)
  for a `Deletion`, a `ReplacedElement` and a `ReplacedBy`.
- `portRef`: the port (PortSId namespace) of the scope model, then the element the port
  references.
- `idRef`: the element with this SId in the scope model.
- `unitRef`: the unit definition (UnitSId namespace) of the scope model.
- `metaIdRef`: the element with this metaid in the scope model.
- A nested `sBaseRef`: the referenced element must be a `Submodel`; the nested reference is
  resolved in the model of that submodel. Recursion to any depth.
- Result type: sealed `SBaseRefResolution` with
  `Resolved(Model model, SBase target, List<Submodel> path)` and `Unresolved(String reason)`.

## 5. `reader.CompReader`

Restructured, one method per comp class, typed lookups instead of linear attribute scans.

- The resolver returns the target object, so the node of a target in the same model is found
  by its metaid. `ConversionContext.nodeById` holds only SIds: unit definitions (UnitSId) and
  ports (PortSId) are left out, so their ids no longer shadow SIds.
- The resolver sets a metaid on every resolved target (`MappingUtil.setSBaseMetaId`), so the
  target metaid is known before the network of the target model is read.
- Nodes (unchanged types): submodel, deletion, port, replaced element, replaced by.
- Edges:
  - port to its target (unchanged, now via the resolver, also for nested references),
  - submodel to each of its deletions (new `INTERACTION_COMP_SUBMODEL_DELETION`),
  - element to its replaced elements and replaced by (unchanged),
  - replaced element and replaced by to the submodel they point into (new
    `INTERACTION_COMP_SBASEREF_SUBMODEL`),
  - deletion, replaced element and replaced by to their target when the target is in the same
    network (for example a reference to a port of the own model).
- Columns (new ones marked):
  - `comp_portRef`, `comp_idRef`, `comp_unitRef`, `comp_metaIdRef`, `comp_submodelRef`,
    `comp_modelRef`, `comp_timeConversionFactor`, `comp_extentConversionFactor`,
  - new: `comp_sBaseRef` (the nested chain, e.g. `sub1 > idRef=S1`),
  - new: `comp_conversionFactor`, `comp_deletion` (replaced element),
  - new: `comp_targetModel`, `comp_targetId`, `comp_targetMetaId`, `comp_targetType` (the
    resolved target), `comp_resolution` (`resolved` or the reason it is not).
- The resolved target of a node is stored by metaid so that the info panel can find the node
  in the network of the target model.

## 6. Networks of one file: `SBMLReaderTask`

- The reader first collects the model sources (record `ModelSource(Model model,
  SBMLDocument document, Kind kind)` with kind `MAIN`, `MODEL_DEFINITION`, `EXTERNAL`,
  `FLAT`), then reads each in a loop. This replaces `readModelDefinitions`.
- One network collection per source: the main model, every model definition, every resolved
  external model (also the ones referenced only by other external documents), and the
  flattened model if the main model has submodels.
- The flattened network has the name prefix `Flat__`. Its source is the document JSBML
  `CompFlatteningConverter` returns.
- `SBMLManager` maps each network to the document of its source (external models to their
  own document, the flattened network to the flattened document).
- A failed external model or a failed flattening logs a warning; the other networks are still
  created.

## 7. Info panel

- `SBaseHTMLFactory` gets sections for `Submodel`, `Deletion`, `ReplacedElement`,
  `ReplacedBy`, `ModelDefinition` and `ExternalModelDefinition` (source, md5, load status),
  in addition to `Port`.
- A resolved target is shown as a link `http://select-target/<model id>/<metaid>`. The link
  action finds the base network of the model with this id that has a node with this metaid,
  makes it current and selects the node (new action in `BrowserHyperlinkListener`, run on the
  event dispatch thread).

## 8. Specification coverage

`docs/guide/packages.md` lists every class and attribute of comp version 1 release 3 with its
conversion:

| Class | Attributes and children |
|---|---|
| SBMLDocument plugin | `required`, listOfExternalModelDefinitions, listOfModelDefinitions |
| ExternalModelDefinition | id, name, source, modelRef, md5 |
| ModelDefinition | the Model |
| Model plugin | listOfSubmodels, listOfPorts |
| Submodel | id, name, modelRef, timeConversionFactor, extentConversionFactor, listOfDeletions |
| SBaseRef | portRef, idRef, unitRef, metaIdRef, sBaseRef |
| Port | id, name, SBaseRef attributes |
| Deletion | id, name, SBaseRef attributes |
| ReplacedElement | submodelRef, deletion, conversionFactor, SBaseRef attributes |
| ReplacedBy | submodelRef, SBaseRef attributes |
| SBase plugin | listOfReplacedElements, replacedBy (on every SBase) |

## 9. Tests

- `SBaseRefResolverTest`, `CompModelsTest`: every reference kind, nested chains, missing
  targets, cycles, md5 mismatch, relative and opaque `file:` sources.
- `CompReaderTest`: new edges and columns.
- `SBMLFileFilter`/`SBMLReaderTaskFactory`: location capture and name check.
- `SBMLReaderTaskTest`: network collections of `toy_top_level.xml` including external and
  flattened networks; import without location.
- Golden snapshots: `toy_top_level` and Watanabe with external models; regenerate the comp
  snapshots and review the diff.
- `SBMLTestSuiteTest` (models group): the flattened model of each comp case matches the
  libSBML reference `src/test/corpora/models/sbml-test-suite/comp-flat-reference.json`,
  written by `tools/pycysbml/comp_flat_reference.py`.
- End to end in Cytoscape 3.10.4 (JDK 17): import `toy_top_level.xml` and a Watanabe model,
  check the collections, the flattened network and the info panel links; update the screenshot
  `docs/images/screenshots/comp-model.png`.

## 10. Documentation

`docs/guide/packages.md`, `docs/guide/import.md`, `docs/guide/network.md`,
`docs/development/dependencies.md`, `CLAUDE.md` and a release notes entry.

## Out of scope

- The layout package (#71) and COMBINE archives (#116).
- Writing SBML.
