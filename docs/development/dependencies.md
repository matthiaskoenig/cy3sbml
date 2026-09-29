# Dependencies

This page lists where the dependencies of cy3sbml come from and how each kind of
dependency is updated.

| Dependency | Source | Update |
|---|---|---|
| Maven dependencies and plugins | Maven Central | Dependabot pull requests, weekly |
| Cytoscape API (`org.cytoscape:*`) | NRNB Nexus (`cytoscape_releases`, `cytoscape_thirdparty`), `provided` scope | Patch versions by Dependabot. Minor and major versions by hand, they set the minimum Cytoscape version |
| JSBML and its package modules | Built from source into `lib/cy3sbml-dep` | [Update JSBML](#update-jsbml) |
| jtidy (`cy3sbml-dep:jtidy:r938`) | `lib/cy3sbml-dep`, the HTML Tidy library used by `jsbml-tidy` | By hand, the version JSBML builds with |
| GitHub Actions | GitHub | Dependabot pull requests, weekly |
| Python helpers (`tools/`) | PyPI, locked in `tools/uv.lock` | Dependabot pull requests, weekly |

The build fails for SNAPSHOT dependencies and duplicate classes (Maven Enforcer). The
`maven-bundle-plugin` embeds every dependency that is not `provided` or `test` into
the app jar, with its transitive dependencies. Test a new runtime dependency in
Cytoscape, to catch class loading problems in OSGi.

## JSBML

cy3sbml reads SBML with [JSBML](https://github.com/sbmlteam/jsbml). cy3sbml does not
use the JSBML release on Maven Central (1.6.1), because it needs fixes that exist only
on the `master` branch of JSBML. The jars are built from one JSBML commit instead:

- `lib/cy3sbml-dep` is a Maven repository inside the project (the `in-project`
  repository in `pom.xml`). It has one directory per jar: `jsbml` (the core), and
  `jsbml-qual`, `jsbml-layout`, `jsbml-comp`, `jsbml-fbc`, `jsbml-groups`,
  `jsbml-distrib` and `jsbml-tidy`. Each holds the jar, a POM without dependencies, and
  their SHA-1 checksums.
- The property `jsbml.version` in `pom.xml` pins the commit, as
  `<JSBML version>-<commit date>-<short sha>`. For example `1.7-20260907-8192a8a7` is
  commit [`8192a8a7`](https://github.com/sbmlteam/jsbml/commit/8192a8a7) of
  2026-09-07. The property `jsbml.osgi.version` holds the same version as an OSGi
  version (`1.7.0.20260907-8192a8a7`). The app exports the `org.sbml.jsbml.*` packages
  with this version, because JSBML types are part of the `SBMLManager` service API.
- The jars contain no test classes. JSBML's Ant build puts its JUnit test classes and
  test data into the jars, next to the production classes, and the update script
  removes them. The app jar copies the classes and resources of the JSBML jars in
  directly (`inline=org/**` in the `Embed-Dependency` instruction). It leaves out
  their `META-INF`: without the `ServiceLoader` files, JSBML uses its built-in list
  of package parsers.
- The dependencies of JSBML (woodstox, staxmate, biojava-ontology, json-simple, ...)
  are normal Maven Central dependencies in `pom.xml`, so Dependabot updates them.

### Current pin: JSBML fork

The pinned commit `3c63ff7f` is on the branch
[`comp-fixes`](https://github.com/matthiaskoenig/jsbml/tree/comp-fixes) of the fork
`matthiaskoenig/jsbml`, one commit on top of JSBML `master` (`8192a8a7`). It rewrites
the flattening of hierarchical models (`CompFlatteningConverter`) and fixes the
resolution of external model definitions, which cy3sbml needs for the comp package
([#401](https://github.com/matthiaskoenig/cy3sbml/issues/401),
[#220](https://github.com/matthiaskoenig/cy3sbml/issues/220)). When the fix is merged
into JSBML, update to JSBML `master` again.

## Update JSBML

`scripts/update_jsbml.py` does the complete update. By default it uses the latest
commit on the JSBML `master` branch. If that commit is already pinned, the script
changes nothing.

### With GitHub Actions

1. Open *Actions*, then *update JSBML*, then *Run workflow*. Keep the default `master`,
   or enter a JSBML branch, tag or commit. From the command line:

    ```bash
    gh workflow run update-jsbml.yml                  # latest JSBML master
    gh workflow run update-jsbml.yml -f ref=<commit>  # a given commit
    gh workflow run update-jsbml.yml -f ref=<commit> \
        -f repository=https://github.com/matthiaskoenig/jsbml  # a commit of a fork
    ```

2. The workflow runs the script and opens the pull request "Update JSBML to
   `<version>`". The description lists the JSBML commits the update adds. The
   workflow starts the CI and documentation checks for the pull request itself, since
   a pull request opened by a workflow starts no workflows.
3. Review the pull request, see [Check the update](#check-the-update).

### Locally

The script needs Git, [uv](https://docs.astral.sh/uv/) and a JDK 17. JSBML compiles
for Java 7, which JDK 20 and newer cannot compile for. If `JAVA_HOME` is set, the
script uses its `javac`, otherwise the `javac` on the `PATH`. You do not need Ant: the
script downloads Apache Ant once, checks it against the published SHA-512, and keeps
it in `~/.cache/cy3sbml`.

```bash
git switch -c update-jsbml
uv run --no-project --python 3.14 python scripts/update_jsbml.py          # latest master
uv run --no-project --python 3.14 python scripts/update_jsbml.py <ref>    # branch, tag or commit
./mvnw -B -q clean verify
```

`--repository <url or path>` builds from another repository than
`https://github.com/sbmlteam/jsbml`: a fork with fixes that are not merged into JSBML
yet, or a local clone while developing a fix. The workflow has the same `repository`
input. Pin only commits that are pushed to a public repository.

The script does these steps:

1. Clone JSBML into a temporary directory and check out the commit.
2. Build the core and package jars with JSBML's Ant build (`ant jar`). All jars get the
   version of the commit.
3. Remove the test classes and the test data from the jars. A test class is a class
   compiled from a source file in the `test/` directory of the JSBML module, found by
   its `package` declaration. Test data is a file from `test/` in a `test` or
   `testdata` package. The other files of `test/` stay in the jar, because
   production code reads some of them (`ASTFactory.parseMathML` reads
   `org/sbml/jsbml/math/compiler/resources/`).
4. Write the jars into `lib/cy3sbml-dep` and delete the previous version. Also
   delete a copy of the new version in `~/.m2/repository`, see
   [Troubleshooting](#troubleshooting).
5. Set `jsbml.version` and `jsbml.osgi.version` in `pom.xml`.
6. Print the JSBML commits between the old and the new version.

Commit `lib/cy3sbml-dep` and `pom.xml`, and open a pull request.

`--force` rebuilds the pinned commit. Use it after a change to the script.
`--summary <file>` writes the printed summary to a Markdown file.

### Check the update

- CI passes, in particular `GoldenModelsTest`. It compares the networks created from the
  reference models with snapshots. A change in JSBML that changes the import makes it
  fail. Check the diff, then update the snapshots, see [Testing](testing.md).
- `BundleJarContentIT` checks the app jar: no JUnit classes or imports, the JSBML
  export version, SBO and jtidy.
- Import some models in Cytoscape, and check the info panel and the validation.
- Add the update, with the JSBML fixes it brings, to the release notes of the next
  version (`release-notes/<version>.md`).

### Troubleshooting

- **"JDK 17 to 19 needed to build JSBML"**: set `JAVA_HOME` to a JDK 17.
- **The Ant build fails**: the script prints the end of the Ant output. JSBML's
  `master` branch can have a broken build. In that case, pin the last working commit
  and report the problem at <https://github.com/sbmlteam/jsbml/issues>.
- **The app jar still has JSBML test classes, or old JSBML behavior, after a
  rebuild with `--force`**: Maven copies the jars from `lib/cy3sbml-dep` into the local
  repository (`~/.m2/repository/cy3sbml-dep`) on first use. After that, it does not
  read `lib/` again for that version. A new JSBML commit has a new version, so this
  applies only to a rebuild of the same version. The script deletes the copy in
  `~/.m2/repository`. On other machines, delete it by hand:
  `rm -rf ~/.m2/repository/cy3sbml-dep/jsbml*`.

JSBML's Maven build (`pom.xml` in the JSBML repository) is not used. It needs
`org.mangosdk.spi:spi:0.2.4`, which is not on Maven Central, and its jars differ from
the Ant jars: the core jar lacks resources that the Ant build includes. For the same
reason, [JitPack](https://jitpack.io) cannot build JSBML.
