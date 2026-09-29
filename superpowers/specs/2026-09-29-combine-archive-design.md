# COMBINE archives (#116)

## Goal

Import the SBML models of a COMBINE archive (OMEX): opening an `.omex` file in Cytoscape
creates the networks of its SBML models as if the SBML files were opened, and the info
panel shows where the model came from and what else the archive contains.

## Decisions

- **Which SBML files:** the master SBML file(s) if the manifest marks a master SBML file,
  otherwise all SBML files of the archive. SBML files the imported ones reference (comp
  external model definitions) are found inside the archive and are not imported as
  separate collections.
- **What else is shown:** an "Archive" section in the info panel of the document (archive
  name, description and creators from `metadata.rdf`, the manifest entries with format and
  master marker, the imported files marked). No archive network.
- **No new dependency:** an OMEX file is a zip with `manifest.xml` and optional
  `metadata.rdf`; `java.util.zip` and the JDK XML parser read them.
- **Test archives** are written with `pymetadata.omex` (`tools/pycysbml`).

## Current state

The package `archive` holds a stub of an old design (robundle): `ArchiveFileFilter` accepts
every zip, `ArchiveReaderTask` creates an empty network, `ArchiveAction` and
`ArchiveReaderTaskFactory` are not registered, and the style `robundle.xml` is unused. They
are removed.

## Design

### Detection (`archive.CombineArchiveFileFilter`)

A `BasicCyFileFilter` for the COMBINE extensions `omex`, `sedx`, `sbex`, `cmex`, `sbox`,
`neux`, `phex` (content types `application/zip`, `application/x-zip-compressed`), category
network. A stream is accepted if it starts with the zip signature `PK\3\4`; a URI if its
extension is one of the above and its content starts with the signature. Plain `.zip` files
are not claimed.

### Reading the archive (`archive.CombineArchive`, `archive.ArchiveInfo`)

- `CombineArchive.extract(InputStream, String name, Path directory)` unpacks the archive
  into the directory and reads `manifest.xml` and `metadata.rdf`. Entries whose normalized
  path leaves the directory (`..`, absolute paths) are rejected (zip slip).
- `ArchiveInfo` (immutable record, JSON serializable with Jackson):
  - `name`: the file name of the archive
  - `description`, `creators` (list of `Creator(givenName, familyName, email,
    organization)`), from the RDF about the archive (`.`, `./` or the archive) in
    `metadata.rdf`: `dcterms:description` and the vCard creators, as written by libCombine
    and pymetadata; missing or unreadable metadata gives empty values and a debug log
  - `entries`: list of `Entry(location, format, master)` in manifest order, without the
    manifest itself
- `ArchiveInfo.sbmlEntries()` / `modelsToImport()`: an entry is SBML if its format starts
  with `http://identifiers.org/combine.specifications/sbml` (all levels and versions, also
  `https`) or is one of the media types `application/sbml+xml`, `text/xml+sbml`. The models
  to import are the SBML entries with `master="true"` if there is at least one, otherwise
  all SBML entries.
- `ArchiveInfo.formatLabel(format)`: short name of a format for the info panel, e.g.
  `SBML L3V1`, `SBML`, `SED-ML L1V3`, `CSV`, `PNG`, `OMEX metadata`; else the last path
  segment of the URI.
- Errors (`CombineArchiveException` with a message for the user): not a zip, no
  `manifest.xml`, a manifest that is not valid XML or has no `content` elements, an entry
  location outside the archive, a manifest location that is not in the zip, no SBML file.

### Import (`archive.CombineArchiveReaderTaskFactory`, `archive.CombineArchiveReaderTask`)

- The factory is an `AbstractInputStreamTaskFactory` with the filter, registered in
  `CyActivator` like `SBMLReaderTaskFactory` (as `InputStreamTaskFactory` with the reader
  id and the network category).
- `CombineArchiveReaderTask implements CyNetworkReader`:
  - `run`: extracts the archive into a new directory below the archive directory of the
    app (`ArchiveDirectories`: a root in the system temporary directory created once per
    Cytoscape session, deleted on `CyActivator.shutDown`), then runs one `SBMLReaderTask`
    per model to import, in manifest order, each with the file's `file:` URI as location
    (so comp external model definitions resolve inside the archive). A model that fails to
    read is reported in the task monitor and the log and skipped; if none can be read the
    task fails with the errors of the models.
  - `getNetworks`: the networks of all SBML reader tasks.
  - `buildCyNetworkView(network)`: delegates to the SBML reader task that created the
    network, then registers the `ArchiveInfo` for the root network of the network in
    `SBMLManager` and sets the column `archive` (`SBML.ATTR_ARCHIVE`) of the root network to
    the archive name.
- The SBML reader task is unchanged.

### SBMLManager and sessions

- `SBMLManager` keeps the `ArchiveInfo` per root network SUID (with the imported
  location) and returns it for a document (`getArchiveInfo(Long rootSUID)`), removed with
  the network like the document mapping.
- `SessionData` writes the archive infos as one JSON file (`archives.json`: root SUID ->
  info and imported location) and restores them. The extracted files are not part of the
  session.

### Info panel

For an `SBMLDocument` from an archive, the info panel shows after the document table an
"Archive" section (`gui.ArchiveHtml`):
- a line with the label `archive` (style of the CVTerm qualifiers), the archive name, and
  the imported location
- the description and the creators (given and family name, organization), if set
- a table of the entries (two columns like the attribute table): location, and the format
  label with `master` if set; the imported file is bold.
All text is escaped.

## Tests

- Test archives written by `tools/pycysbml/omex_models.py` with `pymetadata.omex` into
  `src/test/resources/models/omex/`:
  - `single.omex`: one SBML L3V1 model (master), a SED-ML file, a CSV file, `metadata.rdf`
    with description and creator
  - `comp.omex`: a comp model with an external model definition to a second SBML file in a
    subfolder, the comp model master
  - `no_master.omex`: two SBML models, no master
  - `no_sbml.omex`: only a SED-ML file
  - a broken archive (`broken.omex`, not a zip) and one without manifest are written by the
    tests themselves
- A real BioModels archive (`BIOMD0000000012.omex`) in `src/test/resources/models/omex/`.
- `CombineArchiveTest`: manifest and metadata parsing, selection (master, no master), zip
  slip, the error messages.
- `CombineArchiveFileFilterTest`: extensions, signature, plain zip not claimed.
- `CombineArchiveReaderTaskTest`: networks and collections of each test archive, the
  `archive` column, comp external model definitions read from the archive, a failing model
  skipped, the SBMLManager registration.
- `ArchiveHtmlTest`, `SessionDataTest` (archive info round trip), a golden snapshot of
  `single.omex`.
- Manual check in Cytoscape: open `BIOMD0000000012.omex` and `comp.omex` from the file
  menu, check the collections and the info panel, save and reopen the session.

## Documentation

`docs/guide/import.md` (COMBINE archives section, replacing "not supported"),
`docs/guide/packages.md` (COMBINE row), `docs/index.md`, `docs/development/architecture.md`,
`CLAUDE.md`, `release-notes/0.7.0.md`.

## Out of scope

SED-ML simulations, archive writing, a network of the archive content, archives from
BioModels search (the BioModels dialog keeps importing SBML).
