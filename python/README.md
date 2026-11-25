# cy3sbml for Python

Visualize SBML models in Cytoscape and call the cy3sbml app's automation commands
from Python scripts and notebooks. The package uses [py4cytoscape](https://py4cytoscape.readthedocs.io/).

## Install

Use Python 3.11 or later. Start Cytoscape 3.10 or later with the
[cy3sbml app](https://apps.cytoscape.org/apps/cy3sbml) version 0.8.0 or later
installed. The Python package connects to Cytoscape's CyREST API.

```sh
python -m pip install cy3sbml
```

Install `cy3sbml[antimony]` to convert Antimony text or files to SBML before
visualization.

## Visualize a model

```python
from cy3sbml import export_image, visualize_sbml

suid = visualize_sbml("model.xml")
if suid is not None:
    export_image("model.svg", format="SVG", fit_content=True, network=suid)
```

`visualize_sbml` imports the file and selects the main model's base network.
The return value is the network SUID, or `None` with a logged warning if
Cytoscape cannot import or display the model. `visualize_antimony` accepts
Antimony text or a local file. Pass a `pathlib.Path` when you want it to check
that the file exists.

The visualizers keep the current session open by default. Passing
`delete_session=True` closes the session **without saving** before import.
The file given to `visualize_sbml` and the output path given to `export_image`
are paths on the Cytoscape computer. To connect to a different CyREST endpoint,
pass its `base_url`, including the API version (for example
`http://127.0.0.1:4321/v1`).

Cytoscape renders view changes asynchronously. If Python can read the export
file, use `export_png(path, suid)` to export the view until two successive PNGs match.

To use saved Cytoscape `boundingBox` XML positions, call
`apply_layout(read_layout_xml("layout.xml"), network=suid)`. You can also give
`apply_layout` a pandas DataFrame with `x` and `y` columns indexed by SBML ID.
Use `add_annotations` with `AnnotationText`, `AnnotationShape`, or
`AnnotationBoundedText` to add labels and shapes to the view.

## Call app commands

```python
from cy3sbml import command, network, network_of_type

model = command("import", biomodelsId="BIOMD0000000012")["models"][0]
base = network_of_type(model, "base")
print(command("element", network=network(base), sbmlId="PX")["elements"])
```

See the [automation guide](https://matthiaskoenig.github.io/cy3sbml/guide/automation/)
for command arguments and results.

## Develop

From this directory, use [uv](https://docs.astral.sh/uv/) to run the offline tests
and build the package.

```sh
uv sync --locked --extra antimony
uv run --no-sync ruff check
uv run --no-sync ruff format --check
uv run --no-sync ty check
uv run --no-sync pytest
uv build --no-sources
uv run --no-sync twine check --strict dist/*
```

See the [Python release procedure](https://github.com/matthiaskoenig/cy3sbml/blob/develop/docs/development/release.md#python-package)
for publishing.
