# Open a model in sbml4humans (#473)

## Goal

For a loaded SBML model it is one click in the cy3sbml GUI to open the model in [sbml4humans](https://sbml4humans.de), the human readable report of SBML models. The models in Cytoscape are mostly local files, so cy3sbml uploads the model and opens the report of the upload in the system browser.

The feature spans two repositories: sbml4humans gets an upload that the server keeps for 24 hours under an address, and cy3sbml gets the link that uploads the model and opens that address.

## Decisions

- **Hand-over:** sbml4humans keeps an uploaded model briefly on the server, under a random id, and the report of the id has an address (`/report?upload=<id>`), like a report of a url (`/report?url=<url>`). The existing upload of sbml4humans (`POST /api/file`) answers with the report data, which only the browser tab that uploaded it can show, so it cannot hand a model over to a browser.
- **Storage:** on disk, on a docker volume, 24 hours after the upload, at most 100 MB per upload (the body limit of the nginx proxy). An upload survives a restart and a deploy, so a link works for a day and can be shared.
- **What is sent:** a COMBINE archive with the document of the network and the files of its comp external model definitions, so sbml4humans resolves the external models inside the archive, as it does for archives (it never fetches a file).
- **Where:** an sbml4humans icon in the info panel, in the model row next to the SBML icon. No toolbar button, no command.
- **Consent:** the first upload asks (public server, kept 24 hours, anyone with the link can open it), with "Don't ask again", stored in the cy3sbml properties.
- **Icon:** the sbml4humans logo (`frontend/public/favicon-32x32.png` of sbml4humans).
- **Versions:** sbml4humans 0.8.0 (minor, new endpoint and route), cy3sbml 0.9.0. sbml4humans is released and deployed first.

## Part 1: sbml4humans

### Backend

- `POST /api/upload` takes a multipart field `source` (an SBML file, gzipped SBML or a COMBINE archive), like `/api/file`.
  - It creates the report of the bytes once (`report_for_bytes`, untrusted), so an input which cannot be read fails at once with the message of the report, and nothing is stored.
  - It rejects an upload over `MAX_UPLOAD_BYTES = 100 MB` without reading more of it.
  - It stores the bytes and answers `{"id": "<id>", "expires": "<ISO 8601 time, UTC>"}`.
  - Every failure follows the error contract (status 200, `{"errors": [...], "warnings": [], "info": {...}}`).
- `GET /api/upload/{id}` answers the report of the stored bytes (`report_for_bytes`, untrusted), created on every request like `/api/url`.
  - An id which is not stored or has expired raises `UploadNotFoundError` with the message "The upload <id> is not available: uploads are kept for 24 hours." (error contract).
  - An id which is not of the form of an id (`[A-Za-z0-9_-]{22}`) is not stored, so it gives the same error, and it never names a path.
- `uploads.py`, `UploadStore`:
  - `put(content: bytes) -> Upload(id, expires)`: the id is `secrets.token_urlsafe(16)` (128 bits); the file `<dir>/<id>` is written atomically (temporary file in the same directory, then rename), mode 0600; the expiry is the modification time plus `UPLOAD_LIFETIME = 24 h`.
  - `get(id) -> bytes | None`: None for an id of another form, a missing file or an expired file (which is deleted).
  - `remove_expired()`: deletes the expired files; run at startup and every hour by a task of the `lifespan` of the api.
  - The directory is `SBML4HUMANS_UPLOADS`, else a directory `sbml4humans-uploads` in the temporary directory of the system (development, tests).
- Deployment: a named volume `uploads` in `docker-compose-production.yml`, mounted at `/uploads` in the backend container, with `SBML4HUMANS_UPLOADS=/uploads`; the same in `docker-compose-develop.yml`.

### Frontend

- `/report?upload=<id>` loads the report of the upload, like `?url=`:
  - `getUpload(id)` in `src/api/client.ts` (`GET /api/upload/<id>`, the error contract of `request()`);
  - the source kind `upload` in `src/stores/report.ts` (`ReportSource` with the id, `sameSource`, `loadUpload`);
  - `ReportPage.vue` watches `query.upload` and lists it in `showsReport`;
  - `upload` joins `SOURCE_KEYS` in `src/report/view.ts`, so the parameter stays in the address when the state of the report changes, and the report survives a reload;
  - `feedback.ts` treats an upload like a url (a model the reporter can link).
- The Upload tab of the home page keeps `POST /api/file`; `POST /api/upload` is for other tools.

### Tests and documentation

- Backend (`tests/test_api.py`, `tests/test_uploads.py`): an upload and its report; the archive of a comp model with an external model definition resolved; an unreadable upload (error, nothing stored); an upload over the limit; an unknown, a malformed and an expired id; `remove_expired`; `test_openapi` lists the two routes.
- Frontend: unit tests of the client, the store and the report page; an e2e spec like `tests/e2e/local.spec.ts` (mocked `**/api/upload/*`, the parameter kept and a reload).
- Docs: `docs/inputs.md` (a section "Links from other tools" with the api, the 24 hours and that anyone with the link can open the report; "Sharing a report" names uploads), the parameter `upload` in the table of `docs/report.md`, `CLAUDE.md` (the store, the volume).
- `release-notes/0.8.0.md`, then the version bump, through pull requests.

## Part 2: cy3sbml

### The link

- The model row of the info panel (`SBMLUtil.createModelMap`, next to the SBML icon) gets the icon `images/logos/sbml4humans_icon.png` (height 20, title "Open in sbml4humans"), linking `BrowserHyperlinkListener.URL_SBML4HUMANS = "http://sbml4humans/"`.
- `BrowserHyperlinkListener` handles the link on the Swing event dispatch thread:
  1. `Sbml4HumansConsent`: without the property `cy3sbml.sbml4humans.confirmed=true`, a dialog explains the upload ("The model is uploaded to the public server <url>, which keeps it for 24 hours. Anyone with the link can open the report.") with "Upload" and "Cancel" and a check box "Don't ask again", which sets the property. Cancel ends here.
  2. The task "Opening the model in sbml4humans" runs in the `DialogTaskManager` (progress, cancel): it builds the archive, uploads it and opens the address of the report with `GUIUtil.openURLinExternalBrowser`. A failure is the error of the task, with the message of sbml4humans or the HTTP status.

### What is sent

- The document of the collection of the current network, `SBMLManager.getSBMLDocument(network)`: the main document for the main model and its comp model definitions, the external document for an external model, the flattened document for the flat model.
- The address opens that model: `<url>report?upload=<id>&entry=<location of the master entry>&model=<id of SBMLManager.getModel(root)>` (without `model` for a model without id).

### New units

- `archive.CombineArchiveWriter.write(SBMLDocument document, String location, OutputStream out)`:
  - writes an OMEX: `manifest.xml` (the archive, the manifest and every entry with its format: `http://identifiers.org/combine.specifications/sbml.level-<l>.version-<v>`; the document is the master entry) and the SBML files, written with `SBMLWriter`;
  - follows the comp external model definitions of every written document recursively (through `CompModels`): the document of a definition is written at its `source` resolved against the location of the entry that names it, once per location;
  - leaves out a definition whose source is a url, resolves outside the archive or cannot be read, and logs it; sbml4humans shows it as not resolved;
  - the location of the master entry: the location inside the archive for a model imported from a COMBINE archive (`ArchiveImport`), else the file name of the location of the document, else `model.xml`.
- `HttpJson.postMultipart(URI uri, String field, String fileName, byte[] content) -> FetchResult<JsonNode>`: a multipart/form-data POST with the timeouts, the size limit of the response and the status rules of the GET methods.
- `sbml4humans.Sbml4HumansClient`:
  - `URI upload(byte[] archive, String entry, String model) throws IOException`: posts the archive to `<api>upload` and returns the address of the report, `<url>report?upload=...`; an answer of the error contract (`errors`) becomes an `IOException` with the first error, a failed request one with the status.
  - the address of the reports is the property `cy3sbml.sbml4humans.url` (default `https://sbml4humans.de/`), the api the property `cy3sbml.sbml4humans.api` (default `<url>api/`, where the nginx of sbml4humans.de serves it), so a local sbml4humans can be used, whose frontend (`http://localhost:3456/`) and api (`http://localhost:1444/api/`) run on different ports.
- `sbml4humans.Sbml4HumansConsent`: reads and stores the property of the consent, the dialog is shown by the listener.
- `sbml4humans.Sbml4HumansTask`: the task of the link (archive, upload, browser), with the client and the browser as constructor arguments for the tests.

### Tests and documentation

- `CombineArchiveWriterTest`: the archive of `models/comp/unit/top.xml` and of `models/comp/koenig-toymodel/toy_top_level.xml` read back with `CombineArchive.extract` (manifest, master, the external files at their locations) and imported with the `CombineArchiveReaderTask` with every external model resolved; a source with a url and one outside the archive left out; the location of the master entry of an imported archive.
- `Sbml4HumansClientTest` against a local `HttpServer` (like `BiomodelsQueryHttpTest`): an upload (the multipart request, the address with `entry` and `model`), the error contract, an HTTP error status, an unreachable server.
- `Sbml4HumansConsentTest`, `Sbml4HumansTaskTest` (the address handed to the browser, a failure as the error of the task), and the link in `SBaseHTMLFactoryTest`.
- End to end: by hand against a local sbml4humans (backend `uv run uvicorn sbml4humans.api:api --port 1444`, frontend `npm run dev`), with `cy3sbml.sbml4humans.url=http://localhost:3456/` and `cy3sbml.sbml4humans.api=http://localhost:1444/api/`, for a single SBML file, a comp model with external files, a model definition network, the flat network and a model of a COMBINE archive.
- Docs: a section "sbml4humans" in `docs/guide/info-panel.md` (what is uploaded, the 24 hours, the consent and the properties), `CLAUDE.md` (the package `sbml4humans`, the archive writer), `release-notes/0.9.0.md`.

## Prerequisites

- The TLS certificate of sbml4humans.de has expired (checked 2026-10-01: "certificate has expired"); it has to be renewed before cy3sbml can upload to it.
