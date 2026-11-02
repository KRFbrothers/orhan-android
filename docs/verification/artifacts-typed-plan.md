# Typed artifacts slice

Depends on PR #93 (`5bdf5e0`), stage 2 only. Reuse `ArtifactStore`,
`ArtifactsScreen`, its existing routes, and the layout preference store.
The only Memory-screen dependency is deriving My Artifacts row excerpts from
the canonical typed artifact; its topic/context/pause stores are unchanged.

## Evidence and boundaries

On 2026-11-02, REA verified connected serial `9cefa3cb` and installed reference
package `ai.teamily.mobile`. Local ADB navigation reopened an existing Markdown
row: native formatted TextViews and a close control, no exposed export action.
An existing Web Page row first showed one loading WebView, then a rendered page
with two WebView nodes and native close/title. Reference records were not edited,
saved, shared or deleted. Reopening may change its opened timestamp.

Ignored raw evidence: `captures/artifacts-typed/reference-markdown.xml`
(SHA-256 `9294b12cfa6b8f6dc54860af57e4bd7cbb29b4cb1f9f7c9326b068e7a8206cb4`),
`reference-web-ready.xml`
(`9f3a72637e6fbc540dfd6b1030484b3a1f43f96d7ddcc30a87c0f8c67df479fc`).
The historical captures with matching hashes are corroborated by these fresh
reads. They establish displayed rendering, not the reference's source file format,
backend storage schema, creation service, editor or export implementation.

The New menu exposes Cloud document, Whiteboard, Upload and Create Artifacts.
No item was selected. Prior Media and Messages empty-state evidence remains
insufficient to specify their persisted payloads or lifecycle.

## Orhan-owned implementation contract

- Persist explicit Plain text, Markdown and Web page (HTML) types. Version 1
  records migrate to Plain text without source sniffing, content/id/date changes
  or new records. Preserve existing compact/large choice. Unknown or corrupt
  formats fail writes rather than becoming a writable empty list.
- New document editor offers explicit type selection. Existing documents retain
  their type; editing changes the original source. Failed saves retain title,
  body, type and editor; failed deletes retain detail and confirmation.
- List previews show a labelled text excerpt derived from each actual type.
  Detail renders Markdown formatting/tables and sanitized static HTML. Use
  CommonMark and jsoup for actual parsing, not regex formatting or relabelling.
  Both are focused rendering dependencies with bundled notices.
- HTML preview is deliberately offline: no JavaScript, native bridge, file or
  content access, cookies/storage or remote loads/navigation. No claim of
  interactive/remote website parity. Authenticated app context never enters it.
- Export writes original UTF-8 source with `.txt`, `.md` or `.html` and matching
  MIME through the system document picker. Typed sharing uses a scoped read-only
  FileProvider URI; existing plain-text sharing remains available. Handle failed
  export/share locally without changing/deleting the saved artifact.
- No media/message model, generated content, provider integration or automatic
  reply type inference in this slice. Live reply-save requires ordinary sign-in.

## Verification contract

Automate v1 migration, typed round-trip/edit/delete/reopen, invalid-format
protection, export bytes/MIME/name, HTML/Markdown rendering boundaries and the
mutation state actually used by the editor/delete dialog. Run full unit tests,
debug build and lint with the existing Gradle wrapper. Verify the updated signed
non-downgrade build on the phone with synthetic documents, export/share,
restart/deletion and light/dark samples. Do not inject real-device disk failures
or rewrite real history; report automated failure coverage separately.
