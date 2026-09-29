# Complete comp support Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Import every part of SBML comp v1r3: external model definitions (#220), resolved SBaseRef targets, all comp attributes and a flattened network (#401), with the comp reading restructured.

**Architecture:** JSBML (fixed in the fork `matthiaskoenig/jsbml`, pinned by commit) flattens models and loads external models. A new JSBML-only package `org.cy3sbml.comp` resolves model references (`CompModels`) and SBaseRef chains (`SBaseRefResolver`). `CompReader` turns the results into nodes, edges and columns; `SBMLReaderTask` creates one network collection per model source (main, model definitions, external models, flattened).

**Tech Stack:** Java 17, JSBML 1.7 (fork commit), Cytoscape 3.10 API, JUnit 6, Mockito, python-libsbml (reference data only).

**Spec:** `docs/superpowers/specs/2026-09-29-comp-support-design.md`

## Global Constraints

- JDK 17 for cy3sbml and for building JSBML; JSBML sources stay Java 7 compatible.
- Use `./mvnw`; `./mvnw -B -q verify` prints no warnings; `JAVA_HOME=~/.local/share/jdk-25 ./mvnw -B -Plint clean verify` passes.
- Format with `./mvnw -q spotless:apply` before each commit.
- Constants for node types, interactions and columns live in `SBML`; no string literals.
- `lib/cy3sbml-dep` changes only through `scripts/update_jsbml.py`.
- Never the em dash character. Commit messages without agent co-author lines.
- Golden snapshots change only by `-Dgolden.update=true`, diff reviewed.
- Docs build stays warning-free.

## Review Focus

1. A relative external source imported from a URL (`http://.../top.xml`) - resolves against the URL (test in Task 5).
2. Two external model definitions pointing to the same file with different `modelRef` - file read once, both models resolved (Task 5).
3. An external source that is missing or is not SBML - warning, the other networks are still created, import does not fail (Task 8).
4. An SBaseRef chain that ends at a deleted or missing element in a nested submodel - `comp_resolution` names the failing step, no exception (Task 6).
5. A document with only model definitions and no main model - networks of the definitions, no flattened network, no exception (Task 8).

---

### Task 1: libSBML flattening reference

**Files:**
- Create: `tools/pycysbml/comp_flat_reference.py`
- Modify: `tools/pyproject.toml` (dependency `python-libsbml>=5.21.2`), `tools/uv.lock`
- Create: `src/test/corpora/models/sbml-test-suite/comp-flat-reference.json`, `src/test/resources/models/comp/comp-flat-reference.json`

**Interfaces:**
- Produces: JSON `{ "<path relative to the JSON file>": { "species": [ids], "parameters": [...], "compartments": [...], "reactions": [...], "rules": [variables], "events": [...], "unitDefinitions": [...], "functionDefinitions": [...], "initialAssignments": [symbols] } }`, keys sorted, id lists sorted. Only files libSBML flattens without error; files it cannot flatten are listed under `"_failed"` with the libSBML error.

- [ ] Step 1: Write the script: `comp_flat_reference.py <directory> <output.json>` finds every `*.xml` below the directory containing `comp:submodel`, flattens with `ConversionProperties` option `flatten comp`, writes the JSON above.
- [ ] Step 2: Run for `src/test/corpora/models/sbml-test-suite/semantic` (only `*-sbml-l3v1.xml`) and `src/test/resources/models/comp`. Check counts (expected about 124 test suite cases).
- [ ] Step 3: `uv run --project tools ruff check`, `ruff format --check`, `ty check`.
- [ ] Step 4: Commit "Add the libSBML flattening reference of the comp test models".

### Task 2: JSBML comp fixes (fork)

**Files (in `~/git/jsbml`, branch `comp-fixes` from `origin/master`, remote `fork` = `https://github.com/matthiaskoenig/jsbml`):**
- Modify: `extensions/comp/src/org/sbml/jsbml/ext/comp/ExternalModelDefinition.java`, `extensions/comp/src/org/sbml/jsbml/ext/comp/util/CompFlatteningConverter.java`
- Test: `extensions/comp/test/org/sbml/jsbml/ext/comp/test/` (new resources under `extensions/comp/test/.../resources` or the existing test resource layout)

**Interfaces:**
- Produces: `CompFlatteningConverter.flatten(SBMLDocument)` loads external model definitions itself (through `getReferencedModel()` and the document location), and `ExternalModelDefinition.getAbsoluteSourceURI()` resolves `file:name.xml`.

- [ ] Step 1: Harness in the cy3sbml scratchpad: a Java program (classpath of the JSBML build) that flattens every file of the reference JSON and diffs ids per type against it. Record the baseline (65 identical, 20 different, 39 exceptions).
- [ ] Step 2: For each defect class (opaque `file:` URI; external definitions not internalised; `IndexOutOfBoundsException`; NPE on external documents without comp; each id difference class): write a failing JUnit test in JSBML with a minimal model, fix, rerun the JSBML comp tests and the harness.
- [ ] Step 3: Repeat until the harness shows 0 exceptions and 0 differences, or a remaining difference is a libSBML deviation from the spec (document it in the PR).
- [ ] Step 4: Commit per fix, push the branch to the fork. Ask the user before opening the pull request to `sbmlteam/jsbml`.

### Task 3: Build JSBML from a fork commit

**Files:**
- Modify: `scripts/update_jsbml.py` (option `--repository`, default `https://github.com/sbmlteam/jsbml`), `.github/workflows/update-jsbml.yml` (input `repository`), `docs/development/dependencies.md`, `pom.xml` (`jsbml.version`, `jsbml.osgi.version` via the script), `lib/cy3sbml-dep/**` (via the script)

- [ ] Step 1: Add the option; `checkout` clones the given repository. Ruff and ty clean.
- [ ] Step 2: `uv run --no-project --python 3.14 python scripts/update_jsbml.py --repository https://github.com/matthiaskoenig/jsbml <commit>`.
- [ ] Step 3: `./mvnw -B -q verify` and `./mvnw -B -q test -Pall-tests` (network may flake, rerun) pass; golden snapshots unchanged.
- [ ] Step 4: Document the fork pin and PR link in `dependencies.md`. Commit.

### Task 4: Location of the imported file

**Files:**
- Modify: `src/main/java/org/cy3sbml/SBMLFileFilter.java`, `src/main/java/org/cy3sbml/SBMLReaderTaskFactory.java`, `src/main/java/org/cy3sbml/reader/SBMLReaderTask.java`
- Test: `src/test/java/org/cy3sbml/SBMLFileFilterTest.java` (exists? else create), `src/test/java/org/cy3sbml/SBMLReaderTaskFactoryTaskTest.java`

**Interfaces:**
- Produces: `SBMLFileFilter.takeAcceptedUri(String inputName): Optional<URI>` (returns and clears the thread's last accepted URI if its file name equals `inputName`); `SBMLReaderTask(InputStream, String fileName, URI location /* nullable */, ...)` for both constructors; `SBMLReaderTask` sets `document.setLocationURI(location.toString())`.

```java
// SBMLFileFilter
private final ThreadLocal<URI> acceptedUri = new ThreadLocal<>();

public boolean accepts(URI uri, DataCategory category) {
    ...
    boolean accepted = accepts(stream, category);
    if (accepted) {
        acceptedUri.set(uri);
    }
    return accepted;
}

/** The URI accepted last on this thread if its file name is the input name; clears it. */
public Optional<URI> takeAcceptedUri(String inputName) {
    URI uri = acceptedUri.get();
    acceptedUri.remove();
    if (uri == null || inputName == null) {
        return Optional.empty();
    }
    String path = uri.getPath();
    String name = path == null ? null : path.substring(path.lastIndexOf('/') + 1);
    return inputName.equals(name) ? Optional.of(uri) : Optional.empty();
}
```

- [ ] Step 1: Failing tests: accepted file URI is taken once with matching name; mismatching name gives empty; other thread sees nothing; not-accepted URI is not stored.
- [ ] Step 2: Implement; the factory keeps the `SBMLFileFilter` it is constructed with (constructor type `SBMLFileFilter`, update `CyActivator`).
- [ ] Step 3: Test: `SBMLReaderTask` with location of `toy_top_level.xml` sets the document location.
- [ ] Step 4: Commit.

### Task 5: `comp.CompModels`

**Files:**
- Create: `src/main/java/org/cy3sbml/comp/CompModels.java`, `src/main/java/org/cy3sbml/comp/ModelResolution.java`
- Test: `src/test/java/org/cy3sbml/comp/CompModelsTest.java`, small test models in `src/test/resources/models/comp/unit/`

**Interfaces:**
```java
public sealed interface ModelResolution {
    record Resolved(Model model, SBMLDocument document) implements ModelResolution {}
    record Failed(String reason) implements ModelResolution {}
}

public final class CompModels {
    /** Resolver for the model references of the document and the external documents it loads. */
    public CompModels(SBMLDocument document);
    /** The model with the id in the document: main model, model definition or external model definition. */
    public ModelResolution resolve(SBMLDocument scope, String modelRef);
    /** The model a submodel instantiates. */
    public ModelResolution resolve(Submodel submodel);
    /** Every external model reachable from the document, each once, in reading order, with its definition. */
    public List<External> externalModels();
    public record External(ExternalModelDefinition definition, ModelResolution resolution) {}
}
```
- Loads an external document once per absolute source URI (cache `Map<URI, SBMLDocument>`), sets its location, follows `modelRef` (or its main model if `modelRef` unset), follows chains of external definitions, detects cycles (`Failed("cycle: a.xml#m -> b.xml#m -> a.xml#m")`), checks `md5` (warning, still resolved).

- [ ] Step 1: Failing tests: model definition; main model by id; external relative (`toy_top_level.xml`); `file:represCirc.xml`; relative source of a document loaded from an `http` URL (mock `getReferencedModel` not possible, so test the URI resolution helper `CompModels.sourceUri(ExternalModelDefinition, SBMLDocument)` directly); unknown modelRef; missing file; cycle; md5 mismatch; same file twice read once.
- [ ] Step 2: Implement. Step 3: Tests pass. Step 4: Commit.

### Task 6: `comp.SBaseRefResolver`

**Files:**
- Create: `src/main/java/org/cy3sbml/comp/SBaseRefResolver.java`, `src/main/java/org/cy3sbml/comp/SBaseRefResolution.java`
- Test: `src/test/java/org/cy3sbml/comp/SBaseRefResolverTest.java`

**Interfaces:**
```java
public sealed interface SBaseRefResolution {
    record Resolved(Model model, SBase target, List<Submodel> path) implements SBaseRefResolution {}
    record Unresolved(String reason) implements SBaseRefResolution {}
}

public final class SBaseRefResolver {
    public SBaseRefResolver(CompModels models);
    /** Port: in its own model. Deletion, ReplacedElement, ReplacedBy: in the model of their submodel. */
    public SBaseRefResolution resolve(SBaseRef ref);
    /** Textual chain, e.g. "submodelRef=A > portRef=p > idRef=S1". */
    public static String describe(SBaseRef ref);
}
```
- `portRef`: `CompModelPlugin.getPort(id)`, then resolve the port as an SBaseRef in the same model. `idRef`: `Model.getSBaseById` restricted to SIds (exclude `UnitDefinition`, `Port`). `unitRef`: `Model.getUnitDefinition(id)` (also predefined base units are not SBase targets: Unresolved). `metaIdRef`: `SBMLDocument.getElementByMetaId` restricted to the model. Nested `sBaseRef`: the target must be a `Submodel`; resolve the nested ref in `models.resolve(submodel)`, append to path. Sets a metaid on every resolved target with `MappingUtil.setSBaseMetaId(targetDocument, target)`.

- [ ] Step 1: Failing tests on `01134-sbml-l3v1.xml`, `toy_top_level.xml` and small inline SBML strings: each ref kind, nested two levels, port to port, missing target, nested ref into a non-submodel, unitRef to a base unit.
- [ ] Step 2: Implement. Step 3: Pass. Step 4: Commit.

### Task 7: `CompReader` refactor

**Files:**
- Modify: `src/main/java/org/cy3sbml/reader/CompReader.java`, `ConversionContext.java`, `AttributeWriter.java`, `SBML.java`, `PackageReader.java` if a shared per-document context is needed, `src/main/resources/styles/cy3sbml.xml`, `cy3sbml-dark.xml` (new interactions if interactions are styled), `StyleInfo*.java` as needed
- Test: `src/test/java/org/cy3sbml/reader/CompReaderTest.java`, `ReaderTestSupport.java`

**Interfaces:**
- Consumes: `SBaseRefResolver`, `CompModels`.
- `ConversionContext` gets `CompModels compModels()` (created once per document by `SBMLReaderTask`, passed to every context of the document; `ReaderTestSupport` creates one).
- New `SBML` constants: `ATTR_COMP_SBASEREF = "comp_sBaseRef"`, `ATTR_COMP_CONVERSION_FACTOR = "comp_conversionFactor"`, `ATTR_COMP_DELETION = "comp_deletion"`, `ATTR_COMP_TARGET_MODEL = "comp_targetModel"`, `ATTR_COMP_TARGET_ID = "comp_targetId"`, `ATTR_COMP_TARGET_METAID = "comp_targetMetaId"`, `ATTR_COMP_TARGET_TYPE = "comp_targetType"`, `ATTR_COMP_RESOLUTION = "comp_resolution"`, `INTERACTION_COMP_SUBMODEL_DELETION = "submodel-deletion"`, `INTERACTION_COMP_SBASEREF_SUBMODEL = "sbaseRef-submodel"`; both interactions in the kinetic edge list.
- `ConversionContext.createNode` stores ids of `UnitDefinition` and `Port` not in `id2Node`.

- [ ] Step 1: Failing tests: deletion edge from submodel; replacedElement columns conversionFactor/deletion; target columns for a replaced element into an external model (`toy_top_level.xml` + location); nested sBaseRef column; port with portRef chain; replacedElement edge to submodel; unit id not shadowing species id.
- [ ] Step 2: Rewrite `CompReader` with methods `readSubmodels`, `readPorts`, `readReplacements` (iterating `model.filter(new SBaseFilter())`), `writeTarget(CyNode, SBaseRef)` using the resolver; edge to target only if the resolved model is the read model.
- [ ] Step 3: Tests pass; `./mvnw -B -q test` (golden may change: regenerate and review after Task 8).
- [ ] Step 4: Commit.

### Task 8: Networks per model source

**Files:**
- Create: `src/main/java/org/cy3sbml/reader/ModelSource.java`
- Modify: `src/main/java/org/cy3sbml/reader/SBMLReaderTask.java`, `SubnetworkBuilder.java` (prefix `Flat__` for the flat source), `src/main/java/org/cy3sbml/SBML.java` (`PREFIX_FLAT_NETWORK`)
- Test: `src/test/java/org/cy3sbml/reader/SBMLReaderTaskTest.java`, `src/test/java/org/cy3sbml/golden/GoldenModelsTest.java` (add `toy_top_level` with location), golden JSONs

**Interfaces:**
```java
record ModelSource(Model model, SBMLDocument document, Kind kind) {
    enum Kind { MAIN, MODEL_DEFINITION, EXTERNAL, FLAT }
}
```
- `SBMLReaderTask.sources()`: main model, model definitions, `compModels.externalModels()` resolved ones, flat (`new CompFlatteningConverter().flatten(document)` in try/catch, only if the main model has submodels).
- `buildCyNetworkView` registers each network with the document of its source (map root SUID to source document).

- [ ] Step 1: Failing tests: `toy_top_level.xml` with location gives collections main + 4 external + flat, 3 subnetworks each; without location: main + flat failure warning, no exception; missing external file; document without main model; flat network has the species of the libSBML reference.
- [ ] Step 2: Implement. Step 3: Regenerate golden snapshots, review the diff (only comp changes expected). Step 4: `./mvnw -B -q verify`. Commit.

### Task 9: Info panel

**Files:**
- Modify: `src/main/java/org/cy3sbml/gui/SBaseHTMLFactory.java`, `src/main/java/org/cy3sbml/gui/BrowserHyperlinkListener.java`, `src/main/java/org/cy3sbml/SBMLManager.java` (lookup of the base network of a model id)
- Test: `src/test/java/org/cy3sbml/gui/SBaseHtmlThreadTest.java` or a new `SBaseHTMLFactoryCompTest`, `BrowserHyperlinkListenerTest` if present

**Interfaces:**
- `BrowserHyperlinkListener.URL_SELECT_TARGET = "http://select-target/"`, link `URL_SELECT_TARGET + modelId + "/" + metaId`.
- `SBMLManager.findNetwork(String modelId, String cyId): Optional<CyNetwork>`.
- HTML for `Submodel` (modelRef, conversion factors, deletions), `Deletion`, `ReplacedElement`, `ReplacedBy`, `Port` (target link), `ExternalModelDefinition`, `ModelDefinition`.

- [ ] Step 1: Failing HTML tests (contains modelRef, conversionFactor, target link). Step 2: Implement. Step 3: Pass. Step 4: Commit.

### Task 10: Flattening against the reference in the model suite

**Files:**
- Modify: `src/test/java/org/cy3sbml/models/SBMLTestSuiteTest.java` (tag `models`)
- Create: `src/test/java/org/cy3sbml/comp/CompFlatteningReferenceTest.java` (fast, resources models)

- [ ] Step 1: Parametrized test per reference entry: flatten with JSBML, compare id lists per type. Step 2: Run `./mvnw -B -q test -Dtest.groups=models -Dtest.excludedGroups= -Dtest=SBMLTestSuiteTest`. Step 3: Commit.

### Task 11: Docs

**Files:** `docs/guide/packages.md` (spec coverage table, remove "not loaded"/"not yet" limits), `docs/guide/import.md`, `docs/guide/network.md`, `docs/development/architecture.md`, `docs/development/testing.md`, `CLAUDE.md`, `README.md` if it states limits.

- [ ] Step 1: Update; build docs (`scripts/release_notes.py`, `zensical build --clean`) warning-free. Step 2: Commit.

### Task 12: End to end and quality gate

- [ ] Step 1: `./mvnw clean install -DskipTests`, start Cytoscape 3.10.4 with JDK 17, import `toy_top_level.xml` and `Watanabe2014/test_replacement_1.xml` via File > Import > Network from File. Check collections, flat network, columns, info panel target link. Fix anything that looks off.
- [ ] Step 2: Update `docs/images/screenshots/comp-model.png`.
- [ ] Step 3: `./mvnw -B -q verify`, lint profile, `-Pall-tests`, Python checks, docs build.
- [ ] Step 4: Final review (superpowers:requesting-code-review), push branch, open PR to `develop` after asking.
