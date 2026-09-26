# Repository modernization - design

Date: 2026-09-26
Status: approved in brainstorming, pending review of this document

## Goal

Bring cy3sbml to the same standard as the other maintained repositories
(sbmlutils, pymetadata, pkpdutils, pkdb): protected branches with required
checks, current CI, reproducible builds from current dependencies, enforced
formatting and static analysis, a maintainable code structure and a published
documentation site built with zensical.

Success criteria:

- every change reaches `develop` through a pull request with the required checks
  `tests`, `format`, `lint` and `docs`, with no bypass
- `mvn -B verify` builds without SNAPSHOT dependencies and without warnings
  (`-Xlint:all -Werror`, Error Prone)
- the imported networks of about 30 reference models are unchanged by the
  refactor (golden tests), except for deliberate, documented bug fixes
- the app loads and works in Cytoscape 3.10.4 (manual end-to-end check)
- the documentation is published at `https://matthiaskoenig.github.io/cy3sbml/`

## Constraints

- Target runtime is Cytoscape 3.10.4 on Java 17. The code compiles with
  `--release 17`.
- The newest published Cytoscape API is 3.10.0 (3.10.1 to 3.10.4 published no
  new API artifacts, 3.11.0 is unpublished). The API stays on 3.10.0 until 3.11
  is published.
- The git history is not rewritten.
- Outward-facing GitHub operations (branch rename, branch deletion, applying
  rulesets and repository settings, enabling GitHub Pages) are confirmed with
  the maintainer before they run.

## Delivery

One design, delivered as four sequential pull requests into `develop`. Each
pull request passes its checks and is merged before the next one starts.

1. Repository policy and CI
2. Build, dependencies and quality tooling
3. Code refactor
4. Documentation site

The documentation comes last so that its screenshots show the final code.

## 1. Repository policy and CI

### Workflows

- `.github/workflows/ci.yml`
  - job `test`: matrix Ubuntu and Windows, Temurin JDK 17, Maven cache through
    `actions/setup-java`, runs `mvn -B verify`, publishes the surefire report
  - job `tests`: aggregates the result of the `test` matrix so the name of the
    required check stays stable when the matrix changes (as in sbmlutils)
  - triggers: push to `develop` and `main`, pull requests into them,
    `workflow_dispatch`
  - concurrency group per pull request or ref, cancelling outdated pull request
    runs
  - current versions of all actions, `persist-credentials: false` on checkout,
    `permissions: contents: read`
- `.github/workflows/release.yml`, triggered by a `v*` tag:
  - builds the jar
  - creates the GitHub release with the notes from
    `release-notes/<version>.md`, the jar and its md5/sha1 checksums (replaces
    `tools/checksums.sh`)
  - fast-forwards `main` to the tagged commit
  - the upload to the Cytoscape App Store stays manual, there is no API for it

### Repository policy

- `.github/rulesets/develop.json`, `main.json`, `tags.json` and `apply.sh`,
  following sbmlutils:
  - no bypass actors
  - `develop`: pull request required (0 approvals, stale reviews dismissed,
    conversation resolution required, squash or rebase merges only), linear
    history, no deletion, no force push, required status checks
  - `main`: linear history, no deletion, no force push
  - `tags` (all tags): no deletion, no force push
  - required checks grow with the pull requests: `tests` after PR 1,
    `format` and `lint` after PR 2, `docs` after PR 4. `apply.sh` is re-run
    after each of these merges.
- `apply.sh` also sets the repository settings: auto-merge allowed, branches
  deleted on merge, update branch allowed, squash and rebase merges allowed,
  merge commits disabled.
- `.github/CODEOWNERS` (`* @matthiaskoenig`)
- `.github/pull_request_template.md` with a checklist adapted to Maven (targets
  `develop`, tests added, `mvn verify` passes, `mvn spotless:apply` run, release
  notes and docs updated)
- `.github/dependabot.yml`: weekly grouped updates for `github-actions` and
  `maven`

### Branches

- rename `master` to `main`; `main` tracks the latest release
- delete `bugfixes`, `hek-tests`, `review-changes` and the stale dependabot
  branch after checking that each one is merged or obsolete

### Housekeeping

- `.gitignore`: add `.vscode/`, `.factorypath`, `site/`
- remove `mvn_build.sh`

## 2. Build, dependencies and quality tooling

### Build

- `maven.compiler.release=17` replaces `source`/`target` 15; the unused
  `maven-compiler-plugin.version` property is removed
- all Maven plugins on current versions; the stale pins of
  `maven-install-plugin` 2.5.2 and `maven-resources-plugin` 3.0.2 are removed
- `maven-enforcer-plugin`: Java 17 or newer, Maven 3.9 or newer, no SNAPSHOT
  dependencies, no duplicate classes
- repositories reduced to Maven Central, the NRNB release and third-party
  repositories and the in-project repository `lib/cy3sbml-dep`

### Dependencies

- JSBML (core and the packages qual, layout, comp, fbc, groups, distrib, tidy)
  is built from one exact JSBML commit and installed into `lib/cy3sbml-dep`
  under a non-SNAPSHOT version of the form `1.7-<yyyymmdd>-<shortsha>`.
  `lib/build_jsbml_jars.sh` takes the commit as input and derives the version.
  Stale files in `lib/` are removed, including `lib/fastjson2-2-0-57.jar`.
- `ols-client` 2.14-SNAPSHOT is replaced by `org.cy3sbml.ols.OlsClient`, a
  small client for the OLS4 REST API based on `java.net.http` and jackson. It
  supports the term lookups the app needs (by IRI or CURIE) and keeps the
  existing cache behavior. Offline tests use recorded JSON fixtures; one test
  tagged `network` checks the live API.
- removed dependencies: unirest, httpclient, httpmime, httpasyncclient,
  xstream, controlsfx, jigsaw, staxmate, org.json, fastjson2. JSON is handled by
  jackson only. The ehcache 2.x cache is replaced by a small file-backed cache
  unless other code than the OLS cache depends on ehcache features.
- test stack: JUnit 6 GA through the JUnit BOM, current Mockito
- every test that needs the network is tagged `@Tag("network")`. Surefire
  excludes that tag by default. `-Dgroups=network` runs them, and the profile
  `models` runs the long model suites (SBML Test Suite, BioModels, BiGG).
- verification: bundle jar size and the manifest `Import-Package` are compared
  before and after, and the jar is loaded in Cytoscape 3.10.4 to check for OSGi
  class loading errors

### Quality tooling

- Spotless with palantir-java-format. The whole code base is reformatted in one
  separate commit whose SHA is listed in `.git-blame-ignore-revs`. CI job
  `format` runs `mvn -B spotless:check`.
- Error Prone plus `javac -Xlint:all -Werror`. Existing warnings are fixed;
  unavoidable ones are suppressed at the narrowest scope with a reason. CI job
  `lint` compiles with both enabled. If the current Error Prone release needs a
  newer JDK to run, the `lint` job runs on that JDK while still compiling with
  `--release 17`; the `test` matrix stays on JDK 17.
- jacoco report uploaded as a CI artifact, no coverage threshold
- the pre-commit hook documented for developers runs `mvn spotless:apply`
  instead of the IntelliJ formatter
- the `develop` ruleset gains the required checks `format` and `lint`

## 3. Code refactor

### Safety net

Before any structural change, a separate commit adds golden tests on the
unchanged code:

- about 30 models from `src/test/resources` covering SBML core L1 to L3, qual,
  fbc v1 and v2, comp (including flattening), groups, layout, distrib and one
  COMBINE archive
- each model is imported through `SBMLReaderTask` using `NetworkTestSupport`
  and `GroupTestSupport` from the Cytoscape impl test artifacts
- the result is written as canonical JSON to `src/test/resources/golden/`:
  nodes and edges with all attributes, for each of the three subnetworks,
  sorted, with SUIDs replaced by stable keys
- the tests compare the import against these snapshots. A deliberate behavior
  change updates the snapshot in the same commit and names the change in the
  commit message.

### Reader split

`SBMLReaderTask` (2220 lines) is split into the package `org.cy3sbml.reader`:

- `SBMLReaderTask`: Cytoscape task concerns only (parsing, validation,
  progress, cancellation, network views), under 300 lines
- `ConversionContext`: the network under construction, the `id2Node` and
  `metaId2Node` maps, groups, base unit definitions, and the helpers
  `createNode`, `createEdge`, `createGroup`
- `PackageReader` interface (`read(ConversionContext, Model)`) with the
  implementations `CoreReader`, `QualReader`, `FbcReader` (with
  `CobraNotesParser`), `CompReader` (with the flattened model), `GroupsReader`
  and `LayoutReader`. `CoreReader` has one method per SBML element type.
- `MathGraphBuilder` and `UnitGraphBuilder` for the math and unit subgraphs
- `AttributeWriter` for the `set*Attributes` methods
- `SubnetworkBuilder` for the All, Kinetic and base subnetworks

### Singletons

`SBMLManager`, `ServiceAdapter`, `WebViewPanel`, `StyleManager`,
`CofactorManager` and `BiomodelsDialog` are no longer singletons. `CyActivator`
creates each one once and passes it through constructors. `ServiceAdapter`
remains as a plain holder of Cytoscape services created by `CyActivator`; no
accessor returns null. The static state in `OLSCache` and
`SBaseHTMLFactory.baseDir` moves into instance fields of injected objects.

### Hygiene

- remove the duplicate `BundleInformation`, `BrowserSample` and `Browser`, the
  scratch `main()` methods, commented-out code and `oven/MemoryLeak`
- replace `printStackTrace` and `System.out`/`System.err` with slf4j logging
  that includes context
- catch specific exception types instead of `Exception`/`Throwable`, except at
  the outer boundary of a task or thread where the error is logged and reported
- use try-with-resources for all streams; make the raw `Map` in
  `MappingDiscrete` generic
- review every TODO/FIXME: fix it, move it to a GitHub issue and remove the
  comment, or delete it as obsolete
- `WebViewPanel` renders on a single-thread executor; a new selection cancels
  the pending render
- use Java 17 idioms (lambdas, switch expressions, pattern matching for
  `instanceof`, records for value types) where they make the code clearer
- `SBaseHTMLFactory` gets the hygiene pass only, no redesign; its output is
  checked visually during the end-to-end check

### Tests

New unit tests for `util` (`SBMLUtil`, `AttributeUtil`, `XMLUtil`,
`NetworkUtil`), `archive`, `cofactors`, `layout` (save and load round trip) and
each `PackageReader` on a small model.

### End-to-end check

Load the jar into Cytoscape 3.10.4 and check: import of models for each
supported package, the three subnetworks, styles, the info panel with
annotations, BioModels search and import, layout save and load, cofactor
splitting, and session save and restore. UI defects found on the way are fixed.

## 4. Documentation site

### Setup

- `zensical.toml` at the repository root with the theme configuration of
  sbmlutils (teal palette, light, dark and system toggle, navigation and search
  features, markdown extensions), logo and favicon from `docs/images`,
  `edit_uri = "edit/develop/docs/"`, social links to GitHub and the Cytoscape
  App Store
- `docs/requirements.txt` pins zensical; the site builds with
  `uvx --with-requirements docs/requirements.txt zensical build --clean`
- `scripts/llms_txt.py` (adapted from sbmlutils) writes `llms.txt`,
  `llms-full.txt` and the markdown of every page
- `scripts/release_notes.py` generates `docs/release-notes.md` from
  `release-notes/*.md` (newest first) during the build; the generated file is
  gitignored
- `docs/superpowers/` is excluded from the site

### Workflow

`.github/workflows/docs.yml` follows pkdb: job `docs` builds on pull requests
and on pushes to `develop` and `main`; job `deploy` publishes to GitHub Pages
from `develop` only. GitHub Pages is switched to "GitHub Actions" as source.
The `develop` ruleset gains the required check `docs`.

### Content

- Home: features and screenshot
- Installation: App Store, App Manager, manual jar installation
- User guide: importing SBML (file, URL, BioModels search, COMBINE archives),
  the network model (base, All and Kinetic subnetworks, node and edge types,
  attributes), the SBML info panel and annotations, validation, styles,
  layouts, cofactor nodes, supported SBML packages
- Development: building, architecture (with a diagram of the reader pipeline),
  testing (tags, golden tests, model suites, `tools/pycysbml`), code quality,
  contributing, release process and branch model
- Release notes, citation, license, funding

`docs/develop.md`, `docs/contributing.md` and `docs/release.md` are merged into
the new pages. `CLAUDE.md` is updated to the new structure.

### Screenshots

New screenshots are taken in Cytoscape 3.10.4 with the final jar and stored in
`docs/images/screenshots/`. The maintainer reviews them. If the GUI cannot be
driven from the development session, the maintainer takes them from a shot
list.

### Cleanup

- delete `docs/manuscript`, `docs/presentation`, `docs/publication`,
  `docs/specifications` and the icon source files (`.ai`, `.psd.zip`); the
  paper is cited by DOI and the specifications are linked at sbml.org
- shorten the README to badges (CI, docs, DOI, App Store), a short
  description, a screenshot, links to the documentation and the citation
- add `CITATION.cff` next to `.zenodo.json`

## Out of scope

- rewriting the git history to remove large blobs
- a redesign of the HTML generation in `SBaseHTMLFactory`
- moving to Java 21 or the unpublished Cytoscape 3.11 API
- automating the Cytoscape App Store upload
