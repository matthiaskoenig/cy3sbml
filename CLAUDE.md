# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

cy3sbml is a Cytoscape 3 app (OSGi bundle) that imports SBML models into Cytoscape networks using JSBML. Supports all SBML levels/versions plus the `qual`, `comp`, `fbc`, `groups` and `layout` packages. Main branch is `develop`.

## Build and test

Requires JDK 17 and JavaFX (`sudo apt install openjfx`; the GUI uses JavaFX `WebView`). Use the bundled Maven Wrapper (`./mvnw`, `mvnw.cmd` on Windows) instead of a system Maven install; it downloads the pinned Maven version on first use. CI (`.github/workflows/ci.yml`) builds on Ubuntu and Windows with Temurin 17.

```bash
./mvnw clean install -DskipTests          # build app jar: target/cy3sbml-<version>.jar
./mvnw test                               # fast tests (network and models tests excluded)
./mvnw test -Dtest=IOUtilTest             # single test class
./mvnw test -Dtest=IOUtilTest#testName    # single test method
./mvnw test -Pall-tests                   # all tests incl. network and model suites (slow, needs network)
./mvnw test -Dtest.groups=network -Dtest.excludedGroups=  # only the network tests
```

- Tests are JUnit 6 + Mockito.
- Tests are selected via JUnit tags (`org.junit.jupiter.api.Tag`), controlled by the surefire `<groups>`/`<excludedGroups>` in `pom.xml`, bound to the `test.groups`/`test.excludedGroups` properties. By default `test.excludedGroups` is `network,models`, so tests tagged `network` (hit web services, e.g. `ChebiAccessTest`, `OlsClientTest`, `BioModelInterfaceTest`) and `models` (the long-running `SBMLTestSuiteTest`, `BioModelsTest`, `BiGGTest` suites in `src/test/java/org/cy3sbml/models/`) are skipped. The `all-tests` profile clears `test.excludedGroups` to run everything. Test models live in `src/test/resources/models/`.
- `GoldenModelsTest` (`src/test/java/org/cy3sbml/golden/`) pins the networks `SBMLReaderTask` creates for a set of reference models against a JSON snapshot per model in `src/test/resources/golden/`. After an intended change to the import, regenerate them with `./mvnw -B -q test -Dtest=GoldenModelsTest -Dgolden.update=true` and review the diff before committing.
- `src/test/java/org/cy3sbml/oven/` holds experimental, non-regular tests.
- Java formatting is enforced by Spotless (`palantir-java-format`); run `./mvnw -q spotless:apply` and see the pre-commit hook in `docs/develop.md`.
- The `lint` profile compiles with Error Prone and `-Xlint:all -Werror`. Error Prone needs JDK 21 or newer to run, so point `JAVA_HOME` at a JDK 21+ install for it: `JAVA_HOME=<jdk21> ./mvnw -B -Plint clean verify` (the code still compiles and runs on the pinned JDK 17 otherwise).

### Running in Cytoscape

Symlink the built jar into Cytoscape's apps folder. Cytoscape hot-reloads the app after each `./mvnw install -DskipTests`:

```bash
ln -s $PWD/target/cy3sbml-<version>.jar $HOME/CytoscapeConfiguration/3/apps/installed/cy3sbml-latest.jar
```

Debug by launching `cytoscape.sh debug` and attaching a remote JVM debugger to port 12345. The app writes its log to `~/CytoscapeConfiguration/cy3sbml/`.

## Dependencies

- JSBML (core and its extension modules: `qual`, `layout`, `comp`, `fbc`, `groups`, `distrib`, `tidy`) is not taken from Maven Central. Pre-built jars are in `lib/cy3sbml-dep`, an in-project Maven repository declared in `pom.xml`, pinned to one JSBML commit under the `jsbml.version` property (`1.7-<commit-date>-<short-sha>`). Rebuild them with `lib/build_jsbml_jars.sh <jsbml-commit>` only when upgrading JSBML, and update `jsbml.version` in `pom.xml` to match. Those jars ship JSBML's own JUnit test classes alongside the production ones; see the `maven-bundle-plugin` note below for how the build filters them out.
- Cytoscape API artifacts come from the NRNB Nexus repositories (`cytoscape_releases`, `cytoscape_thirdparty`) and have `provided` scope. Repositories are Maven Central (the default), those two NRNB repositories and the in-project `lib/cy3sbml-dep`; there is no EBI repository, and no SNAPSHOT dependency.
- `maven-bundle-plugin` embeds all non-provided, non-test dependencies (transitively) into the bundle jar and marks imports `resolution:=optional`. New runtime dependencies end up inside the jar automatically. Check for OSGi class loading issues when adding them. The `jsbml`/`jsbml-*` dependencies are the exception: a `maven-dependency-plugin` execution unpacks them into `target/jsbml-classes` with their test/testdata packages and `*Test`/`*Tests`/`*JUnitTests` classes filtered out, and the bundle plugin copies that filtered set in via `Include-Resource` and exports `org.sbml.jsbml.*` from it (`-exportcontents`), instead of embedding those jars and classpath-borrowing from them as-is.

## Architecture

All code is under `org.cy3sbml` (`src/main/java/org/cy3sbml/`).

- **No singletons**: `CyActivator` is the OSGi bundle activator and the single wiring point. It fetches Cytoscape services, constructs every manager/reader/client and injects them into each other and into the actions and task factories, and registers the ones other apps may need as OSGi services (`SBMLManager`, `CyAction`, listeners, `InputStreamTaskFactory`). Add new UI actions or listeners here.
- **Import pipeline** (`reader`): `SBMLFileFilter` + `SBMLReaderTaskFactory` create `SBMLReaderTask`, which reads an `SBMLDocument` with JSBML and turns it into Cytoscape nodes and edges. It holds a `List<PackageReader>` (`CoreReader`, `QualReader`, `FbcReader`, `CompReader`, `GroupsReader`, `LayoutReader`), one per SBML package, run in order against a shared `ConversionContext` (the network plus the SBML id/metaId to node lookups for one model). `AttributeWriter` writes the common SBase/NamedSBase attributes; `MathGraphBuilder` and `UnitGraphBuilder` turn math (`ASTNode`) and unit definitions into subgraphs. One root network gets three subnetworks: `All__<name>`, `Kinetic__<name>`, and a base network (node/edge type sets in `SBML`, built by `SubnetworkBuilder`). The `archive` package does the same for COMBINE/OMEX archives.
- **`SBML`**: constants for node types, edge/interaction types, attribute (column) names and network prefixes. Use these constants, not string literals.
- **`SBMLManager`**: maps `CyNetwork` SUIDs to their `SBMLDocument` through `mapping.Network2SBMLMapper` and `One2ManyMapping` (SBase id/metaId to node SUIDs). Go through `SBMLManager` for all access to an SBML document from a network. `CyActivator` creates the one instance and registers it as an OSGi service so other apps can look it up. Networks and mappings are persisted across Cytoscape sessions by `SessionData`.
- **GUI** (`gui`): `WebViewPanel` is a JavaFX WebView cytopanel. It listens to selection/network events and, via `PanelUpdater`, renders HTML for the selected SBase using `SBaseHTMLFactory`/`SBaseHTMLThread`, with templates from `src/main/resources/gui`. A `RenderCoalescer` skips re-rendering a request that resolves to the same document/SBase as the last one actually rendered, so the several Cytoscape events fired while loading one model render it once. `BrowserHyperlinkListener` routes link clicks (actions, external URLs). `resources/extension` contains a bundled JavaScript extension jar built from `extension/`.
- **Annotation resolution**: `miriam.MiriamRegistry` holds the MIRIAM/identifiers.org registry, seeded from a bundled resource and refreshed from `https://registry.api.identifiers.org/` in the background. The `ols`, `chebi` and `uniprot` web clients share `util.HttpJson` (an HTTP/1.1 JSON client; HTTP/2 to `ebi.ac.uk` is unreliable) and `cache.MemoryCache`, which caches a found result until evicted and a "not found" result (e.g. an HTTP 404) for a short TTL, while never caching a transport/parse error so an outage recovers on the next lookup. `ConnectionProxy` applies Cytoscape proxy settings.
- **Other**: `styles` (`StyleManager`, `StyleFactory`) creates and applies the cy3sbml visual styles from `resources/styles`. `layout` saves and loads node positions (XML). `cofactors` splits cofactor nodes. `biomodel` holds the BioModels search/import dialog. `util` holds shared helpers (`SBMLUtil`, `AttributeUtil`, `NetworkUtil`, `ASTNodeUtil`, ...).
- **`ServiceAdapter`**: bundles the Cytoscape services that actions and tasks need, so they do not each take long constructor lists.

`tools/pycysbml` is a separate Python (uv) helper package for downloading and preparing test models. It is not part of the app build.

## Release

See `docs/release.md`. Release notes go in `release-notes/`. The version lives in `pom.xml`.
