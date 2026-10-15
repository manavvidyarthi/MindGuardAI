package com.manav.mindguardai.data

data class TextAnalysisResult(
    val title: String,
    val level: String,
    val detail: String,
    val suggestion: String,
    val signal: Float,
    val matchedTerms: Int,
    val urgentPhraseFound: Boolean
)

/** Transparent keyword reflection only; it is not a diagnostic or clinical model. */
object TextAnalyzer {
    private val urgentPhrases = listOf(
        "no reason to live", "end it all", "can't go on", "cant go on", "want to disappear"
    )
    private val stressTerms = listOf(
        "overwhelmed", "anxious", "anxiety", "panic", "stressed", "stressful",
        "exhausted", "lonely", "alone", "can't sleep", "cant sleep", "worried",
        "pressure", "burnt out", "burned out", "tired", "frustrated", "sad", "scared"
    )

    fun analyze(input: String): TextAnalysisResult {
        val text = input.lowercase().trim()
        val urgent = urgentPhrases.any { text.contains(it) }
        val matched = stressTerms.filter { text.contains(it) }.distinct()

        return when {
            urgent -> TextAnalysisResult(
                title = "Please reach for support",
                level = "Support may be important",
                detail = "Some wording in this entry can be associated with significant distress. A keyword tool cannot understand the full context or assess your safety.",
                suggestion = "Consider contacting someone you trust now. If you might be in immediate danger, contact local emergency services or a crisis service in your area.",
                signal = 0.88f,
                matchedTerms = matched.size,
                urgentPhraseFound = true
            )
            matched.size >= 3 -> TextAnalysisResult(
                title = "A lot may be on your mind",
                level = "Several reflection signals",
                detail = "This entry includes ${matched.size} different words or phrases from a small stress-related list (${matched.take(4).joinToString(", ")}). That does not tell us how you are doing overall.",
                suggestion = "Try naming one thing you can control today, take a short pause, or talk with someone supportive.",
                signal = 0.72f,
                matchedTerms = matched.size,
                urgentPhraseFound = false
            )
            matched.isNotEmpty() -> TextAnalysisResult(
                title = "Pause and check in with yourself",
                level = "A few reflection signals",
                detail = "This entry contains ${matched.joinToString(", ")}. One word or phrase cannot capture your whole experience, and context matters.",
                suggestion = "Consider what would help right now: a break, food or water, rest, movement, or a conversation.",
                signal = 0.42f,
                matchedTerms = matched.size,
                urgentPhraseFound = false
            )
            else -> TextAnalysisResult(
                title = "Your entry is more than a score",
                level = "No listed terms found",
                detail = "This simple demo did not find common words from its small stress-related list. It can miss context, humour, and many emotions.",
                suggestion = "Use your own judgement about how you feel. You do not need a keyword result to ask for support.",
                signal = 0.12f,
                matchedTerms = 0,
                urgentPhraseFound = false
            )
        }
    }
}
