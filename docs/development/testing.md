# Testing

The tests use JUnit 6 (Jupiter) and Mockito. They are in `src/test/java`, the test models in
`src/test/resources/models`. The large model corpora of the `models` tests (BiGG, BioModels
and the SBML test suite, about 1.2 GB) are in `src/test/corpora/models`. Maven does not copy
them to `target/test-classes`; surefire adds `src/test/corpora` to the test classpath, so
they are read as the classpath resources `/models/bigg_models/...` and so on.

The test classes run in parallel, the tests of one class on one thread
(`src/test/resources/junit-platform.properties`). A test class that changes global state,
such as system properties, is annotated with `@Isolated`. `JsbmlSetupListener` sets up
JSBML once before the tests, as `CyActivator` does with `JsbmlSetup`, because JSBML's
`ParserManager` singleton is not thread-safe.

## Run the tests

```bash
./mvnw -B -q test                          # fast tests
./mvnw -B -q test -Dtest=IOUtilTest        # one test class
./mvnw -B -q test -Dtest=IOUtilTest#name   # one test method
./mvnw -B -q verify                        # fast tests and the integration test
```

## Test tags

Two groups of tests are excluded by default with JUnit tags:

| Tag | Tests | Why excluded |
|---|---|---|
| `network` | tests that call web services: OLS, UniProt, ChEBI, the identifiers.org registry, BioModels | need network access, and fail when a service is down |
| `models` | `SBMLTestSuiteTest`, `BioModelsTest`, `BiGGTest`: the import of the SBML Test Suite, of the curated BioModels and of the BiGG models; `CompFlatteningSuiteTest`: the flattening of the comp cases of the SBML Test Suite | take a long time |

Select the tags with the properties `test.groups` and `test.excludedGroups`:

```bash
./mvnw -B -q test -Pall-tests                                   # all tests
./mvnw -B -q test -Dtest.groups=network -Dtest.excludedGroups=  # only the network tests
./mvnw -B -q test -Dtest.groups=models -Dtest.excludedGroups=   # only the model suites
```

Add `@Tag("network")` to every new test that needs the network. The web service clients
have unit tests with recorded responses in `src/test/resources`, which run without the
network.

## Golden snapshot tests

`GoldenModelsTest` (with `NetworkSnapshot`) pins the result of the import. For a list of
reference models (for example the unit test models, models with `comp`, `fbc`, `qual`,
`layout` and `distrib`, models from BioModels, the SBML Test Suite and BiGG, and two
COMBINE archives, read with the archive reader), it imports the model and compares the
networks, nodes, edges, groups and table values with a JSON snapshot per model in
`src/test/resources/golden/`. The columns `SUID` and `selected` and the SUID reference
columns are left out.

If you change the import on purpose, regenerate the snapshots and review the diff:

```bash
./mvnw -B -q test -Dtest=GoldenModelsTest -Dgolden.update=true
git diff src/test/resources/golden
```

Commit the changed snapshots with the code change.

The tests read a model with its location, as Cytoscape does for a file, so the snapshots
also pin the networks of external model definitions, of the flattened comp model and of
the layouts.

## Flattening of comp models

The flattened comp model comes from JSBML's `CompFlatteningConverter`. The tests compare
it with the flattening of libSBML, the reference implementation: the ids of the elements
per type and the ids each element references (math, species of reactions, compartment of
species). `tools/pycysbml/comp_flat_reference.py` writes the libSBML result:

- `src/test/resources/models/comp/comp-flat-reference.json`, checked by
  `CompFlatteningReferenceTest`,
- `src/test/corpora/models/sbml-test-suite/comp-flat-reference.json` for the 123 comp
  cases of the SBML Test Suite, checked by `CompFlatteningSuiteTest` (`models` suite).

```bash
uv run --project tools python tools/pycysbml/comp_flat_reference.py \
    src/test/corpora/models/sbml-test-suite/semantic \
    src/test/corpora/models/sbml-test-suite/comp-flat-reference.json --pattern '*-sbml-l3v1.xml'
uv run --project tools python tools/pycysbml/comp_flat_reference.py \
    src/test/resources/models/comp src/test/resources/models/comp/comp-flat-reference.json
```

## Test models

The test models are in `src/test/resources/models`, grouped by source or package (`comp`,
`distrib`, `fbc`, `layout`, `omex`, `qual`, `unittests`, ...). The Python helpers in
`tools/pycysbml` download or write some of them; run them with
`uv run --project tools python tools/pycysbml/<script>.py`, the arguments are in the
docstring of every script:

| Script | Writes |
|---|---|
| `bigg_download.py` | the BiGG models of `BiGGTest` in `src/test/corpora/models/bigg_models` |
| `biomodels_download.py` | the curated BioModels of `BioModelsTest` in `src/test/corpora/models/biomodels` |
| `comp_flat_reference.py` | the libSBML flattening of the comp models, see [Flattening of comp models](#flattening-of-comp-models) |
| `distrib_models.py` | the distrib models in `models/distrib`, written and validated with libSBML |
| `omex_models.py` | the COMBINE archives in `models/omex`, written with pymetadata |
| `graph_to_sbml.py` | `models/styles/graph.xml`, the test model of the visual styles, from Antimony |

## Test logging

JSBML logs through the log4j 1.x API, which `log4j-over-slf4j` routes to slf4j, so
logback configures all logging: `src/main/resources/logback.xml` for the app,
`src/test/resources/logback-test.xml` for the tests. `logback-test.xml` raises the
loggers that warn on the deliberate test inputs to `ERROR`, and `ExpectedMessageFilter`
drops the errors the tests cause on purpose. So `./mvnw -B -q verify` prints no
warnings, and a new warning in the test output needs a look.

## Packaged jar test

`BundleJarContentIT` is an integration test that runs with the Maven Failsafe plugin in
the `verify` phase, after the jar is built. It opens `target/cy3sbml-<version>.jar`
itself and checks that the jar contains the resources the app needs at runtime: the GUI
templates and images, the JavaScript extension jar, the styles, the bundled MIRIAM
registry, and the JSBML classes and resources, for example `org/sbml/jsbml/SBO.class`. It
also checks the manifest: the activator, the export of `org.sbml.jsbml.*` with the JSBML
version, and no JUnit classes or imports. A unit test cannot find such a packaging error,
because `target/classes` still has all files.

## Continuous integration

The workflow `.github/workflows/ci.yml` runs on pull requests and on pushes to `develop`
and `main`:

- `test`: `./mvnw verify` on Ubuntu and Windows with Temurin 17. It publishes the test
  report and, on Ubuntu, the JaCoCo coverage report. The check `tests` sums up the
  result of both systems.
- `format`, `lint` and `python`, see [Code quality](quality.md).

The workflow `.github/workflows/docs.yml` builds this documentation (check `docs`) and
publishes it from `develop`.

## Test in Cytoscape

The tests do not run Cytoscape. Check changes of the panel, the actions or the import in
Cytoscape, see [Building](building.md#run-in-cytoscape).
