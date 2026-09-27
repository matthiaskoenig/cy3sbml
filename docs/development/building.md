# Building

cy3sbml is a Cytoscape app, packaged as an OSGi bundle with Maven. The general
documentation of Cytoscape app development is in the
[Cytoscape App Developer Guide](https://github.com/cytoscape/cytoscape/wiki/Cytoscape-App-Ladder).

## Requirements

- Git.
- JDK 17, for example [Eclipse Temurin](https://adoptium.net) 17 (the CI uses it).
  The build requires Java 17 or newer and compiles for Java 17. The Error Prone check
  needs JDK 21, see [Code quality](quality.md#error-prone).
- Cytoscape 3.10 to run the app.

Maven does not need to be installed: the repository has the Maven Wrapper (`./mvnw`, on
Windows `mvnw.cmd`), which downloads Maven 3.9.11. JavaFX is a `provided` Maven
dependency for compiling and testing; at runtime Cytoscape provides it.

## Build

```bash
git clone https://github.com/matthiaskoenig/cy3sbml.git
cd cy3sbml
./mvnw -B -q clean install -DskipTests
```

The default branch is `develop`. The build writes the app jar to
`target/cy3sbml-<version>.jar`. The version is set in `pom.xml`.

`./mvnw verify` also runs the tests and the packaged-jar integration test, see
[Testing](testing.md).

## Dependencies

- The Cytoscape API artifacts (API version 3.10.0) come from the NRNB Nexus repositories
  and have `provided` scope.
- JSBML and its package modules are not taken from Maven Central. The jars are in
  `lib/cy3sbml-dep`, a Maven repository inside the project, pinned to one JSBML commit
  (property `jsbml.version` in `pom.xml`). See [Update JSBML](#update-jsbml).
- The `maven-bundle-plugin` embeds all dependencies that are not `provided` or `test`,
  with their transitive dependencies, into the bundle jar, and marks the imports as
  optional. A new runtime dependency ends up in the jar automatically. Test the app in
  Cytoscape after adding one, to catch class loading problems in OSGi.
- The build fails for SNAPSHOT dependencies and duplicate classes (Maven Enforcer).

## Run in Cytoscape

Link the built jar into the apps folder of Cytoscape:

```bash
ln -s "$PWD/target/cy3sbml-<version>.jar" \
  "$HOME/CytoscapeConfiguration/3/apps/installed/cy3sbml-latest.jar"
```

Cytoscape installs the app at the next start, and reloads it after every
`./mvnw -B -q install -DskipTests` while it runs (hot reload). Update the link when the
version changes. Remove the App Store version of cy3sbml first, so that only one version
is installed.

cy3sbml writes its log to `~/CytoscapeConfiguration/cy3sbml/cy3sbml-v<version>.log`.

## Debug

Start Cytoscape in debug mode:

```bash
./cytoscape.sh debug      # Linux and macOS
cytoscape.bat debug       # Windows
```

Cytoscape then prints `Listening for transport dt_socket at address: 12345`. Attach a
remote JVM debugger of your IDE to `localhost:12345`, for example a **Remote JVM Debug**
run configuration in IntelliJ IDEA.

## Update JSBML

The jars in `lib/cy3sbml-dep` are built from the JSBML sources with
`lib/build_jsbml_jars.sh`. Rebuild them only to upgrade JSBML:

```bash
lib/build_jsbml_jars.sh <jsbml-commit>
```

The script needs `ant` and a JDK 17 on the `PATH`. It clones JSBML into `$JSBMLCODE`
(default `$HOME/git/jsbml`), checks out the commit, builds the core and package jars
with the version `1.7-<commit date>-<short sha>`, installs them into `lib/cy3sbml-dep`,
and removes the jars of the previous version. Afterwards:

1. Set the property `jsbml.version` in `pom.xml` to the printed version, and update the
   commit in the comment next to it.
2. Run `./mvnw -B -q clean verify`.
3. Check the import of the models in Cytoscape, and note the upgrade in the release
   notes.

The header of the script documents how the current jars were built.
