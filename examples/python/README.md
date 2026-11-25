# cy3sbml Python examples

Examples of the automation commands of cy3sbml with the
[cy3sbml Python package](../../python/README.md). The commands are
documented in the
[Automation and REST API guide](https://matthiaskoenig.github.io/cy3sbml/guide/automation/).

## Setup

1. Start Cytoscape (3.10 or newer) with the cy3sbml app (0.8.0 or newer)
   installed. CyREST, which is part of Cytoscape, listens on `http://localhost:1234`.
2. Install [uv](https://docs.astral.sh/uv/) and run the examples in this
   directory. On the first run, uv installs the Python package from
   `../../python` and its dependencies.

The examples read the test models of the cy3sbml repository
(`src/test/resources/models`) and write their images to `results/`. The files
are read and written by Cytoscape, so Cytoscape and the examples run on the same
computer.

## Examples

| Script | Commands |
|---|---|
| `import_and_style.py` | import a file and a BioModel, apply a style and export images |
| `explore_model.py` | list the SBML models, get the SBML, read elements, select nodes by SBML id |
| `cofactors_and_layout.py` | split and merge cofactor nodes, save and load node positions |
| `map_data.py` | map data with SBML ids onto the nodes (table import, style mapping, bypasses) |
| `biomodels_search.py` | search BioModels and import a model |

```bash
cd examples/python
uv run import_and_style.py
uv run explore_model.py
uv run cofactors_and_layout.py
uv run map_data.py
uv run biomodels_search.py repressilator
```

The Python package provides `command(name, **arguments)` to run a cy3sbml
command through CyREST and return its JSON result:

```python
from cy3sbml import command, network, network_of_type

model = command("import", biomodelsId="BIOMD0000000012")["models"][0]
base = network_of_type(model, "base")
print(command("element", network=network(base), sbmlId="PX")["elements"])
```

## Checks

```bash
uv run ruff check . && uv run ruff format --check . && uv run ty check
```
