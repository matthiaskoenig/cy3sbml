# Automation commands and REST API (#18)

## Goal

Make the SBML functionality of cy3sbml scriptable: from Python (Jupyter notebooks,
scripts), other programming languages and other Cytoscape apps. cy3sbml registers
Cytoscape commands in the namespace `cy3sbml`; Cytoscape exposes every command in its
command line, in automation scripts and in CyREST as `POST /v1/commands/cy3sbml/<command>`
(with the Swagger documentation of CyREST). Python examples with py4cytoscape show the
workflows, and a documentation page describes every command.

Decisions (brainstorming):

- Cytoscape commands (task factories with command service properties, `ObservableTask`
  with a JSON result), not a custom JAX-RS REST resource: the commands are available in
  CyREST, the command line and scripts at once, without new dependencies, like the
  commands of the Cytoscape core apps.
- The commands cover what the core commands cannot do: the SBML specific functionality.
  Loading of files, styles, image export, selection and table import are core commands
  (`network load file`, `vizmap apply`, `view export`, `network select`,
  `table import file`), which the Python examples use next to the cy3sbml commands.
- Python examples use py4cytoscape and are scripts (not notebooks).
- Out of scope: events from cy3sbml to an external program (for example the node
  selection); CyREST has no push channel. An external program polls the selection with the
  core commands.

## Current state

- cy3sbml registers no command. All actions are Swing actions (BioModels dialog, cofactor
  split/merge toolbar buttons, layout save/load with a file dialog, info panel).
- The logic behind the actions has no UI: `BiomodelsQuery` (search, download),
  `BiomodelLoader` (download and load a BioModel), `CofactorManager` (split, merge,
  mergeAll), `LayoutTools` (save/load positions), `SBMLManager` (network to SBML
  document, metaId to nodes).
- The import is the Cytoscape network reader of cy3sbml (`SBMLReaderTaskFactory`, and the
  COMBINE archive reader), used by `network load file` / `network load url`. Their result
  are the Cytoscape networks, not the SBML structure (which networks belong to which model,
  which is the base, kinetic, all or a layout network).
- `SBMLManager` knows a network only after its view is built (`buildCyNetworkView`).
- The Cytoscape API is 3.10.0; `work-api` has `ObservableTask`, `JSONResult` and
  `ServiceProperties`. `command-executor-api` (not a dependency yet) has the `NodeList`
  tunable type that the core commands use for node arguments; Cytoscape provides the
  string handlers of `CyNetwork` and `NodeList` arguments (`current`, a name,
  `SUID:<suid>`; `all`, `selected`, `<column>:<value>`).

## Commands

Namespace `cy3sbml`. Every command returns JSON (`COMMAND_SUPPORTS_JSON`); an error (a
missing or contradicting argument, a network that is not an SBML network, a failed
download) fails the task with a message, which CyREST returns in `errors`. The `network`
argument is a `CyNetwork` (default: the current network); `nodeList` a `NodeList` of that
network.

A network in the results is `{"suid": <SUID>, "name": <name>, "type": <type>}`, with the type
`base`, `kinetic`, `all` or `layout` from the new network column `sbmlSubnetwork`
(`SBML.SUBNETWORK_ATTR`, set by `SubnetworkBuilder` and `LayoutNetworkBuilder`), and
`other` for any other subnetwork of the root network. The name suffixes are not reliable:
Cytoscape renames the networks of a model imported twice (`BIOMD0000000012__all_1`); they
are only the fallback for networks of sessions of older versions without the column. The
networks of a model are ordered base, kinetic, all, layout, other. A model is
`{"rootNetwork": <SUID>, "name": <root network name>, "modelId", "modelName", "networks": [...]}`
(`modelId`/`modelName` empty if the model has none).

| Command | Arguments | Result |
|---|---|---|
| `import` | exactly one of `file` (path of an SBML file or COMBINE archive), `url`, `sbml` (the SBML as string), `biomodelsId` | `{"models": [<model>, ...]}` of the imported models |
| `biomodels search` | `query` | `{"matches": <n>, "models": [{"id", "name", "submissionDate", "lastModified"}]}` |
| `networks` | none | `{"models": [<model>, ...]}` of all SBML models open in Cytoscape, each with `level`, `version`, `packages` (`{"fbc": 2}`) |
| `document` | `network`; optional `file` | `{"sbml": "<SBML>"}`, or with `file` the SBML is written to the file and the result is `{"file": "<path>"}` |
| `element` | `network`; one of `nodeList`, `sbmlId`, `metaId` | `{"elements": [<element>, ...]}` |
| `nodes` | `network`; optional `sbmlIds` (comma separated) | `{"nodes": {"<sbml id>": [<node SUID>, ...]}}` for the given ids (all ids of the network if not given) |
| `cofactors split` | `network`, `nodeList` | `{"clones": [<node SUID>, ...]}` |
| `cofactors merge` | `network`; optional `nodeList` (the clones; all split nodes if not given) | `{"merged": [<node SUID>, ...]}` |
| `layout save` | `network`, `file` | `{"file": "<path>", "nodes": <number of saved positions>}` |
| `layout load` | `network`, `file` | `{"file": "<path>", "nodes": <number of positioned nodes>}` |

An element is `{"class": "<JSBML class>", "id", "name", "metaId", "sboTerm", "cvTerms":
[{"qualifier": "BQB_IS", "resources": [...]}], "notes": "<notes as text>", "nodes": [<node SUIDs in the network>]}`,
with empty strings/lists for what is not set.

### import

- `import` runs the tasks of Cytoscape's network loaders (`LoadNetworkFileTaskFactory`,
  `LoadNetworkURLTaskFactory`, `BiomodelLoader` for `biomodelsId`) in the
  `SynchronousTaskManager`, so the networks, views and styles are created as by a user
  import. Inserting the loader tasks into the task iterator of the command would add the
  results of the core loader to the command result (CyREST returns the results of all
  observable tasks as a list); a failed loader task fails the command with its error.
  `sbml` is written to a temporary file, deleted after the import (also on failure).
- The task compares the networks of the `CyNetworkManager` before and after the
  import (not the `SBMLManager`, which knows a network only with a view) and groups the new
  networks by root network; model id and name come from the `SBMLManager` (the
  `SBMLDocument` of the root network).
- No new network: the command fails (for example not an SBML file).

### element, nodes

- `element` finds the SBase of a node through its `cyId` (the metaId, `SBMLManager.getSBaseByCyId`),
  of an `sbmlId` through `SBMLDocument.getElementBySId`, of a `metaId` through
  `getElementByMetaId`. `nodes` of an element: the nodes of the network with the `cyId` of
  the element.
- `nodes` reads the node table (`sbml id`), so it works for every network of cy3sbml
  (including the layout networks, whose glyph nodes copy the `sbml id`).

### cofactors, layout

- The cofactor commands call `CofactorManager` with the view of the network (if any) and
  re-apply the style of the view, as the toolbar actions do; the actions and the commands
  share this code.
- `layout save`/`layout load` call `LayoutTools`; `saveLayoutOfViewInFile` returns the
  number of saved positions. A missing view or file fails the command. Writing a layout
  file no longer swallows errors (`XMLInterface.writeXMLFileForLayout` throws an
  `IOException`; the Save Layout button shows it).

### Registration

- Package `org.cy3sbml.commands`: one class per command (task factory with its task) and
  `Commands`, the registry of all commands (name, description, long description, example
  JSON, factory). `CyActivator` registers them in `startCore` (not in the optional GUI
  phase), as `TaskFactory` services with `COMMAND_NAMESPACE`, `COMMAND`,
  `COMMAND_DESCRIPTION`, `COMMAND_LONG_DESCRIPTION`, `COMMAND_SUPPORTS_JSON`,
  `COMMAND_EXAMPLE_JSON`.
- `BiomodelsQuery` and `BiomodelLoader` are created in `startCore` and passed to the GUI
  (BioModels dialog).
- `command-executor-api` becomes a `provided` dependency (for `NodeList`).
- JSON is written with Jackson (`ObjectMapper`), already a dependency.
- Cytoscape sets the `@Tunable` arguments by reflection: the task classes and their tunable
  members are public (checked by `CommandsTest`).

## Python examples

`examples/python/`, a uv project (`pyproject.toml` with `py4cytoscape`), checked by the
ruff/ty CI job like `tools/`. Each script runs against a running Cytoscape with cy3sbml:

1. `import_and_style.py`: import a model from a file and a BioModel, apply the style
   `cy3sbml-dark` (`vizmap apply`), export PNG images (`view export`).
2. `explore_model.py`: list the SBML networks, get the SBML document, read elements of
   nodes and of SBML ids, select the nodes of SBML ids (`network select`).
3. `cofactors_and_layout.py`: split and merge cofactor nodes, save and load a layout.
4. `map_data.py`: map a flux distribution with SBML ids onto the reactions: a data table
   joined on `sbml id` (py4cytoscape `load_table_data`) with a size mapping in a copy of the
   style, and colors as bypasses on the node SUIDs of `cy3sbml nodes` (a color mapping
   would replace the fill colors of the style for all other nodes), the "set node images via
   id mapping" of #18.
5. `biomodels_search.py`: search BioModels and import the first result.

A shared `cy3sbml_client.py` has the helper `command(name, **args)`, which posts the
arguments as JSON to `commands/cy3sbml/<name>` (`py4cytoscape.cyrest_post`; the
command-line syntax of `commands_post` cannot pass values with quotes such as an SBML
string), and `export_png`: Cytoscape draws a change of a view asynchronously, so an image
exported right after `set_visual_style` shows the old style (also with the core styles); the
helper exports until two images are identical.

## Documentation

`docs/guide/automation.md` ("Automation and REST API", in the user guide after the
packages): CyREST and py4cytoscape, the command reference (arguments, result, example per
command), the Python examples, and how styles, images and selection work with the core
commands; `import.md` links it. Release notes of 0.8.0, `CLAUDE.md` and
`docs/development/architecture.md` describe the `commands` package.

## Testing

- JUnit per command with mocked Cytoscape services and networks read with the test
  support of the reader tests: JSON results and error messages of wrong or missing
  arguments.
- `CommandsTest`: every command of the registry has a name, descriptions and an example
  JSON that parses, and is documented in `docs/guide/automation.md`.
- E2E: the Python examples run against Cytoscape 3.10 with the built app (CyREST), results
  and images checked.
