# Orhan backend plan

Status: **plan + minimal stub** (not deployed). Orhan is fully separate from Dua —
no Dua Supabase project, keys, Firebase, or services are reused.

## Survey (2026-10-28)
- No Orhan backend repo exists (only `orhan-android` and `orhan-android-design-kit`).
- `ChatScreen.kt` is local-only; no networking libs, no `INTERNET` permission.

## Options
| Option | Cost | Pros | Cons |
|---|---|---|---|
| **A. Cloudflare Worker + OpenRouter (free models)** | $0 (100k req/day Workers free) | One file, secret stays server-side, SSE streaming, fast global | No DB (add D1/KV later) |
| B. Supabase Edge Function + OpenRouter | $0 free tier | Auth + Postgres for history built in | New Supabase project required (must NOT be Dua's); projects pause when idle |
| C. Direct app → OpenRouter | $0 | Zero backend | API key ships in APK — **rejected** |

## Recommended: Option A
Worker in `backend/worker/` exposes `POST /chat`:
```json
{ "messages": [{"role":"user","content":"hi"}] }
→ { "reply": "..." }
```
Guards: shared `X-Orhan-Key` app header (light abuse filter), max 20 messages / 8k chars,
model from `MODEL` var (default a `:free` OpenRouter model). Add Supabase (new, Orhan-only)
later only if accounts/history are needed.

### Deploy (owner does this; new Orhan-only accounts/keys)
```
cd backend/worker && npm i -g wrangler && wrangler login
wrangler secret put OPENROUTER_API_KEY   # new key, Orhan-only
wrangler secret put ORHAN_APP_KEY        # random string
wrangler deploy
```

## Android wiring steps
1. Manifest: `<uses-permission android:name="android.permission.INTERNET"/>`.
2. Deps: OkHttp + kotlinx-serialization (or Ktor client).
3. `BuildConfig.ORHAN_API_URL` / `ORHAN_APP_KEY` from `local.properties` (not committed).
4. `ChatRepository.send(messages): Result<String>` (suspend, Dispatchers.IO).
5. `ChatViewModel` holding `StateFlow<List<Message>>` + loading/error; `ChatScreen` reads it
   instead of local list; send button → `viewModel.send(text)`.
6. Later: SSE streaming, retry/backoff, persisted history (Room).

No APK release is part of this change.
