# pycysbml

Python helpers for downloading and preparing the cy3sbml test models. Run them from the
repository root, uv installs the dependencies on first use:

```bash
uv run --project tools python tools/pycysbml/bigg_download.py       # corpora/models/bigg_models
uv run --project tools python tools/pycysbml/biomodels_download.py  # corpora/models/biomodels
uv run --project tools python tools/pycysbml/graph_to_sbml.py       # resources/models/styles/graph.xml
uv run --project tools python tools/pycysbml/distrib_models.py \
    src/test/resources/models/distrib                               # distrib test models
uv run --project tools python tools/pycysbml/omex_models.py \
    src/test/resources/models/omex                                  # COMBINE archive test models
uv run --project tools python tools/pycysbml/fbc_v3_models.py \
    src/test/resources/models/fbc                                   # fbc version 3 test model
```

`comp_flat_reference.py` writes the libSBML flattening of the comp models, the reference of
the comp flattening tests; its arguments are in its docstring, see the section "Flattening
of comp models" in `docs/development/testing.md`.

libSBML comes from `python-libsbml-experimental`, the libSBML build with all SBML Level 3
packages (including distrib).

The downloads write into `src/test/corpora/models/`, which the `models` test suite
(`./mvnw test -Pall-tests`) reads. See `docs/development/quality.md` for running ruff and
ty on the code.
