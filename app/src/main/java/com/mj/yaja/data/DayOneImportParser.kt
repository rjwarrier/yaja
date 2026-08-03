package com.mj.yaja.data

internal object DayOneImportParser {
    /**
     * Cleans a Day One entry text and returns non-blank lines ready to be stored
     * as individual Yaja entries.
     */
    fun cleanAndSplit(raw: String): List<String> {
        var text = raw
        // Remove embedded image links: ![](dayone-moment://XXXX)
        text = text.replace(Regex("""!\[.*?]\(dayone-moment://.*?\)"""), "")
        // Remove HTML paragraph tags
        text = text.replace("<p dir=\"auto\">", "").replace("</p>", "")
        // Remove any remaining HTML-ish tags
        text = text.replace(Regex("<[^>]+>"), "")

        return text.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }
}
