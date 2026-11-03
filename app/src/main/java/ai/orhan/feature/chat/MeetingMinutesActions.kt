package ai.orhan.feature.chat

/** Extracts only explicit bullet/numbered items under the model's Action items heading. */
internal fun meetingActionItems(minutes: String): List<String> {
    val lines = minutes.lines()
    val start = lines.indexOfFirst {
        it.trim().replace(Regex("^#{1,6}\\s*"), "").trim().removeSuffix(":").equals("Action items", ignoreCase = true)
    }
    if (start < 0) return emptyList()
    val result = mutableListOf<String>()
    for (line in lines.drop(start + 1)) {
        if (line.trim().matches(Regex("^#{1,6}\\s+.+$"))) break
        val trimmed = line.trim()
        val item = when {
            trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("\u2022 ") -> trimmed.drop(2).trim()
            else -> Regex("^\\d+[.)]\\s+(.+?)\\s*$").matchEntire(trimmed)?.groupValues?.get(1).orEmpty()
        }
        if (item.isNotBlank() && !item.equals("None", ignoreCase = true) && !item.equals("Unspecified", ignoreCase = true)) {
            result += item.take(2_000)
            if (result.size == 20) break
        }
    }
    return result.distinctBy { it.lowercase() }
}
