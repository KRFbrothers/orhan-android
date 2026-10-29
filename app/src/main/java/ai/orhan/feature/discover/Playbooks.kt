package ai.orhan.feature.discover

/** A curated, on-device starter prompt. Tapping one opens a chat with the prompt pre-filled (never auto-sent). */
data class Playbook(
    val id: String,
    val title: String,
    val blurb: String,
    val tag: String,
    /** Agent to open; blank means a chat titled after the playbook. */
    val agentName: String = "",
    val prompt: String,
)

val StarterPlaybooks = listOf(
    Playbook(
        "landing-page", "Launch a landing page", "Describe your product and get a one-page site with hero, features and a call to action.",
        "Web", "",
        "Build a landing page for [product]. Audience: [who it's for]. Include a hero with a clear headline, three feature sections, a short FAQ and a sign-up call to action. Keep the design clean and mobile-first.",
    ),
    Playbook(
        "pitch-deck", "Draft a 10-slide pitch", "Problem, solution, market, traction and ask \u2014 outlined and designed as a deck.",
        "Slides", "",
        "Create a 10-slide pitch deck for [company]. Cover: problem, solution, market size, product, business model, traction, competition, team, roadmap and the ask. Give each slide a headline and 3 short bullets.",
    ),
    Playbook(
        "competitor-brief", "Competitor snapshot", "A cited one-page brief comparing you with your top competitors.",
        "Research", "",
        "Research the top 3\u20135 competitors of [company/product] in [market]. For each, summarise positioning, pricing, key features and recent news, with sources. End with gaps we could exploit.",
    ),
    Playbook(
        "expense-review", "Tidy up monthly expenses", "Paste transactions and get them categorised with anything unusual flagged.",
        "Finance", "",
        "Here are my transactions for [month]:\n[paste list]\nCategorise each one, total by category, and flag duplicates, unusual amounts or anything that needs a receipt.",
    ),
    Playbook(
        "design-critique", "Critique a screen", "Get honest feedback on layout, hierarchy, contrast and accessibility.",
        "Design", "",
        "Act as a senior product designer. Critique this screen: [describe or paste]. Review visual hierarchy, spacing, typography, colour contrast and accessibility, then list the top 5 fixes in priority order.",
    ),
    Playbook(
        "rewrite-clearly", "Rewrite it clearly", "Turn a rough draft into something concise, friendly and easy to scan.",
        "Writing", "",
        "Rewrite the text below so it is clear, concise and friendly. Keep my meaning, use short paragraphs, and suggest a better subject line or title.\n\n[paste text]",
    ),
    Playbook(
        "weekly-plan", "Plan my week", "Turn a messy to-do list into a realistic day-by-day plan.",
        "Productivity", "",
        "Here is everything on my plate this week:\n[paste list]\nPrioritise it, estimate time for each item, and lay out a realistic Monday\u2013Friday plan with buffer time.",
    ),
)
