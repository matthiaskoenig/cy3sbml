"""Write the fbc version 3 test model with libSBML.

The model uses the SBML Level 3 fbc package, version 3: the user defined
constraints of the example of the specification (`RGLX - RBTK = 5` and
`2 * p1var - RGDP >= 2`), a quadratic constraint component with a second
variable, flux objectives with a linear and a quadratic variable type,
key-value pairs on the model, a species, a reaction and a user defined
constraint, and the elements of fbc version 2 (flux bounds, gene products and
associations, charges and chemical formulas). libSBML is the reference
implementation of fbc, so the model is written and validated with it; a model
with an error is not written.

```bash
uv run --project tools python tools/pycysbml/fbc_v3_models.py \
    src/test/resources/models/fbc
```
"""

import argparse
from pathlib import Path

import libsbml

KVP_URI: str = "https://github.com/matthiaskoenig/cy3sbml/kvp"


def parameter(
    model: libsbml.Model, pid: str, value: float | None, constant: bool = True
) -> libsbml.Parameter:
    """Adds a parameter; a parameter without value has no value attribute."""
    p = model.createParameter()
    assert isinstance(p, libsbml.Parameter)
    p.setId(pid)
    if value is not None:
        p.setValue(value)
    p.setConstant(constant)
    return p


def species(
    model: libsbml.Model, sid: str, charge: int, formula: str
) -> libsbml.Species:
    """Adds a species with an fbc charge and chemical formula."""
    s = model.createSpecies()
    assert isinstance(s, libsbml.Species)
    s.setId(sid)
    s.setMetaId(f"meta_{sid}")
    s.setName(sid)
    s.setCompartment("c")
    s.setInitialConcentration(0)
    s.setHasOnlySubstanceUnits(False)
    s.setBoundaryCondition(False)
    s.setConstant(False)
    plugin = s.getPlugin("fbc")
    # the charge of fbc v3 is a double: libSBML 5.21 writes 0 for an int
    plugin.setCharge(float(charge))
    plugin.setChemicalFormula(formula)
    return s


def reaction(
    model: libsbml.Model,
    rid: str,
    reactant: str,
    product: str,
    lower: str,
    upper: str,
) -> libsbml.Reaction:
    """Adds an irreversible reaction with fbc flux bounds."""
    r = model.createReaction()
    assert isinstance(r, libsbml.Reaction)
    r.setId(rid)
    r.setMetaId(f"meta_{rid}")
    r.setReversible(lower != "zero")
    r.setFast(False)
    for sid, create in ((reactant, r.createReactant), (product, r.createProduct)):
        ref = create()
        ref.setSpecies(sid)
        ref.setStoichiometry(1)
        ref.setConstant(True)
    plugin = r.getPlugin("fbc")
    plugin.setLowerFluxBound(lower)
    plugin.setUpperFluxBound(upper)
    return r


def key_value_pair(
    sbase: libsbml.SBase,
    key: str,
    value: str | None = None,
    uri: str | None = KVP_URI,
    kid: str | None = None,
    name: str | None = None,
) -> None:
    """Adds a key-value pair to the annotation of the element."""
    kvp = sbase.getPlugin("fbc").createKeyValuePair()
    kvp.setKey(key)
    if value is not None:
        kvp.setValue(value)
    if uri is not None:
        kvp.setUri(uri)
    if kid is not None:
        kvp.setId(kid)
    if name is not None:
        kvp.setName(name)


def constraint(
    fbc: libsbml.FbcModelPlugin,
    cid: str,
    lower: str,
    upper: str,
    components: list[tuple[str, str, str | None, str]],
    name: str | None = None,
) -> libsbml.UserDefinedConstraint:
    """Adds a user defined constraint with components
    (coefficient, variable, variable2, variable type)."""
    udc = fbc.createUserDefinedConstraint()
    assert isinstance(udc, libsbml.UserDefinedConstraint)
    udc.setId(cid)
    udc.setMetaId(f"meta_{cid}")
    if name is not None:
        udc.setName(name)
    udc.setLowerBound(lower)
    udc.setUpperBound(upper)
    for k, (coefficient, variable, variable2, variable_type) in enumerate(
        components, start=1
    ):
        c = udc.createUserDefinedConstraintComponent()
        c.setId(f"{cid}_c{k}")
        c.setCoefficient(coefficient)
        c.setVariable(variable)
        if variable2 is not None:
            c.setVariable2(variable2)
        c.setVariableType(variable_type)
    return udc


def fbc_v3_model() -> libsbml.SBMLDocument:
    """The fbc v3 example model."""
    doc = libsbml.SBMLDocument(libsbml.SBMLNamespaces(3, 1, "fbc", 3))
    doc.setPackageRequired("fbc", False)
    model = doc.createModel()
    model.setId("fbc_v3_example")
    model.setMetaId("meta_fbc_v3_example")
    model.setName("fbc version 3 example")
    fbc = model.getPlugin("fbc")
    # non-constant parameters as variables are not allowed in strict mode
    fbc.setStrict(False)
    key_value_pair(model, "author", "cy3sbml")
    key_value_pair(model, "created", "2026-09-29", uri=None, kid="kvp_created")

    c = model.createCompartment()
    c.setId("c")
    c.setSize(1)
    c.setConstant(True)
    species(model, "A", -1, "C6H12O6")
    species(model, "B", 0, "C3H6O3")
    species(model, "C", -2, "C3H4O3")
    s = species(model, "D", 0, "CO2")
    key_value_pair(s, "compartment_label", "cytosol", name="compartment label")

    for pid, value in (
        ("zero", 0),
        ("lb", -1000),
        ("ub", 1000),
        ("inf", float("inf")),
        ("five", 5),
        ("two", 2),
        ("one", 1),
        ("negone", -1),
    ):
        parameter(model, pid, value)
    parameter(model, "p1var", None, constant=False)

    r = reaction(model, "RGLX", "A", "B", "zero", "ub")
    key_value_pair(r, "confidence", "4")
    key_value_pair(r, "curated", uri=None)
    reaction(model, "RBTK", "B", "C", "lb", "ub")
    reaction(model, "RGDP", "C", "D", "zero", "ub")

    # gene products and associations
    for gid in ("g1", "g2", "g3"):
        gp = fbc.createGeneProduct()
        gp.setId(gid)
        gp.setMetaId(f"meta_{gid}")
        gp.setLabel(gid.upper())
    fbc.getGeneProduct("g1").setAssociatedSpecies("A")
    for rid, rule in (("RGLX", "g1 and g2"), ("RBTK", "g3 or g1")):
        gpa = model.getReaction(rid).getPlugin("fbc").createGeneProductAssociation()
        gpa.setAssociation(rule, True, False)

    # objectives with a linear and a quadratic flux objective
    for oid, kind, rid, variable_type in (
        ("obj_linear", "maximize", "RGDP", "linear"),
        ("obj_quadratic", "minimize", "RGLX", "quadratic"),
    ):
        o = fbc.createObjective()
        o.setId(oid)
        o.setType(kind)
        f = o.createFluxObjective()
        f.setReaction(rid)
        f.setCoefficient(1)
        f.setVariableType(variable_type)
    fbc.setActiveObjectiveId("obj_linear")

    # user defined constraints of the specification example and a quadratic one
    udc = constraint(
        fbc,
        "uc1",
        "five",
        "five",
        [("one", "RGLX", None, "linear"), ("negone", "RBTK", None, "linear")],
        name="RGLX - RBTK = 5",
    )
    key_value_pair(udc, "source", "fbc v3 specification")
    constraint(
        fbc,
        "uc2",
        "two",
        "inf",
        [("two", "p1var", None, "linear"), ("negone", "RGDP", None, "linear")],
    )
    constraint(fbc, "uc3", "zero", "ub", [("one", "RGLX", "RBTK", "quadratic")])
    return doc


def validate(doc: libsbml.SBMLDocument) -> list[str]:
    """The errors of the document (unit consistency is not checked)."""
    doc.setConsistencyChecks(libsbml.LIBSBML_CAT_UNITS_CONSISTENCY, False)
    doc.checkConsistency()
    return [
        f"{e.getErrorId()} line {e.getLine()}: {e.getMessage().strip()}"
        for e in (doc.getError(i) for i in range(doc.getNumErrors()))
        if e.getSeverity() >= libsbml.LIBSBML_SEV_ERROR
    ]


def main() -> None:
    parser = argparse.ArgumentParser(
        description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter
    )
    parser.add_argument(
        "directory", type=Path, help="directory the models are written to"
    )
    args = parser.parse_args()

    models = {"fbc_v3_example_L3V1_fbcV3.xml": fbc_v3_model()}
    for file_name, doc in models.items():
        errors = validate(doc)
        if errors:
            raise SystemExit(f"{file_name} is not valid:\n" + "\n".join(errors))
        path = args.directory / file_name
        if not libsbml.writeSBMLToFile(doc, str(path)):
            raise SystemExit(f"{path} could not be written")
        # validate the written file as well: what is read back is what the tests see
        errors = validate(libsbml.readSBMLFromFile(str(path)))
        if errors:
            raise SystemExit(f"{path} is not valid:\n" + "\n".join(errors))
        print(f"{path}: written")


if __name__ == "__main__":
    main()
