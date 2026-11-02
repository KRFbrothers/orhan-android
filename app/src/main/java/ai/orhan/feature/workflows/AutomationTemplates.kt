package ai.orhan.feature.workflows

/** Product templates are draft starters, never saved user records or active jobs. */
data class AutomationTemplate(val id: String, val title: String, val description: String, val intent: AutomationIntent)

enum class AutomationIntent(val label: String, val prefix: String) {
    MONITOR("Monitor", "Watch and brief me on"),
    SUMMARIZE("Summarize", "Prepare a concise summary of"),
    CREATE("Create", "Create"),
    PLAN("Plan", "Prepare a practical plan for"),
}

val WorkAutomationTemplates = listOf(
    AutomationTemplate("meeting-prep", "Meeting reminder + prep", "Prepare an agenda and a reminder request.", AutomationIntent.PLAN),
    AutomationTemplate("daily-brief", "Daily work brief", "Turn your work context into a brief request.", AutomationIntent.MONITOR),
    AutomationTemplate("industry-monitor", "Competitor / industry monitor", "Specify the changes you want to track.", AutomationIntent.MONITOR),
    AutomationTemplate("research", "Expert group research", "Prepare a research question for your agent.", AutomationIntent.SUMMARIZE),
    AutomationTemplate("report", "Make a deck / report", "Describe the deliverable you need.", AutomationIntent.CREATE),
    AutomationTemplate("web-tool", "Build a webpage / tool", "Describe a page or utility to build.", AutomationIntent.CREATE),
    AutomationTemplate("task-plan", "Break down & assign tasks", "Prepare a task breakdown for review.", AutomationIntent.PLAN),
)

/** Plain prompt compilation: no provider call, schedule creation or inferred completion. */
fun compileAutomationPrompt(intent: AutomationIntent, task: String, agentName: String): String = buildString {
    append(intent.prefix).append(' ').append(task.trim()).append(".\n\n")
    append("Handle this request now as ").append(agentName.ifBlank { "Orhan" })
    append(". Reply in this chat with a concise result and any unresolved questions. ")
    append("Run once, then stop. Do not claim background monitoring, reminders, or external actions are active unless they have actually been configured and confirmed.")
}
