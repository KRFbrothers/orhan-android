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

## Auth (Supabase email/password)
- `data/auth/AuthRepository.kt`: Supabase GoTrue REST (`/auth/v1/token?grant_type=password|refresh_token`,
  `/auth/v1/logout`) with `SUPABASE_URL` + `SUPABASE_ANON_KEY` only. Session stored in app-private DataStore
  (`orhan_auth`); access token auto-refreshes when <60 s from expiry. No new dependencies.
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
