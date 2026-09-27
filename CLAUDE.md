# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

cy3sbml is a Cytoscape 3 app (OSGi bundle) that imports SBML models into Cytoscape networks using JSBML. Supports all SBML levels/versions plus the `qual`, `comp`, `fbc`, `groups` and `layout` packages. Main branch is `develop`.

## Build and test

Requires JDK 17, Maven 3, and JavaFX (`sudo apt install openjfx`; the GUI uses JavaFX `WebView`). CI (`.github/workflows/ci.yml`) builds on Ubuntu and Windows with Temurin 17.

```bash
mvn clean install -DskipTests          # build app jar: target/cy3sbml-<version>.jar
mvn test                               # fast tests (long-running model tests excluded)
mvn test -Dtest=IOUtilTest             # single test class
mvn test -Dtest=IOUtilTest#testName    # single test method
mvn clean install -Dmodels.test.excludes=""  # all tests incl. model suites (slow, needs network)
```

- Tests are JUnit 5 + Mockito.
- `models.test.excludes` in `pom.xml` excludes `**/models/*Test.java` (SBML Test Suite, BioModels, BiGG) plus `OLSClientTest` and `BioModelInterfaceTest`, which hit web services. Test models live in `src/test/resources/models/`.
- `src/test/java/org/cy3sbml/oven/` holds experimental, non-regular tests.
- Java formatting uses the IntelliJ formatter (see pre-commit hook in `docs/develop.md`).

### Running in Cytoscape

Symlink the built jar into Cytoscape's apps folder. Cytoscape hot-reloads the app after each `mvn install -DskipTests`:

```bash
ln -s $PWD/target/cy3sbml-<version>.jar $HOME/CytoscapeConfiguration/3/apps/installed/cy3sbml-latest.jar
```

Debug by launching `cytoscape.sh debug` and attaching a remote JVM debugger to port 12345. The app writes its log to `~/CytoscapeConfiguration/cy3sbml/`.

## Dependencies

- JSBML (`1.7-SNAPSHOT` and its extension modules) is not taken from Maven Central. Pre-built jars are in `lib/cy3sbml-dep`, an in-project Maven repository declared in `pom.xml`. Rebuild them with `lib/build_jsbml_jars.sh` only when upgrading JSBML, and update versions in `pom.xml` and the script together.
- Cytoscape API artifacts come from the NRNB Nexus repositories and have `provided` scope.
- `maven-bundle-plugin` embeds all non-provided, non-test dependencies (transitively) into the bundle jar and marks imports `resolution:=optional`. New runtime dependencies end up inside the jar automatically. Check for OSGi class loading issues when adding them.

## Architecture

All code is under `org.cy3sbml` (`src/main/java/org/cy3sbml/`).

- **`CyActivator`**: OSGi bundle activator and the single wiring point. It fetches Cytoscape services, builds all managers, actions and task factories, and registers them as OSGi services (`CyAction`, listeners, `InputStreamTaskFactory`). Add new UI actions or listeners here.
- **Import pipeline**: `SBMLFileFilter` + `SBMLReaderTaskFactory` create `SBMLReaderTask`, the core converter (~2200 lines). It reads an `SBMLDocument` with JSBML and turns SBML objects into Cytoscape nodes and edges, with one `read*` method per package (`readCore`, `readQual`, `readFBC`, `readComp`, `readGroups`, `readLayouts`). Math (`ASTNode`) and unit definitions become subgraphs. One root network gets three subnetworks: `All__<name>`, `Kinetic__<name>`, and a base network (node/edge type sets in `SBML`). The `archive` package does the same for COMBINE/OMEX archives.
- **`SBML`**: constants for node types, edge/interaction types, attribute (column) names and network prefixes. Use these constants, not string literals.
- **`SBMLManager`** (singleton): maps `CyNetwork` SUIDs to their `SBMLDocument` through `mapping.Network2SBMLMapper` and `One2ManyMapping` (SBase id/metaId to node SUIDs). Go through `SBMLManager` for all access to an SBML document from a network. Networks and mappings are persisted across Cytoscape sessions by `SessionData`.
- **GUI** (`gui`): `WebViewPanel` is a JavaFX WebView cytopanel. It listens to selection/network events and renders HTML for the selected SBase via `SBaseHTMLFactory`/`SBaseHTMLThread`, with templates from `src/main/resources/gui`. `BrowserHyperlinkListener` routes link clicks (actions, external URLs). `resources/extension` contains a bundled JavaScript extension jar built from `extension/`.
- **Annotation resolution**: `miriam` (MIRIAM/identifiers.org registry from bundled resources), and `ols`, `chebi`, `uniprot` web clients with local caches. `ConnectionProxy` applies Cytoscape proxy settings.
- **Other**: `styles` (`StyleManager`, `StyleFactory`) creates and applies the cy3sbml visual styles from `resources/styles`. `layout` saves and loads node positions (XML). `cofactors` splits cofactor nodes. `biomodel` holds the BioModels search/import dialog. `util` holds shared helpers (`SBMLUtil`, `AttributeUtil`, `NetworkUtil`, `ASTNodeUtil`, ...).
- **`ServiceAdapter`**: bundles the Cytoscape services that actions and tasks need, so they do not each take long constructor lists.

`tools/pycysbml` is a separate Python (uv) helper package for downloading and preparing test models. It is not part of the app build.

## Release

See `docs/release.md`. Release notes go in `release-notes/`. The version lives in `pom.xml`.
