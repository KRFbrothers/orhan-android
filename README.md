orhan.ai (KRFbrothers/orhan-ai)
orhan.ai (com.orhan.ai) is a private, owner-locked multimodal GenAI assistant built across native Android (Jetpack Compose + Material 3), Web (React 19 + TypeScript + Vite 6), a Node.js API & Web Host (orhan-api), and a Python Cloud Run AI Engine (orhan-ai-engine, powered by Vertex AI Gemini and Google ADK), backed by Supabase Auth, PostgreSQL RLS, and private Cloud Storage.
For AI Coding Agents (Antigravity, Grok Build, Claude Code, Codex, Cursor): Read AGENTS.md first! It enforces the Zero-Assumption & Anti-Hallucination Protocol (Section 0), Canonical File Ownership Map (Section 2), Strict Anti-Duplication Rules, Three-Layer Design Token Architecture, and Mandatory Verification Commands.
1. Repository Structure (Monorepo Layout)
All five monorepo consolidation stages are complete (see docs/architecture/REPO_STRUCTURE.md and docs/ARCHITECTURE.md):
orhan-ai/
├── AGENTS.md                    # Mandatory rules, zero-assumption lock & canonical file map for AI agents
├── README.md                    # Project overview, setup, verification & AI prompt guide
├── SUPABASE.md                  # Supabase Auth, RLS, Storage & cloud snapshot contracts
├── SIGNING.md                   # APK signing & personal release verification guide
├── SECURITY.md                  # Security & owner-lock policies
├── apps/
│   ├── android/                 # Android Gradle workspace (:app and :benchmark modules)
│   │   ├── app/                 # Native Android app (com.orhan.ai) — Compose UI, ViewModel, data, tests
│   │   ├── benchmark/           # Macrobenchmark & Baseline Profile generator
│   │   ├── gradle/              # libs.versions.toml & Gradle 8.14.4 wrapper
│   │   ├── gradlew / gradlew.bat
│   │   └── build.gradle.kts / settings.gradle.kts
│   ├── api/                     # Node.js 24+ backend service (orhan-api) & static web host
│   ├── web/                     # React 19 + TypeScript + Vite 6 browser frontend (builds to apps/api/site/)
│   └── ai-engine/               # Python 3.12 Flask + Vertex AI + Google ADK engine (orhan-ai-engine)
├── infra/
│   └── database/migrations/     # PostgreSQL RLS, cloud snapshots, Storage, agent_notes & workflow SQL
├── docs/                        # Detailed architecture, UI ownership, operations & testing guides
└── tooling/scripts/             # Repository helper scripts (e.g., verify-apk-signature.py)
2. Android App
The Android application lives in apps/android/app/ under package com.orhan.ai.
Key Architecture Highlights
Single-Activity Jetpack Compose Architecture: MainActivity.kt → ui/OrhanApp.kt → ui/navigation/OrhanNavHost.kt → ui/chat/ChatRoute.kt → ui/chat/ChatScreen.kt.
Clean Architecture & Modular State:
UI Layer (ui/): Pure, stateless @Composable screens and sheets (ui/chat/, ui/history/, ui/agent/, ui/projects/, ui/features/, ui/voice/, ui/settings/, ui/components/).
Presentation Layer (viewmodel/): ChatViewModel.kt decomposed into 15 focused domain extension files (ChatAccount.kt, ChatAgentControls.kt, ChatAttachments.kt, ChatMemory.kt, ChatModes.kt, ChatProjects.kt, ChatVoice.kt, etc.).
Data & Domain Layer: Dedicated packages for account/, network/, history/, projects/, files/, attachments/, settings/, setup/, tasks/, voice/, imagine/, ondevice/, backup/, library/, and model/.
Three-Layer Design System (ui/theme/):
Raw hex colors live only in ui/theme/Color.kt (enforced by NoRawHexColorTest.kt).
Font sizes (.sp) live only in ui/theme/Type.kt (enforced by TypographyAliasesTest.kt).
Light and Dark mode tokens are consumed via OrhanTheme.colors, MaterialTheme.colorScheme, and scoped token objects (HomeTheme.colors, ChatTheme.colors, DrawerTheme.colors, SheetTokens, PageTokens, SettingsTokens).
Customizing Backend URLs in Android Builds
apps/android/app/build.gradle.kts configures two independent HTTPS endpoints:
ORHAN_API_BASE_URL (defaults to https://orhan-api-228305912525.us-central1.run.app) for account, cloud snapshot sync, files, Imagine, and voice services.
ORHAN_CHAT_BASE_URL (defaults to https://orhan-ai-engine-228305912525.us-central1.run.app) for streaming Gemini chat and Agent requests.
To override them for a local or custom build without editing tracked source files, pass Gradle properties:
bash apps/android/gradlew -p apps/android :app:assembleDebug \
  -PORHAN_API_BASE_URL="https://your-api.example.com" \
  -PORHAN_CHAT_BASE_URL="https://your-engine.example.com"
3. Local Setup
Prerequisites
Android Development: Android Studio (Canary / Ladybug or newer), JDK 17 or JDK 21 LTS (or Android Studio's bundled jbr), and Android SDK Platform 35.
Node API & Web (apps/api/, apps/web/): Node.js 24+ and npm.
Cloud Run AI Engine (apps/ai-engine/): Python 3.12+ and pip.
Running Each Layer Locally
Android App (apps/android/):
Open the apps/android directory in Android Studio (or open the repository root and run Gradle tasks with -p apps/android).
Connect your physical Android phone (with Developer Options > USB Debugging enabled) or start an emulator.
Node API (apps/api/):
Copy apps/api/.env.example to apps/api/.env (never commit .env).
Run:
cd apps/api
npm ci
npm start
Web Frontend (apps/web/):
Build the static site into apps/api/site/:
cd apps/web
npm ci
npm run build
Python AI Engine (apps/ai-engine/):
Copy apps/ai-engine/.env.example to apps/ai-engine/.env, export required variables, and see apps/ai-engine/README.md for local Flask / Gunicorn startup.
4. Android and Routine Checks
Always run checks from the repository root (or from apps/android for Gradle):
Windows PowerShell (From Repository Root)
# 1. Compile Debug APK
.\apps\android\gradlew.bat -p apps/android :app:assembleDebug --no-daemon --max-workers=2

# 2. Run Full Unit & Compose UI Test Suite (includes NoRawHexColorTest & Contrast checks)
.\apps\android\gradlew.bat -p apps/android :app:testDebugUnitTest -Porhan.ui-record --no-daemon --max-workers=2

# 3. Run Android Lint
.\apps\android\gradlew.bat -p apps/android :app:lintDebug --no-daemon --max-workers=2

# 4. Install Debug APK on Connected Phone
.\apps\android\gradlew.bat -p apps/android :app:installDebug --no-daemon
macOS / Linux / Git Bash (From Repository Root)
# 1. Compile Debug APK
bash apps/android/gradlew -p apps/android :app:assembleDebug --no-daemon --max-workers=2

# 3. Run Full Unit & Compose UI Test Suite
bash apps/android/gradlew -p apps/android :app:testDebugUnitTest -Porhan.ui-record --no-daemon --max-workers=2

# 3. Run Android Lint
bash apps/android/gradlew -p apps/android :app:lintDebug --no-daemon --max-workers=2

# 4. Node API, Python Engine & Web Tests
(cd apps/api && npm test)
(cd apps/ai-engine && python -m pytest -q tests)
(cd apps/web && npm test && npm run build)
5. Non-Coder Owner Guide: Zero-Assumption Prompts for AI Agents
Because AGENTS.md now enforces Section 0 (Zero-Assumption & Anti-Hallucination Protocol), Section 2 (Canonical File Ownership Map), and Section 5.2 (Updating Hardcoded Test Assertions), copy-paste these prompts into Google Antigravity (agy), Grok Build (grok), or Codex (codex):
Prompt A: Replace UI/UX Design In-Place (Zero Assumptions, Zero Duplicates)
Read AGENTS.md first and strictly obey Section 0 (Zero-Assumption Protocol), Section 2 (Zero Duplication & Canonical File Map), and Section 5 (Three-Layer Design Token Architecture & Test Updates). HARD STOP RULES: - Ignore legacy files SPEC-gemini-android-ui.md, tasks/plan.md, tasks/todo.md, and design/. - DO NOT assume, guess, or invent any colors, fonts, layouts, fake buttons, or unrequested features on your own. - Every single UI change must come strictly from verified REA evidence (decompiled classes/resources or live screenshots of the reference app on my phone). If anything is missing or unclear, STOP and ask me in simple Hinglish/English before writing code. - Edit ONLY the existing canonical files in apps/android/app/src/main/java/com/orhan/ai/ in-place (Color.kt, OrhanTheme.kt, Type.kt, Shape.kt, Spacing.kt, *Tokens.kt, ChatScreen.kt, ChatTopBar.kt, ChatContent.kt, ChatComposer.kt, AttachmentSheet.kt, HistoryDrawer.kt, AgentScreen.kt, ProjectsList.kt, SettingsScreen.kt, ProfileScreen.kt, MemoryScreen.kt, ArtifactCanvasSheet.kt). Never create duplicate files. - Update expected token assertions in existing UI tests (such as PolishedThemeTest.kt or ChatTokensComponentsTest.kt) to match the new approved tokens rather than reverting the UI. - Verify with .\apps\android\gradlew.bat -p apps/android :app:assembleDebug and .\apps\android\gradlew.bat -p apps/android :app:testDebugUnitTest -Porhan.ui-record.
Prompt B: Clean Up Old Confusing Spec Files (One-Time Cleanup Prompt)
Read AGENTS.md first. Remove the obsolete historical task files SPEC-gemini-android-ui.md, tasks/plan.md, and tasks/todo.md so future agent sessions do not confuse the old Gemini recording spec with our active UI goals, then run git status to confirm a clean working tree.
Prompt C: Live Phone Comparison & Auto-Fix Loop
Read AGENTS.md first. Build and install orhan-ai on my connected phone (.\apps\android\gradlew.bat -p apps/android :app:installDebug), launch it via REA's start_adb_app, capture screenshots with capture_adb_screen, compare them against the reference app screenshots on my phone, and fix only the observed visual differences in-place inside orhan-ai's canonical files without inventing any unrequested changes.
6. Further Documentation
docs/README.md: Full documentation index.
docs/ARCHITECTURE.md: End-to-end runtime map, chat request lifecycle, data ownership, and GitHub permission boundaries.
docs/AGENT.md: Cloud Run orhan-ai-engine tool loop, Google ADK parallel mode, and coding workspace details.
docs/ui/COMPONENT_OWNERSHIP.md & docs/ui/LEGACY_CHAT_SHELL.md: Single-owner UI component contracts.
docs/ui/design-system-tokens.md: Design token migration and contrast verification rules.
docs/TESTING.md & docs/OPERATIONS.md: CI test suites and Cloud Run deployment runbooks.