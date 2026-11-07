# Orhan backend

**Decision (2026-10-28):** Orhan chat uses the **Dua AI engine**. The earlier
Cloudflare Worker + OpenRouter plan (PR #2) is superseded and not used.

## Endpoints
| Service | Base URL | Used for |
|---|---|---|
| Dua AI engine | `https://dua-ai-engine-228305912525.us-central1.run.app` | `POST /api/chat` (chat) |
| Dua API | `https://dua-api-228305912525.us-central1.run.app` | reserved for later (history, accounts, etc.) |

## `POST /api/chat` (non-streaming, current)
Headers:
- `Authorization: Bearer <Supabase user JWT>` (required)
- `X-Dua-App-Token: <token>` (only if `DUA_APP_TOKEN` is configured)
- `Content-Type: application/json`

```json
{ "stream": false, "mode": "agent",
  "messages": [ {"role":"user","text":"hi"}, {"role":"model","text":"..."} ] }
→ 200 { "text": "..." }      | 4xx/5xx { "error": "..." }
```
Roles are `user` / `model`. The app sends the last 20 turns.

**Agent mode:** every request includes `"mode": "agent"` (streaming and non-stream fallback), matching Dua Android. This enables the engine's Agent pipeline; the server uses `AGENT_ENGINE=adk` when configured.

## Streaming (`"stream": true`, SSE) — default
The app first sends `"stream": true` with `Accept: text/event-stream`. The engine replies with SSE frames
`event: <type>\ndata: <json>\n\n`; the app uses `text` (`{"type":"text","delta":"..."}`), `error`
(`{"message":"..."}`) and `done`; `thinking`/`progress` provide activity labels, while other valid JSON events
(such as `sources`, `notice`, `usage`) are ignored.
Each delta is appended to the agent bubble live (the typing indicator is replaced by the bubble on the first delta).
**Completion:** success requires a complete, blank-line-delimited `done` JSON event (or an
`event: done` frame carrying a JSON object), plus non-empty text. EOF alone, malformed data or an
unterminated final frame is not completion. JSON event types are authoritative; `event:` supplies
the type when omitted from JSON. Comments/heartbeats, multiline `data:` and unknown valid JSON
events remain supported. Raw `[DONE]` is not part of this engine's protocol. A `done` event with
`truncated: true` reports an incomplete output-limit result, not a completed answer.
**Fallback:** one non-stream request is allowed only for HTTP 406/415 or a non-SSE response before
any text was shown. Network errors, missing completion, parse errors, server errors and partial
output never trigger automatic replay. Partial chat text stays visible with the existing error/
manual-retry UI. Translation returns a failure rather than a completed partial translation;
close the error and explicitly select Translate again if you want another request. Stop/cancellation
is not shown as an interruption error.

## Android wiring
- `AndroidManifest.xml`: `INTERNET` permission.
- `app/build.gradle.kts`: reads `local.properties` → `BuildConfig.DUA_ENGINE_URL`, `DUA_API_URL`,
  `DUA_APP_TOKEN`, `SUPABASE_URL`, `SUPABASE_ANON_KEY`, and (debug builds only) `DUA_DEV_JWT`.
- `data/chat/ChatRepository.kt`: `HttpURLConnection` + `org.json` (no new deps), `Dispatchers.IO`.
- `feature/chat/ChatViewModel.kt`: `StateFlow<ChatUiState>` (messages, loading, error).
- `OrhanNavHost` passes the ViewModel state into `ChatScreen`; Send calls `ChatViewModel.send`.

## Auth (Supabase email/password)
- `data/auth/AuthRepository.kt`: Supabase GoTrue REST (`/auth/v1/token?grant_type=password|refresh_token`,
  `/auth/v1/logout`) with `SUPABASE_URL` + `SUPABASE_ANON_KEY` only. Session stored in app-private DataStore
  (`orhan_auth`); access token auto-refreshes when <60 s from expiry. No new dependencies.
- Secure persistence: both tokens must be encrypted successfully before one atomic session save. There is no plaintext fallback. Existing `enc:v1` records remain compatible. Legacy plaintext is resealed before any trusted session is published; unreadable/unsupported records or failed legacy migration require sign-in again and remove only local Auth fields when storage is writable—not chats, memory or files.
- Recovery: a failed new sign-in save shows the existing sign-in error and never publishes that new account. A safe prior encrypted session is retained if one existed. If refresh rotated tokens but the new pair cannot be saved securely, require sign-in again instead of trusting an unsaved pair. A failed explicit sign-out storage write is reported in Settings and can be retried; it is not silently claimed successful.
- Local race guards: refresh network I/O is outside the auth-mutation lock; logout/new sign-in invalidate older results. Request-scoped generation handles ensure old HTTP 401s cannot clear a newer login or refreshed session, even if token strings match. Incomplete session writes are never published.
- Remote limitation: the existing logout REST call remains best-effort and its server-side scope is unchanged. Local race protection is not an immediate access-JWT revocation guarantee; Supabase documents that revoked-session JWTs can remain valid until expiry. A remote/global logout can also affect other server sessions independently of these local guards. No Supabase settings, sessions or keys were changed by this fix. References: [sessions](https://supabase.com/docs/guides/auth/sessions), [signing out](https://supabase.com/docs/guides/auth/signout).
- Opening a chat while signed out shows `feature/auth/SignInScreen`. After sign-in the session JWT is sent as
  `Authorization: Bearer …` to the Dua engine. A 401 from the engine clears the session (sign in again).
- Use the same Supabase project and owner account as Dua (`DUA_ALLOWED_EMAIL` on the backend); Email provider
  must be enabled and the account's email confirmed. No sign-up UI (owner-only app).
- `DUA_DEV_JWT` is still honored as a debug-only fallback when no session exists; leave it empty to use sign-in.

## `local.properties` template (never commit — it is in `.gitignore`)
```properties
sdk.dir=/path/to/Android/sdk
DUA_ENGINE_URL=https://dua-ai-engine-228305912525.us-central1.run.app
DUA_API_URL=https://dua-api-228305912525.us-central1.run.app
# Optional: only if the engine enforces the app token
DUA_APP_TOKEN=
# Supabase sign-in: same project as Dua (anon/publishable key is public-safe but keep it out of git)
SUPABASE_URL=https://<project-ref>.supabase.co
SUPABASE_ANON_KEY=
# Optional, debug only: fallback access token when not signed in (leave empty)
DUA_DEV_JWT=
```

## Attachments & voice (client)
- **Attach:** the paperclip opens the system document picker (image/*, PDF, text/*, audio/*, video/*). One file per message,
  max 4 MB, sent as the engine's existing per-message field `"attachments":[{"mimeType":"...","data":"<base64>"}]`
  on the **latest** user turn only (older turns send no bytes, even in-session). The bubble shows `📎 name`. File bytes are kept in memory only (not saved in history), so after
  an app restart old files are no longer resent.
- **Mic:** opens the system speech recognizer (`RecognizerIntent`) and inserts the transcript into the composer; the
  user reviews and taps Send. No `RECORD_AUDIO` permission needed. Raw voice-note upload is not wired yet.
