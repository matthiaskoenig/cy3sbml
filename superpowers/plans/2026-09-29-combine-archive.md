# COMBINE archives Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Import the SBML models of COMBINE archives (OMEX) into Cytoscape, with an "Archive" section in the info panel.

**Architecture:** `CombineArchive` unpacks the archive and parses `manifest.xml`/`metadata.rdf` into an immutable `ArchiveInfo`. `CombineArchiveReaderTask` (a `CyNetworkReader`) runs the unchanged `SBMLReaderTask` per selected SBML file with the extracted file as location, registers the `ArchiveInfo` in `SBMLManager`, which the info panel (`ArchiveHtml`) and `SessionData` use.

**Tech Stack:** Java 17, `java.util.zip`, JDK DOM parser, Jackson (already a dependency), Cytoscape 3 API, JUnit 6; Python `pymetadata.omex` for test archives.

**Spec:** `superpowers/specs/2026-09-29-combine-archive-design.md`

## Global Constraints

- No new runtime dependency.
- Import: the master SBML files if the manifest marks one, else all SBML files; referenced SBML files are read through the archive location, not imported separately.
- COMBINE extensions `omex`, `sedx`, `sbex`, `cmex`, `sbox`, `neux`, `phex` with the zip signature; plain `.zip` is not claimed.
- Entries outside the extraction directory are rejected (zip slip).
- `SBMLReaderTask` is unchanged.
- No new warnings in `./mvnw -B -q verify`; Spotless and the `lint` profile pass; docs build warning-free.
- Never use the em dash character; no agent attribution anywhere.

## Review Focus

1. An archive whose manifest lists a file that is not in the zip, or a location with `./` prefix (pymetadata writes `./model.xml`): the location is normalized and a missing file is a clear error for that model only. Test in Task 2.
2. A manifest without the `omex` namespace prefix or with other formats' casing (`https://identifiers.org/...`): SBML still recognized. Test in Task 2.
3. Two archives with the same SBML file name imported one after the other: separate extraction directories, no overwriting. Test in Task 4.
4. An SBML file of the archive that fails to read: the other models are imported, the error is reported. Test in Task 4.
5. The info panel for a document not from an archive: no Archive section, no exception. Test in Task 5.

---

### Task 1: test archives

**Files:** `tools/pyproject.toml` (+ `pymetadata`), `tools/uv.lock`, create `tools/pycysbml/omex_models.py`, create `src/test/resources/models/omex/{single,comp,no_master,no_sbml}.omex`, `src/test/resources/models/omex/BIOMD0000000012.omex` (download from BioModels, keep the file as downloaded), `tools/README.md`.

- [ ] Write `omex_models.py`: builds the SBML with libSBML (`single`: L3V1 model with one reaction; `comp`: main model with a submodel referencing an external model definition `models/sub.xml`, plus `models/sub.xml`; `no_master`: two L3V1 models), SED-ML as a minimal valid file, CSV, `metadata.rdf` (description, one creator in vCard 4 as libCombine writes it), and writes the archives with `pymetadata.omex.Omex` (`add_entry`, `to_omex`). Validates every SBML with libSBML before writing.
- [ ] Run it; inspect each `manifest.xml` (`unzip -p`): note the exact location format (`./x.xml` or `x.xml`) and the format URIs, record in the ledger.
- [ ] ruff, ruff format, ty clean. Commit.

### Task 2: ArchiveInfo and CombineArchive

**Files:** create `src/main/java/org/cy3sbml/archive/{ArchiveInfo,CombineArchive,CombineArchiveException}.java`; test `src/test/java/org/cy3sbml/archive/CombineArchiveTest.java`.

**Interfaces (produces):**
- `record ArchiveInfo(String name, String description, List<Creator> creators, List<Entry> entries)` with `record Creator(String givenName, String familyName, String email, String organization)`, `record Entry(String location, String format, boolean master)`; methods `List<Entry> sbmlEntries()`, `List<Entry> modelsToImport()`, `static boolean isSbml(String format)`, `static String formatLabel(String format)`. Locations are normalized (no leading `./`).
- `final class CombineArchive { static ArchiveInfo extract(InputStream in, String name, Path directory) throws CombineArchiveException, IOException }`; after it, `directory.resolve(entry.location())` is the file.
- `class CombineArchiveException extends Exception`.

- [ ] Failing tests: `single.omex` (entries, master, description, creator, `modelsToImport` = the master), `no_master.omex` (both), `no_sbml.omex` (exception "contains no SBML file"), not a zip (bytes `hello`: exception "not a zip"), zip without manifest (built in the test with `ZipOutputStream`: "no manifest.xml"), zip slip entry `../evil.xml` (exception, nothing written outside), a manifest entry missing in the zip (exception naming the location), `https://identifiers.org/combine.specifications/sbml.level-3.version-2` and `application/sbml+xml` are SBML, `formatLabel` for SBML L3V1, SED-ML L1V3, CSV, PNG, metadata.
- [ ] Implement with `ZipInputStream`, DOM (`XMLUtil` secure factory if present, else `DocumentBuilderFactory` with external entities disabled), namespace-aware lookups by local name.
- [ ] Pass. Commit.

### Task 3: file filter, remove the stub

**Files:** create `archive/CombineArchiveFileFilter.java`, test `CombineArchiveFileFilterTest`; delete `archive/{ArchiveAction,ArchiveFileFilter,ArchiveReaderTask,ArchiveReaderTaskFactory}.java`, `src/test/java/org/cy3sbml/archive/ArchiveReaderTaskTest.java`, `src/main/resources/styles/robundle.xml`, `GUIConstants` `ICON_ARCHIVE`, `GRAVITY_ARCHIVE`, `DESCRIPTION_ARCHIVE` and `gui/images/archive.png` if unused, the comment in `CyActivator`, the javadoc mention in `GoldenModelsTest`.

- [ ] Failing test: accepts `.omex` stream with `PK\3\4`, rejects a stream without it, rejects category table; `accepts(URI)` for `single.omex` true, for a `.zip` copy false, for an `.xml` false.
- [ ] Implement; delete the stub (check `rg -i "robundle|ArchiveAction|ICON_ARCHIVE"` is empty, `BundleJarContentIT` still passes).
- [ ] Pass, commit.

### Task 4: reader task, factory, SBMLManager, registration

**Files:** create `archive/{ArchiveDirectories,CombineArchiveReaderTask,CombineArchiveReaderTaskFactory}.java`, `ArchiveImport` record (`ArchiveInfo info, String location`) in `archive`; modify `SBMLManager` (archive map), `SBML` (`ATTR_ARCHIVE = "archive"`), `CyActivator` (register factory with `readerId` `cy3sbmlArchiveReader`, delete directories in `shutDown`); tests `CombineArchiveReaderTaskTest`, `SBMLManagerTest`.

**Interfaces:**
- `SBMLManager.addArchive(Long rootSUID, ArchiveImport archive)`, `Optional<ArchiveImport> getArchive(Long rootSUID)`, `Optional<ArchiveImport> getArchive(SBMLDocument document)`, `Map<Long, ArchiveImport> getArchives()`, `setArchives(Map<Long, ArchiveImport>)`; removed in `handleEvent(NetworkAboutToBeDestroyedEvent)` with the document.
- `ArchiveDirectories { Path newDirectory(String archiveName) throws IOException; void deleteAll() }`.
- `CombineArchiveReaderTask(InputStream, String name, ArchiveDirectories, ServiceAdapter-like dependencies as SBMLReaderTask, SBMLManager)`; package-private constructor for tests with factories only (as `SBMLReaderTask`).

- [ ] Failing tests: `single.omex` gives one collection with the model networks, the root network has `archive` = `single.omex`, after `buildCyNetworkView` the manager has the archive for the root SUID; `comp.omex` gives the collections of the comp model (incl. `Flat__`) and no separate collection for `models/sub.xml`; `no_master.omex` two collections; an archive with one broken SBML (written in the test) imports the other and the task monitor got an error; two imports of `single.omex` use different directories; `no_sbml.omex` fails with the message.
- [ ] Implement; register in `CyActivator`.
- [ ] Pass, full fast suite. Commit.

### Task 5: info panel

**Files:** create `gui/ArchiveHtml.java`, test `ArchiveHtmlTest`; modify `SBaseHTMLFactory` (constructor gets a `Function<SBMLDocument, Optional<ArchiveImport>>` or an `ArchiveLookup` interface implemented by `SBMLManager`; `CyActivator` passes the manager), `SBaseHTMLFactoryTest`/`CompHtmlTest` constructor calls.

- [ ] Failing tests: HTML for `single.omex` info has the qualifier `archive`, the name, description, creator, one row per entry with format label and `master`, the imported location bold, escaping of a description with `<`; a document without archive gives no section.
- [ ] Implement; pass; commit.

### Task 6: sessions

**Files:** modify `SessionData` (write `archives.json` with Jackson, read it on load), `SessionDataTest`.

- [ ] Failing test: session round trip of an imported `single.omex` restores `getArchive(rootSUID)` equal to the original.
- [ ] Implement (root SUIDs are unchanged in the loaded session as for the other mappings; a missing or unreadable file logs and keeps the rest). Pass, commit.

### Task 7: golden snapshot and documentation

- [ ] Golden: `GoldenModelsTest` can only read SBML resources; add a separate `@Test` in `CombineArchiveReaderTaskTest` or extend `NetworkSnapshot` use for `omex/single.omex` via the archive reader, snapshot `src/test/resources/golden/omex__single.json`.
- [ ] Docs: `import.md` COMBINE section (what is imported, the info panel section, the extensions), `packages.md`, `index.md`, `architecture.md` (archive package), `CLAUDE.md` (architecture bullet, remove "not supported"), `release-notes/0.7.0.md`. Docs build warning-free. Commit.

### Task 8: verification

- [ ] `./mvnw -B -q verify` (no output), lint profile, docs build, Python checks.
- [ ] Cytoscape: open `BIOMD0000000012.omex` and `comp.omex` through CyREST `network load file`, check collections, `archive` column, the info panel of the document (screenshot), session save and reopen.
- [ ] Final review by a fresh reviewer, fix Critical/Important with TDD.
