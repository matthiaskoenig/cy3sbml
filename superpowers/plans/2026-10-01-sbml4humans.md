# Open a model in sbml4humans Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** One click on an sbml4humans icon in the info panel opens the model of the current network in sbml4humans (#473).

**Architecture:** `CombineArchiveWriter` writes the document of the network and the files of its comp external model definitions into a COMBINE archive; `Sbml4HumansClient` posts it to `POST <api>upload` (sbml4humans 0.8.0) and builds the address of the report; `Sbml4HumansTask` runs both in the task manager and opens the address in the system browser; `BrowserHyperlinkListener` asks for consent once (`Sbml4HumansConsent`) and starts the task.

**Tech Stack:** Java 17, JSBML, `java.net.http`, `java.util.zip`, Cytoscape 3.10 API, JUnit 6, Mockito.

**Spec:** `superpowers/specs/2026-10-01-sbml4humans-design.md` (Part 2). Branch `feature/sbml4humans`. Needs the sbml4humans endpoints of `/home/mkoenig/git/sbml4humans/superpowers/plans/2026-10-01-uploads.md` for the end to end check.

## Global Constraints

- Properties: `cy3sbml.sbml4humans.url` (default `https://sbml4humans.de/`), `cy3sbml.sbml4humans.api` (default `<url>api/`), `cy3sbml.sbml4humans.confirmed` (`true` after "Don't ask again").
- The address of a report: `<url>report?upload=<id>&entry=<location of the master entry>&model=<model id>`, values URL-encoded, `model` only for a model with id.
- Uploads are at most 100 MB (the limit of sbml4humans); the request may take minutes (sbml4humans reports the model while it answers), timeout 15 minutes.
- No upload without consent; the upload runs off the Swing event dispatch thread, the dialog on it.
- Never use the em dash. Spotless (`./mvnw -q spotless:apply`), `./mvnw -B -q verify` without output, the `lint` profile with JDK 21 (`JAVA_HOME=~/.jdks/temurin-21.0.12`), the `javadoc` profile (`JAVA_HOME=~/.jdks/ms-17.0.16`).

## Review Focus

- A model of a COMBINE archive import must keep the location of its entry, so its external definitions resolve as in the original archive (test in Task 2).
- An external model definition pointing outside the archive (`../x.xml`) or to a URL must be left out, not written outside the zip (test in Task 2).
- A comp model definition network must open the report at the model definition, not at the main model (test in Task 4).
- An sbml4humans error (error contract, HTTP status, unreachable server, archive above 100 MB) must reach the user as the error of the task with a readable message (tests in Tasks 1, 3, 4).
- "Cancel" in the consent dialog must upload nothing (test in Task 4 of `Sbml4HumansConsent`, the dialog by hand).

---

### Task 1: Multipart POST in HttpJson

**Files:**
- Modify: `src/main/java/org/cy3sbml/util/HttpJson.java`
- Test: `src/test/java/org/cy3sbml/util/HttpJsonPostTest.java`

**Interfaces:**
- Produces: `public JsonNode postMultipart(URI uri, String field, String fileName, byte[] content) throws IOException` (throws with a message naming the URI and the reason: a non-2xx status, a body which is no JSON, a transport error, a timeout).

- [ ] **Step 1: Write the failing test** against a local `com.sun.net.httpserver.HttpServer` (as `BiomodelsQueryHttpTest`): a handler on `/api/upload` that reads the request, records the `Content-Type` and the body, and answers `{"id": "x"}`. Tests: `postsTheContentAsMultipartFormData` (the body contains `name="source"; filename="model.omex"`, the bytes and the closing boundary; the answer is parsed), `failsWithTheStatus` (a 500 answer, the message contains `HTTP status 500` and the URI), `failsForABodyWhichIsNoJson`, `failsForAnUnreachableServer` (`http://127.0.0.1:1/`).

- [ ] **Step 2: Run** `./mvnw -B -q test -Dtest=HttpJsonPostTest`; expected: compilation error, no `postMultipart`.

- [ ] **Step 3: Implement.** Constants `UPLOAD_TIMEOUT = Duration.ofMinutes(15)`. Refactor `send(URI)` into `send(HttpRequest request, Duration timeout)` (the GET path passes `jsonRequest(uri)` and `responseTimeout`). `postMultipart` builds the body with a boundary `"cy3sbml-" + UUID.randomUUID()`:

```java
byte[] head = ("--" + boundary + "\r\n"
        + "Content-Disposition: form-data; name=\"" + field + "\"; filename=\"" + fileName + "\"\r\n"
        + "Content-Type: application/octet-stream\r\n\r\n").getBytes(StandardCharsets.UTF_8);
byte[] tail = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);
HttpRequest request = HttpRequest.newBuilder()
        .uri(uri)
        .header("Accept", "application/json")
        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
        .timeout(UPLOAD_TIMEOUT)
        .POST(HttpRequest.BodyPublishers.ofByteArrays(List.of(head, content, tail)))
        .build();
```

and waits for `send(request, UPLOAD_TIMEOUT)`: `ExecutionException` -> `IOException("Could not upload to " + uri + ": " + describe(cause))`, `CancellationException` -> `"... timed out after 900 s"`, `InterruptedException` -> cancel, re-interrupt, `InterruptedIOException`; a status outside 2xx -> `"... HTTP status " + status`; a body which is no JSON -> `"... the answer is no JSON"`. `field` and `fileName` are checked to contain no `"`, CR or LF (`IllegalArgumentException`).

- [ ] **Step 4: Run** the test and `HttpJsonTest`; expected: pass.

- [ ] **Step 5: Commit** "Post a file as multipart form data with HttpJson".

### Task 2: CombineArchiveWriter

**Files:**
- Create: `src/main/java/org/cy3sbml/archive/CombineArchiveWriter.java`
- Test: `src/test/java/org/cy3sbml/archive/CombineArchiveWriterTest.java`

**Interfaces:**
- Consumes: `CompModels.externalModels()` (`External(definition, resolution)`, `ModelResolution.Resolved(model, document)`).
- Produces: `public static String write(SBMLDocument document, String location, CompModels models, OutputStream out) throws IOException` - writes the archive, returns the location of the master entry (`./<location>`); `public static String masterLocation(SBMLDocument document, Optional<ArchiveImport> archive)`.

Behavior:
- `masterLocation`: the location of the imported entry for an archive import (without a leading `./`), else the last path segment of the location URI of the document if it is a safe file name (`[A-Za-z0-9._-]+`), else `model.xml`.
- Entries: the document at `location`; for every external of `models.externalModels()` (repeat until no entry is added, an external is written when the document of its definition has a location): the source of the definition without a `file:` scheme, resolved against the directory of the location of the document that contains the definition (`URI.create(parent).resolve(source).normalize()`); left out (logged at warn) if the source has another scheme, the result is absolute or starts with `..`, the resolution failed or the location holds another document already. A document already written (identity) is not written again.
- Each document is written with `new SBMLWriter().writeSBMLToString(document)` (UTF-8).
- `manifest.xml`: the archive (`location="."`, format `http://identifiers.org/combine.specifications/omex`), the manifest (`./manifest.xml`, `.../omex-manifest`) and every entry `./<location>` with the format `http://identifiers.org/combine.specifications/sbml.level-<l>.version-<v>`, `master="true"` for the document.

- [ ] **Step 1: Write the failing tests**:
  - `writesTheDocumentAsMasterEntry`: `core_01.xml` -> zip with `manifest.xml` and `model.xml`; `CombineArchive.extract` of it into a temp dir gives one entry `model.xml`, format SBML, master.
  - `writesTheExternalModelsAtTheirSources`: `models/comp/unit/top.xml` read with location (as `CompModelsTest.read`), `new CompModels(document)`; the archive has `top.xml`, `ext.xml`, `sub/ext2.xml` and the files they name; leaves out `missing.xml` (it does not exist).
  - `importedArchiveResolvesTheExternalModels`: the archive of `models/comp/koenig-toymodel/toy_top_level.xml` imported with `CombineArchiveReaderTask` (as `CombineArchiveReaderTaskTest`) creates the networks of the external models and the flat network (as the import of the file does).
  - `leavesOutSourcesOutsideTheArchive`: a programmatic document with external model definitions with the sources `../outside.xml` and `https://example.org/x.xml` (resolutions failed) -> only the master entry.
  - `masterLocation`: an archive import with the location `./models/m1.xml` -> `models/m1.xml`; a document with the location `file:/tmp/m1.xml` -> `m1.xml`; no location -> `model.xml`; the location `file:/tmp/a%20b.xml` -> `model.xml`.
- [ ] **Step 2: Run** `./mvnw -B -q test -Dtest=CombineArchiveWriterTest`; expected: compilation error.
- [ ] **Step 3: Implement** the class (final, private constructor, `java.util.zip.ZipOutputStream`, the manifest written as a string with the locations XML-escaped by `HtmlUtil.escape`).
- [ ] **Step 4: Run** the test and the archive tests; expected: pass.
- [ ] **Step 5: Commit** "Write a COMBINE archive of a document and its external models".

### Task 3: Sbml4HumansClient

**Files:**
- Create: `src/main/java/org/cy3sbml/sbml4humans/Sbml4HumansClient.java`, `package-info.java`
- Test: `src/test/java/org/cy3sbml/sbml4humans/Sbml4HumansClientTest.java`

**Interfaces:**
- Consumes: Task 1 `postMultipart`.
- Produces: `Sbml4HumansClient(HttpJson http, URI url, URI api)`, `static Sbml4HumansClient fromProperties(Properties properties)`, `URI upload(byte[] archive, String entry, String model) throws IOException`, `static final String PROPERTY_URL = "cy3sbml.sbml4humans.url"`, `PROPERTY_API = "cy3sbml.sbml4humans.api"`, `DEFAULT_URL = "https://sbml4humans.de/"`, `MAX_UPLOAD_BYTES = 100 MB`.

Behavior: an archive above 100 MB -> `IOException("The model is larger than 100 MB, the limit of sbml4humans.")` without a request. Posts to `api.resolve("upload")` with field `source`, file name `model.omex`. An answer with `errors` -> `IOException("sbml4humans could not read the model: " + errors[0])`; without a text `id` -> `IOException("sbml4humans gave no upload id")`. The address: `url.resolve("report?upload=" + enc(id) + "&entry=" + enc(entry) + (model != null ? "&model=" + enc(model) : ""))` with `URLEncoder` UTF-8. `fromProperties`: url from the property (a trailing `/` added), api from its property, else `url.resolve("api/")`.

- [ ] **Step 1: Write the failing tests** against a local HttpServer: `uploadsAndReturnsTheAddressOfTheReport` (the request goes to `/api/upload`, the address `http://127.0.0.1:<port>/report?upload=abc&entry=.%2Fmodel.xml&model=m1`), `withoutModelTheAddressHasNoModel`, `errorContractBecomesAnIOException`, `httpErrorBecomesAnIOException`, `tooLargeArchiveIsNotSent`, `propertiesGiveTheAddresses` (defaults; a local url and api).
- [ ] **Step 2-4:** run (fail), implement, run (pass).
- [ ] **Step 5: Commit** "Upload a model to sbml4humans".

### Task 4: Consent and task

**Files:**
- Create: `src/main/java/org/cy3sbml/sbml4humans/Sbml4HumansConsent.java`, `Sbml4HumansTask.java`
- Test: `src/test/java/org/cy3sbml/sbml4humans/Sbml4HumansConsentTest.java`, `Sbml4HumansTaskTest.java`

**Interfaces:**
- Consumes: Tasks 2 and 3, `SBMLManager.getSBMLDocument(CyNetwork)`, `getModel(Long)`, `getArchive(SBMLDocument)`, `getSBaseRefResolver(SBase)`.
- Produces: `Sbml4HumansConsent(CyProperty<Properties>)` with `boolean isConfirmed()`, `void confirm()` (`PROPERTY_CONFIRMED = "cy3sbml.sbml4humans.confirmed"`); `Sbml4HumansTask(SBMLManager sbmlManager, CyNetwork network, Sbml4HumansClient client, Consumer<String> browser)` extends `AbstractTask`, which reads the document and the model of the network from the manager in `run`.

`run`: title "Opening the model in sbml4humans"; the document of the network (none -> `IllegalStateException("The network has no SBML document.")`); the CompModels of `sbmlManager.getSBaseRefResolver(document).models()`; `masterLocation(document, sbmlManager.getArchive(document))`; the archive into a `ByteArrayOutputStream`; the model id of `getModel(root)` if it is set; `client.upload(...)`; stop if cancelled; `browser.accept(address.toString())`.

- [ ] **Step 1: Write the failing tests**: consent (no property -> false; `confirm` -> true and stored in the properties object); task with a mocked client (Mockito) and a recording browser: the import of `01134-sbml-l3v1.xml` through `CommandTestSupport`-like setup (`SBMLReaderTask` with an `SBMLManager`): the main network -> `upload(..., "./01134-sbml-l3v1.xml", "case01134")`; the network of `moddef1` -> model `moddef1`; the browser gets the returned address; a failing client -> the `IOException` escapes `run` (the task manager shows it); a cancelled task opens no browser.
- [ ] **Step 2-4:** run (fail), implement, run (pass).
- [ ] **Step 5: Commit** "Open the model of a network in sbml4humans with a task".

### Task 5: The link in the info panel

**Files:**
- Create: `src/main/resources/gui/images/logos/sbml4humans_icon.png` (copy of `/home/mkoenig/git/sbml4humans/frontend/public/favicon-32x32.png`)
- Modify: `src/main/java/org/cy3sbml/util/SBMLUtil.java` (model row), `src/main/java/org/cy3sbml/gui/BrowserHyperlinkListener.java` (`URL_SBML4HUMANS`, `processURL` branch, consent dialog, task)
- Test: `src/test/java/org/cy3sbml/gui/SBaseHTMLFactoryTest.java`, `BrowserHyperlinkListenerTest.java`

**Interfaces:**
- Consumes: Task 4; `ServiceAdapter.dialogTaskManager`, `cy3sbmlProperties`, `openBrowser`.
- Produces: `BrowserHyperlinkListener.URL_SBML4HUMANS = "http://sbml4humans/"`.

The model row: after the SBML icon, ` <a href="http://sbml4humans/"><img src="./images/logos/sbml4humans_icon.png" height="20" title="Open in sbml4humans" /></a>`. The listener: the URL is in `URLS_ACTION`-like handling of `processURL` on the dispatch executor: without a current SBML network -> nothing; consent: if not confirmed, `JOptionPane.showConfirmDialog(frame, panel, "Open in sbml4humans", OK_CANCEL_OPTION)` with a message and a `JCheckBox("Don't ask again")`; OK with the box checked -> `confirm()`; Cancel -> return; then `adapter.dialogTaskManager.execute(new TaskIterator(new Sbml4HumansTask(sbmlManager, network, Sbml4HumansClient.fromProperties(properties), GUIUtil::openURLinExternalBrowser)))`. The consent check and the dialog are a method `boolean confirmUpload(Component frame)` with the dialog behind a `BooleanSupplier`-like seam so the test can answer it.

- [ ] **Step 1: Write the failing tests**: the model info contains `href="http://sbml4humans/"` and the icon; the listener with a confirmed consent executes a `Sbml4HumansTask` in the mocked `DialogTaskManager`; with a dialog that answers Cancel nothing is executed; an unconfirmed consent with "Don't ask again" stores the property.
- [ ] **Step 2-4:** run (fail), implement, run (pass); `GuiPageResourcesTest` finds the icon.
- [ ] **Step 5: Commit** "Open the model in sbml4humans from the info panel".

### Task 6: Documentation, release notes, end to end

**Files:**
- Modify: `docs/guide/info-panel.md` (section "sbml4humans"), `CLAUDE.md` (Architecture: package `sbml4humans`, `CombineArchiveWriter`)
- Create: `release-notes/0.9.0.md`

- [ ] **Step 1:** the docs: what the icon does, what is uploaded (the archive with the external models), the 24 hours, the consent and the properties `cy3sbml.sbml4humans.url`, `.api`, `.confirmed` (with the local development values). `release-notes/0.9.0.md` in the form of `0.8.1.md` with "## New" and the feature (#473).
- [ ] **Step 2:** build the docs (`CLAUDE.md` commands, warning-free), `./mvnw -B -q verify`, lint and javadoc profiles.
- [ ] **Step 3: End to end** against a local sbml4humans of the branch `feature/uploads` (`cd ~/git/sbml4humans/backend && uv run uvicorn sbml4humans.api:api --port 1444`, `cd ../frontend && npm run dev`): with the properties set to `http://localhost:3456/` and `http://localhost:1444/api/`, run the task for `core_01.xml`, the comp model `toy_top_level.xml` (external models resolved in the report), the network of a model definition of `01134-sbml-l3v1.xml`, the flat network and a model of `models/omex/*.omex`; open each address in a browser (chrome-devtools-axi) and check the report, the entry and the model.
- [ ] **Step 4: Commit** "Document opening a model in sbml4humans".
