# pycysbml

Python helpers for downloading and preparing the cy3sbml test models. Run them from the
repository root, uv installs the dependencies on first use:

```bash
uv run --project tools python tools/pycysbml/bigg_download.py       # corpora/models/bigg_models
uv run --project tools python tools/pycysbml/biomodels_download.py  # corpora/models/biomodels
uv run --project tools python tools/pycysbml/graph_to_sbml.py       # resources/models/styles/graph.xml
```

The downloads write into `src/test/corpora/models/`, which the `models` test suite
(`./mvnw test -Pall-tests`) reads. See `docs/development/quality.md` for running ruff and
ty on the code.
