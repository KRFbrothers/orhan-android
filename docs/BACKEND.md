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
{ "stream": false,
  "messages": [ {"role":"user","text":"hi"}, {"role":"model","text":"..."} ] }
→ 200 { "text": "..." }      | 4xx/5xx { "error": "..." }
```
Roles are `user` / `model`. The app sends the last 20 turns. Streaming (`"stream": true`, SSE) is a follow-up.

## Android wiring
- `AndroidManifest.xml`: `INTERNET` permission.
- `app/build.gradle.kts`: reads `local.properties` → `BuildConfig.DUA_ENGINE_URL`, `DUA_API_URL`,
  `DUA_APP_TOKEN`, `SUPABASE_URL`, `SUPABASE_ANON_KEY`, and (debug builds only) `DUA_DEV_JWT`.
- `data/chat/ChatRepository.kt`: `HttpURLConnection` + `org.json` (no new deps), `Dispatchers.IO`.
- `feature/chat/ChatViewModel.kt`: `StateFlow<ChatUiState>` (messages, loading, error).
- `OrhanNavHost` passes the ViewModel state into `ChatScreen`; Send calls `ChatViewModel.send`.

## Auth (interim)
Orhan has no Supabase sign-in UI yet. For development, put a valid Supabase access token
in `DUA_DEV_JWT` (debug builds only; release builds always get an empty value). Next step:
add Supabase Auth sign-in and pass the session token via `ChatRepository(tokenProvider = …)`.

## `local.properties` template (never commit — it is in `.gitignore`)
```properties
sdk.dir=/path/to/Android/sdk
DUA_ENGINE_URL=https://dua-ai-engine-228305912525.us-central1.run.app
DUA_API_URL=https://dua-api-228305912525.us-central1.run.app
# Optional: only if the engine enforces the app token
DUA_APP_TOKEN=
# For upcoming Supabase sign-in (anon key is public-safe but keep it out of git)
SUPABASE_URL=https://<project-ref>.supabase.co
SUPABASE_ANON_KEY=
# Debug only: a Supabase user access token (expires ~1h)
DUA_DEV_JWT=
```
