# Repository Modernization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Bring cy3sbml to the standard of the other maintained repositories: protected branches, current CI, reproducible build on current dependencies, enforced formatting and static analysis, a modular SBML import and a zensical documentation site.

**Architecture:** Four sequential pull requests into `develop` (policy and CI, build and tooling, refactor, docs). The refactor is guarded by golden snapshot tests written against the unchanged code. The web clients (OLS, UniProt, ChEBI) move to `java.net.http` + jackson with a shared in-memory cache.

**Tech Stack:** Java 17, Maven 3.9, OSGi (maven-bundle-plugin), Cytoscape API 3.10.0, JSBML 1.7 (pinned build), JavaFX 17 WebView, JUnit 6, Mockito, Spotless + palantir-java-format, Error Prone, jacoco, GitHub Actions, zensical, uv.

**Spec:** `superpowers/specs/2026-09-26-repository-modernization-design.md`

## Global Constraints

- Compile with `maven.compiler.release=17`; runtime is Cytoscape 3.10.4 on Java 17.
- Cytoscape API version stays `3.10.0` (newest published).
- No SNAPSHOT dependencies after PR 2; repositories: Maven Central, NRNB releases and thirdparty, `lib/cy3sbml-dep`.
- Required checks on `develop`: `tests` (PR 1), `format` and `lint` (PR 2), `docs` (PR 4).
- No git history rewrite. Commit messages are plain prose, without an agent co-author line (user rule).
- Never use the em dash character in any written file.
- Do not edit auto-generated files; `docs/release-notes.md` is generated and gitignored.
- Tests that need the network carry `@Tag("network")` and are excluded by default.
- Use the constants in `org.cy3sbml.SBML`, not string literals, for types and attributes.

## Deviations from the spec found while planning

- The spec replaces ehcache "unless other code depends on it". ChEBI, OLS and UniProt caches all use ehcache 2.x only as an in-memory, session-lifetime map. All three move to one shared `org.cy3sbml.cache.MemoryCache`.
- `uniprot/UniprotAccess` uses the retired UniProt JAPI (`uk.ac.ebi.kraken`, `japi` 1.3.6). It moves to the UniProt REST API (`https://rest.uniprot.org/uniprotkb/{accession}.json`) with the same pattern as the OLS client, and `japi` is removed.
- `chebi/ChebiAccessTest.getChebiHTML` fails on the baseline (`EOF reached while reading`): the ChEBI backend response changed. Fixed in Task 2.4.

## Review Focus

1. An SBML file that fails to parse or validate: the import must report an error through the task monitor and not leave half-built networks registered. Test in Task 3.2 (`readerReportsInvalidSbml`).
2. Annotation lookups while offline or when a service returns 404/500/malformed JSON: the info panel must show the "term not found" fallback, never throw into the UI thread. Tests in Tasks 2.3 and 2.4 (`returnsEmptyOnHttpError`, `returnsEmptyOnMalformedJson`).
3. Rapid selection changes in the network: only the latest selection must be rendered, older renders cancelled. Test in Task 3.6 (`latestSelectionWins`).
4. A model using several packages at once (comp + fbc + groups + layout): each package reader must run and the result must equal the golden snapshot. Covered by the golden models list in Task 3.1 (`e_coli_core` style fbc, `comp` flattening, groups, layout models).
5. Session save and restore: networks restored from a session must map to their SBML documents again. Test in Task 3.5 (`sessionRoundTripRestoresMapping`) plus the E2E check in Task 3.8.

---

## PR 1: Repository policy and CI (branch `repo-policy-ci`)

### Task 1.1: CI workflow, dependabot, CODEOWNERS, PR template, gitignore

**Files:**
- Replace: `.github/workflows/ci.yml`
- Create: `.github/dependabot.yml`, `.github/CODEOWNERS`, `.github/pull_request_template.md`
- Modify: `.gitignore`
- Delete: `mvn_build.sh`

- [ ] **Step 1: Write `.github/workflows/ci.yml`**

```yaml
name: CI

on:
  push:
    # feature branches are tested through their pull request
    branches: [develop, main]
  pull_request:
    branches: [develop, main]
  workflow_dispatch:

permissions:
  contents: read

concurrency:
  group: ${{ github.workflow }}-${{ github.event.pull_request.number || github.ref }}
  cancel-in-progress: true

jobs:
  test:
    runs-on: ${{ matrix.os }}
    timeout-minutes: 45
    strategy:
      fail-fast: false
      matrix:
        os: [ubuntu-latest, windows-latest]
    steps:
      - uses: actions/checkout@v5
        with:
          persist-credentials: false
      - name: Set up JDK 17
        uses: actions/setup-java@v5
        with:
          java-version: "17"
          distribution: temurin
          cache: maven
      - name: Build and test
        run: mvn --batch-mode --no-transfer-progress verify
      - name: Publish the test report
        if: success() || failure()
        uses: scacap/action-surefire-report@v1
        with:
          check_name: surefire (${{ matrix.os }})

  tests:
    # the check the branch protection requires; the names of the matrix jobs
    # change with the matrix, this name does not
    name: tests
    needs: test
    if: always()
    runs-on: ubuntu-latest
    timeout-minutes: 5
    steps:
      - name: Check the result of the test matrix
        env:
          RESULT: ${{ needs.test.result }}
        run: |
          echo "test matrix: $RESULT"
          test "$RESULT" = "success"
```

Before committing, resolve the current major versions: `gh-axi api repos/actions/checkout/releases/latest --jq .tag_name` (same for `actions/setup-java`, `scacap/action-surefire-report`) and use them.

- [ ] **Step 2: Write `.github/dependabot.yml`**

```yaml
# Keep the GitHub Actions and the Maven dependencies up to date.
# See https://docs.github.com/code-security/dependabot/dependabot-version-updates
version: 2
updates:
  - package-ecosystem: "github-actions"
    directory: "/"
    schedule:
      interval: "weekly"
    groups:
      github-actions:
        patterns:
          - "*"
    open-pull-requests-limit: 5
    commit-message:
      prefix: "ci"
  - package-ecosystem: "maven"
    directory: "/"
    schedule:
      interval: "weekly"
    groups:
      maven-plugins:
        patterns:
          - "org.apache.maven.plugins:*"
          - "*-maven-plugin"
      maven-dependencies:
        patterns:
          - "*"
    ignore:
      # built from source into lib/cy3sbml-dep, see lib/build_jsbml_jars.sh
      - dependency-name: "org.sbml.jsbml*"
    open-pull-requests-limit: 5
    commit-message:
      prefix: "deps"
```

- [ ] **Step 3: Write `.github/CODEOWNERS`** with the single line `* @matthiaskoenig`.

- [ ] **Step 4: Write `.github/pull_request_template.md`**

```markdown
## Summary

<!-- what does this change and why -->

## Checklist

- [ ] the pull request targets `develop`
- [ ] tests were added or updated for the change
- [ ] `mvn verify` passes locally
- [ ] `mvn spotless:apply` was run
- [ ] user visible changes are in `release-notes/` and, if needed, in `docs/`
```

- [ ] **Step 5: `.gitignore`** append `.vscode/`, `.factorypath`, `site/`, `docs/release-notes.md`. Delete `mvn_build.sh` (`git rm mvn_build.sh`).

- [ ] **Step 6: Verify** `mvn -B -q verify` locally still passes, except the known failing `ChebiAccessTest` (fixed in PR 2). To keep PR 1 green, tag the network tests now: add `@Tag("network")` on the classes `chebi/ChebiAccessTest`, `uniprot/UniprotAccessTest`, `ols/OLSAccessTest`, `ols/OLSClientTest`, `biomodel/BioModelInterfaceTest`, and the live URL test methods of `miriam/RegistryUtilTest`; add `<excludedGroups>network</excludedGroups>` to the surefire configuration in `pom.xml`. Also tag `chebi/ChebiCacheTest`, `uniprot/UniprotCacheTest`, `ols/OLSCacheTest` if they call the live services (check with `rg -n 'getTerm|getEntry|getChebi' src/test`).

- [ ] **Step 7: Commit** `git add -A .github .gitignore pom.xml src/test && git commit -m "Run CI on pull requests with a stable tests check and tag the network tests"`

### Task 1.2: Rulesets and apply script

**Files:**
- Create: `.github/rulesets/develop.json`, `main.json`, `tags.json`, `apply.sh`

- [ ] **Step 1:** Copy `main.json` and `tags.json` verbatim from `/home/mkoenig/git/sbmlutils/.github/rulesets/`.
- [ ] **Step 2:** Copy `develop.json` from sbmlutils and set `required_status_checks` to `[{"context": "tests"}]` only.
- [ ] **Step 3:** Copy `apply.sh` from sbmlutils, replace `sbmlutils` by `cy3sbml` in the comment and in `REPO="${1:-matthiaskoenig/cy3sbml}"`. `chmod +x`.
- [ ] **Step 4:** `bash -n .github/rulesets/apply.sh && python3 -m json.tool .github/rulesets/develop.json >/dev/null` (syntax check).
- [ ] **Step 5: Commit** `git commit -m "Protect develop, main and the tags with rulesets"`

### Task 1.3: Release workflow

**Files:**
- Create: `.github/workflows/release.yml`
- Delete: `tools/checksums.sh`
- Modify: `docs/release.md`

- [ ] **Step 1: Write `.github/workflows/release.yml`**

```yaml
name: release

on:
  push:
    tags: ["v*"]

permissions:
  contents: read

concurrency:
  group: release-${{ github.ref }}
  cancel-in-progress: false

jobs:
  release:
    runs-on: ubuntu-latest
    timeout-minutes: 30
    permissions:
      contents: write  # create the GitHub release
    steps:
      - uses: actions/checkout@v5
        with:
          persist-credentials: false
      - uses: actions/setup-java@v5
        with:
          java-version: "17"
          distribution: temurin
          cache: maven
      - name: Build the app
        run: mvn --batch-mode --no-transfer-progress verify
      - name: Checksums
        run: |
          VERSION="${GITHUB_REF_NAME#v}"
          echo "NOTES=release-notes/${VERSION}.md" >> "$GITHUB_ENV"
          test -f "release-notes/${VERSION}.md"
          cd target
          JAR="cy3sbml-${VERSION}.jar"
          test -f "$JAR"
          md5sum "$JAR" > "$JAR.md5"
          sha1sum "$JAR" > "$JAR.sha1"
      - name: Create the GitHub release
        uses: softprops/action-gh-release@v2
        with:
          body_path: ${{ env.NOTES }}
          files: |
            target/cy3sbml-*.jar
            target/cy3sbml-*.jar.md5
            target/cy3sbml-*.jar.sha1

  sync-main:
    # main tracks the latest release
    needs: release
    runs-on: ubuntu-latest
    timeout-minutes: 5
    permissions:
      contents: write
    steps:
      - uses: actions/checkout@v5
        with:
          fetch-depth: 0
      - name: Fast-forward main to the release
        # no --force: the push fails if it is not a fast-forward
        run: git push origin HEAD:main
```

Release notes are named `release-notes/<version>.md` (`0.5.1.md`) while the tag is `v0.5.1`, hence the `NOTES` variable. Resolve action versions as in Task 1.1.

- [ ] **Step 2:** `git rm tools/checksums.sh`. Rewrite `docs/release.md` to: bump version in `pom.xml` via PR, write `release-notes/<version>.md`, merge, tag `v<version>` on `develop` and push the tag, the workflow creates the release and fast-forwards `main`, then upload the jar to the App Store manually, then open the next version PR.
- [ ] **Step 3:** `python3 -c "import yaml,sys;yaml.safe_load(open('.github/workflows/release.yml'))"`.
- [ ] **Step 4: Commit** `git commit -m "Release from tags and fast-forward main to the release"`

### Task 1.4: Open PR 1, merge, apply repository policy

- [ ] Push branch, open PR into `develop` with `gh-axi pr create`, wait for CI green (`gh-axi pr checks`), fix any failure.
- [ ] Merge with squash (`gh-axi pr merge --squash`).
- [ ] Branches: for each of `bugfixes`, `hek-tests`, `review-changes`, `dependabot/maven/ch.qos.logback-logback-core-1.5.25`: `git log --oneline origin/develop..origin/<b> | head` ; if empty or clearly superseded (the dependabot bump is superseded by PR 2), delete with `git push origin --delete <b>`. If a branch holds unmerged work, keep it and record it in the final report.
- [ ] Rename `master` to `main`: `gh-axi api -X POST repos/matthiaskoenig/cy3sbml/branches/master/rename -f new_name=main`. Then fast-forward `main` to the latest tag commit only if it is an ancestor; otherwise leave it.
- [ ] Run `.github/rulesets/apply.sh` (uses `gh`; the script is a checked-in tool, running it is allowed). Verify with `gh-axi api repos/matthiaskoenig/cy3sbml/rulesets`.

---

## PR 2: Build, dependencies and quality tooling (branch `build-tooling`)

### Task 2.1: pom modernization and enforcer

**Files:** Modify `pom.xml`

- [ ] **Step 1:** Replace the compiler `source`/`target` 15 with property `<maven.compiler.release>17</maven.compiler.release>`; remove property `maven-compiler-plugin.version`.
- [ ] **Step 2:** Update plugin versions to the latest releases (look up on Maven Central `https://repo1.maven.org/maven2/<group path>/<artifact>/maven-metadata.xml`): compiler, surefire, jar, resources, install, bundle, jacoco. Add `maven-enforcer-plugin` with rules `requireJavaVersion [17,)`, `requireMavenVersion [3.9,)`, `requireReleaseDeps` (fails on SNAPSHOT; activate it at the end of Task 2.2 once the SNAPSHOTs are gone), `banDuplicatePomDependencyVersions`, and `banDuplicateClasses` from `org.codehaus.mojo:extra-enforcer-rules` (scope: runtime dependencies; list known harmless duplicates explicitly with a reason).
- [ ] **Step 3:** Replace the three JUnit 6.0.0-M2 dependencies with the `org.junit:junit-bom` (latest 6.x GA) in `dependencyManagement` and versionless `junit-jupiter`. Mockito to latest 5.x.
- [ ] **Step 4:** Repositories: keep Central (implicit), NRNB `cytoscape_releases`, NRNB `cytoscape_thirdparty`, and `in-project`. Remove the rest.
- [ ] **Step 5:** `mvn -B -q clean verify` passes (network tests excluded). Compare `unzip -l target/cy3sbml-*.jar | tail -1` against the baseline value recorded before the change (write both numbers into the PR body).
- [ ] **Step 6: Commit** `git commit -m "Compile for Java 17 and update the Maven plugins and the test stack"`

### Task 2.2: Pin JSBML

**Files:** Modify `lib/build_jsbml_jars.sh`, `lib/cy3sbml-dep/**`, `pom.xml`; delete `lib/fastjson2-2-0-57.jar`

- [ ] **Step 1:** Read `lib/build_jsbml_jars.sh` fully. Change it to take the JSBML commit as argument (`JSBML_COMMIT="${1:?usage: build_jsbml_jars.sh <jsbml commit>}"`), check it out in `$JSBMLCODE`, and derive `JSBML_VERSION="1.7-$(git -C "$JSBMLCODE" show -s --format=%cd --date=format:%Y%m%d "$JSBML_COMMIT")-$(git -C "$JSBMLCODE" rev-parse --short=8 "$JSBML_COMMIT")"`. Install all jars with `mvn install:install-file -DlocalRepositoryPath=cy3sbml-dep -DgroupId=... -Dversion=$JSBML_VERSION` for core and every package; package jars get the same version string.
- [ ] **Step 2:** Clone JSBML (`git clone https://github.com/sbmlteam/jsbml.git $HOME/git/jsbml` if missing), use the current `master` head commit, run the script. Requires `ant`; if not installed, `sudo` is not available to agents: fall back to re-versioning the existing jars in `lib/cy3sbml-dep` via `install:install-file` with the commit that built them (find it from the jar manifest `unzip -p <jar> META-INF/MANIFEST.MF`), and document that in the script header.
- [ ] **Step 3:** Update all JSBML versions in `pom.xml` to the new version (one property `jsbml.version`); remove old SNAPSHOT directories from `lib/cy3sbml-dep`; `git rm lib/fastjson2-2-0-57.jar`.
- [ ] **Step 4:** `mvn -B -q clean verify` passes.
- [ ] **Step 5: Commit** `git commit -m "Pin JSBML to one commit under a release version"`

### Task 2.3: Shared cache and OLS REST client

**Files:**
- Create: `src/main/java/org/cy3sbml/cache/MemoryCache.java`, `src/main/java/org/cy3sbml/ols/OlsClient.java`, `src/main/java/org/cy3sbml/ols/OlsTerm.java`, `src/main/java/org/cy3sbml/util/HttpJson.java`
- Replace: `ols/OLSAccess.java`, `ols/OLSCache.java` (deleted; callers use `OlsClient`)
- Modify: `gui/SBaseHTMLFactory.java` (term rendering), `miriam/*` if it uses `IdentifiersConstants` from ols-client
- Test: `src/test/java/org/cy3sbml/cache/MemoryCacheTest.java`, `src/test/java/org/cy3sbml/ols/OlsClientTest.java`, fixtures `src/test/resources/ols/go_0042752.json`

**Interfaces:**
- Produces:
  - `final class MemoryCache<K, V>`: `MemoryCache(int maxEntries)`, `Optional<V> get(K key, Function<K, Optional<V>> loader)` (empty results are not cached), `void clear()`, `int size()`.
  - `final class HttpJson`: `HttpJson(HttpClient client, ObjectMapper mapper)`, `Optional<JsonNode> get(URI uri)`; empty on non-2xx, IO error, timeout (10 s) or malformed JSON; logs at warn.
  - `record OlsTerm(String iri, String label, String ontologyName, String oboId, List<String> synonyms, List<String> descriptions)`.
  - `final class OlsClient`: `OlsClient(HttpJson http)`, `Optional<OlsTerm> term(String identifier)` for `GO:0042752` (CURIE) and `GO_0042752` (short form); `static boolean isOlsResource(Resource resource)`.

- [ ] **Step 1: Write the failing tests**

```java
package org.cy3sbml.cache;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class MemoryCacheTest {
    @Test
    void loadsOnceAndCachesPresentValues() {
        var cache = new MemoryCache<String, String>(10);
        var calls = new AtomicInteger();
        assertEquals(Optional.of("a"), cache.get("k", k -> { calls.incrementAndGet(); return Optional.of("a"); }));
        assertEquals(Optional.of("a"), cache.get("k", k -> { calls.incrementAndGet(); return Optional.of("b"); }));
        assertEquals(1, calls.get());
    }

    @Test
    void doesNotCacheEmptyResults() {
        var cache = new MemoryCache<String, String>(10);
        assertTrue(cache.get("k", k -> Optional.empty()).isEmpty());
        assertEquals(Optional.of("x"), cache.get("k", k -> Optional.of("x")));
    }

    @Test
    void evictsLeastRecentlyUsed() {
        var cache = new MemoryCache<Integer, Integer>(2);
        cache.get(1, Optional::of);
        cache.get(2, Optional::of);
        cache.get(1, Optional::of);
        cache.get(3, Optional::of);
        assertEquals(2, cache.size());
        assertEquals(Optional.of(-2), cache.get(2, k -> Optional.of(-2)));
    }
}
```

```java
package org.cy3sbml.ols;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.Optional;
import org.cy3sbml.util.HttpJson;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class OlsClientTest {
    private static HttpJson fixture(String resource) {
        return new HttpJson(null, new ObjectMapper()) {
            @Override
            public Optional<com.fasterxml.jackson.databind.JsonNode> get(URI uri) {
                try (var in = OlsClientTest.class.getResourceAsStream(resource)) {
                    return in == null ? Optional.empty() : Optional.of(new ObjectMapper().readTree(in));
                } catch (java.io.IOException e) {
                    return Optional.empty();
                }
            }
        };
    }

    @Test
    void parsesTermFromCurie() {
        var term = new OlsClient(fixture("/ols/go_0042752.json")).term("GO:0042752").orElseThrow();
        assertEquals("regulation of circadian rhythm", term.label());
        assertEquals("go", term.ontologyName());
        assertEquals("http://purl.obolibrary.org/obo/GO_0042752", term.iri());
        assertFalse(term.descriptions().isEmpty());
    }

    @Test
    void returnsEmptyOnHttpError() {
        assertTrue(new OlsClient(fixture("/ols/missing.json")).term("GO:0042752").isEmpty());
    }

    @Test
    void returnsEmptyForNonOntologyIdentifier() {
        assertTrue(new OlsClient(fixture("/ols/go_0042752.json")).term("P10415").isEmpty());
    }

    @Test
    @Tag("network")
    void liveLookup() {
        var client = new OlsClient(HttpJson.createDefault());
        assertEquals("regulation of circadian rhythm", client.term("GO:0042752").orElseThrow().label());
    }
}
```

Create the fixture by downloading once: `curl -s 'https://www.ebi.ac.uk/ols4/api/ontologies/go/terms?obo_id=GO:0042752' > src/test/resources/ols/go_0042752.json` (the `_embedded.terms[0]` object holds `iri`, `label`, `ontology_name`, `obo_id`, `synonyms`, `description`). Add `returnsEmptyOnMalformedJson` to an `HttpJsonTest` using a `com.sun.net.httpserver.HttpServer` on port 0 that serves `{not json` and a 500 response, asserting `Optional.empty()` for both.

- [ ] **Step 2:** `mvn -B -q test -Dtest='MemoryCacheTest,OlsClientTest,HttpJsonTest'` fails (classes missing).

- [ ] **Step 3: Implement**

```java
package org.cy3sbml.cache;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/** Thread-safe, size-bounded least-recently-used cache for web service lookups. */
public final class MemoryCache<K, V> {
    private final Map<K, V> map;

    public MemoryCache(int maxEntries) {
        this.map = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > maxEntries;
            }
        };
    }

    /** Returns the cached value or loads it; empty results are not cached. */
    public Optional<V> get(K key, Function<K, Optional<V>> loader) {
        synchronized (map) {
            V value = map.get(key);
            if (value != null) {
                return Optional.of(value);
            }
        }
        Optional<V> loaded = loader.apply(key);
        loaded.ifPresent(v -> {
            synchronized (map) {
                map.put(key, v);
            }
        });
        return loaded;
    }

    public int size() {
        synchronized (map) {
            return map.size();
        }
    }

    public void clear() {
        synchronized (map) {
            map.clear();
        }
    }
}
```

`HttpJson`: holds `HttpClient` (built with `HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).followRedirects(NORMAL).proxy(ProxySelector.getDefault())`) and `ObjectMapper`; `static HttpJson createDefault()`; `get(URI)` sends `GET` with `Accept: application/json` and a 10 s request timeout, returns `Optional.of(mapper.readTree(body))` for 2xx, else logs `warn` with status and URI and returns empty; catches `IOException` (includes `JsonProcessingException`) and `InterruptedException` (re-sets the interrupt flag), both return empty.

`OlsClient.term(identifier)`: if identifier matches `^([A-Za-z]+)[:_](.+)$` build `https://www.ebi.ac.uk/ols4/api/ontologies/{prefix lowercase}/terms?obo_id={PREFIX}:{local}` (URL-encode), read `_embedded.terms[0]`, map fields (`synonyms` and `description` are arrays, missing means empty list), wrap the lookup in a `MemoryCache<String, OlsTerm>(5000)` held by the instance. `isOlsResource` keeps the logic of `OLSAccess.isPhysicalLocationOLS` (`resource.getResourceHomeUrl().contains("ebi.ac.uk/ols")`; copy the constant value from the current `IdentifiersConstants.OLS_BASE_URL` before removing the library).

- [ ] **Step 4:** Tests pass.
- [ ] **Step 5:** Rewire `SBaseHTMLFactory` to take an `OlsClient` (static field for now, set by `CyActivator`; PR 3 turns it into an instance dependency), render `OlsTerm` fields (`label`, `ontologyName().toUpperCase()`, `iri`, `synonyms`, `descriptions`); drop the OBO synonyms block (OLS4 merges them into `synonyms`). Delete `OLSAccess`, `OLSCache`, `OLSAccessTest`, `OLSCacheTest`, `OLSClientTest`, and rewrite the OLS parts of `RegistryUtilTest`. Remove `ols-client` from `pom.xml`.
- [ ] **Step 6:** `mvn -B -q clean verify` passes; `rg -n 'uk.ac.ebi.pride' src` returns nothing.
- [ ] **Step 7: Commit** `git commit -m "Replace ols-client with a small OLS4 REST client and a shared cache"`

### Task 2.4: UniProt and ChEBI clients on the shared stack

**Files:**
- Replace: `uniprot/UniprotAccess.java`, `uniprot/UniprotCache.java` (delete), `chebi/ChebiAccess.java`, `chebi/ChebiCache.java` (delete)
- Create: `uniprot/UniprotEntry.java` (record), `chebi/ChebiCompound.java` (record)
- Test: `uniprot/UniprotAccessTest.java`, `chebi/ChebiAccessTest.java` with fixtures `src/test/resources/uniprot/P10415.json`, `src/test/resources/chebi/<id>.json`

**Interfaces:**
- Consumes: `HttpJson`, `MemoryCache` (Task 2.3)
- Produces: `UniprotAccess(HttpJson)`: `Optional<UniprotEntry> entry(String accession)`, `String html(String accession)`; `ChebiAccess(HttpJson)`: `Optional<ChebiCompound> compound(String chebiId)`, `String html(String chebiId)`. The `html` methods return the same HTML fragments as today (see `UniprotHTMLFields` and `GUIConstants.htmlFragments`).

- [ ] **Step 1:** Record fixtures: `curl -s https://rest.uniprot.org/uniprotkb/P10415.json > src/test/resources/uniprot/P10415.json`. For ChEBI, find the current public API (the old `/chebi/backend/api/public/compound/{id}/` returns a changed payload): `curl -sI` the old URL and the documented replacement, pick the one that returns JSON for `CHEBI:15422`, save its body as fixture.
- [ ] **Step 2: Failing tests** (fixture-backed `HttpJson` as in Task 2.3): `UniprotAccessTest.parsesEntry` asserts `uniProtId` `BCL2_HUMAN`, recommended full name `Apoptosis regulator Bcl-2`, organism `Homo sapiens`, gene `BCL2`; `html` contains `BCL2_HUMAN` and `Homo sapiens`; `returnsEmptyOnHttpError`. `ChebiAccessTest.parsesCompound` asserts the ATP name and formula from the fixture; `returnsEmptyOnHttpError`; live tests tagged `network`.
- [ ] **Step 3: Implement** mapping from UniProt REST JSON: `primaryAccession`, `uniProtkbId`, `proteinDescription.recommendedName.fullName.value`, `proteinDescription.recommendedName.ecNumbers[].value`, `proteinDescription.alternativeNames[].fullName.value`, `organism.scientificName`, `organism.commonName`, `genes[].geneName.value`, `comments[]` with `commentType` `FUNCTION` (`texts[].value`), `CATALYTIC ACTIVITY` (`reaction.name`), `PATHWAY` (`texts[].value`). Each client holds a `MemoryCache` (5000). HTML strings escape values with `StringEscapeUtils.escapeHtml4`.
- [ ] **Step 4:** Tests pass. Update `SBaseHTMLFactory.createSecondaryInformation` to use the instances. Remove `japi`, `ehcache`, `fastjson2`, `org.json` (after Task 2.5) from `pom.xml`; `ChebiAccess` uses jackson.
- [ ] **Step 5: Commit** `git commit -m "Move the UniProt and ChEBI lookups to their REST APIs and drop ehcache"`

### Task 2.5: One JSON library and unused dependencies

**Files:** `biomodel/Biomodel.java`, `biomodel/BiomodelsQuery.java`, `biomodel/BiomodelsQueryResult.java`, `miriam/RegistryUtil.java`, `pom.xml`

- [ ] **Step 1:** Port `org.json` usage in `biomodel/*` and `fastjson2` usage in `miriam/RegistryUtil` to jackson (`ObjectMapper.readTree`, `JsonNode.path(...)`). Keep behavior; `SearchContentTest` and `RegistryUtilTest` must still pass. Add a fixture-backed test `BiomodelsQueryTest.parsesSearchResult` with a recorded response of `https://www.ebi.ac.uk/biomodels/search?query=glucose&format=json&numResults=2`.
- [ ] **Step 2:** Remove from `pom.xml`: `unirest-java`, `httpclient`, `httpmime`, `httpasyncclient`, `json`, `fastjson2`, `xstream`, `controlsfx`, `jigsaw`, `staxmate`, and any other dependency with no import in `src/main` (check each with `rg -l '<package prefix>' src/main`; JSBML runtime needs such as `woodstox-core`, `jtidy`, `biojava-ontology` stay if JSBML needs them at runtime: verify by running the model tests `mvn -B -q test -Dtest='SBML*Test'`).
- [ ] **Step 3:** `mvn -B -q clean verify`; `mvn dependency:analyze -q` lists no "Used undeclared" for main code; record jar size before/after in the PR body.
- [ ] **Step 4:** Enable the enforcer `requireReleaseDeps` rule; `mvn -B -q verify` passes.
- [ ] **Step 5: Commit** `git commit -m "Use jackson for all JSON and remove unused dependencies"`

### Task 2.6: Test profiles

**Files:** `pom.xml`, `src/test/java/org/cy3sbml/models/*.java`

- [ ] Replace the `models.test.excludes` property with JUnit tags: tag the three model suite classes `@Tag("models")`; surefire `excludedGroups` default `network,models`; profile `all-tests` sets `excludedGroups` empty; `-Dgroups=network` runs the network tests. Update `CLAUDE.md` build section commands accordingly.
- [ ] `mvn -B -q test` passes; `mvn -B -q test -Pall-tests -Dtest=SBMLTestSuiteTest` runs (may be slow).
- [ ] Commit `git commit -m "Select the slow and the network tests with JUnit tags"`

### Task 2.7: Spotless formatting

**Files:** `pom.xml`, all `src/**/*.java`, `.git-blame-ignore-revs`, `docs/develop.md`

- [ ] **Step 1:** Add `com.diffplug.spotless:spotless-maven-plugin` (latest) with `<java><palantirJavaFormat><version>latest palantir</version></palantirJavaFormat><removeUnusedImports/><importOrder/><trimTrailingWhitespace/><endWithNewline/></java>`; also `<pom><sortPom><expandEmptyElements>false</expandEmptyElements></sortPom></pom>`.
- [ ] **Step 2:** Commit the plugin config alone: `git commit -m "Format the code with palantir-java-format through spotless"`.
- [ ] **Step 3:** `mvn -B -q spotless:apply`; `mvn -B -q verify`; commit as `git commit -am "Apply palantir-java-format to the whole code base"`.
- [ ] **Step 4:** Write `.git-blame-ignore-revs` with a comment line and the SHA of step 3; commit.
- [ ] **Step 5:** Add CI job `format` to `ci.yml`: checkout, setup-java 17, `mvn -B --no-transfer-progress spotless:check`, `name: format`.
- [ ] **Step 6:** Replace the IntelliJ pre-commit hook section in `docs/develop.md` with a hook running `mvn -q spotless:apply` and re-adding staged files. Commit.

### Task 2.8: Error Prone and -Werror

**Files:** `pom.xml`, any `src/main/**` with warnings, `.github/workflows/ci.yml`

- [ ] **Step 1:** Check the Error Prone minimum runtime JDK for the latest release (`https://github.com/google/error-prone/releases`). Add profile `lint` in `pom.xml` configuring maven-compiler-plugin with `-XDcompilePolicy=simple`, `--should-stop=ifError=FLOW`, `-Xplugin:ErrorProne`, the `--add-exports`/`--add-opens` jdk.compiler flags from the Error Prone installation docs (in `.mvn/jvm.config`), `<annotationProcessorPaths>` with `error_prone_core`, and `<compilerArgs>` `-Xlint:all,-processing,-serial` `-Werror`. Keep default builds without Error Prone so tests stay fast.
- [ ] **Step 2:** `mvn -B -q -Plint compile` and fix every warning and Error Prone finding in the code (not suppress), excluding findings that are only fixable by the refactor of PR 3 in `SBMLReaderTask`: fix those anyway if trivial, else suppress at method level with a `// reason:` comment and a note to remove in Task 3.2.
- [ ] **Step 3:** Add CI job `lint` (`name: lint`) running `mvn -B --no-transfer-progress -Plint -DskipTests compile test-compile` on the JDK Error Prone needs, still with `release 17`.
- [ ] **Step 4:** jacoco: keep `prepare-agent` and `report`; upload `target/site/jacoco` as artifact in the ubuntu `test` job.
- [ ] **Step 5: Commit** `git commit -m "Check the code with Error Prone and fail on compiler warnings"`

### Task 2.9: OSGi smoke check, PR 2, rulesets

- [ ] Compare `unzip -p target/cy3sbml-*.jar META-INF/MANIFEST.MF | grep -A200 Import-Package` before (baseline jar from develop) and after; no new mandatory import.
- [ ] Load the jar in Cytoscape 3.10.4 (see Task 3.8 for how) and import `src/test/resources/models/unittests/fbc_01.xml`; check `~/CytoscapeConfiguration/cy3sbml/*.log` and `~/CytoscapeConfiguration/3/framework-cytoscape.log` for `ClassNotFoundException`/`NoClassDefFoundError`.
- [ ] Open PR `build-tooling`, green CI, squash merge. Add `format` and `lint` to `develop.json` required checks, re-run `apply.sh`, commit that to the next branch.

---

## PR 3: Code refactor (branch `refactor-reader`)

### Task 3.1: Golden snapshot tests

**Files:**
- Create: `src/test/java/org/cy3sbml/golden/NetworkSnapshot.java`, `src/test/java/org/cy3sbml/golden/GoldenModelsTest.java`, `src/test/resources/golden/*.json`

**Interfaces:**
- Produces: `NetworkSnapshot.of(CyNetwork[] networks) -> ObjectNode` canonical JSON; `GoldenModelsTest` parameterized over `MODELS` (list of resource paths).

- [ ] **Step 1:** Fix `TestUtils.readNetwork`: stop swallowing `Throwable` (let the exception fail the test), use try-with-resources on the stream, pass a no-op `TaskMonitor` mock.
- [ ] **Step 2: Snapshot format.** For each network in `networks` (sorted by the `name` column): name, node count, edge count, and nodes and edges as sorted arrays of attribute maps. A node is keyed by `SBML.ATTR_ID` (fallback `SBML.ATTR_METAID`, fallback `shared name`, fallback the sorted attribute JSON). An edge is serialized as `{source: key, target: key, interaction, attributes}`. All columns of the default node/edge table except `SUID`, `selected`, and columns whose values are SUIDs (list the column names in the class). `Double` values rounded to 12 significant digits. Lists sorted when the column is a set by meaning (sort all lists; order of list columns is not semantic in cy3sbml, verify by reading where list attributes are set).
- [ ] **Step 3: Models.** Pick about 30 covering: `unittests/*` (all), `comp/*` (2 with flattening), `fbc/*` (v1 and v2), `qual/*` (2), `layout/*` (2), `distrib/*` (1), `koenig/*` (2), `biomodels/*` (5 small), `bigg_models/e_coli_core.xml`, `sbml-test-suite` (5 across L1, L2, L3). List them as a `static final List<String> MODELS` constant.
- [ ] **Step 4:** Test: when `-Dgolden.update=true` write the snapshot file (pretty printed, `\n` endings), else compare with `assertEquals(expected.toPrettyString(), actual.toPrettyString())` so diffs are readable.
- [ ] **Step 5:** Generate on the unchanged code, run twice to verify determinism (`git status` clean after the second update run), run on Windows line endings safe (read with UTF-8, normalize `\r\n`).
- [ ] **Step 6: Commit** `git commit -m "Pin the imported networks of reference models with golden snapshots"`

### Task 3.2: Split SBMLReaderTask into org.cy3sbml.reader

**Files:**
- Move: `src/main/java/org/cy3sbml/SBMLReaderTask.java` to `src/main/java/org/cy3sbml/reader/SBMLReaderTask.java`
- Create in `org.cy3sbml.reader`: `ConversionContext`, `PackageReader`, `CoreReader`, `QualReader`, `FbcReader`, `CobraNotesParser`, `CompReader`, `GroupsReader`, `LayoutReader`, `MathGraphBuilder`, `UnitGraphBuilder`, `AttributeWriter`, `SubnetworkBuilder`
- Modify: `SBMLReaderTaskFactory`, `archive/ArchiveReaderTask`, all callers found by `rg -l SBMLReaderTask src`
- Test: `src/test/java/org/cy3sbml/reader/*ReaderTest.java`

**Interfaces:**
- `interface PackageReader { void read(ConversionContext context, Model model); }`
- `final class ConversionContext`: `ConversionContext(CyNetwork network, CyGroupFactory groupFactory)`; `CyNetwork network()`; `CyNode createNode(SBase sbase, String sbmlType)`; `CyEdge createEdge(CyNode source, CyNode target, String interactionType)`; `CyGroup createGroup(Group group)`; `Optional<CyNode> nodeById(String id)`; `Optional<CyNode> nodeByMetaId(String metaId)`; `Set<CyGroup> groups()`; `Map<String, UnitDefinition> baseUnitDefinitions()`.
- `final class AttributeWriter` (static methods moved verbatim from `SBMLReaderTask`: `setSBaseAttributes`, `setNamedSBaseAttributes`, `setSBaseRefAttributes`, `setNamedSBaseWithDerivedUnitAttributes`, `setQuantityWithUnitAttributes`, `setSymbolNodeAttributes`, `setUnitAttributes`, `setAbstractMathContainerNodeAttributes`).
- `final class MathGraphBuilder`: `void createMathNetwork(ConversionContext c, AbstractMathContainer container, CyNode containerNode, String edgeType)`.
- `final class UnitGraphBuilder`: `createUnitDefinitionGraph(ConversionContext, UnitDefinition)`, `createUnitEdge(ConversionContext, CyNode, QuantityWithUnit)`.
- `final class SubnetworkBuilder`: `List<CyNetwork> build(CyRootNetwork root, CyNetwork network, Set<CyGroup> groups)` returning `[base, all, kinetic]` in the order the task produced them.
- `SBMLReaderTask` keeps its public API (`getNetworks`, `buildCyNetworkView`, `mappingFromNetwork`, `getError`, `cancel`, `run`).

Procedure (one commit per step, `mvn -B -q test -Dtest='GoldenModelsTest,SBML*Test'` green after each):

- [ ] **Step 1:** Move the class to the new package (update imports), commit.
- [ ] **Step 2:** Extract `AttributeWriter` (pure move of static methods), commit.
- [ ] **Step 3:** Extract `ConversionContext` holding `network`, `id2Node`, `metaId2Node`, `cyGroupSet`, `baseUnitDefinitions`, and move `createNode`, `createEdge`, `createGroup` into it. The task creates one context per model in `readModelInNetwork`. Commit.
- [ ] **Step 4:** Extract `MathGraphBuilder`, `UnitGraphBuilder` (`createMathNetwork`, `createUnitEdge`, `createUnitDefinitionGraph`), commit.
- [ ] **Step 5:** Extract `CobraNotesParser` (`parseCobraNotes`) and `FbcReader` (`readFBC`, `processAssociation`), commit.
- [ ] **Step 6:** Extract `QualReader`, `GroupsReader`, `LayoutReader` (`readLayouts`, `readLayout`), `CompReader` (`readComp`, `getRefFromSBaseRef`, `createSBaseRefEdge`; `readFlattenedModel` stays in the task because it creates networks, but calls the readers), commit.
- [ ] **Step 7:** Extract `CoreReader` from `readCore` plus `addCompartmentCodes`, `addSBMLTypesExtended`, `addSBMLInteractionExtended`; inside split into private methods `readModelNode`, `readCompartments`, `readSpecies`, `readParameters`, `readInitialAssignments`, `readRules`, `readConstraints`, `readFunctionDefinitions`, `readUnitDefinitions`, `readEvents`, `readReactions` (follow the order of the current code so node creation order and therefore any order-dependent attributes do not change). Commit.
- [ ] **Step 8:** Extract `SubnetworkBuilder` (`addAllNetworks`, `addSubNetwork`, `getNetworkEdges`, `getNetworkNodes`). `SBMLReaderTask` now wires readers in a `List<PackageReader>` built in the constructor and is under 300 lines (`wc -l`). Commit.
- [ ] **Step 9:** Tests per reader on small models (`src/test/resources/models/unittests`): `CoreReaderTest` (species/reaction node counts and one attribute), `FbcReaderTest` (gene product association edges on `fbc_01.xml`), `CobraNotesParserTest` (parses `<p>GENE_ASSOCIATION: b0001</p>` notes into `{GENE_ASSOCIATION=b0001}`, ignores malformed lines), `QualReaderTest`, `CompReaderTest`, `GroupsReaderTest`, `LayoutReaderTest`. Add `SBMLReaderTaskTest.readerReportsInvalidSbml`: running the task on a stream containing `<sbml>broken` sets `getError()` true, reports through a mocked `TaskMonitor` `showMessage(Level.ERROR, ...)`, and `getNetworks()` is empty. Commit.

### Task 3.3: Hygiene pass

**Files:** as listed by the audit; verify each with `rg` before editing.

- [ ] Delete `BundleInformation` duplicate (keep `org.cy3sbml.BundleInformation`, update `archive` imports), `gui/BrowserSample.java`, `gui/Browser.java` (move anything `GUIUtil` needs into `GUIUtil`), `src/test/java/org/cy3sbml/oven/MemoryLeak.java`, the scratch `main` methods in `miriam/RegistryUtil`, `HtmlTemplateParser`, `gui/SBaseHTMLFactory`, `styles/StyleFactory`, `biomodel/BiomodelsQuery` (and the already removed ols/chebi ones).
- [ ] Remove commented-out code blocks (`rg -n '^\s*//.*[;{}]\s*$' src/main`), keep explanatory comments.
- [ ] Replace every `printStackTrace()` and `System.out`/`System.err` in `src/main` with slf4j calls (`logger.error("Could not <action> <subject>: {}", subject, e)`), removing a duplicate `logger.error` next to it. `rg -c 'printStackTrace|System\.(out|err)' src/main` returns nothing.
- [ ] Replace `catch (Exception|Throwable)` with specific types except at task/thread boundaries (`SBMLReaderTask.run`, `ArchiveReaderTask.run`, `CyActivator.start`, executor tasks), where they log and report.
- [ ] try-with-resources for the streams in `SessionData`, `ResourceExtractor`, `GUIUtil`, `SBMLFileFilter`, `SBaseHTMLFactory`, `BiomodelsQuery`.
- [ ] `styles/MappingDiscrete`: generic `Map<String, String>` (check the value type from usage).
- [ ] TODO/FIXME review: `rg -n 'TODO|FIXME' src/main`. For each: fix if small and covered by tests, else create an issue (`gh-axi issue create --title ... --body ...` with file, line and the text) and remove the comment, or delete if obsolete. Record the created issue numbers in the PR body.
- [ ] Java 17 idioms where clearer: anonymous `Runnable`/listeners to lambdas, `instanceof` patterns, switch expressions on strings/enums, records for pure value holders (e.g. `biomodel/Biomodel` if immutable).
- [ ] `mvn -B -q verify -Plint` and golden tests green. One commit per bullet.

### Task 3.4: Remove singletons

**Files:** `SBMLManager`, `ServiceAdapter`, `gui/WebViewPanel`, `styles/StyleManager`, `cofactors/CofactorManager`, `biomodel/BiomodelsDialog`, `CyActivator`, `SessionData`, all callers (`rg -n 'getInstance\(' src/main`)

- [ ] **Step 1:** `ServiceAdapter`: public constructor taking the services; remove static instance and both `getInstance` methods; `CyActivator` creates it once.
- [ ] **Step 2:** For each other singleton: make the constructor public with its dependencies, remove the static instance and `getInstance`, create it in `CyActivator.start`, pass it via constructor to every consumer (actions, listeners, task factories, `SBaseHTMLThread`, `BrowserHyperlinkListener`, `SessionData`). Consumers store it in a `private final` field.
- [ ] **Step 3:** `SBaseHTMLFactory`: turn static state (`baseDir`, the `OlsClient`/`UniprotAccess`/`ChebiAccess` fields from PR 2) into constructor-injected instance fields; `CyActivator` creates one factory.
- [ ] **Step 4:** `CyActivator.logger` becomes `private static final`.
- [ ] **Step 5:** Tests: update `SBMLManagerTest` and others to construct instances directly; `rg -n 'getInstance\(' src` returns only third-party calls.
- [ ] **Step 6:** Golden tests, `mvn -B -q verify -Plint` green. Commit per singleton.

### Task 3.5: Session round trip test

**Files:** `src/test/java/org/cy3sbml/SessionDataTest.java`

- [ ] Test `sessionRoundTripRestoresMapping`: read `fbc_01.xml` through the task with an `SBMLManager` instance, save session files through `SessionData` into a temp dir (`@TempDir`), create a fresh `SBMLManager`, restore from the files, assert the restored manager maps the network SUIDs to a document with the same model id and the same `One2ManyMapping` sizes. Use the real `SessionData` API (read it first; mock only Cytoscape session event objects).
- [ ] Commit.

### Task 3.6: WebViewPanel render executor

**Files:** `gui/WebViewPanel.java`, create `gui/LatestTaskExecutor.java`; test `gui/LatestTaskExecutorTest.java`

**Interfaces:** `final class LatestTaskExecutor implements AutoCloseable`: `void submit(Runnable task)` cancels the pending/running previous task (`Future.cancel(true)`) and runs the new one on a single daemon thread named `cy3sbml-info-renderer`; `close()` shuts down.

- [ ] Test `latestSelectionWins`: submit 10 tasks that each sleep 50 ms then record their index; after awaiting, the recorded list ends with 9 and has fewer than 10 entries. Test `closeStopsExecutor`.
- [ ] Implement, replace `new Thread(updater).start()` in `WebViewPanel` with `executor.submit(updater)`; `SBaseHTMLThread` checks `Thread.currentThread().isInterrupted()` before posting HTML to the JavaFX thread. Close the executor in `CyActivator.shutDown`.
- [ ] Commit.

### Task 3.7: Unit tests for untested packages

**Files:** tests under `src/test/java/org/cy3sbml/{util,archive,cofactors,layout}/`

- [ ] `util/SBMLUtilTest`, `util/AttributeUtilTest`, `util/XMLUtilTest`, `util/NetworkUtilTest`: at least one test per public method used from `src/main` (list via `rg -o 'SBMLUtil\.\w+' src/main | sort -u`), using small inline SBML strings and `NetworkTestSupport`.
- [ ] `archive/ArchiveReaderTaskTest`: read a COMBINE archive from `src/test/resources` (find with `find src/test/resources -name '*.omex'`; if none, build a minimal one in the test with `java.util.zip` containing `manifest.xml` and one SBML file) and assert one network per SBML entry. Add a test that an entry named `../evil.xml` is not extracted outside the target directory.
- [ ] `cofactors/CofactorManagerTest`: split a cofactor node on a small network and assert the new node count and that the undo restores the original.
- [ ] `layout/LayoutRoundTripTest`: save positions of a small network to XML in `@TempDir`, change positions, load, assert original positions.
- [ ] Commit per package.

### Task 3.8: End-to-end check in Cytoscape 3.10.4

- [ ] Find Cytoscape: `ls ~/Cytoscape_v3.10.4 /opt/cytoscape* ~/cytoscape* 2>/dev/null`. If missing, download `https://github.com/cytoscape/cytoscape/releases/download/3.10.4/cytoscape-unix-3.10.4.tar.gz` into the scratchpad and extract.
- [ ] `mvn -B -q install -DskipTests`; symlink the jar as in `CLAUDE.md`; start `cytoscape.sh` in the background with `DISPLAY` of the user session.
- [ ] Drive the GUI with `xdotool` and capture with `import -window root` (ImageMagick) or `gnome-screenshot`; view screenshots with the Read tool. Check: import of `fbc_01.xml`, a comp model, a qual model, a layout model; subnetworks present; style applied; info panel for species and reaction with annotations (OLS, UniProt, ChEBI); BioModels search; layout save and load; cofactor split; session save and reopen.
- [ ] Record each defect, fix it (TDD where testable), repeat.
- [ ] If the GUI cannot be driven (no display, no xdotool), write the check list into the PR body as a manual test list and continue.

### Task 3.9: PR 3

- [ ] Update `CLAUDE.md` architecture section to the new reader package, no singletons, test tags. Open PR, green CI, squash merge.

---

## PR 4: Documentation site (branch `docs-site`)

### Task 4.1: zensical setup and workflow

**Files:** create `zensical.toml`, `docs/requirements.txt`, `docs/index.md` (initial), `scripts/llms_txt.py`, `scripts/release_notes.py`, `.github/workflows/docs.yml`

- [ ] `docs/requirements.txt`: `zensical==<latest from https://pypi.org/pypi/zensical/json>`.
- [ ] `zensical.toml`: copy `/home/mkoenig/git/sbmlutils/zensical.toml` and change `site_name = "cy3sbml"`, `site_url = "https://matthiaskoenig.github.io/cy3sbml/"`, `site_description = "SBML for Cytoscape"`, copyright `2012-2026`, repo URLs, `logo = "images/logo100.png"`, favicon (create `docs/images/favicon.ico` from `logo.png` with `convert -resize 48x48`), social links GitHub and `https://apps.cytoscape.org/apps/cy3sbml` (icon `fontawesome/solid/store`), remove the `mkdocstrings` plugin blocks, nav from Task 4.2. Exclude `docs/superpowers/` via `exclude_docs` (check the zensical option name in the zensical docs; if unsupported, move specs and plans to `superpowers/` at repo root and update links).
- [ ] `scripts/llms_txt.py`: copy `/home/mkoenig/git/pkdb/scripts/llms_txt.py` (no API pages), adapt names.
- [ ] `scripts/release_notes.py`: reads `release-notes/*.md`, sorts by version (`tuple(int(x) for x in stem.split('.'))`), writes `docs/release-notes.md` with `# Release notes` and each file content with headings demoted one level. Standard library only.
- [ ] `.github/workflows/docs.yml`: copy pkdb's, add step `uv run --no-project --python 3.14 python scripts/release_notes.py` before the build; keep `name: docs` job and deploy on `develop`.
- [ ] Local check: `uv run --no-project --python 3.14 python scripts/release_notes.py && uvx --python 3.14 --with-requirements docs/requirements.txt zensical build --clean && uv run --no-project --python 3.14 python scripts/llms_txt.py` succeeds without warnings.
- [ ] Commit.

### Task 4.2: Content

**Files:** `docs/index.md`, `docs/installation.md`, `docs/guide/{import,network,info-panel,validation,styles,layouts,cofactors,packages}.md`, `docs/development/{building,architecture,testing,quality,contributing,release}.md`, `docs/citation.md`; delete `docs/develop.md`, `docs/contributing.md`, `docs/release.md` after merging their content.

- [ ] Write each page from the code (the facts: node/edge types and attributes from `SBML.java`, subnetwork names from `SBML.PREFIX_*`, actions from `actions/`, BioModels dialog from `biomodel/`, packages from the readers). No invented features. Architecture page contains a mermaid diagram of `SBMLReaderTaskFactory -> SBMLReaderTask -> PackageReader[] -> ConversionContext -> SubnetworkBuilder`, and the managers wired by `CyActivator`.
- [ ] `docs/development/quality.md`: spotless, Error Prone profile `lint`, tags `network`/`models`, golden snapshot update command `mvn test -Dtest=GoldenModelsTest -Dgolden.update=true`.
- [ ] `docs/development/release.md`: content of the rewritten `docs/release.md` plus the branch model (develop default, main = latest release, rulesets, `apply.sh`).
- [ ] Build locally without warnings; check every internal link resolves (zensical reports broken links).
- [ ] Commit.

### Task 4.3: Screenshots

- [ ] In the running Cytoscape from Task 3.8, capture: main window with an imported fbc model and info panel, the BioModels dialog, a comp model, the kinetic subnetwork, an annotation panel with an OLS term. Save as `docs/images/screenshots/<name>.png`, crop to the window (`import -window <id>`), width at most 1600 px. View each with the Read tool and retake if anything looks off.
- [ ] If no GUI is available, list the needed shots in the PR body and keep the old screenshot referenced.
- [ ] Commit.

### Task 4.4: Cleanup, README, CITATION.cff

- [ ] `git rm -r docs/manuscript docs/presentation docs/publication docs/specifications docs/images/*.ai docs/images/*.psd.zip` and old screenshots no longer referenced.
- [ ] README: logo, badges (CI `https://github.com/matthiaskoenig/cy3sbml/actions/workflows/ci.yml/badge.svg`, docs, Zenodo DOI, App Store), 3-sentence description, screenshot, links to docs site sections, citation, license, funding (keep the funding text verbatim).
- [ ] `CITATION.cff` (cff-version 1.2.0) with title, authors (Matthias König, ORCID from `.zenodo.json`), the preferred citation of the 2012 Bioinformatics paper (DOI 10.1093/bioinformatics/bts432, verify via `curl -s https://api.crossref.org/works/10.1093/bioinformatics/bts432 | head -c 300`), repository URL, license MIT.
- [ ] Update `CLAUDE.md` docs and release sections.
- [ ] Commit.

### Task 4.5: PR 4, Pages, final ruleset

- [ ] Enable Pages with Actions as source: `gh-axi api -X POST repos/matthiaskoenig/cy3sbml/pages -f build_type=workflow` (or `PUT` if it exists).
- [ ] Open PR, green CI incl. `docs`, squash merge. Check deploy run succeeded and the site loads (`curl -sI https://matthiaskoenig.github.io/cy3sbml/`).
- [ ] Add `docs` to `develop.json` required checks via a small PR, run `apply.sh` after merge.
