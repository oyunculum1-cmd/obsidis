package com.obsidis.app.model

/**
 * One step in a guided task, e.g. "tap the Effects icon". `matchTexts` are
 * candidate labels/content-descriptions the accessibility service will look
 * for on screen to find the right element to highlight.
 */
data class GuideStep(
    val instruction: String,
    val matchTexts: List<String>
)

data class GuideSession(
    val goal: String,
    val steps: List<GuideStep>,
    val currentIndex: Int = 0
) {
    val currentStep: GuideStep? get() = steps.getOrNull(currentIndex)
    val isComplete: Boolean get() = currentIndex >= steps.size
    fun advance(): GuideSession = copy(currentIndex = currentIndex + 1)
}
