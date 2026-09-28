package com.obsidis.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.obsidis.app.service.BubbleService
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val overlayRunning = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                var showSplash by remember { mutableStateOf(true) }
                if (showSplash) {
                    SplashScreen(onFinished = { showSplash = false })
                } else {
                    ObsidisHome(overlayRunning)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        overlayRunning.value = BubbleService.isRunning
    }
}

/**
 * A five-beat open: a spark is born in the dark, swells into a burst of
 * warm light, the screen flashes white, and the Obsidis badge — a bold A
 * with a living eye as its crossbar — drops in and settles. The eye then
 * opens, blinks twice and glances side to side before the badge hands off
 * to the home screen.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var phase by remember { mutableStateOf(0) } // 0 dark,1 spark,2 burst,3 white,4 badge
    val sparkScale = remember { Animatable(0f) }
    val burstScale = remember { Animatable(0f) }
    val burstAlpha = remember { Animatable(1f) }
    val whiteAlpha = remember { Animatable(0f) }
    val badgeY = remember { Animatable(-260f) }
    val badgeScale = remember { Animatable(0.7f) }
    val eyeOpen = remember { Animatable(0f) }
    val eyeLook = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        delay(250)
        phase = 1
        sparkScale.animateTo(1f, tween(260, easing = FastOutSlowInEasing))
        phase = 2
        burstScale.animateTo(1f, tween(340, easing = FastOutLinearInEasing))
        burstAlpha.animateTo(0f, tween(340))
        phase = 3
        whiteAlpha.animateTo(1f, tween(140))
        delay(90)
        whiteAlpha.animateTo(0f, tween(260))
        phase = 4
        badgeScale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 260f))
        badgeY.animateTo(0f, spring(dampingRatio = 0.55f, stiffness = 220f))
        eyeOpen.animateTo(1f, tween(220, easing = LinearOutSlowInEasing))
        delay(180)
        // two quick blinks
        repeat(2) {
            eyeOpen.animateTo(0.05f, tween(70))
            eyeOpen.animateTo(1f, tween(90))
        }
        delay(120)
        // glance left, right, center
        eyeLook.animateTo(-1f, tween(220, easing = FastOutSlowInEasing))
        delay(160)
        eyeLook.animateTo(1f, tween(260, easing = FastOutSlowInEasing))
        delay(160)
        eyeLook.animateTo(0f, tween(200, easing = FastOutSlowInEasing))
        delay(380)
        onFinished()
    }

    Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        if (phase in 1..2) {
            Canvas(Modifier.fillMaxSize()) {
                val c = Offset(size.width / 2f, size.height / 2f)
                if (phase == 1) {
                    drawCircle(Color(0xFFFFD54F), radius = 10f * sparkScale.value + 2f, center = c)
                } else {
                    val r = size.minDimension * 0.85f * burstScale.value
                    drawCircle(Color(0xFFFFC107), radius = r, center = c, alpha = burstAlpha.value)
                }
            }
        }
        if (phase == 4) {
            ObsidisBadge(
                modifier = Modifier
                    .offset(y = badgeY.value.dp)
                    .scale(badgeScale.value),
                eyeOpenness = eyeOpen.value,
                eyeLook = eyeLook.value
            )
        }
        Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = whiteAlpha.value)))
    }
}

/** The badge mark: black rounded square, bold white A, eye as its crossbar. */
@Composable
fun ObsidisBadge(modifier: Modifier = Modifier, eyeOpenness: Float, eyeLook: Float, size: androidx.compose.ui.unit.Dp = 128.dp) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        drawRoundRect(Color(0xFF0A0A0A), cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.24f))

        val path = Path().apply {
            moveTo(s * 0.22f, s * 0.78f)
            lineTo(s * 0.5f, s * 0.22f)
            lineTo(s * 0.78f, s * 0.78f)
        }
        drawPath(
            path,
            color = Color.White,
            style = Stroke(width = s * 0.09f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        val eyeCx = s * 0.5f
        val eyeCy = s * 0.62f
        val eyeW = s * 0.36f
        val openness = eyeOpenness.coerceIn(0.06f, 1f)

        withTransform({
            scale(1f, openness, pivot = Offset(eyeCx, eyeCy))
        }) {
            drawOval(
                Color(0xFF7F77DD),
                topLeft = Offset(eyeCx - eyeW / 2f, eyeCy - eyeW / 2.6f),
                size = androidx.compose.ui.geometry.Size(eyeW, eyeW / 1.3f)
            )
            val pupilOffset = eyeLook * eyeW * 0.14f
            drawCircle(Color(0xFF0A0A0A), radius = eyeW * 0.2f, center = Offset(eyeCx + pupilOffset, eyeCy))
            drawCircle(Color.White.copy(alpha = 0.85f), radius = eyeW * 0.06f, center = Offset(eyeCx + pupilOffset - eyeW * 0.06f, eyeCy - eyeW * 0.06f))
        }
    }
}

/** Home / profile screen: badge mark, live status of both permissions, API key entry, island toggle. */
@Composable
fun ObsidisHome(overlayRunning: MutableState<Boolean>) {
    val context = LocalContext.current
    var apiKey by remember { mutableStateOf(loadApiKey(context)) }

    val infinite = rememberInfiniteTransition(label = "idle")
    val idleBlink by infinite.animateFloat(
        initialValue = 1f, targetValue = 1f,
        animationSpec = infiniteRepeatable(keyframes {
            durationMillis = 3400
            1f at 0
            1f at 2900
            0.08f at 3050
            1f at 3200
        }), label = "blink"
    )

    Column(
        Modifier.fillMaxSize().background(Color.Black).padding(horizontal = 22.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(36.dp))
        ObsidisBadge(eyeOpenness = idleBlink, eyeLook = 0f, size = 92.dp)
        Spacer(Modifier.height(14.dp))
        Text("OBSIDIS", color = Color(0xFFF2F2F2), fontSize = 21.sp, letterSpacing = 6.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(30.dp))

        SectionCard(title = "Ekran üstünde gösterme izni") {
            Text("CapCut'ın üzerinde ada görünebilmesi için gerekli.", color = Color(0xFFAAAAAA), fontSize = 13.sp)
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = {
                context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
            }) { Text("İzni aç") }
        }

        SectionCard(title = "Erişilebilirlik servisi") {
            Text("Obsidis'in gerçek düğmeleri bulup göstermesi için gerekli.", color = Color(0xFFAAAAAA), fontSize = 13.sp)
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }) { Text("Ayarları aç") }
        }

        SectionCard(title = "Yapay zeka anahtarı") {
            Text("console.groq.com üzerinden ücretsiz alınan anahtarı buraya yapıştır.", color = Color(0xFFAAAAAA), fontSize = 13.sp)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it; saveApiKey(context, it) },
                label = { Text("API anahtarı") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(20.dp))
        Text(
            if (overlayRunning.value) "Ada şu anda ekranın üstünde duruyor. Uygulamadan çıksan bile orada kalır."
            else "Ada kapalı. Başlatınca uygulamadan çıksan da ekranda kalır, sadece bu düğmeyle durur.",
            color = Color(0xFF888888), fontSize = 12.sp, modifier = Modifier.padding(bottom = 12.dp)
        )

        Button(
            onClick = {
                if (overlayRunning.value) BubbleService.stop(context) else BubbleService.start(context)
                overlayRunning.value = !overlayRunning.value
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F77DD)),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text(if (overlayRunning.value) "Adayı durdur" else "Adayı başlat", color = Color.White)
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 7.dp).background(Color(0xFF121212)).padding(16.dp)
    ) {
        Text(title, color = Color(0xFFF2F2F2), fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(6.dp))
        content()
    }
}

private fun loadApiKey(context: Context): String =
    context.getSharedPreferences("obsidis_prefs", Context.MODE_PRIVATE).getString("api_key", "") ?: ""

private fun saveApiKey(context: Context, key: String) {
    context.getSharedPreferences("obsidis_prefs", Context.MODE_PRIVATE).edit().putString("api_key", key).apply()
}
