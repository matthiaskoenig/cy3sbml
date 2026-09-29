# Automation commands (#18) implementation plan

Spec: `superpowers/specs/2026-09-30-automation-design.md`. Branch `automation-commands`.

## Task 1: foundation

1. `pom.xml`: `command-executor-api` (`provided`, `cytoscape.api.version`).
2. `commands/CommandJson` (Jackson `ObjectMapper`, `JSONResult` of a map), `commands/CommandNetworks`
   (network/model JSON, network type from the name suffix, the SBML document of a network
   or an error).
3. `commands/AbstractJsonTask` (an `ObservableTask` returning a JSON `String` and
   `JSONResult`).
4. `commands/Commands`: registry (record `Command(namespace, name, description,
   longDescription, exampleJson, factory)`), `register` used by `CyActivator`.

## Task 2: read commands

`NetworksCommand`, `DocumentCommand`, `ElementCommand`, `NodesCommand`,
`BiomodelsSearchCommand`, each with tests (mocked `CyApplicationManager`,
`CyNetworkManager`; networks from `ReaderTestSupport`/`NetworkTestSupport`).

## Task 3: change commands

1. `ImportCommand` (+ result task), with tests of the argument checks and the result of
   the new networks.
2. `CofactorsSplitCommand`, `CofactorsMergeCommand`: move the shared view/style code of
   `AbstractCofactorAction` into `CofactorManager` users (a static helper).
3. `LayoutSaveCommand`, `LayoutLoadCommand`; `LayoutTools.saveLayoutOfViewInFile` returns
   the count.

## Task 4: wiring

`CyActivator.startCore`: `BiomodelsQuery`, `BiomodelLoader`, `Commands.register`; GUI uses
the loader of the core. `CommandsTest` (registry complete, example JSON parses, documented).

## Task 5: Python examples

`examples/python/` uv project, scripts, ruff/ty in CI (`ci.yml` python job), README.

## Task 6: docs

`docs/guide/automation.md`, nav in `zensical.toml`, link from `import.md`, release notes,
`CLAUDE.md`, `architecture.md`.

## Task 7: verify

`./mvnw -B -q verify`, lint profile, javadoc profile, docs build; E2E: build, hot reload in
Cytoscape, run every example against CyREST, check results and images; PR closing #18.
