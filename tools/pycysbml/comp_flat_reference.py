"""Write the libSBML flattening of the comp test models as a JSON reference.

The SBML test suite ships no flattened models, so the ids of the elements of the
models flattened by libSBML are the reference for the flattening in cy3sbml
(JSBML `CompFlatteningConverter`). For every SBML file below the directory with
submodels, the reference lists the ids per element type, keyed by the path of the
file relative to the JSON file. Files libSBML cannot flatten are listed under
`_failed` with the libSBML error.

```bash
uv run --project tools python tools/pycysbml/comp_flat_reference.py \
    src/test/corpora/models/sbml-test-suite/semantic \
    src/test/corpora/models/sbml-test-suite/comp-flat-reference.json \
    --pattern '*-sbml-l3v1.xml'
uv run --project tools python tools/pycysbml/comp_flat_reference.py \
    src/test/resources/models/comp \
    src/test/resources/models/comp/comp-flat-reference.json
```
"""

import argparse
import json
from pathlib import Path

import libsbml

FAILED_KEY: str = "_failed"


def _ids(elements: libsbml.ListOf) -> list[str]:
    return sorted(element.getId() for element in elements)


def element_ids(model: libsbml.Model) -> dict[str, list[str]]:
    """Ids of the elements of the model per element type, each list sorted."""
    return {
        "compartments": _ids(model.getListOfCompartments()),
        "events": _ids(model.getListOfEvents()),
        "functionDefinitions": _ids(model.getListOfFunctionDefinitions()),
        "initialAssignments": sorted(
            assignment.getSymbol() for assignment in model.getListOfInitialAssignments()
        ),
        "parameters": _ids(model.getListOfParameters()),
        "reactions": _ids(model.getListOfReactions()),
        "rules": sorted(
            rule.getVariable()
            for rule in model.getListOfRules()
            if not rule.isAlgebraic()
        ),
        "species": _ids(model.getListOfSpecies()),
        "unitDefinitions": _ids(model.getListOfUnitDefinitions()),
    }


def errors(document: libsbml.SBMLDocument) -> str:
    """The messages of the errors (not the warnings) of the document, one per line."""
    log = document.getErrorLog()
    messages: list[str] = [
        " ".join(str(log.getError(i).getMessage()).split())
        for i in range(log.getNumErrors())
        if log.getError(i).getSeverity() >= libsbml.LIBSBML_SEV_ERROR
    ]
    return "\n".join(messages)


def flatten(path: Path) -> dict[str, list[str]] | str:
    """Element ids of the flattened model, or the libSBML errors."""
    document = libsbml.readSBMLFromFile(str(path))
    if document.getNumErrors(libsbml.LIBSBML_SEV_ERROR) > 0:
        return errors(document)
    properties = libsbml.ConversionProperties()
    properties.addOption("flatten comp", True)
    if document.convert(properties) != libsbml.LIBSBML_OPERATION_SUCCESS:
        return errors(document) or "conversion failed"
    return element_ids(document.getModel())


def has_submodels(path: Path) -> bool:
    return "comp:submodel" in path.read_text(encoding="utf-8", errors="replace")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("directory", type=Path, help="directory with the models")
    parser.add_argument("output", type=Path, help="JSON file to write")
    parser.add_argument("--pattern", default="*.xml", help="file name pattern")
    args = parser.parse_args()

    base = args.output.resolve().parent
    reference: dict[str, dict[str, list[str]] | dict[str, str]] = {}
    failed: dict[str, str] = {}
    for path in sorted(args.directory.resolve().rglob(args.pattern)):
        if not has_submodels(path):
            continue
        key = path.relative_to(base).as_posix()
        result = flatten(path)
        if isinstance(result, str):
            failed[key] = result
        else:
            reference[key] = result
    if failed:
        reference[FAILED_KEY] = failed
    args.output.write_text(
        json.dumps(reference, indent=1, sort_keys=True) + "\n", encoding="utf-8"
    )
    flattened = len(reference) - bool(failed)
    print(f"{args.output}: {flattened} flattened, {len(failed)} failed")


if __name__ == "__main__":
    main()
