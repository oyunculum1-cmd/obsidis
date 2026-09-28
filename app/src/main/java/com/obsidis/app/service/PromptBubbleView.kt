package com.obsidis.app.service

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.view.Gravity
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout

/**
 * The pill-shaped prompt that drops down directly under the island when the
 * keyboard half is tapped: a single-line input for the user's goal
 * ("glitch efekti yapmak istiyorum"), submitted on the IME action.
 */
class PromptBubbleView(
    context: Context,
    private val onSubmit: (String) -> Unit
) : FrameLayout(context) {

    val input: EditText

    init {
        val density = context.resources.displayMetrics.density
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((18 * density).toInt(), (10 * density).toInt(), (14 * density).toInt(), (10 * density).toInt())
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#F00B0B0B"))
                cornerRadius = 30 * density
                setStroke((1.2f * density).toInt(), Color.parseColor("#2A2A2A"))
            }
        }

        input = EditText(context).apply {
            hint = "Ne yapmak istiyorsun?"
            setHintTextColor(Color.parseColor("#7A7A7A"))
            setTextColor(Color.WHITE)
            textSize = 14f
            background = null
            inputType = InputType.TYPE_CLASS_TEXT
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnEditorActionListener { v, _, _ ->
                val text = v.text.toString().trim()
                if (text.isNotEmpty()) onSubmit(text)
                true
            }
        }

        container.addView(input)
        addView(
            container,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply { gravity = Gravity.TOP }
        )
    }
}
