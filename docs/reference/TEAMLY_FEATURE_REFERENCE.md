# Teamily Non-Social Feature Reference — for the Orhan Android Project

**Document type:** Implementation reference / acceptance baseline
**Scope:** Chats · Agents · Memory · Artifacts · Meetings
**Explicitly out of scope:** Discover / Feeds / Explore, public posts, social profile, followers, likes, comments, leaderboards, invites, billing.
**Version of this document:** 1.0 — 2026-11-02

---

## 0. Scope, method, and honesty statement

This document was produced **without access to the Orhan source tree and without a running Android device or a signed-in Teamily account**. The sandbox contained no Orhan repository, no APK, and no emulator. Therefore:

- **No screen was exercised at runtime for this document.**
- Every behavioural statement below is derived from (a) Teamily's own public pages and public changelog, and (b) a published, machine-generated screen/route reference for the Teamily web and mobile clients.
- Where a behaviour could not be established from those sources, it is marked **unknown** rather than guessed.
- No backend API, endpoint, database schema, table, or storage mechanism is asserted anywhere in this document. Where persistence is described, it is described only as *user-observable persistence* ("still there after restart"), never as an implementation.

> **Read this before using the labels:** "Verified" in this document means *verified as a source statement* — the text was read verbatim in a primary source during this run. It does **not** mean "tested on an Orhan build". Nothing in this document is device-verified. See §0.1.

### 0.1 Legend

| Label | Meaning | What it licenses you to do |
|---|---|---|
| **Verified** | Read verbatim in a primary source during this run (Teamily public page, public changelog, or the published screen/route reference). The quoted behaviour is stated by the source, not inferred by me. | Treat as a documented fact about Teamily. Still re-test on device before shipping. |
| **documentation-based** | Stated by Teamily's public documentation or changelog, or described in the published screen/route reference, but not independently confirmed by me beyond reading that description. | Treat as the intended behaviour. Verify on device. |
| **inferred** | My conclusion drawn from documented behaviour. Plausible and useful for planning, but **not itself stated** by any source. | Treat as a hypothesis. Must be confirmed before it becomes a requirement. |
| **unknown** | Not observable from anything available. Could not be established. | **Do not implement from this document.** Must be resolved by inspecting the app or asking the product owner. |

### 0.2 Sources used

| # | Source | Type | Used for |
|---|---|---|---|
| S1 | `https://teamily.ai` (product landing page) | Official | Feature vocabulary: studios, living memory, connected context, agent surfaces |
| S2 | `https://teamily.ai/about` | Official | Global memory management, custom agents, cross-context memory |
| S3 | `https://teamily.ai/changelog` | Official | Concrete shipped behaviour: Agent Flows, Artifacts/Saves, Meeting Minutes, voice, search |
| S4 | `https://teamily.ai/about/help-center` | Official | Confirms the Help Center is billing/account only — **no feature documentation there** |
| S5 | `https://blog.teamily.ai/what-problems-we-learn-from-users-and-how-we-solve-them` | Official | Product intent: collaborative studios, living memory, agent handoff |
| S6 | `teamily-ai-webapp-handbook` (public GitHub repository, `SKILL.md`, `mobile/SKILL.md`, per-route `llm.md`) | Third-party, machine-generated from the client's own route tree | Screen inventory, routes, per-control behaviour, empty/error states |

**Important caveat on S6:** it is a third-party repository, not published by Teamily. It is machine-generated from route and screen metadata, which makes it unusually precise about *controls and routes*, but it is **not authoritative** and may lag the shipped build. Everything sourced only to S6 is labelled **documentation-based**, never **Verified**.

### 0.3 A naming caution that affects the whole document

The Memory area contains a view called **"My Profile"**. This is a *memory view* — a generated markdown summary of the user — and is **not** the social profile page. The social profile is out of scope. Do not conflate them when implementing. (documentation-based, S6)

---

## 1. Chats

### F1.1 — Send a text message, with @mentions

- **Label:** documentation-based
- **User goal:** Say something to a person, a group, or an agent, and have it appear in the thread immediately.
- **Path:** Chats tab → conversation list → tap a conversation → composer at the bottom.
  - Web: `/chat`
  - Android: `/conversation` → `/chat`
- **Steps & expected result:**
  1. Tap the composer. The keyboard opens and the field takes focus.
  2. Type text. The send control becomes active only when the field has content.
  3. In a **group**, type `@`. A member picker appears with "Mention everyone" as the first row, then individual members with avatar and name. Arrow keys / scrolling move the selection; tapping inserts the mention.
  4. Press send (or Enter on a hardware keyboard). The message appears at the bottom of the list, attributed to the sender, with a timestamp.
  5. Shift+Enter (desktop) inserts a newline instead of sending.
- **What is saved / persists after restart:** The message text, sender, timestamp, and its position in the thread. Draft text in the composer is also preserved across app restarts (documented as persistent draft support). **Verified** that the changelog lists persistent composer drafts; the exact restore semantics are **documentation-based**.
- **Empty / error states:**
  - Empty composer: send control inactive; the mic control is shown instead of send.
  - No conversation selected: a "not found" fallback with a "Back to conversations" action.
  - Send failure: the message is marked as failed and forward/recall are blocked on it (documented). Retry affordance: **unknown**.
- **Connections:** Feeds F1.2 (actions become available on the sent message), F1.3 (attachments), F1.4 (voice), F2.x (mentioning an agent starts an agent turn), F3.x (the message becomes a memory source).
- **Acceptance checks:**
  - [ ] Sending with an empty field is impossible (control disabled, not a silent no-op).
  - [ ] A sent message appears without a manual refresh.
  - [ ] `@` in a group opens a picker whose first row is "everyone".
  - [ ] `@` in a 1:1 chat does **not** open a member picker. *(inferred — confirm on device)*
  - [ ] Force-quit and relaunch: the message is still present, in the same position.
  - [ ] Type a draft, force-quit, relaunch: the draft is still in the composer. *(documentation-based)*

---

### F1.2 — Message actions (reply, copy, forward, translate, transcribe, react, unsend, delete, report)

- **Label:** documentation-based
- **User goal:** Do something with a message that already exists, without leaving the thread.
- **Path:** Any conversation → hover a message (desktop) or long-press it (Android) → action sheet.
- **Steps & expected result:** The action sheet exposes, per the published screen reference:

  | Action | Expected result |
  |---|---|
  | Reply | The message is quoted into a bar above the composer; an X clears it. Sending produces a message carrying the quote. Tapping the quote later scrolls to and highlights the original. |
  | Copy | Message text goes to the clipboard. For an image, the image is copied as PNG. For a media message, text plus attachment references are copied. |
  | Forward | A forward dialog opens to pick a destination conversation. |
  | Translate | A language picker opens; the chosen language is applied to that message. |
  | Transcribe | A voice message is converted to text in place. |
  | Save as sticker | An image message is added to the sticker library. |
  | Multi-select | Enters selection mode with this message selected and reveals the bulk bar. |
  | Recall / Unsend | Revokes a message **you** sent. (The changelog records the label being renamed from "Recall" to "Unsend".) |
  | Delete | Confirmation dialog, then the message is removed. |
  | Report | Group messages only: a reason list (spam, violence, child abuse, pornography, copyright, illegal drugs, personal details). |
  | React | An emoji strip (👍 ❤️ 😂 😮 😢 😡 👌) toggles a reaction. |

  Bulk bar (multi-select): Forward step-by-step, Merge forward (one combined message), Delete selected, Cancel.
- **What is saved / persists after restart:** Reactions, edits to state (recalled/deleted), and the quote relationship persist. A recalled message is replaced by a placeholder for other members. Whether a *deleted* message is removed for all participants or only locally is **unknown**.
- **Empty / error states:**
  - Actions that do not apply are absent or disabled: recall is own-messages-only; report is group-only; forward and recall are blocked on failed sends; quote and "Ask AI" are blocked on unsent messages; copy is enabled for agent text-only messages.
  - Reaction popover with no reactors: **unknown**.
- **Connections:** F1.1, F1.5 (jump-to-message from a quote), F4.5 (a message can be saved as an artifact), F3.x (a message can be a memory source).
- **Acceptance checks:**
  - [ ] Long-press opens the sheet; the sheet is dismissible without acting.
  - [ ] Recall is not offered on someone else's message.
  - [ ] Report is not offered in a 1:1 chat.
  - [ ] Reply → send → tap the quote → the original scrolls into view and is highlighted.
  - [ ] Merge-forward of 3 messages produces exactly one bubble that opens a read-only transcript.
  - [ ] A reaction survives app restart and is visible to the other participant.

---

### F1.3 — Attachments (file, image, video, camera)

- **Label:** documentation-based
- **User goal:** Send a document, photo, or video into the conversation.
- **Path:** Conversation → composer → paperclip (files) or camera icon (image/video).
- **Steps & expected result:**
  1. Tap the paperclip. The OS file picker opens.
  2. Select one or more files. A **batch preview dialog** opens showing thumbnails; a plus button appends more; each thumbnail has an X to remove it.
  3. Tap Send. A confirm dialog with upload progress appears, then the message is sent.
  4. For images/videos: the camera icon opens a dropdown with "Upload image" and "Upload video"; selection opens the corresponding confirm dialog.
  5. Tapping a sent image opens a full-screen viewer with previous/next, a thumbnail strip, zoom (double-click and scroll wheel on desktop), rotate, and download.
- **What is saved / persists after restart:** The uploaded file and its message. Uploads are described as resumable with durable placeholders, so an interrupted upload should not lose the message. **documentation-based**.
- **Empty / error states:**
  - Picker cancelled: nothing is staged, composer unchanged.
  - Upload failure: **unknown** — the changelog mentions durable placeholders and resumable uploads but does not state the failure UI.
  - Unsupported file type: **unknown**.
- **Connections:** F1.2 (copy/download from the viewer), F4.1/F4.4 (a file can become an artifact), F3.x (an attachment can be a memory source).
- **Acceptance checks:**
  - [ ] Selecting 3 files shows 3 thumbnails before anything is sent.
  - [ ] Removing one thumbnail leaves 2 and sends 2.
  - [ ] Cancelling the confirm dialog sends nothing.
  - [ ] A sent image opens full-screen and can be downloaded at full resolution.
  - [ ] Kill the app mid-upload and relaunch: the message is either sent or clearly marked failed — never silently vanished.

---

### F1.4 — Voice messages

- **Label:** documentation-based
- **User goal:** Send a spoken message instead of typing.
- **Path:** Conversation → composer → mic button (shown when the field is empty).
- **Steps & expected result:**
  1. Tap the mic. The input is replaced by recorder controls and recording starts.
  2. Controls: pause/resume (toggle), cancel (trash — discards), send (finalises).
  3. On send, the voice bubble appears with a play control and a duration.
  4. Playback: play/pause, a seek scrubber, and volume.
  5. Long-press a voice message → speech-to-text, or use the Transcribe action (F1.2).
  6. Clips up to 60 s are auto-transcribed on send (documented for the mobile client).
- **What is saved / persists after restart:** The audio file, its duration, and any transcript. A clip whose length the server never declared still streams (documented fix), so a missing duration must not block playback.
- **Empty / error states:**
  - Microphone permission denied: **unknown** — the changelog records that Android notification permission is requested before recording, but the denial UI is not described.
  - Recording interrupted by a call or backgrounding: recording survives audio interruptions and background kills (documented).
  - Zero-length recording: **unknown**.
- **Connections:** F1.2 (transcribe, forward), F5.x (a meeting recording surfaces as a voice message with a Meeting Minutes badge).
- **Acceptance checks:**
  - [ ] Mic is shown only when the composer is empty.
  - [ ] Cancel discards and restores the normal composer.
  - [ ] Pause then resume produces one continuous clip, not two.
  - [ ] A sent clip plays, seeks, and pauses correctly.
  - [ ] A clip with no declared duration still plays.
  - [ ] Transcript appears for a clip under 60 s.

---

### F1.5 — History and search

- **Label:** documentation-based
- **User goal:** Find an old message, file, or voice note — either in this conversation or anywhere.
- **Path:**
  - In-conversation: conversation header → search icon → `/chat/history-search`
  - Global: conversation list → search box → `/search`; per-category full results at `/search/conversation` (mobile) or a category dialog (desktop)
- **Steps & expected result:**
  1. **In-conversation search:** an auto-focused field (max 64 characters) with four tabs — Messages, Media, Files, Voice.
     - Messages requires a keyword.
     - Media / Files / Voice browse by type **without** a keyword.
  2. Results list with a live match count ("Searching…" while loading) and infinite scroll.
  3. Tapping a result stages a jump, dismisses the keyboard, and returns to the chat scrolled to that message.
  4. **Global search:** tabs All / Contacts / Groups / Agents / Messages / Files / Media. Empty query shows recents (recent contacts, groups, chat history, files, media). Tapping a conversation group opens the full per-conversation match list.
  5. Scrolling up in a thread paginates older history.
- **What is saved / persists after restart:** Search history (recent searches) persists and can be cleared from the search screen. Message history persists. Search runs off an index rather than a full scan (documented as a performance change) — this is an implementation note, not a user-visible guarantee.
- **Empty / error states:**
  - No results: "No messages found" (in-conversation) / "Type keywords to search" before a query.
  - Empty global query: recents, or an empty recents state — **unknown** what it renders when there is no history at all.
  - Search backend failure: **unknown**.
- **Connections:** F1.2 (jump-to-message from a quote uses the same mechanism), F3.x (memory pages link back to source chats), F4.5 (saved items are separately searchable).
- **Acceptance checks:**
  - [ ] Media/Files/Voice tabs return results with an empty keyword.
  - [ ] Messages tab with an empty keyword does not fire a query.
  - [ ] Tapping a hit lands on the exact message, highlighted, not merely the conversation.
  - [ ] Match count matches the number of rows actually listed.
  - [ ] Clearing search history empties the recents list and survives restart.

---

### F1.6 — Conversation-level controls (pin, mute, clear history, delete)

- **Label:** documentation-based
- **User goal:** Keep the important threads on top, silence the noisy ones, and get rid of the ones you do not want.
- **Path:** Conversation list → long-press / right-click a row (context menu); or open a conversation → header avatar / title / ⋯ → info panel.
- **Steps & expected result:**
  - **Pin / unpin** — the row moves to the top of the list and shows a pinned indicator.
  - **Mark as read** — clears the unread count and @mention markers (offered only when unread).
  - **Delete conversation** — confirmation, then the row and its messages are removed.
  - **Mute** — notifications for that conversation stop; a mute indicator appears on the row.
  - **Clear chat history** — confirmation, then all messages in the conversation are deleted but the conversation remains.
  - **Conversation translation** — a per-conversation toggle for automatic message translation.
- **What is saved / persists after restart:** Pin state, mute state, and translation setting persist per conversation. Deleted/cleared content does not return.
- **Empty / error states:**
  - Empty conversation list: an empty state with a "Discover agents" action (out of scope for Orhan — replace with a neutral empty state).
  - Clear-history confirmation dismissed: nothing changes.
- **Connections:** F1.5, F2.x (agent conversations expose extra sections in the same panel: skills, connectors).
- **Acceptance checks:**
  - [ ] Pin survives restart and reorders the list.
  - [ ] Mute survives restart and suppresses the notification.
  - [ ] Clear history keeps the conversation row but empties the thread.
  - [ ] Delete removes the row entirely.
  - [ ] Both destructive actions require confirmation.

---

## 2. Agents

### F2.1 — Create an agent

- **Label:** documentation-based
- **User goal:** Get a new AI agent that appears in your contacts and can be dropped into a conversation.
- **Path:** Conversation list → "+" (new/contact menu) → "Create agent"; or `/chat/create-agent`; or Contacts → My Agents → create.
- **Steps & expected result:**
  1. The create-agent dialog opens.
  2. Provide at minimum a name. (The editor requires a non-empty nickname; Save errors if blank.)
  3. Confirm. On success the route is replaced by the chat workspace with the new agent; on cancel it pops back to the previous screen.
  4. The agent now appears in Contacts → My Agents and can be messaged.
- **What is saved / persists after restart:** The agent's identity (avatar, name, description) and its presence in the agent list. Agents are account-scoped and sync across devices (documented: "same agents, same memory" across platforms).
- **Empty / error states:**
  - Blank name: Save is refused with an error.
  - Creation failure: **unknown**.
  - Agent quota reached: the public pricing material states a cap on custom agents per plan (5 free / 50 on Plus, per a third-party summary). The in-app message at the cap is **unknown**.
- **Connections:** F2.2 (edit), F2.4 (skills/connectors), F2.5 (flows), F1.1 (mention it in a group).
- **Acceptance checks:**
  - [ ] Creating with a blank name is blocked with a visible error.
  - [ ] After creation the agent is reachable from Contacts → My Agents without a refresh.
  - [ ] Cancelling creates nothing.
  - [ ] The agent survives restart and appears on a second device.

---

### F2.2 — Edit or delete an agent

- **Label:** documentation-based
- **User goal:** Change how an agent looks and is described, or remove it.
- **Path:** Contacts → My Agents → agent row → profile → Edit; or `/agents/config/[agentId]` (Android: `/agent-settings/[agentId]`).
- **Steps & expected result:**
  1. The editor shows an avatar block, a nickname field, and a description textarea.
  2. **Avatar:** camera button or "Change Photo" opens the image picker. A non-GIF image opens a 1:1 crop dialog with a 100–300 % zoom slider and drag-to-pan; Cancel discards, Save crop uploads and sets the draft avatar. GIFs skip cropping.
  3. Edit name/description. **Save** is disabled until the draft differs from the saved state, and shows a spinner while saving. Only changed fields are sent.
  4. **Delete agent** sits in a danger zone, behind a confirmation dialog naming the agent. On confirm the agent is permanently deleted and you are redirected to your profile.
- **What is saved / persists after restart:** Name, description, and avatar persist. Deletion is permanent — the agent is removed from cached lists and does not return.
- **Empty / error states:**
  - Blank nickname: Save errors.
  - Upload failure during avatar change: **unknown**.
  - Delete confirmation dismissed: nothing is deleted.
- **Connections:** F2.1, F2.4, F2.5, F1.6 (agent conversations show skills/connectors in the info panel).
- **Acceptance checks:**
  - [ ] Save is disabled on an unmodified form.
  - [ ] Changing only the description does not re-upload the avatar.
  - [ ] Cropping produces a square avatar.
  - [ ] Delete requires confirmation and the agent is gone after restart.
  - [ ] A deleted agent's conversations are handled gracefully (not a crash). *(inferred — confirm)*

---

### F2.3 — Agent instructions / persona

- **Label:** inferred (with a large unknown)
- **User goal:** Tell the agent who it is, how it should behave, and what it should refuse.
- **Path:** **unknown.** The documented agent editor exposes only avatar, nickname, and description. There is no documented free-text "instructions" or "system prompt" field.
- **Steps & expected result:** Teamily's public material states that agents are created and customised "in seconds" and that you can "create agents by chatting" — i.e. the configuration conversation is the configuration surface. The published screen reference does not show a dedicated instructions editor. **The exact mechanism is unknown.**
- **What is saved / persists after restart:** **unknown.** Whether a description field alone shapes behaviour, and whether conversational instructions persist as agent configuration, could not be established.
- **Empty / error states:** **unknown.**
- **Connections:** F2.4 (skills), F2.5 (flows), F3.x (memory may inform behaviour).
- **Acceptance checks (to be completed once the mechanism is known):**
  - [ ] There is a discoverable place to state the agent's role.
  - [ ] Instructions survive restart.
  - [ ] Instructions are visible/editable after creation, not write-once.
  - [ ] Two agents with different instructions behave differently on the same prompt.

> **Action required:** this is the single largest gap in the document. Resolve F2.3 by inspecting a real agent's settings before implementing.

---

### F2.4 — Agent tools: skills and connectors

- **Label:** documentation-based
- **User goal:** Give an agent extra capabilities — packaged skills, and access to third-party apps.
- **Path:**
  - Skills: agent conversation → info panel → "Agent skills" → `/chat/agent-skills`
  - Connectors: agent conversation → info panel → "Agent connectors" (per-connector toggles) → "Connect app" → `/settings/connectors`
- **Steps & expected result:**
  1. **Skills** has three tabs:
     - **Browse** — searchable, tag-filterable library. An Add/Added toggle adds or removes a skill and syncs the enabled set. Load more / Retry for paging and errors.
     - **Upload** — pick a `.skill`/`.zip` package, or paste a GitHub URL and Import.
     - **Create** — describe the skill in a multiline prompt (with template presets) and send it to the agent conversation.
  2. **Connectors** — an expandable list with a toggle per connector, plus "Connect app" which opens the account-level connector directory.
  3. In the directory: search apps; **Connect** opens an OAuth popup and, on success, the app moves to the connected group at the top; **Disconnect** removes the connection.
- **What is saved / persists after restart:** The enabled-skill set per agent, and the connected/disconnected state per app, both persist and sync.
- **Empty / error states:**
  - Skill library load error: a Retry action.
  - No skills added: **unknown** what the agent panel shows.
  - OAuth popup closed or failed: the popup closes and the list is not updated.
  - Disconnect partial failure: a partial-success toast (documented).
- **Connections:** F2.2, F2.5, F1.6.
- **Acceptance checks:**
  - [ ] Adding a skill then restarting keeps it added.
  - [ ] Removing a skill is reflected in the agent's behaviour on the next turn.
  - [ ] A connector toggle off actually prevents the agent from using it. *(inferred — confirm)*
  - [ ] Disconnect removes the app from the connected group.
  - [ ] A failed OAuth leaves the app disconnected and shows an error, not a false "connected".

---

### F2.5 — Workflows (Agent Flows, automations)

- **Label:** documentation-based
- **User goal:** Define, once, how work moves between agents — and optionally run it on a schedule.
- **Path:**
  - **Agent Flows:** chat header, or the composer's create menu, or a pushed page on phones. (Route not published in the screen reference — **unknown** exact path.)
  - **Automations:** `/automations`
- **Steps & expected result:**
  1. **Agent Flows** — author a flow as a single diagram of *who hands work to whom*: first edit creates it; rich-text prompts per node; re-pluggable connectors; per-side slots; cards grown from a dot. Canvas supports undo. A flow can be **published**, and once live it can be **triggered by @mention** on the agent where the flow starts.
  2. **Automations** — a template gallery with search and category pills. Picking a template opens an inline task builder with four editable chips: **intent** (verb), **schedule** (when/how often, with date/time/weekday/condition inputs and a live preview), **team** (add agents / add people), and **scope** (one-time delivery vs. a lasting goal). The compiled prompt updates live. **Generate Prompt** prefills the composer in Personal AI, or creates a new group with the chosen members.
- **What is saved / persists after restart:** A published flow persists and remains mention-triggerable. An automation, once generated into a conversation, persists as that conversation's task. Whether a flow is editable by non-owners is governed by a per-flow access model (documented: drive-style defaults, edit rights split into policy/lease/manage, an advisory edit lease with heartbeat and takeover).
- **Empty / error states:**
  - Create menu hides the flow section when there is nothing to offer (documented).
  - Flow canvas refusals are rendered readably (documented fix).
  - Automations with no templates in a category: **unknown**.
  - Signed out on Generate Prompt: redirect to login.
- **Connections:** F2.1, F2.2, F2.6, F1.1 (@mention trigger).
- **Acceptance checks:**
  - [ ] A flow can be created from the chat header without leaving the chat.
  - [ ] Publishing a flow makes it mention-triggerable on the starting agent.
  - [ ] Undo works on the canvas.
  - [ ] An automation's compiled prompt reflects every chip change.
  - [ ] Generating with only Personal AI prefills Personal AI; with members added it creates a group.

---

### F2.6 — Handoffs between agents

- **Label:** documentation-based (mechanism) / inferred (semantics)
- **User goal:** Have one agent pass work to another instead of you re-briefing each one.
- **Path:** Agent Flows canvas (F2.5); or, in a group, @mention a second agent in the same thread.
- **Steps & expected result:**
  - **Designed handoff:** in a flow, a connector between two agent cards defines who hands work to whom. The receiving agent's prompt is authored per node.
  - **Ad-hoc handoff:** in a group containing several agents, @mentioning agent B after agent A has replied gives B the thread context; B answers in the same thread.
  - Teamily's public material describes a "messaging-native communication protocol" that lets humans and agents "communicate, delegate, and execute together", and states that agents can hand work to each other inside one shared conversation.
- **What is saved / persists after restart:** The flow definition and its published state. The conversation transcript, including each agent's contribution. Whether an in-flight handoff resumes after a restart is **unknown**.
- **Empty / error states:**
  - Handoff to an agent that has left the group: **unknown**.
  - Flow with a dangling connector: the canvas is documented to allow re-plugging connectors; the runtime behaviour of an incomplete flow is **unknown**.
- **Connections:** F2.5, F1.1, F3.x (handoff context is drawn from memory).
- **Acceptance checks:**
  - [ ] A two-agent flow visibly routes work from A to B.
  - [ ] B's answer reflects A's output without the user restating it.
  - [ ] @mentioning B in a group gives B the preceding thread context.
  - [ ] An unpublished flow cannot be triggered by mention.
  - [ ] Killing the app mid-handoff does not corrupt the thread.

---

## 3. Memory

> **Framing:** Teamily describes memory as *living memory* — it "continuously builds and reorganizes itself from every conversation", and is explicitly **not** a searchable chat history. (Verified: this wording appears on the public landing page.) The Memory workspace is a two-pane knowledge base: a left index (search, All/Updates/Tags filters, reserved views, grouped page list) and a main area that switches between Overview, a page detail, My Profile, To-dos, and an Artifacts gallery. The `page` parameter selects what the main area shows. (documentation-based, S6)

### F3.1 — Automatic memories

- **Label:** documentation-based
- **User goal:** Not have to write anything down — the system captures what mattered from conversations.
- **Path:** Memory → left index → **All** (default) shows captured pages. No explicit "capture" action exists.
- **Steps & expected result:**
  1. Hold a conversation (1:1, group, or with an agent).
  2. Open Memory. New pages appear in the index with a summary, an updated time, and up to two tags.
  3. A page marked **new** or **conflict** carries a chip in the list.
  4. **Dream** (an emerald sparkles button on a page) kicks off a background re-organise of that page; the editor locks read-only while it runs.
- **What is saved / persists after restart:** Pages, their bodies, tags, and version history. Deletion is a soft delete to trash for 30 days (documented), so a deleted page is recoverable within that window.
- **Empty / error states:**
  - No pages yet: the empty list offers "Write a page", "Open Personal AI", or "Clear tags" (when the emptiness is caused by tag filters).
  - Overview with fewer than 2 nodes: a "Create first page" action.
  - Memory service unavailable: a full-view maintenance banner (documented as a 503 fallback).
  - Conflict: a review card showing existing vs. new content side by side with Accept (merge) / Deny (dismiss).
- **Connections:** F1.x (every conversation is a potential source), F3.8 (source chats), F3.7 (graph), F4.x (artifacts are a separate gallery in the same workspace).
- **Acceptance checks:**
  - [ ] A substantive conversation produces at least one new page without any user action.
  - [ ] The page's summary is a one-liner, not a transcript dump.
  - [ ] Dream locks the editor while running and unlocks on completion.
  - [ ] A deleted page is recoverable from trash within 30 days.
  - [ ] A 503 shows the maintenance banner, not a blank screen.

---

### F3.2 — Topics (memory pages)

- **Label:** documentation-based (page model) / inferred (that "topics" == pages)
- **User goal:** Have one durable place per subject, which you can read and correct.
- **Path:** Memory → left index → tap a page; or **+ New page**.
- **Steps & expected result:**
  1. **+ New page** creates an "Untitled page" and opens it (not in edit mode). The button is disabled while a create is pending, to prevent double-creation.
  2. The detail view has: an editable **title** (click to edit; Enter/Escape/blur commits; empty reverts), an always-on **summary** textarea, a **tag editor**, and a single expandable **markdown body** editor with `##` sections inline.
  3. Edits autosave with a debounce. Images can be dropped or pasted into the body; the whole panel is a drop zone while editing.
  4. **Version history** lists revisions; selecting an older one previews it; a per-row Restore restores it.
  5. **Download page** exports the page as a file. **Share page** packs title + summary + body into a `.md` and opens the chat share sheet; an empty page gets a toast instead.
  6. **Delete page** soft-deletes to trash (30 days) and returns to Overview.
- **What is saved / persists after restart:** Title, summary, tags, body, attachments, and full revision history. Autosave means there is no explicit Save button — a change made and then backgrounded should still be there.
- **Empty / error states:**
  - Empty body: renders as an empty editor; Share is refused with a toast.
  - Save conflict (409): a dialog offering **Overwrite** (re-save against the server's current version) or **Discard** (refetch latest).
  - Read-only page: renders static markdown, no editing affordances.
  - Download failure: a toast.
- **Connections:** F3.3 (tags), F3.4 (Updates), F3.7 (graph node), F3.8 (source chats), F4.2 (markdown artifacts).
- **Acceptance checks:**
  - [ ] Typing in the body and immediately backgrounding the app does not lose the edit.
  - [ ] Emptying the title reverts to the previous title rather than saving blank.
  - [ ] Two tags AND together in the filter (not OR).
  - [ ] Restoring an old revision is reflected after restart.
  - [ ] A 409 produces the Overwrite/Discard dialog, never silent data loss.

---

### F3.3 — Tags

- **Label:** documentation-based
- **User goal:** Group memories across topics so you can pull up everything about one theme.
- **Path:** Memory → left index → **Tags** toggle (expands a tag cloud); and inside a page → tag editor.
- **Steps & expected result:**
  1. In a page, type a tag and press Enter to add it; each existing tag chip has an X to remove it.
  2. In the index, tap a tag chip to add it to the filter. Multiple tags **AND** into the query.
  3. The tag cloud collapses/expands via the Tags toggle.
- **What is saved / persists after restart:** The tag set per page, and the tag cloud derived from them.
- **Empty / error states:**
  - No tags exist: the cloud is empty — **unknown** whether it shows a hint.
  - Filter yields nothing: the empty state offers "Clear tags".
- **Connections:** F3.2, F3.4 (Updates can be filtered by tag), F3.1.
- **Acceptance checks:**
  - [ ] Adding a tag then restarting keeps it.
  - [ ] Two selected tags return only pages carrying both.
  - [ ] Removing the last tag chip restores the unfiltered list.
  - [ ] "Clear tags" empties the filter in one action.

---

### F3.4 — Updates

- **Label:** documentation-based
- **User goal:** See what changed recently — new memories, edits, and conflicts — without hunting.
- **Path:** Memory → left index → **Updates** filter (with a count badge).
- **Steps & expected result:**
  1. Tap Updates. The list switches to a feed of recently-changed, conflict, and new pages.
  2. Each row shows a conflict/new chip, a summary, an updated time, and up to two tags.
  3. An active search overrides Updates and returns to normal page results.
  4. Updates can be filtered by tag within update items (documented fix).
- **What is saved / persists after restart:** The underlying change history. Whether the Updates feed itself is "read" and resets is **unknown**.
- **Empty / error states:**
  - Nothing changed: **unknown** — no documented empty copy for Updates.
  - Load more is not offered in Updates mode (documented).
- **Connections:** F3.1, F3.2, F3.3.
- **Acceptance checks:**
  - [ ] A page edited in another session appears in Updates.
  - [ ] A conflict page appears in Updates with a conflict chip.
  - [ ] Typing a search term leaves Updates and shows page results.
  - [ ] The badge count matches the number of rows.

---

### F3.5 — My Profile (memory view)

- **Label:** documentation-based
- **User goal:** Read the system's current understanding of you, and correct it by regenerating.
- **Path:** Memory → left index → reserved view **Profile**.
- **Steps & expected result:**
  1. A read-only markdown body renders the profile.
  2. **Refresh** (a Dream button) bypasses the cache and regenerates it, with a toast for refreshed / empty / error.
  3. **Share** opens the chat share sheet to send the profile markdown as a `.md`; it is hidden when the profile is empty.
  4. On a non-503 error, a **Retry** action is shown.
- **What is saved / persists after restart:** The generated profile text and its freshness. It is read-only — there is no in-place editing documented.
- **Empty / error states:**
  - Empty: a backend-localised placeholder is rendered.
  - Error: Retry (non-503); maintenance banner (503).
- **Connections:** F3.1, F3.6, F3.7. **Not** the social profile (out of scope).
- **Acceptance checks:**
  - [ ] The view is read-only (no editable fields).
  - [ ] Refresh changes the content or reports "empty" honestly.
  - [ ] Share is hidden when empty.
  - [ ] The profile survives restart.

---

### F3.6 — Next Actions (the "To-dos" view)

- **Label:** inferred (that "Next Actions" is the To-dos view) / documentation-based (the view itself)
- **User goal:** See what you committed to, without re-reading conversations.
- **Path:** Memory → left index → reserved view **To-dos**.
- **Steps & expected result:**
  1. A read-only markdown body lists the extracted actions, with a freshness / updated-at label.
  2. **Refresh** regenerates it (toast: refreshed / empty / error).
  3. **Share** sends the to-dos markdown as a `.md`; hidden when empty.
  4. **Retry** on non-503 errors.
- **What is saved / persists after restart:** The generated list and its updated-at timestamp.
- **Empty / error states:** Empty → placeholder; error → Retry; 503 → maintenance banner.
- **Connections:** F3.1, F3.5, F5.2 (meeting minutes produce action items with owners — a likely feed into this view, **inferred**).
- **Acceptance checks:**
  - [ ] The view is read-only.
  - [ ] The updated-at label changes after a refresh.
  - [ ] Share is hidden when empty.
  - [ ] Items are actionable statements, not raw quotes. *(inferred quality bar)*

> **Naming note:** the user-facing label in the documented client is **"To-dos"**. "Next Actions" is the term used in the Orhan brief. Treat the mapping as **inferred** and confirm the label Orhan should ship.

---

### F3.7 — Overview (knowledge graph)

- **Label:** documentation-based
- **User goal:** See the shape of what the system knows, and jump into any part of it.
- **Path:** Memory → left index → reserved view **Overview** (the default main view).
- **Steps & expected result:**
  1. A force-directed graph renders pages as nodes, sized by reference count and coloured by category; conflict nodes are dashed.
  2. Hover/focus highlights a node and its connected edges and neighbours.
  3. Clicking a node opens that page in the detail view.
  4. Degraded or truncated data shows a notice rather than a silently incomplete graph.
- **What is saved / persists after restart:** Nothing user-authored here; the graph is derived. Node positions are not documented as persisted.
- **Empty / error states:**
  - Fewer than 2 nodes: an empty graph state with "Create first page".
  - Degraded/truncated: an explicit notice.
  - 503: maintenance banner.
- **Connections:** F3.2, F3.1, F3.3 (colour by category).
- **Acceptance checks:**
  - [ ] Clicking a node opens the right page.
  - [ ] A conflict page renders as a dashed node.
  - [ ] The empty state appears below 2 nodes and its action works.
  - [ ] A truncated graph says so.

---

### F3.8 — Source-chat connections

- **Label:** documentation-based
- **User goal:** Check where a memory came from, and get back to the conversation that produced it.
- **Path:** Memory → open a page → **Source chat** section.
- **Steps & expected result:**
  1. The page lists the chats the memory was extracted from.
  2. Tapping one opens that group or Personal-AI conversation.
- **What is saved / persists after restart:** The page↔conversation link. Whether the link survives deletion of the source conversation is **unknown**.
- **Empty / error states:**
  - No source chats: **unknown** whether the section is hidden or shows an empty label.
  - Source conversation deleted: **unknown**.
- **Connections:** F1.1, F1.5, F3.1, F3.2.
- **Acceptance checks:**
  - [ ] Every auto-captured page lists at least one source chat.
  - [ ] Tapping a source opens the correct conversation.
  - [ ] A manually created page shows no source chats (or an honest empty state).
  - [ ] A deleted source conversation does not crash the page.

---

## 4. Artifacts

> **Framing:** Teamily's public material describes "chat in, artifacts out" — agents ship web pages, slides, video, images, docs and whiteboards straight from a conversation, and every artifact is built with the context of the thread behind it. (Verified: this wording appears in Teamily's public article.) The changelog records `/docs` being renamed to `/artifacts`, with the sidebar label settling on **Artifacts**. (Verified: changelog entry.)

### F4.1 — Document artifacts

- **Label:** documentation-based
- **User goal:** Produce a real, editable document from a conversation and keep working on it with others.
- **Path:** Conversation → agent produces a document card → tap to open in Studio; or Memory → Artifacts gallery → a document card.
- **Steps & expected result:**
  1. Ask an agent for a document. A card appears in the thread.
  2. Tapping the card opens the document in the Studio surface (a docx editor for Word documents).
  3. The document supports collaborative editing with live presence (avatars of present collaborators; hover shows identity/status/mode; click follows a peer's cursor).
  4. A **Download** button in the Studio shell exports the docx.
- **What is saved / persists after restart:** The document body and its revision history. A shared document gets its own page rather than living only in the library.
- **Empty / error states:**
  - Document will not open: the client states *why* rather than showing a blank page (documented fix).
  - Invalid Studio parameters: a route-error page with a "Back to chat" link.
  - Sync failure: a source-sync failure notice.
- **Connections:** F4.6 (editing), F4.7 (export/share), F3.x (artifacts gallery lives in Memory), F1.3 (a chat upload can be saved as an artifact).
- **Acceptance checks:**
  - [ ] A generated document opens in an editor, not a read-only preview.
  - [ ] Two collaborators see each other's edits without a refresh.
  - [ ] Download produces a file that opens in a standard word processor.
  - [ ] A broken document explains itself instead of rendering blank.

---

### F4.2 — Markdown artifacts

- **Label:** documentation-based
- **User goal:** Get a clean, portable text artifact you can take anywhere.
- **Path:** Conversation → markdown card; or Memory → Artifacts gallery → markdown card; or Memory page → Share/Download.
- **Steps & expected result:**
  1. Markdown artifacts render with a source-mode toggle in the editor (documented).
  2. Collaborative markdown editing is supported, with Drive favourites and a library rebuilt around views and row items.
  3. Memory pages can be shared as `.md` into a chat, or downloaded.
- **What is saved / persists after restart:** The markdown body and revisions. Artifact covers render content thumbnails for markdown, text, JSON, CSV, and code (documented).
- **Empty / error states:**
  - Empty markdown: **unknown**.
  - Share of an empty memory page: a toast instead of a share sheet.
- **Connections:** F3.2, F4.6, F4.7.
- **Acceptance checks:**
  - [ ] Source-mode toggle round-trips without corrupting the body.
  - [ ] A markdown card shows a content thumbnail, not a generic icon.
  - [ ] Exported `.md` opens correctly in an external editor.

---

### F4.3 — Webpage / web app artifacts

- **Label:** documentation-based
- **User goal:** Ship a working page or small app from a conversation, with a link you can share.
- **Path:** Conversation → webpage card → open in Studio; or the Studio webapp route.
- **Steps & expected result:**
  1. Ask for a page/app. A preview renders inside Studio with back/forward/home controls.
  2. **Version picker** — for versioned webpage artifacts, a pill opens a list of every version (newest first) and switches the preview.
  3. **Edit mode** — edit a selected element's properties directly in the preview; anchor a comment to the code or the element you picked.
  4. **Share** — a share-link popover for the artifact.
  5. Code revision history is available; a source-sync failure is surfaced.
- **What is saved / persists after restart:** The artifact, its versions, and its revision history. A saved webapp is saved by claiming the row its workspace already owns (documented) — i.e. saving does not duplicate it.
- **Empty / error states:**
  - Invalid/missing artifact parameters: route-error page with "Back to chat".
  - Preview proxy failure: the client must not show the proxy's error JSON as the page (documented fix).
  - Sleeping sandbox: a toolbar control wakes it.
- **Connections:** F4.6, F4.7, F4.5.
- **Acceptance checks:**
  - [ ] The preview renders the actual page, not an error payload.
  - [ ] Switching versions changes the preview and the URL.
  - [ ] Share produces a link that opens the artifact for an authorised viewer.
  - [ ] Revision history lists real revisions, not raw ids.

---

### F4.4 — Media artifacts (image, video, slides)

- **Label:** documentation-based
- **User goal:** Get a visual deliverable — a poster, a clip, a deck — without leaving the chat.
- **Path:** Conversation → media card → tap to open the viewer; or Memory → Artifacts gallery → type filter.
- **Steps & expected result:**
  1. Images open in a lightbox with zoom/rotate and download.
  2. Video plays in a media lightbox; a sent video must not degrade to a tiny stamp (documented fix).
  3. Presentations open in a dedicated slides viewer; decks can be zipped with inlined HTML for copy and download (documented).
  4. The Artifacts gallery offers type chips (All + each type) to narrow the grid.
- **What is saved / persists after restart:** The media file and its card. Artifact cards open with the viewer that matches the type: code → new tab, docx → Office viewer, presentation → slides viewer, others → in-app file viewer.
- **Empty / error states:**
  - Media fails to load: **unknown**.
  - Empty gallery after filtering: "Clear filters" / "Clear search" / "View all" actions.
- **Connections:** F4.5, F4.6, F4.7, F1.3.
- **Acceptance checks:**
  - [ ] Each artifact type opens in its matching viewer.
  - [ ] A video plays at a sensible size, not a thumbnail-sized stamp.
  - [ ] Type filters narrow the grid and the empty state offers a reset.
  - [ ] Downloading an image yields full resolution.

---

### F4.5 — Saved messages (Saves)

- **Label:** documentation-based
- **User goal:** Keep a message or a finished card for later, without scrolling back to find it.
- **Path:** Message action menu → save; or a completed card's star; then Memory → Artifacts gallery / the saved shelf.
- **Steps & expected result:**
  1. Save a completed card **as the card itself** (not a text copy).
  2. Saved items are shelved **by kind**, with author attribution.
  3. Uploaded files and composite messages get their own save entry.
  4. A saved row can be **forwarded as the message it came from**.
  5. Each row opens with the viewer that matches it — including Word documents in the docx editor.
  6. A shared cloud document or whiteboard can be saved straight from the message menu.
- **What is saved / persists after restart:** The saved row, its kind, its author, and its star state (documented fix: the star now persists).
- **Empty / error states:**
  - Nothing saved: **unknown** empty copy.
  - Saved row whose source was deleted: **unknown**.
- **Connections:** F1.2, F4.1–F4.4, F4.7.
- **Acceptance checks:**
  - [ ] Saving a card then restarting keeps it starred.
  - [ ] The saved row opens in the correct viewer.
  - [ ] Forwarding a saved row reproduces the original message, not a link.
  - [ ] Rows are grouped by kind, not one flat list.

---

### F4.6 — Editing artifacts (Studio)

- **Label:** documentation-based
- **User goal:** Change the artifact in place, with the team, instead of asking the agent to regenerate it.
- **Path:** Artifact card → Studio (`/studio`), or the chat drawer's studio surface.
- **Steps & expected result:**
  1. **Header:** type icon, title, live-viewer avatars, version pill, share, close.
  2. **Mode toolbar** — buttons appear only for capabilities the artifact type declares:
     - Refresh preview
     - **Comments** mode (badge with open comment count)
     - **Markup** mode (free-draw brush strokes over the canvas)
     - **Edit** mode (editable artifacts only)
     - Zoom (25–400 % presets plus a custom value)
     - Fullscreen (desktop)
     - Collapse header
  3. **Adapter composer** at the bottom for add-comment / send-to-agent, targeting the conversation named in the URL.
  4. **Context slot** on the right for comments/inspector; on mobile it becomes a collapsible bottom sheet.
- **What is saved / persists after restart:** Edits, comments, and revisions. A solo (non-collaborative) document has a bounded unsaved window that flushes on page hide (documented) — i.e. a backgrounded edit is not lost.
- **Empty / error states:**
  - Missing/invalid `artifact_ref`, `artifact_type`, or `mode`: route-error page with "Back to chat".
  - Artifact type with no capabilities: the mode toolbar collapses entirely.
  - Send-to-agent with an unresolvable conversation: degrades gracefully (documented).
- **Connections:** F4.1–F4.4, F4.7, F2.x (send-to-agent).
- **Acceptance checks:**
  - [ ] Mode buttons appear only for supported types.
  - [ ] A comment survives restart and is visible to a collaborator.
  - [ ] Markup strokes are not saved as document content. *(inferred — confirm)*
  - [ ] An invalid deep link shows the error page, not a blank canvas.
  - [ ] Editing then backgrounding the app does not lose the last edit.

---

### F4.7 — Export and share

- **Label:** documentation-based
- **User goal:** Get the artifact out of Teamily, or give someone a link to it.
- **Path:** Studio header → **Share**; or the file viewer → Download / Download all; or a memory page → Download / Share.
- **Steps & expected result:**
  - **Share link** — a popover for the artifact reference; disabled when there is no artifact reference.
  - **Download** — the current file.
  - **Download all** — zips every workspace file, with progress feedback.
  - **docx Download** — a button in the Studio shell.
  - **Whiteboard export** — a portable scene file.
  - **Memory page** — Download (authenticated fetch → file download) or Share as `.md` into a chat.
  - **Shared thread** — a public, read-only replay of an agent conversation with playback controls (play/pause, step, speed 1×–4×, skip to end) and a workspace panel; the composer and agent controls are hidden.
- **What is saved / persists after restart:** The share link remains valid for authorised viewers. Downloaded files are outside the app.
- **Empty / error states:**
  - Share with no artifact reference: the control is disabled.
  - Download failure: a toast.
  - Unknown/removed shared thread: a "conversation not found" toast, then redirect home.
  - Share link with no destination: must not be produced (documented fix).
- **Connections:** F4.1–F4.6, F3.2.
- **Acceptance checks:**
  - [ ] Share is disabled when there is nothing to share.
  - [ ] A share link opens for an authorised viewer and is refused for an unauthorised one.
  - [ ] Download all produces a zip containing every file.
  - [ ] A revoked/unknown share link shows a clear message, not a blank page.
  - [ ] A shared thread is genuinely read-only (no composer).

---

## 5. Meetings

> **Support status:** Meetings are **partially supported and unevenly documented**. Teamily's public changelog contains concrete meeting entries, but the published screen reference for the mobile client contains **no meeting screen at all**, and the web route list contains no meeting route. Treat this whole section as the least certain part of the document.

### F5.1 — Recording

- **Label:** documentation-based (existence and limits) / unknown (Android specifics)
- **User goal:** Capture a meeting's audio so it can be turned into minutes.
- **Path:** **unknown for Android.** Documented entry points:
  - Desktop: background meeting detection offers to record, plus a draggable floating recording panel.
  - Mobile: a **Personal AI entry point for Meeting Minutes** (documented in the changelog).
- **Steps & expected result:**
  1. Start a recording from the documented entry point.
  2. A recording panel shows a soundwave that reacts to live microphone level.
  3. If the meeting starts mic-only, the app prompts to reshare with system audio; system audio can be mixed into the recording.
  4. Stop the recording. The cap is **4 hours** (raised from a lower value — documented twice in the changelog).
  5. Recording survives audio interruptions and background kills (documented).
- **What is saved / persists after restart:** The recording. Detected meetings are recorded into **Personal AI** rather than the open conversation (documented fix), and clicking the message banner reopens the chat.
- **Empty / error states:**
  - Microphone permission denied: **unknown**.
  - Recording interrupted: survives interruptions (documented).
  - Cap reached: **unknown** what happens at 4 hours.
- **Connections:** F5.2, F5.3, F1.4 (the recording surfaces as a voice message), F3.6 (action items).
- **Acceptance checks:**
  - [ ] Recording can be started and stopped from a discoverable entry point.
  - [ ] The soundwave reflects real input level.
  - [ ] A recording survives backgrounding the app.
  - [ ] The 4-hour cap is enforced with a clear message.
  - [ ] A detected meeting lands in Personal AI, not the open chat.

---

### F5.2 — Minutes

- **Label:** documentation-based (existence) / unknown (structure and quality)
- **User goal:** Get a structured summary of the meeting — decisions, action items, owners — without writing it.
- **Path:** **unknown for Android.** Documented: a Personal AI entry point for Meeting Minutes; a **Meeting Minutes badge** on the relevant voice message, which routes to the minutes.
- **Steps & expected result:**
  1. After a recording, a voice message carries a Meeting Minutes badge.
  2. Tapping the badge routes to the minutes.
  3. Teamily's public blog states that meetings and conversations are converted into "structured summaries with decisions, action items, owners, and follow-ups" that feed back into living memory.
  4. The exact on-screen structure of the minutes document is **unknown**.
- **What is saved / persists after restart:** The minutes document and its link to the recording. Whether minutes are written into Memory as pages is **inferred** from the "feed back into your living memory" statement, not confirmed.
- **Empty / error states:** **unknown** across the board — no documented empty, failure, or in-progress state.
- **Connections:** F5.1, F5.3, F3.1, F3.6, F4.1 (minutes are likely a document artifact — **inferred**).
- **Acceptance checks:**
  - [ ] A completed recording produces minutes without a manual prompt.
  - [ ] Minutes contain decisions, action items, and owners as separate sections.
  - [ ] The badge on the voice message routes to the right minutes.
  - [ ] Minutes survive restart.
  - [ ] Minutes are reachable from Memory. *(inferred — confirm)*

---

### F5.3 — Playback

- **Label:** documentation-based (player behaviour) / unknown (meeting-specific playback)
- **User goal:** Re-listen to what was said, and jump to the relevant moment.
- **Path:** The meeting voice message in the conversation, or the minutes' link back to the recording.
- **Steps & expected result:**
  1. The voice player is **seekable** (documented), with play/pause and a scrubber.
  2. A meeting clip can be resent gracefully (documented).
  3. Native playback is supported for non-Opus voice messages (documented).
  4. Whether playback is **synced to the transcript** (tap a line → jump to that audio position) is **unknown**.
- **What is saved / persists after restart:** The audio and its duration. Playback position persistence is **unknown**.
- **Empty / error states:**
  - Audio unavailable: **unknown**.
  - A clip whose length was never declared: still streams (documented).
- **Connections:** F5.1, F5.2, F1.4.
- **Acceptance checks:**
  - [ ] The player seeks to an arbitrary position.
  - [ ] Playback continues correctly after backgrounding and returning.
  - [ ] A meeting clip can be resent without corrupting the original.
  - [ ] Transcript-to-audio sync: confirm whether it exists before promising it.

---

## 6. Summary table

| ID | Feature | Area | Label | Confidence note |
|---|---|---|---|---|
| F1.1 | Send text message + @mentions | Chats | documentation-based | Composer, mentions, drafts well described |
| F1.2 | Message actions | Chats | documentation-based | Full action list documented; delete scope unknown |
| F1.3 | Attachments | Chats | documentation-based | Batch preview documented; failure UI unknown |
| F1.4 | Voice messages | Chats | documentation-based | Recorder + 60 s auto-transcript documented |
| F1.5 | History & search | Chats | documentation-based | Four-tab in-chat search documented |
| F1.6 | Conversation controls | Chats | documentation-based | Pin/mute/clear/delete documented |
| F2.1 | Create agent | Agents | documentation-based | Dialog + post-create routing documented |
| F2.2 | Edit / delete agent | Agents | documentation-based | Avatar crop, dirty-state Save documented |
| F2.3 | Agent instructions | Agents | **inferred** | **No documented instructions field — largest gap** |
| F2.4 | Tools: skills & connectors | Agents | documentation-based | Three-tab skill editor, OAuth connectors |
| F2.5 | Workflows (Flows, automations) | Agents | documentation-based | Flow canvas + automation builder documented |
| F2.6 | Handoffs | Agents | documentation-based | Flow connectors documented; runtime semantics partly inferred |
| F3.1 | Automatic memories | Memory | documentation-based | Living memory + Dream documented |
| F3.2 | Topics (pages) | Memory | documentation-based | Full page model + version history documented |
| F3.3 | Tags | Memory | documentation-based | AND semantics documented |
| F3.4 | Updates | Memory | documentation-based | Feed documented; empty state unknown |
| F3.5 | My Profile | Memory | documentation-based | Read-only + Dream refresh documented |
| F3.6 | Next Actions (To-dos) | Memory | **inferred** | View documented; the "Next Actions" name is a mapping |
| F3.7 | Overview (graph) | Memory | documentation-based | Force-directed graph documented |
| F3.8 | Source-chat connections | Memory | documentation-based | Link documented; deleted-source behaviour unknown |
| F4.1 | Document artifacts | Artifacts | documentation-based | docx editor + collaboration documented |
| F4.2 | Markdown artifacts | Artifacts | documentation-based | Source-mode toggle documented |
| F4.3 | Webpage / web app | Artifacts | documentation-based | Versions, element editing documented |
| F4.4 | Media artifacts | Artifacts | documentation-based | Per-type viewers documented |
| F4.5 | Saved messages | Artifacts | documentation-based | Save-as-card, shelve-by-kind documented |
| F4.6 | Editing (Studio) | Artifacts | documentation-based | Modes, presence, route errors documented |
| F4.7 | Export & share | Artifacts | documentation-based | Share/download/download-all documented |
| F5.1 | Meeting recording | Meetings | documentation-based | 4 h cap, detection documented; Android path unknown |
| F5.2 | Meeting minutes | Meetings | documentation-based | Existence + badge documented; structure unknown |
| F5.3 | Meeting playback | Meetings | documentation-based | Seekable player documented; transcript sync unknown |

### 6.1 Items that are **unknown** and must be resolved before implementation

| # | Unknown | Why it matters |
|---|---|---|
| U1 | Whether a dedicated agent **instructions / system-prompt** field exists (F2.3) | Blocks the entire "Agents → instructions" requirement |
| U2 | Android entry point for **meeting recording** (F5.1) | Blocks the Meetings area on the target platform |
| U3 | On-screen **structure of meeting minutes** (F5.2) | Blocks the minutes UI |
| U4 | Whether **transcript-to-audio sync** exists (F5.3) | Determines whether playback is a player or a transcript reader |
| U5 | Scope of **message delete** — local vs. for everyone (F1.2) | Affects the confirmation copy and the data model |
| U6 | **Upload failure** UI for attachments (F1.3) | Affects retry design |
| U7 | Empty-state copy for **Updates**, **Saves**, **source chats** (F3.4, F4.5, F3.8) | Affects polish and acceptance |
| U8 | Whether **minutes are written into Memory** as pages (F5.2) | Determines the Memory↔Meetings integration |
| U9 | Whether **markup strokes** are persisted as content (F4.6) | Affects the artifact data model |
| U10 | Behaviour when a **source conversation is deleted** (F3.8) | Affects referential integrity |

### 6.2 Recommended verification order

1. **F2.3** — open a real agent's settings and settle the instructions question. Everything in Agents depends on it.
2. **F5.1–F5.3** — establish whether Meetings exist on Android at all. If not, cut the area rather than build a guess.
3. **F1.2 / F1.3** — settle delete scope and upload failure, the two chat behaviours most likely to be wrong.
4. **F3.6** — confirm the shipped label ("To-dos" vs "Next Actions").
5. Everything else — walk the acceptance checks on device.

---

## 7. Cross-cutting notes for the Orhan implementation

- **Persistence is the product.** Teamily's own framing is that memory is not chat history and that agents have "zero cold starts". Any Orhan feature that forgets across a restart contradicts the product's core claim. Every acceptance list above therefore includes a restart check. (documentation-based)
- **Read-only vs. editable is a real distinction in Memory.** My Profile and To-dos are read-only; pages are editable. Do not make everything editable "for consistency". (documentation-based)
- **Destructive actions are always confirmed.** Delete agent, delete page, clear history, delete conversation, delete message, leave/dismiss group — all documented behind a confirmation. (documentation-based)
- **Errors are named, not blank.** The documented client repeatedly fixes "blank page" failures into named errors with a reason and a recovery action (document not opening, route errors, sync failures, share links with no destination). Treat "explain the failure" as a requirement, not a nicety. (documentation-based)
- **Do not build Discover, Feeds, Explore, posts, follows, likes, comments, or the social profile.** They are excluded by the brief and are the largest surface in the documented client — easy to drift into. (scope)

---

## 8. Disclaimer

This document is a technical reference for implementing UI and behaviour in the Orhan Android project. It is not legal, security, or compliance advice, and it makes no claim about Teamily's internal architecture, APIs, or data storage. All third-party product names belong to their owners. Behaviour described here may change between Teamily releases; re-verify against the shipped app before treating any item as a requirement.
