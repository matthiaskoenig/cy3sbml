"""Write the distrib test models with libSBML.

The models use the SBML Level 3 distrib package, version 1: uncertainties on the
elements of the core, with every kind of uncertainty parameter and span, `var`
references, units, and a distribution with math and nested parameters; and a
comp model whose submodel has uncertainties, for the flattening. libSBML is
the reference implementation of distrib, so the models are written and validated
with it; a model with an error is not written.

```bash
uv run --project tools python tools/pycysbml/distrib_models.py \
    src/test/resources/models/distrib
```
"""

import argparse
from pathlib import Path

import libsbml

NORMAL_URL: str = "http://www.sbml.org/sbml/symbols/distrib/normal"
SKEWNESS_URL: str = "http://www.probonto.org/ontology#PROB_k0000225"
UNCERTML_URL: str = "http://www.uncertml.org/distributions/normal"


def uncertainty(
    sbase: libsbml.SBase, uid: str | None = None, name: str | None = None
) -> libsbml.Uncertainty:
    """Adds an uncertainty to the element."""
    u = sbase.getPlugin("distrib").createUncertainty()
    assert isinstance(u, libsbml.Uncertainty)
    if uid is not None:
        u.setId(uid)
    if name is not None:
        u.setName(name)
    return u


def parameter(
    parent: libsbml.Uncertainty | libsbml.UncertParameter,
    kind: int,
    value: float | None = None,
    var: str | None = None,
    units: str | None = None,
) -> libsbml.UncertParameter:
    """Adds an uncertainty parameter with a value or a var."""
    p = parent.createUncertParameter()
    assert isinstance(p, libsbml.UncertParameter)
    p.setType(kind)
    if value is not None:
        p.setValue(value)
    if var is not None:
        p.setVar(var)
    if units is not None:
        p.setUnits(units)
    return p


def span(
    parent: libsbml.Uncertainty,
    kind: int,
    lower: float | str | None = None,
    upper: float | str | None = None,
) -> libsbml.UncertSpan:
    """Adds an uncertainty span; a bound is a value (number) or a var (id)."""
    s = parent.createUncertSpan()
    assert isinstance(s, libsbml.UncertSpan)
    s.setType(kind)
    if isinstance(lower, str):
        s.setVarLower(lower)
    elif lower is not None:
        s.setValueLower(lower)
    if isinstance(upper, str):
        s.setVarUpper(upper)
    elif upper is not None:
        s.setValueUpper(upper)
    return s


def uncertainties_model() -> libsbml.SBMLDocument:
    """Uncertainties on the core elements, with all kinds of parameters and spans."""
    doc = libsbml.SBMLDocument(libsbml.SBMLNamespaces(3, 2, "distrib", 1))
    doc.setPackageRequired("distrib", True)
    model = doc.createModel()
    model.setId("distrib_uncertainties")
    model.setName("distrib uncertainties")

    c = model.createCompartment()
    c.setId("C")
    c.setSize(1.0)
    c.setUnits("litre")
    c.setConstant(True)
    parameter(
        uncertainty(c),
        libsbml.DISTRIB_UNCERTTYPE_STANDARDDEVIATION,
        value=0.1,
        units="litre",
    )

    for sid, amount in [("S1", 4.2), ("S2", 0.0)]:
        s = model.createSpecies()
        s.setId(sid)
        s.setCompartment("C")
        s.setInitialConcentration(amount)
        s.setHasOnlySubstanceUnits(False)
        s.setBoundaryCondition(False)
        s.setConstant(False)
    s1 = model.getSpecies("S1")
    u = uncertainty(s1, "u_S1_a", "measured")
    parameter(u, libsbml.DISTRIB_UNCERTTYPE_MEAN, value=4.2)
    parameter(u, libsbml.DISTRIB_UNCERTTYPE_VARIANCE, value=0.3)
    span(
        uncertainty(s1, "u_S1_b"),
        libsbml.DISTRIB_UNCERTTYPE_CONFIDENCEINTERVAL,
        3.5,
        4.9,
    )

    for pid, value in [("k1", 1.0), ("sd_k1", 0.2), ("p_ia", 0.0), ("p_rule", 0.0)]:
        p = model.createParameter()
        p.setId(pid)
        p.setValue(value)
        p.setUnits("dimensionless")
        p.setConstant(pid in ("k1", "sd_k1", "p_ia"))

    u = uncertainty(model.getParameter("k1"))
    parameter(u, libsbml.DISTRIB_UNCERTTYPE_STANDARDDEVIATION, var="sd_k1")
    span(u, libsbml.DISTRIB_UNCERTTYPE_RANGE, "sd_k1", 10.0)
    distribution = parameter(u, libsbml.DISTRIB_UNCERTTYPE_DISTRIBUTION)
    distribution.setDefinitionURL(NORMAL_URL)
    distribution.setMath(libsbml.parseL3FormulaWithModel("normal(1, sd_k1)", model))
    skew = parameter(
        distribution, libsbml.DISTRIB_UNCERTTYPE_EXTERNALPARAMETER, value=0.1
    )
    skew.setName("skew")
    skew.setDefinitionURL(SKEWNESS_URL)

    ia = model.createInitialAssignment()
    ia.setSymbol("p_ia")
    ia.setMath(libsbml.parseL3Formula("2 * k1"))
    parameter(
        uncertainty(ia), libsbml.DISTRIB_UNCERTTYPE_COEFFIENTOFVARIATION, value=0.05
    )

    rule = model.createAssignmentRule()
    rule.setVariable("p_rule")
    rule.setMath(libsbml.parseL3Formula("k1 * S1"))
    parameter(
        uncertainty(rule), libsbml.DISTRIB_UNCERTTYPE_COEFFIENTOFVARIATION, value=0.05
    )

    r = model.createReaction()
    r.setId("J0")
    r.setReversible(False)
    parameter(uncertainty(r), libsbml.DISTRIB_UNCERTTYPE_MEDIAN, value=1.5)
    reactant = r.createReactant()
    reactant.setId("sr1")
    reactant.setSpecies("S1")
    reactant.setStoichiometry(1.0)
    reactant.setConstant(True)
    parameter(
        uncertainty(reactant), libsbml.DISTRIB_UNCERTTYPE_STANDARDDEVIATION, value=0.5
    )
    product = r.createProduct()
    product.setSpecies("S2")
    product.setStoichiometry(1.0)
    product.setConstant(True)
    parameter(uncertainty(product), libsbml.DISTRIB_UNCERTTYPE_MEAN, value=1.0)

    law = r.createKineticLaw()
    law.setMath(libsbml.parseL3Formula("k1 * kl * S1"))
    external = parameter(uncertainty(law), libsbml.DISTRIB_UNCERTTYPE_EXTERNALPARAMETER)
    external.setDefinitionURL(UNCERTML_URL)
    kl = law.createLocalParameter()
    kl.setId("kl")
    kl.setValue(1.0)
    kl.setUnits("dimensionless")
    span(uncertainty(kl), libsbml.DISTRIB_UNCERTTYPE_INTERQUARTILERANGE, lower=0.1)
    return doc


def comp_model() -> libsbml.SBMLDocument:
    """A submodel with uncertainties that reference its elements, for the flattening."""
    ns = libsbml.SBMLNamespaces(3, 2)
    ns.addPackageNamespace("comp", 1)
    ns.addPackageNamespace("distrib", 1)
    doc = libsbml.SBMLDocument(ns)
    doc.setPackageRequired("comp", True)
    doc.setPackageRequired("distrib", True)

    definition = doc.getPlugin("comp").createModelDefinition()
    definition.setId("sub")
    ud = definition.createUnitDefinition()
    ud.setId("per_s")
    unit = ud.createUnit()
    unit.setKind(libsbml.UNIT_KIND_SECOND)
    unit.setExponent(-1)
    unit.setScale(0)
    unit.setMultiplier(1.0)
    for pid, value in [("sd", 0.2), ("k1", 1.0)]:
        p = definition.createParameter()
        p.setId(pid)
        p.setValue(value)
        p.setUnits("per_s")
        p.setConstant(True)
    u = uncertainty(definition.getParameter("k1"))
    parameter(u, libsbml.DISTRIB_UNCERTTYPE_STANDARDDEVIATION, var="sd", units="per_s")
    span(u, libsbml.DISTRIB_UNCERTTYPE_RANGE, "sd", "k1")

    model = doc.createModel()
    model.setId("distrib_comp")
    submodel = model.getPlugin("comp").createSubmodel()
    submodel.setId("A")
    submodel.setModelRef("sub")
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

    models = {
        "distrib_uncertainties.xml": uncertainties_model(),
        "distrib_comp.xml": comp_model(),
    }
    for file_name, doc in models.items():
        errors = validate(doc)
        if errors:
            raise SystemExit(f"{file_name} is not valid:\n" + "\n".join(errors))
        path = args.directory / file_name
        if not libsbml.writeSBMLToFile(doc, str(path)):
            raise SystemExit(f"{path} could not be written")
        print(f"{path}: written")


if __name__ == "__main__":
    main()
