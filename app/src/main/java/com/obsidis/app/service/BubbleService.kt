package com.obsidis.app.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.FrameLayout
import androidx.core.app.NotificationCompat
import com.obsidis.app.MainActivity
import com.obsidis.app.ai.AiClient
import com.obsidis.app.model.GuideSession
import com.obsidis.app.model.GuideStep
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray

/**
 * Owns the fixed "island" at the top of the screen and everything it opens.
 * This is a foreground service, so it keeps running when the user leaves
 * or closes Obsidis and even after the app is swiped away from recents —
 * it only stops when BubbleService.stop() is called, which happens from
 * the home screen's toggle button. Reopening the app and pressing the
 * toggle again is the only way to end it.
 */
class BubbleService : Service() {

    private lateinit var windowManager: WindowManager
    private var islandView: View? = null
    private var promptView: View? = null
    private var highlightView: View? = null
    private var promptOpen = false
    private var session: GuideSession? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startForeground(NOTIF_ID, buildNotification())
        addIsland()
    }

    private fun buildNotification(): Notification {
        val channelId = "obsidis_overlay"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Obsidis rehberi", NotificationManager.IMPORTANCE_MIN)
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
        val openIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Obsidis aktif")
            .setContentText("Ada ekranın üstünde duruyor.")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .build()
    }

    private fun overlayType() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

    private fun statusBarHeightPx(): Int {
        val id = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (id > 0) resources.getDimensionPixelSize(id) else (24 * resources.displayMetrics.density).toInt()
    }

    private fun addIsland() {
        val density = resources.displayMetrics.density
        val h = (34 * density).toInt()
        val w = (108 * density).toInt()
        val island = IslandView(this).apply { layoutParams = FrameLayout.LayoutParams(w, h) }

        val params = WindowManager.LayoutParams(
            w, h,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = statusBarHeightPx() + (4 * density).toInt()
        }

        island.setOnClickListener { togglePrompt() }
        windowManager.addView(island, params)
        islandView = island
    }

    private fun togglePrompt() {
        if (promptOpen) { removePrompt(); return }
        val density = resources.displayMetrics.density
        val prompt = PromptBubbleView(this) { text -> onQuestionSubmitted(text) }
        val params = WindowManager.LayoutParams(
            (300 * density).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = statusBarHeightPx() + (44 * density).toInt()
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
        }
        windowManager.addView(prompt, params)
        promptView = prompt
        promptOpen = true
        prompt.post {
            prompt.input.requestFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.showSoftInput(prompt.input, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun removePrompt() {
        promptView?.let { runCatching { windowManager.removeView(it) } }
        promptView = null
        promptOpen = false
    }

    private fun onQuestionSubmitted(question: String) {
        removePrompt()
        val prefs = getSharedPreferences("obsidis_prefs", Context.MODE_PRIVATE)
        val apiKey = prefs.getString("api_key", null)
        if (apiKey.isNullOrBlank()) {
            showMessage("Önce Obsidis uygulamasından bir API anahtarı ekle (Ayarlar).")
            return
        }
        showMessage("Düşünüyor…")
        scope.launch {
            val raw = AiClient(apiKey).planSteps("CapCut", question)
            val steps = parseSteps(raw)
            if (steps.isEmpty()) {
                showMessage("Adımlar çözülemedi, tekrar dener misin?")
                return@launch
            }
            session = GuideSession(question, steps)
            runStep()
        }
    }

    private fun parseSteps(raw: String): List<GuideStep> {
        return try {
            val cleaned = raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```")
            val arr = JSONArray(cleaned)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                val texts = obj.optJSONArray("matchTexts")
                val list = mutableListOf<String>()
                if (texts != null) for (j in 0 until texts.length()) list += texts.getString(j)
                GuideStep(obj.optString("instruction"), list)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun runStep() {
        val s = session ?: return
        val step = s.currentStep
        if (step == null) {
            showMessage("Bitti! İşlem tamamlandı.")
            session = null
            return
        }
        GuideAccessibilityService.instance?.setTarget(
            step,
            foundCallback = { bounds -> drawHighlight(bounds, step.instruction) },
            clickedCallback = { session = s.advance(); runStep() }
        )
    }

    private fun drawHighlight(bounds: Rect, instruction: String) {
        removeHighlight()
        val view = HighlightRingView(this, instruction)
        val params = WindowManager.LayoutParams(
            bounds.width() + 40, bounds.height() + 60,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = bounds.left - 20
            y = bounds.top - 50
        }
        windowManager.addView(view, params)
        highlightView = view
    }

    private fun removeHighlight() {
        highlightView?.let { runCatching { windowManager.removeView(it) } }
        highlightView = null
    }

    private fun showMessage(msg: String) {
        removeHighlight()
        val density = resources.displayMetrics.density
        val view = HighlightRingView(this, msg, ringOnly = false)
        val params = WindowManager.LayoutParams(
            (300 * density).toInt(), (60 * density).toInt(),
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = statusBarHeightPx() + (44 * density).toInt()
        }
        windowManager.addView(view, params)
        highlightView = view
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        islandView?.let { runCatching { windowManager.removeView(it) } }
        promptView?.let { runCatching { windowManager.removeView(it) } }
        highlightView?.let { runCatching { windowManager.removeView(it) } }
    }

    companion object {
        private const val NOTIF_ID = 1001

        /** True whenever the overlay service is alive, checked by MainActivity so the toggle button reflects reality. */
        var isRunning: Boolean = false
            private set

        fun start(context: Context) {
            val intent = Intent(context, BubbleService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
            else context.startService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, BubbleService::class.java))
        }
    }
}
