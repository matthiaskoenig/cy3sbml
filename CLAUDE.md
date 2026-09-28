# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

cy3sbml is a Cytoscape 3 app (OSGi bundle) that imports SBML models into Cytoscape networks using JSBML. Supports all SBML levels/versions plus the `qual`, `comp`, `fbc` and `groups` packages; the `layout` package (#71) and COMBINE archives (#116) are not supported yet. Main branch is `develop`.

## Build and test

Requires JDK 17. JavaFX (the GUI uses JavaFX `WebView`) comes from Maven Central as `provided` dependencies with the platform-specific jars, so no system JavaFX is needed to build and test; no test starts the JavaFX runtime, and Cytoscape ships JavaFX at runtime. Use the bundled Maven Wrapper (`./mvnw`, `mvnw.cmd` on Windows) instead of a system Maven install; it downloads the pinned Maven version on first use. CI (`.github/workflows/ci.yml`) builds on Ubuntu and Windows with Temurin 17.

```bash
./mvnw clean install -DskipTests          # build app jar: target/cy3sbml-<version>.jar
./mvnw test                               # fast tests (network and models tests excluded)
./mvnw test -Dtest=IOUtilTest             # single test class
./mvnw test -Dtest=IOUtilTest#testName    # single test method
./mvnw test -Pall-tests                   # all tests incl. network and model suites (slow, needs network)
./mvnw test -Dtest.groups=network -Dtest.excludedGroups=  # only the network tests
```

- Tests are JUnit 6 + Mockito. Test classes run in parallel (`src/test/resources/junit-platform.properties`); a test class that changes global state (system properties) needs `@Isolated`. `JsbmlSetupListener` sets up JSBML once before the tests, as `CyActivator` does via `JsbmlSetup` (JSBML's `ParserManager` singleton is not thread-safe).
- Tests are selected via JUnit tags (`org.junit.jupiter.api.Tag`), controlled by the surefire `<groups>`/`<excludedGroups>` in `pom.xml`, bound to the `test.groups`/`test.excludedGroups` properties. By default `test.excludedGroups` is `network,models`, so tests tagged `network` (hit web services, e.g. `ChebiAccessTest`, `OlsClientTest`, `BioModelInterfaceTest`) and `models` (the long-running `SBMLTestSuiteTest`, `BioModelsTest`, `BiGGTest` suites in `src/test/java/org/cy3sbml/models/`) are skipped. The `all-tests` profile clears `test.excludedGroups` to run everything. Test models live in `src/test/resources/models/`; the large corpora of the `models` suites (BiGG, BioModels, SBML test suite, 1.2 GB) live in `src/test/corpora/models/`, which surefire puts on the test classpath without copying.
- `GoldenModelsTest` (`src/test/java/org/cy3sbml/golden/`) pins the networks `SBMLReaderTask` creates for a set of reference models against a JSON snapshot per model in `src/test/resources/golden/`. After an intended change to the import, regenerate them with `./mvnw -B -q test -Dtest=GoldenModelsTest -Dgolden.update=true` and review the diff before committing.
- Test logging: JSBML logs through the log4j 1.x API, which `log4j-over-slf4j` routes to slf4j, so logback configures all logging (app: `src/main/resources/logback.xml`, tests: `src/test/resources/logback-test.xml`). `logback-test.xml` raises the loggers that warn on the deliberate test inputs to ERROR and drops the errors the tests cause on purpose (`ExpectedMessageFilter`), so `./mvnw -B -q verify` prints no warnings; a new warning in the test output needs a look.
- Java formatting is enforced by Spotless (`palantir-java-format`); run `./mvnw -q spotless:apply` and see the pre-commit hook in `docs/development/quality.md`.
- The `lint` profile compiles with Error Prone and `-Xlint:all,-processing,-serial -Werror`. Error Prone needs JDK 21 or newer to run, so point `JAVA_HOME` at a JDK 21+ install for it: `JAVA_HOME=<jdk21> ./mvnw -B -Plint clean verify` (the code still compiles and runs on the pinned JDK 17 otherwise).

### Running in Cytoscape

Symlink the built jar into Cytoscape's apps folder. Cytoscape hot-reloads the app after each `./mvnw install -DskipTests`:

```bash
ln -s $PWD/target/cy3sbml-<version>.jar $HOME/CytoscapeConfiguration/3/apps/installed/cy3sbml-latest.jar
```

Debug by launching `cytoscape.sh debug` and attaching a remote JVM debugger to port 12345. The app writes its log to `~/CytoscapeConfiguration/cy3sbml/`.

## Dependencies

- JSBML (core and its extension modules: `qual`, `layout`, `comp`, `fbc`, `groups`, `distrib`, `tidy`) is not taken from Maven Central. The jars are built from one JSBML commit with JSBML's Ant build, without JSBML's test classes, and stored in `lib/cy3sbml-dep`, an in-project Maven repository declared in `pom.xml`. The `jsbml.version` property (`1.7-<commit-date>-<short-sha>`) pins the commit, `jsbml.osgi.version` is the same version as an OSGi version (used for the exported `org.sbml.jsbml.*` packages). Update both the jars and the properties only with `scripts/update_jsbml.py [<jsbml-ref>]` (default: JSBML `master`) or the manual `update-jsbml` GitHub workflow, which runs it and opens a pull request; never edit `lib/cy3sbml-dep` by hand. See `docs/development/dependencies.md`.
- Cytoscape API artifacts come from the NRNB Nexus repositories (`cytoscape_releases`, `cytoscape_thirdparty`) and have `provided` scope. Repositories are Maven Central (the default), those two NRNB repositories and the in-project `lib/cy3sbml-dep`; there is no EBI repository, and no SNAPSHOT dependency.
- `maven-bundle-plugin` embeds all non-provided, non-test dependencies (transitively) into the bundle jar and marks imports `resolution:=optional`. New runtime dependencies end up inside the jar automatically. Check for OSGi class loading issues when adding them. The `jsbml`/`jsbml-*` jars are the exception: they are inlined (`inline=org/**` in `Embed-Dependency`, so their `META-INF` is left out) and `org.sbml.jsbml.*` is exported with `-exportcontents` and the version `jsbml.osgi.version`, since JSBML types are part of the `SBMLManager` service API. Everything else, `jtidy` included, is embedded as a nested jar. `BundleJarContentIT` checks the packaged jar (no JUnit classes or imports, the JSBML export).

## Architecture

All code is under `org.cy3sbml` (`src/main/java/org/cy3sbml/`).

- **No singletons**: `CyActivator` is the OSGi bundle activator and the single wiring point. It fetches Cytoscape services, constructs every manager/reader/client and injects them into each other and into the actions and task factories, and registers the ones other apps may need as OSGi services (`SBMLManager`, `CyAction`, listeners, `InputStreamTaskFactory`). Add new UI actions or listeners here.
- **Import pipeline** (`reader`): `SBMLFileFilter` + `SBMLReaderTaskFactory` create `SBMLReaderTask`, which reads an `SBMLDocument` with JSBML and turns it into Cytoscape nodes and edges. It holds a `List<PackageReader>` (`CoreReader`, `QualReader`, `FbcReader`, `CompReader`, `GroupsReader`, `LayoutReader`), one per SBML package, run in order against a shared `ConversionContext` (the network plus the SBML id/metaId to node lookups for one model). `AttributeWriter` writes the common SBase/NamedSBase attributes; `MathGraphBuilder` and `UnitGraphBuilder` turn math (`ASTNode`) and unit definitions into subgraphs. One root network gets three subnetworks: `All__<name>`, `Kinetic__<name>`, and a base network (node/edge type sets in `SBML`, built by `SubnetworkBuilder`). The `archive` package holds a COMBINE/OMEX archive reader that does not read the archive content yet; `CyActivator` does not register it (#116).
- **`SBML`**: constants for node types, edge/interaction types, attribute (column) names and network prefixes. Use these constants, not string literals.
- **`SBMLManager`**: maps `CyNetwork` SUIDs to their `SBMLDocument` through `mapping.Network2SBMLMapper` and `One2ManyMapping` (SBase id/metaId to node SUIDs). Go through `SBMLManager` for all access to an SBML document from a network. `CyActivator` creates the one instance and registers it as an OSGi service so other apps can look it up. Networks and mappings are persisted across Cytoscape sessions by `SessionData`.
- **GUI** (`gui`): `WebViewPanel` is a JavaFX WebView cytopanel. It listens to selection/network events and, via `PanelUpdater`, renders HTML for the selected SBase using `SBaseHTMLFactory`/`SBaseHTMLThread`, with templates from `src/main/resources/gui`. `WebViewPanel.updateInformation` resolves the render target (`PanelUpdater.resolveTarget`: the current `SBMLDocument`, the selected node's `SBase`, or a fixed message) and submits it as the key to `LatestTaskExecutor.submit(key, task)`, which coalesces a resubmission of the target it is already rendering (rather than cancelling and restarting it) but still cancels and replaces a render for a different target; several Cytoscape events fired while loading one model can resolve to the same target, so it renders once. Every render publishes its page through `LatestTaskExecutor.publishIfCurrent`, which drops the page of a render that was superseded (checked under the submit lock), and the accepted pages (rendered HTML, help, examples) reach the `Browser` through `PageLoader` in that order, so the page accepted last is shown; `PageLoader` also holds the latest page until the browser exists. `BrowserHyperlinkListener` routes link clicks (actions, external URLs) and runs their action on the Swing event dispatch thread. `resources/extension` contains a bundled JavaScript extension jar built from `extension/`.
- **Annotation resolution**: `miriam.MiriamRegistry` holds the MIRIAM/identifiers.org registry, seeded from a bundled resource and refreshed from `https://registry.api.identifiers.org/` in the background. The `ols`, `chebi` and `uniprot` web clients share `util.HttpJson` (an HTTP/1.1 JSON client; HTTP/2 to `ebi.ac.uk` is unreliable) and `cache.MemoryCache`. `HttpJson` distinguishes a deterministic failure for a URI (a 404 or other 4xx status other than 407/408/429) from a transient one (a timeout, connection failure, an empty or malformed body, or a 5xx/407/408/429 status); each client additionally treats a well-formed but incomplete response (missing the fields it needs) as deterministic for that identifier. `MemoryCache` caches a found result until evicted and a deterministic "not found" result for a short TTL, while never caching a transient one, and loads each key once (concurrent lookups of a key share one request), so an outage (or a captive portal/proxy answering with HTML instead of JSON) recovers on the next lookup. `ConnectionProxy` applies Cytoscape proxy settings.
- **Other**: `styles` (`StyleManager`, `StyleFactory`) creates and applies the cy3sbml visual styles from `resources/styles`. `layout` saves and loads node positions (XML). `cofactors` splits cofactor nodes. `biomodel` holds the BioModels search/import dialog. `util` holds shared helpers (`SBMLUtil`, `AttributeUtil`, `NetworkUtil`, `ASTNodeUtil`, ...).
- **`ServiceAdapter`**: bundles the Cytoscape services that actions and tasks need, so they do not each take long constructor lists.

`tools/pycysbml` is a separate Python (uv) helper package for downloading and preparing test models. It is not part of the app build. The Python code (`scripts/`, `tools/pycysbml`) is checked with ruff and ty from that project: `uv run --project tools ruff check`, `uv run --project tools ruff format --check`, `uv run --project tools ty check` (config in `ruff.toml`, `ty.toml`; the `python` CI job).

## Documentation

The documentation site is built with [zensical](https://zensical.org) from Markdown in `docs/` (config in `zensical.toml`), and published to <https://matthiaskoenig.github.io/cy3sbml/> by `.github/workflows/docs.yml` on every push to `develop`. To build it locally:

```bash
uv run --no-project --python 3.14 python scripts/release_notes.py   # docs/release-notes.md from release-notes/*.md, gitignored
uvx --python 3.14 --with-requirements docs/requirements.txt zensical build --clean
uv run --no-project --python 3.14 python scripts/llms_txt.py        # llms.txt, llms-full.txt and per-page markdown in site/
```

The build must stay warning-free. Do not hand-edit `docs/release-notes.md` or anything under `site/`, both are generated.

## Release

See `docs/development/release.md` for the full process. In short: set the version in `pom.xml` and add `release-notes/<version>.md` in a pull request to `develop`, then tag the merged commit (`v<version>`); `.github/workflows/release.yml` builds and tests the tag, creates the GitHub release with the jar and checksums, and fast-forwards `main` to it. Branch and tag protection (`develop`, `main`, tags) are GitHub rulesets under `.github/rulesets/`, applied with `.github/rulesets/apply.sh`.
