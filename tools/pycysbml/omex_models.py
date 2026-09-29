"""Write the COMBINE archive (OMEX) test models with pymetadata.

The SBML models are built and validated with libSBML, the archives are written with
`pymetadata.omex`:

- `single.omex`: one SBML model (master), a SED-ML file, a CSV file and `metadata.rdf`
  with a description and a creator
- `comp.omex`: a comp model (master) with an external model definition to the SBML file
  `models/sub.xml` of the archive
- `no_master.omex`: two SBML models, none of them master
- `no_sbml.omex`: only a SED-ML file

```bash
uv run --project tools python tools/pycysbml/omex_models.py \
    src/test/resources/models/omex
```
"""

import argparse
import tempfile
import zipfile
from pathlib import Path

import libsbml
from pymetadata.omex import EntryFormat, ManifestEntry, Omex

SEDML: str = """<?xml version="1.0" encoding="UTF-8"?>
<sedML xmlns="http://sed-ml.org/sed-ml/level1/version3" level="1" version="3">
  <listOfModels>
    <model id="model" source="model.xml"
           language="urn:sedml:language:sbml.level-3.version-1"/>
  </listOfModels>
</sedML>
"""

CSV: str = "time,S1\n0,10\n1,5\n"

METADATA: str = """<?xml version="1.0" encoding="UTF-8"?>
<rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
         xmlns:dcterms="http://purl.org/dc/terms/"
         xmlns:vCard="http://www.w3.org/2006/vcard/ns#">
  <rdf:Description rdf:about=".">
    <dcterms:description>A reaction with a simulation and data.</dcterms:description>
    <dcterms:creator>
      <rdf:Description>
        <vCard:hasName>
          <rdf:Description>
            <vCard:family-name>Doe</vCard:family-name>
            <vCard:given-name>Jane</vCard:given-name>
          </rdf:Description>
        </vCard:hasName>
        <vCard:hasEmail rdf:resource="mailto:jane.doe@example.org"/>
        <vCard:organization-name>Example University</vCard:organization-name>
      </rdf:Description>
    </dcterms:creator>
  </rdf:Description>
</rdf:RDF>
"""


def reaction_model(model_id: str) -> libsbml.SBMLDocument:
    """An SBML L3V1 model with the reaction S1 -> S2."""
    doc = libsbml.SBMLDocument(3, 1)
    model = doc.createModel()
    model.setId(model_id)
    c = model.createCompartment()
    c.setId("c")
    c.setSize(1.0)
    c.setConstant(True)
    for sid in ("S1", "S2"):
        s = model.createSpecies()
        s.setId(sid)
        s.setCompartment("c")
        s.setInitialAmount(10.0)
        s.setHasOnlySubstanceUnits(True)
        s.setBoundaryCondition(False)
        s.setConstant(False)
    k = model.createParameter()
    k.setId("k")
    k.setValue(0.5)
    k.setConstant(True)
    r = model.createReaction()
    r.setId("R1")
    r.setReversible(False)
    r.setFast(False)
    reactant = r.createReactant()
    reactant.setSpecies("S1")
    reactant.setStoichiometry(1.0)
    reactant.setConstant(True)
    product = r.createProduct()
    product.setSpecies("S2")
    product.setStoichiometry(1.0)
    product.setConstant(True)
    r.createKineticLaw().setMath(libsbml.parseL3Formula("k * S1"))
    return doc


def comp_model() -> libsbml.SBMLDocument:
    """A comp model with a submodel of the external model `models/sub.xml`."""
    ns = libsbml.SBMLNamespaces(3, 1, "comp", 1)
    doc = libsbml.SBMLDocument(ns)
    doc.setPackageRequired("comp", True)
    external = doc.getPlugin("comp").createExternalModelDefinition()
    external.setId("sub_def")
    external.setSource("models/sub.xml")
    external.setModelRef("sub")
    model = doc.createModel()
    model.setId("top")
    submodel = model.getPlugin("comp").createSubmodel()
    submodel.setId("A")
    submodel.setModelRef("sub_def")
    return doc


def validate_sbml(path: Path) -> None:
    """Validates the SBML file with libSBML, read from the file for relative sources."""
    doc = libsbml.readSBMLFromFile(str(path))
    doc.setConsistencyChecks(libsbml.LIBSBML_CAT_UNITS_CONSISTENCY, False)
    doc.checkConsistency()
    errors = [
        doc.getError(i).getMessage().strip()
        for i in range(doc.getNumErrors())
        if doc.getError(i).getSeverity() >= libsbml.LIBSBML_SEV_ERROR
    ]
    if errors:
        raise SystemExit(f"{path.name} is not valid:\n" + "\n".join(errors))


def reproducible(path: Path) -> None:
    """Rewrites the zip with a fixed time of every entry, so the file only changes with
    its content."""
    with zipfile.ZipFile(path) as source:
        entries = [(info.filename, source.read(info)) for info in source.infolist()]
    with zipfile.ZipFile(path, "w", zipfile.ZIP_DEFLATED) as target:
        for name, data in entries:
            info = zipfile.ZipInfo(name, date_time=(1980, 1, 1, 0, 0, 0))
            info.compress_type = zipfile.ZIP_DEFLATED
            target.writestr(info, data)


def write_omex(
    path: Path, files: dict[str, tuple[str | libsbml.SBMLDocument, str, bool]]
) -> None:
    """Writes the archive with the files: location -> (content, format, master)."""
    omex = Omex()
    with tempfile.TemporaryDirectory() as tmp:
        for location, (content, _, _) in files.items():
            file = Path(tmp) / location
            file.parent.mkdir(parents=True, exist_ok=True)
            if isinstance(content, libsbml.SBMLDocument):
                libsbml.writeSBMLToFile(content, str(file))
            else:
                file.write_text(content, encoding="utf-8")
        for location, (content, entry_format, master) in files.items():
            file = Path(tmp) / location
            if isinstance(content, libsbml.SBMLDocument):
                validate_sbml(file)
            omex.add_entry(
                file,
                ManifestEntry(
                    location=f"./{location}", format=entry_format, master=master
                ),
            )
        omex.to_omex(path)
    reproducible(path)
    print(f"{path}: written")


def main() -> None:
    parser = argparse.ArgumentParser(
        description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter
    )
    parser.add_argument(
        "directory", type=Path, help="directory the archives are written to"
    )
    args = parser.parse_args()
    directory = Path(args.directory)
    directory.mkdir(parents=True, exist_ok=True)

    sbml = EntryFormat.SBML_L3V1.value
    sedml = EntryFormat.SEDML_L1V3.value
    write_omex(
        directory / "single.omex",
        {
            "model.xml": (reaction_model("single"), sbml, True),
            "simulation.sedml": (SEDML, sedml, False),
            "data/data.csv": (CSV, EntryFormat.CSV.value, False),
            "metadata.rdf": (METADATA, EntryFormat.OMEX_METADATA.value, False),
        },
    )
    write_omex(
        directory / "comp.omex",
        {
            "top.xml": (comp_model(), sbml, True),
            "models/sub.xml": (reaction_model("sub"), sbml, False),
        },
    )
    write_omex(
        directory / "no_master.omex",
        {
            "first.xml": (reaction_model("first"), sbml, False),
            "second.xml": (reaction_model("second"), sbml, False),
        },
    )
    write_omex(directory / "no_sbml.omex", {"simulation.sedml": (SEDML, sedml, False)})


if __name__ == "__main__":
    main()
