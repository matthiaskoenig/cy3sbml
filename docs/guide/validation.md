# Validation

cy3sbml does not validate SBML. It imports every file that JSBML can read, also files
with SBML errors, and converts what it finds. Check your models with an SBML validator
before you rely on the import:

- the online [SBML validator](https://sbml.org/facilities/validator/) of the SBML team,
- [libSBML](https://sbml.org/software/libsbml/), for example with `python-libsbml`,
- [sbmlutils](https://github.com/matthiaskoenig/sbmlutils), which reports validation and
  modeling practice warnings.

## Files that cannot be read

If JSBML cannot read a file, for example because the XML is not well-formed or the file
is not SBML, the import fails. Cytoscape shows one error message that names the file, a
short cause (for XML errors with the line in the file), and the link to the SBML
validator. No network is created. If the validator accepts the file, please
[report the problem](https://github.com/matthiaskoenig/cy3sbml/issues) with the file
attached.

## Annotation checks

The [info panel](info-panel.md#annotations) checks the annotations of the selected
object against the identifiers.org registry. It warns if an identifier does not match
the identifier pattern of its data collection, and if a data collection is not in the
registry.

## Import warnings

Content that cy3sbml cannot convert is skipped with a warning in the log file
(`~/CytoscapeConfiguration/cy3sbml/cy3sbml-v<version>.log`), for example an SBML
layout, or an external file of the `comp` package that cannot be read. See
[Supported SBML packages](packages.md).
