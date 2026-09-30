# Automation and REST API

cy3sbml can be driven from scripts and other programs. From Python (a script or a Jupyter
notebook), R or any other language you can import SBML models, read the SBML behind the
networks and map your data onto them.

cy3sbml registers Cytoscape commands in the namespace `cy3sbml`. Cytoscape offers every
command in three places:

- in the **Command Line** of Cytoscape (**View → Show Command Panel**), for example
  `cy3sbml import biomodelsId=BIOMD0000000012`;
- in automation scripts (**Tools → Execute Command File**);
- in [CyREST](https://github.com/cytoscape/cyREST), the REST API of Cytoscape, as
  `POST http://localhost:1234/v1/commands/cy3sbml/<command>` with the arguments as JSON
  body. **Help → Automation → CyREST Command API** opens the interactive documentation
  (Swagger) of all commands, including those of cy3sbml.

Every cy3sbml command returns JSON. In CyREST the result is `data`, and an error (a wrong
argument, a network that is not an SBML network, a failed download) is a message in
`errors`:

```bash
curl -X POST -H "Content-Type: application/json" \
    -d '{"biomodelsId": "BIOMD0000000012"}' \
    http://localhost:1234/v1/commands/cy3sbml/import
```

```json
{
  "data": {
    "models": [{
      "rootNetwork": 52, "name": "BIOMD0000000012", "modelId": "BIOMD0000000012",
      "modelName": "Elowitz2000 - Repressilator",
      "networks": [
        {"suid": 1024, "name": "BIOMD0000000012", "type": "base"},
        {"suid": 1167, "name": "BIOMD0000000012__kinetic", "type": "kinetic"},
        {"suid": 1310, "name": "BIOMD0000000012__all", "type": "all"}
      ]
    }]
  },
  "errors": []
}
```

The cy3sbml commands cover what is specific to SBML. Styles, layouts, images, the
selection and tables are handled by the commands of Cytoscape itself, for example
`vizmap apply`, `layout force-directed`, `view export`, `network select` and
`table import file`, or the corresponding functions of
[py4cytoscape](https://py4cytoscape.readthedocs.io) and
[RCy3](https://bioconductor.org/packages/RCy3/).

## Python

The [Python examples](https://github.com/matthiaskoenig/cy3sbml/tree/develop/examples/python)
use py4cytoscape. `cy3sbml_client.py` sends the arguments of a command as JSON through
CyREST and returns its result:

````python
--8<-- "examples/python/cy3sbml_client.py"
````

For example, `explore_model.py` imports a model, reads the SBML, reads SBML elements and
selects the nodes of SBML ids:

````python
--8<-- "examples/python/explore_model.py"
````

The other examples:

| Script | What it shows |
|---|---|
| `import_and_style.py` | import a file and a BioModel, apply the style `cy3sbml-dark`, export PNG images |
| `cofactors_and_layout.py` | split and merge cofactor nodes, save and restore the node positions |
| `map_data.py` | map a flux distribution with SBML ids onto the reactions: a data column joined on `sbml id` with a style mapping, and colors on the node SUIDs of the SBML ids |
| `biomodels_search.py` | search BioModels and import the first result |

Run them with [uv](https://docs.astral.sh/uv/) while Cytoscape with cy3sbml runs on the
same computer (the files are read and written by Cytoscape):

```bash
cd examples/python
uv run map_data.py
```

Cytoscape draws a change of a view (a style, a layout, bypasses) asynchronously. An image
exported right after the change can still show the view before the change; the helper
`export_png` of the examples exports until two images are identical.

## Arguments

- `network`: a network of an SBML model imported by cy3sbml, the base, kinetic, all or a
  layout network. It is given as the network name, `SUID:<SUID>` or `current` (the default,
  the current network).
- `nodeList`: nodes of the network: `all`, `selected`, `unselected`, a comma separated list
  of node names, or `<column>:<value>`, for example `sbml id:PX` or `SUID:1047`.
- File paths are paths on the computer Cytoscape runs on.

The networks of a model in the results have a `type`: `base`, `kinetic`, `all` or `layout`,
the value of the network column `sbmlSubnetwork` (see [Network model](network.md)). The
types do not depend on the network names, which Cytoscape changes when a model is imported
twice (for example `BIOMD0000000012_1`).

## Commands

### `cy3sbml import`

Imports an SBML model like an import in the GUI: the base, kinetic and all network and a
network per layout, with views, the cy3sbml style and the layout. A COMBINE archive (OMEX)
imports its SBML models.

| Argument | Description |
|---|---|
| `file` | path of an SBML file or a COMBINE archive |
| `url` | http or https URL of an SBML file or a COMBINE archive |
| `sbml` | the SBML as a string |
| `biomodelsId` | id of a BioModels model, which is downloaded and imported |

Give exactly one of the arguments. The result has the imported models, each with its root
network SUID, model id and name, and its networks (SUID, name, type), as in the example
above. The command fails if nothing is imported, for example for a file that is no SBML.
Other URLs than http and https (for example `file:` URLs) are rejected, give a local file
with `file`. A COMBINE archive is imported only if it has at most 100000 entries and its
unpacked files have at most 4 GiB in total.

### `cy3sbml biomodels search`

Searches [BioModels](https://www.biomodels.org) like the BioModels dialog.

| Argument | Description |
|---|---|
| `query` | the search query, for example a model name, a species or a gene |

```json
{"matches": 2, "models": [
  {"id": "BIOMD0000000012", "name": "Elowitz2000 - Repressilator",
   "submissionDate": "2005-09-13T00:00:00Z", "lastModified": ""}, ...]}
```

At most 1000 models are returned; `matches` is the number of all matching models. Import a
model with `cy3sbml import biomodelsId=<id>`.

### `cy3sbml networks`

Lists the SBML models open in Cytoscape. No arguments. Per model: the root network, model
id and name, the SBML `level` and `version`, the `packages` with their versions (for example
`{"fbc": 2}`) and the networks.

### `cy3sbml document`

The SBML document of the model of a network.

| Argument | Description |
|---|---|
| `network` | the network, default: the current network |
| `file` | optional path the SBML is written to |

Without `file` the result is `{"sbml": "<SBML>"}`, with `file` `{"file": "<path>"}`.

### `cy3sbml element`

The SBML elements of nodes, of an SBML id or of a metaid.

| Argument | Description |
|---|---|
| `network` | the network, default: the current network |
| `nodeList` | the nodes, for example `selected` |
| `sbmlId` | the SBML id (SId) of an element |
| `metaId` | the metaid of an element |

Give one of `nodeList`, `sbmlId` and `metaId`. Per element: the JSBML class, `id`, `name`,
`metaId`, `sboTerm`, the `cvTerms` of the annotation (qualifier and resources), the `notes`
(XHTML) and the SUIDs of its `nodes` in the network:

```json
{"elements": [{"class": "Species", "id": "PX", "name": "LacI protein",
  "metaId": "_000006", "sboTerm": "SBO:0000252",
  "cvTerms": [{"qualifier": "BQB_IS_VERSION_OF",
               "resources": ["http://identifiers.org/uniprot/P03023"]}],
  "notes": "", "nodes": [1047]}]}
```

### `cy3sbml nodes`

The nodes of SBML ids in a network (the column `sbml id`), to map data with SBML ids onto
the nodes.

| Argument | Description |
|---|---|
| `network` | the network, default: the current network |
| `sbmlIds` | comma separated SBML ids; default: all SBML ids of the network |

```json
{"nodes": {"PX": [1047], "PY": [1049], "missing": []}}
```

An id without node in the network has an empty list. An element can have several nodes, for
example the clones of a split cofactor node or the aliases of a layout network.

### `cy3sbml cofactors split`

Splits nodes into one node per edge, like **Split cofactor nodes** (see
[Cofactor nodes](cofactors.md)).

| Argument | Description |
|---|---|
| `network` | the network, default: the current network |
| `nodeList` | the nodes to split |

The result has the SUIDs of the new nodes: `{"clones": [2101, 2102, 2103]}`.

### `cy3sbml cofactors merge`

Merges split nodes back into their node, like **Merge cofactor nodes**.

| Argument | Description |
|---|---|
| `network` | the network, default: the current network |
| `nodeList` | the split nodes to merge; default: all split nodes of the network |

The result has the SUIDs of the merged nodes: `{"merged": [1047]}`.

### `cy3sbml layout save`

Saves the node positions of the view of a network in a layout file, like **Save Layout**
(see [Layouts](layouts.md)).

| Argument | Description |
|---|---|
| `network` | the network, default: the current network |
| `file` | path of the layout file (XML) |

The result has the file and the number of saved positions: `{"file": "<path>", "nodes": 12}`.

### `cy3sbml layout load`

Moves the nodes of the view of a network to their positions in a layout file, like
**Load Layout**.

| Argument | Description |
|---|---|
| `network` | the network, default: the current network |
| `file` | path of the layout file (XML) |

The result has the file and the number of moved nodes: `{"file": "<path>", "nodes": 12}`.
