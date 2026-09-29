"""Write the libSBML flattening of the comp test models as a JSON reference.

The SBML test suite ships no flattened models, so the ids of the elements of the
models flattened by libSBML are the reference for the flattening in cy3sbml
(JSBML `CompFlatteningConverter`). For every SBML file below the directory with
submodels, the reference lists the ids per element type, keyed by the path of the
file relative to the JSON file, and the ids each element references (math,
species of reactions, compartment of species). Files libSBML cannot flatten are
listed under `_failed` with the libSBML error.

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


def math_names(math: libsbml.ASTNode | None) -> set[str]:
    """The names of the math: the referenced ids and the called functions."""
    if math is None:
        return set()
    names: set[str] = set()
    if math.getType() in (libsbml.AST_NAME, libsbml.AST_FUNCTION):
        names.add(str(math.getName()))
    for i in range(math.getNumChildren()):
        names |= math_names(math.getChild(i))
    return names


def references(model: libsbml.Model) -> dict[str, list[str]]:
    """The ids each element references, keyed by `<type>:<id>`, each list sorted.

    Only elements with references are listed. Elements without id (events,
    constraints) are keyed by their index.
    """
    refs: dict[str, set[str]] = {}
    for species in model.getListOfSpecies():
        refs[f"species:{species.getId()}"] = {species.getCompartment()}
    for reaction in model.getListOfReactions():
        names = {
            ref.getSpecies()
            for refs_of_type in (
                reaction.getListOfReactants(),
                reaction.getListOfProducts(),
                reaction.getListOfModifiers(),
            )
            for ref in refs_of_type
        }
        if reaction.isSetKineticLaw():
            names |= math_names(reaction.getKineticLaw().getMath())
        refs[f"reaction:{reaction.getId()}"] = names
    for rule in model.getListOfRules():
        if not rule.isAlgebraic():
            refs[f"rule:{rule.getVariable()}"] = math_names(rule.getMath())
    for assignment in model.getListOfInitialAssignments():
        key = f"initialAssignment:{assignment.getSymbol()}"
        refs[key] = math_names(assignment.getMath())
    for index, event in enumerate(model.getListOfEvents()):
        names = set()
        if event.isSetTrigger():
            names |= math_names(event.getTrigger().getMath())
        if event.isSetDelay():
            names |= math_names(event.getDelay().getMath())
        if event.isSetPriority():
            names |= math_names(event.getPriority().getMath())
        for event_assignment in event.getListOfEventAssignments():
            names.add(event_assignment.getVariable())
            names |= math_names(event_assignment.getMath())
        refs[f"event:{event.getId() or f'#{index}'}"] = names
    for index, constraint in enumerate(model.getListOfConstraints()):
        refs[f"constraint:#{index}"] = math_names(constraint.getMath())
    return {key: sorted(names) for key, names in refs.items() if names}


def errors(document: libsbml.SBMLDocument) -> str:
    """The messages of the errors (not the warnings) of the document, one per line."""
    log = document.getErrorLog()
    messages: list[str] = [
        " ".join(str(log.getError(i).getMessage()).split())
        for i in range(log.getNumErrors())
        if log.getError(i).getSeverity() >= libsbml.LIBSBML_SEV_ERROR
    ]
    return "\n".join(messages)


def flatten(path: Path) -> dict[str, list[str] | dict[str, list[str]]] | str:
    """Element ids of the flattened model, or the libSBML errors."""
    document = libsbml.readSBMLFromFile(str(path))
    if document.getNumErrors(libsbml.LIBSBML_SEV_ERROR) > 0:
        return errors(document)
    properties = libsbml.ConversionProperties()
    properties.addOption("flatten comp", True)
    if document.convert(properties) != libsbml.LIBSBML_OPERATION_SUCCESS:
        return errors(document) or "conversion failed"
    model = document.getModel()
    return {**element_ids(model), "references": references(model)}


def has_submodels(path: Path) -> bool:
    return "comp:submodel" in path.read_text(encoding="utf-8", errors="replace")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("directory", type=Path, help="directory with the models")
    parser.add_argument("output", type=Path, help="JSON file to write")
    parser.add_argument("--pattern", default="*.xml", help="file name pattern")
    args = parser.parse_args()

    base = args.output.resolve().parent
    reference: dict[
        str, dict[str, list[str] | dict[str, list[str]]] | dict[str, str]
    ] = {}
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
