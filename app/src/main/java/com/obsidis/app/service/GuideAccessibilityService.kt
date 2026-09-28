package com.obsidis.app.service

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.obsidis.app.model.GuideStep

/**
 * The core of the "orange light points at the real button" feature.
 *
 * Android's accessibility tree gives every visible, interactive element a
 * bounding box plus its text/content-description — the same data screen
 * readers use. We search the foreground app's tree for a node whose text
 * matches one of the current step's candidate labels, and report its exact
 * screen coordinates back to BubbleService, which draws the highlight ring
 * there. When the user taps that real button, TYPE_VIEW_CLICKED fires and
 * we advance to the next step automatically.
 *
 * This is why guidance can be accurate rather than guessed: we're reading
 * the actual UI, not estimating from a screenshot.
 */
class GuideAccessibilityService : AccessibilityService() {

    private var activeStep: GuideStep? = null
    private var onFound: ((Rect) -> Unit)? = null
    private var onStepClicked: (() -> Unit)? = null

    override fun onServiceConnected() {
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) instance = null
    }

    fun setTarget(step: GuideStep?, foundCallback: (Rect) -> Unit, clickedCallback: () -> Unit) {
        activeStep = step
        onFound = foundCallback
        onStepClicked = clickedCallback
        step?.let { locateAndReport(it) }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val step = activeStep ?: return
        when (event?.eventType) {
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> locateAndReport(step)
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                val clickedText = event.text?.joinToString(" ") ?: ""
                val clickedDesc = event.contentDescription?.toString() ?: ""
                if (step.matchTexts.any { candidate ->
                        clickedText.contains(candidate, ignoreCase = true) ||
                            clickedDesc.contains(candidate, ignoreCase = true)
                    }
                ) {
                    onStepClicked?.invoke()
                }
            }
            else -> {}
        }
    }

    private fun locateAndReport(step: GuideStep) {
        val root = rootInActiveWindow ?: return
        val match = findMatchingNode(root, step.matchTexts)
        if (match != null) {
            val bounds = Rect()
            match.getBoundsInScreen(bounds)
            onFound?.invoke(bounds)
        }
    }

    private fun findMatchingNode(node: AccessibilityNodeInfo, candidates: List<String>): AccessibilityNodeInfo? {
        val text = node.text?.toString() ?: ""
        val desc = node.contentDescription?.toString() ?: ""
        val isMatch = candidates.any { c ->
            text.contains(c, ignoreCase = true) || desc.contains(c, ignoreCase = true)
        }
        if (isMatch && (node.isClickable || node.isCheckable)) return node

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findMatchingNode(child, candidates)
            if (found != null) return found
        }
        return null
    }

    override fun onInterrupt() {}

    companion object {
        var instance: GuideAccessibilityService? = null
    }
}
