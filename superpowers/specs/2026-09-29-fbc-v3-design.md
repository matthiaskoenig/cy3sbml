# fbc version 3 support (#461) and javadoc in releases (#460)

## Goal

1. Read and display SBML models of the `fbc` package version 3
   (`http://www.sbml.org/sbml/level3/version1/fbc/version3`), the version libSBML (5.19 and
   newer) and COBRApy write, with everything cy3sbml shows for fbc version 2 plus the new
   elements of version 3: user defined constraints, the variable type of flux objectives and
   key-value pairs.
2. Build the javadoc of cy3sbml without errors and attach the javadoc jar to every GitHub
   release (#460).

## Current state

- JSBML (the pinned fork commit, and `sbmlteam/jsbml` `master`) knows the fbc namespaces of
  version 1 and 2 only. An fbc v3 model reads with the fbc elements as unknown XML: no gene
  products, objectives, flux bounds, charges or formulas, so cy3sbml shows none of it. The
  upstream branch `fbcv3draft_implementation` (2020) is an unfinished key-value pair draft
  and is not used.
- libSBML 5.21 implements fbc v3 as in the specification (`sbml-specifications`,
  `sbml-level-3/version-1/fbc/spec/syntax_user_constraints.tex`):
  - `fbc:listOfUserDefinedConstraints` on the model with `fbc:userDefinedConstraint`
    (optional `id`, `name`; required `lowerBound`, `upperBound`: SIdRefs of parameters) and
    its `fbc:listOfUserDefinedConstraintComponents` with
    `fbc:userDefinedConstraintComponent` (optional `id`, `name`; required `coefficient`: an
    SIdRef of a parameter; required `variable`, optional `variable2`: SIdRefs of a reaction
    or a non-constant parameter; required `variableType`: `linear` or `quadratic`).
  - `fbc:variableType` (`linear`, `quadratic`) on `fbc:fluxObjective`.
  - Key-value pairs: a `listOfKeyValuePairs` element in the namespace
    `http://sbml.org/fbc/keyvaluepair` in the annotation of any SBase, with `keyValuePair`
    children (required `key`; optional `value`, `uri`, `id`, `name`; attributes without
    prefix).
- `mvn javadoc:jar` fails with 6 doclint errors (unescaped `&`, `<->`, `<notes>` and
  `<body>` in comments); the release workflow uploads only the app jar and its checksums.

## JSBML (upstream)

Branch `fbc-v3` of the fork `matthiaskoenig/jsbml`, from `sbmlteam/jsbml` `master` (so it
can be proposed upstream), merged into the fork branch `cy3sbml`; every change with a JSBML
test. The comp flattening lives on the fork only (branch `comp-fixes` and descendants), so
the fbc v3 references of the flattening go on a branch `comp-fbc-v3-flattening` from
`comp-distrib-flattening`, also merged into `cy3sbml`.

### Namespace

- `FBCConstants.namespaceURI_L3V1V3`, in `FBCConstants.namespaces`, returned by
  `FBCConstants.getNamespaceURI(3, 1, 3)` and `FBCParser.getNamespaceFor(3, 1, 3)`.
- `FBCConstants.namespaceURI` (the namespace of new documents) stays version 2: writing
  version 3 is opt-in, existing JSBML users see no change.
- Every place that distinguishes fbc v1 from v2 treats v3 like v2 (`>= 2`, not `== 2`).

### User defined constraints

- `FBCVariableType` enum (`LINEAR` / `linear`, `QUADRATIC` / `quadratic`) with
  `fromString`.
- `UserDefinedConstraint` (`AbstractNamedSBase`, `UniqueNamedSBase`, id optional):
  `lowerBound`, `upperBound` (String, with `get…Instance()` returning the `Parameter`),
  `ListOf<UserDefinedConstraintComponent> listOfUserDefinedConstraintComponents` with the
  usual `create…/add…/get…/get…Count/isSet…` methods.
- `UserDefinedConstraintComponent` (`AbstractNamedSBase`, `UniqueNamedSBase`, id
  optional): `coefficient` (String, `getCoefficientInstance()` → `Parameter`),
  `variable`, `variable2` (String, `get…Instance()` → the `Reaction` or `Parameter`, as
  `NamedSBase`), `variableType` (`FBCVariableType`).
- `FBCModelPlugin`: `listOfUserDefinedConstraints` with `create/add/get/getCount/isSet/
  unset` methods, a child of the plugin (tree, `getChildAt`, `clone`, `equals`,
  `hashCode`).
- `FluxObjective`: optional `variableType`, read and written with the `fbc` prefix, only
  when set.
- `FBCParser` reads and writes the two new lists and elements.
- Writing keeps the element order of libSBML (objectives, gene products, user defined
  constraints).

### Key-value pairs

The key-value pairs stay in the non-RDF annotation XML, which JSBML already reads and
writes unchanged; there is no second copy that could get out of sync. The fbc package
gives a typed view of them:

- `KeyValuePair`: a value class (`key`, `value`, `uri`, `id`, `name`, `equals`,
  `hashCode`, `toString`).
- `KeyValuePairs.get(SBase)`: the pairs of the SBase in document order, empty if it has
  none (no annotation, no `listOfKeyValuePairs` in the key-value pair namespace).
- `KeyValuePairs.set(SBase, List<KeyValuePair>)`: replaces the `listOfKeyValuePairs` of the
  annotation (removes it for an empty list), leaving the rest of the annotation unchanged;
  writes the XML libSBML writes.

### Validation

`SBMLDocument.checkConsistencyOffline()` reports no fbc errors for the valid v3 test model
(the fbc constraints of v2 apply to v3 as well).

### comp flattening

`CompFlatteningConverter.PACKAGE_SID_REFERENCES` gets `fbc:lowerBound`, `fbc:upperBound`,
`fbc:coefficient`, `fbc:variable`, `fbc:variable2`, so the references of user defined
constraints of submodels are renamed. `fbc:coefficient` of a flux objective is a number and
must stay unchanged (test).

## cy3sbml

- Re-pin JSBML with `scripts/update_jsbml.py --repository
  https://github.com/matthiaskoenig/jsbml <cy3sbml commit>`.
- Test model `src/test/resources/models/fbc/fbc_v3_example_L3V1_fbcV3.xml`, written by
  libSBML from a new `tools/pycysbml/fbc_v3_models.py`: the two constraints of the
  specification example (`RGLX - RBTK = 5`, `2 * p1var - RGDP >= 2`), a quadratic
  component with `variable2`, flux objectives with a linear and a quadratic variable type,
  gene products with associations, species charges and formulas, and key-value pairs on the
  model, a reaction, a species and a user defined constraint. Pinned by a golden snapshot.

### FbcReader

- Everything read for fbc v2 is read for v3 (the plugin is version independent).
- A user defined constraint is a node, type `fbc_userDefinedConstraint`
  (`SBML.NODETYPE_FBC_USER_DEFINED_CONSTRAINT`), with the named SBase attributes and the
  columns `fbc_lowerBound` and `fbc_upperBound` (the parameter ids). Label: name, else id,
  else `UDC`.
- Edges, interaction `parameter_userDefinedConstraint`, from the lower and upper bound
  parameters to the constraint node (like the flux bound edges to reactions).
- Per component an edge from the node of `variable` (and one from the node of
  `variable2`, if set) to the constraint node, interaction `variable_userDefinedConstraint`,
  with the edge columns `fbc_coefficient` (the parameter id) and `fbc_variableType`.
  The component itself is not a node: it is the weight of the edge. A variable that has no
  node is logged and skipped.
- The constraint nodes and both edge types belong to the `__kinetic` network (which has
  the reactions and parameters) and to `__all`, not to the base network.
- The variable type of a flux objective: a reaction column `fbc_objective-<id>_variableType`
  next to the coefficient column `fbc_objective-<id>`, only when set.
- Styles: the constraint node gets the shape, color and size of a core `constraint` node
  (both styles; the style files regenerated with `StyleFactory.createStyle`).

### Info panel

- `UserDefinedConstraint`: table with id, name, lower and upper bound (parameter id and
  value) and a row per component: `coefficient * variable [* variable2]`
  (`linear`/`quadratic`).
- `UserDefinedConstraintComponent` (if selected through the tree): its attributes.
- `FluxObjective` variable type in the reaction table next to its objective coefficient
  is covered by the columns; the model table lists the objectives unchanged.
- Key-value pairs of any SBase: a section `key-value pairs` (after the uncertainties) with
  a table key, value, uri (a link) and id/name if set. The `listOfKeyValuePairs` is not
  repeated in the raw non-RDF annotation XML.

## Javadoc (#460)

- `maven-javadoc-plugin` (pinned version) with `doclint` `all,-missing` and
  `failOnWarnings`: malformed HTML, broken references and similar fail the build; missing
  comments do not.
- Fix the 6 doclint errors.
- Profile `javadoc` binds `javadoc:jar` to `package`, producing
  `target/cy3sbml-<version>-javadoc.jar`. It is a profile, since it needs a full JDK (the
  `javadoc` tool) and slows the local `install` used to hot-reload the app.
- CI runs `verify` with `-Pjavadoc` on Ubuntu and Windows, so a javadoc error fails the
  pull request.
- The release workflow builds with `-Pjavadoc`, creates MD5/SHA-1 checksums for the app
  jar and the javadoc jar and uploads both.
- Docs: `docs/development/release.md`, `building.md` and `CLAUDE.md` mention the profile.

## Out of scope

- Conversion between fbc v2 and v3 (JSBML converters).
- Writing fbc v3 from cy3sbml (cy3sbml does not write SBML).
- New JSBML validation rules specific to v3 (e.g. `coefficient` must reference a constant
  parameter).
