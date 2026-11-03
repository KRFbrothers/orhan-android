package ai.orhan.feature.workflows

/** Product templates are draft starters, never saved user records or active jobs. */
data class AutomationTemplate(val id: String, val title: String, val description: String, val intent: AutomationIntent)

enum class AutomationIntent(val label: String, val prefix: String, val output: String) {
    RESEARCH("Research", "Investigate", "Return findings with sources you actually consulted; identify anything you could not verify."),
    MONITOR("Monitor", "Watch and brief me on", "Return a compact brief and identify the sources and time period checked."),
    SUMMARIZE("Summarize", "Prepare a concise summary of", "Summarize the supplied material without inventing missing facts."),
    CREATE("Create", "Create", "Prepare a private deliverable for review. Do not publish it or claim it was saved as an artifact unless a supported tool confirms that action."),
    ANALYZE("Analyze", "Analyze", "Compare the evidence, explain assumptions and return a structured recommendation."),
    PLAN("Plan", "Prepare a practical plan for", "Return a proposed plan for review. Do not claim invitations, assignments or calendar events were sent or created."),
}

val WorkAutomationTemplates = listOf(
    AutomationTemplate("meeting-prep", "Meeting reminder + prep", "Prepare an agenda and a reminder request.", AutomationIntent.PLAN),
    AutomationTemplate("daily-brief", "Daily work brief", "Turn your work context into a brief request.", AutomationIntent.MONITOR),
    AutomationTemplate("industry-monitor", "Competitor / industry monitor", "Specify the changes you want to track.", AutomationIntent.MONITOR),
    AutomationTemplate("research", "Expert group research", "Prepare a research question for your agent.", AutomationIntent.RESEARCH),
    AutomationTemplate("report", "Make a deck / report", "Describe the deliverable you need.", AutomationIntent.CREATE),
    AutomationTemplate("web-tool", "Build a webpage / tool", "Describe a page or utility to build.", AutomationIntent.CREATE),
    AutomationTemplate("task-plan", "Break down & assign tasks", "Prepare a task breakdown for review.", AutomationIntent.PLAN),
)

/** Plain prompt compilation: no provider call, schedule creation or inferred completion. */
fun compileAutomationPrompt(intent: AutomationIntent, task: String, agentName: String): String = buildString {
    append(intent.prefix).append(' ').append(task.trim()).append(".\n\n")
    append("Handle this request now as ").append(agentName.ifBlank { "Orhan" })
    append(". Reply in this chat with a concise result and any unresolved questions. ")
    append(intent.output).append(' ')
    append("Run once, then stop. Do not claim background monitoring, reminders, or external actions are active unless they have actually been configured and confirmed.")
}
