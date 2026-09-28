package com.obsidis.app.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * Talks to an OpenAI-compatible chat completions endpoint. This is
 * deliberately provider-agnostic: point baseUrl/model at any compatible
 * service. Groq's free tier (console.groq.com) is a reasonable default to
 * suggest to users — generous free daily limits, no cost — but nothing here
 * hardcodes a paid dependency, and the key lives only on the user's device.
 *
 * No request is ever sent without the user's own key configured in Settings.
 */
class AiClient(
    private val apiKey: String,
    private val baseUrl: String = "https://api.groq.com/openai/v1/chat/completions",
    private val model: String = "llama-3.3-70b-versatile",
    private val client: OkHttpClient = OkHttpClient()
) {

    /**
     * Asks the model to break the user's goal (e.g. "glitch effect ekle")
     * into an ordered list of short, literal on-screen labels a video
     * editor app would show for that task. Returns raw JSON text steps;
     * caller parses into GuideStep list.
     */
    suspend fun planSteps(appName: String, goal: String): String = withContext(Dispatchers.IO) {
        val system = """
            Sen bir mobil video düzenleme uygulaması olan $appName için adım adım
            rehberlik veren bir asistansın. Kullanıcının hedefini, o uygulamada
            gerçekten görünen menü/düğme etiketlerine karşılık gelen kısa adımlara
            böl. Sadece JSON dizisi döndür, başka hiçbir şey yazma. Her öğe:
            {"instruction": "kullanıcıya gösterilecek kısa Türkçe talimat",
             "matchTexts": ["ekranda aranacak olası düğme etiketleri"]}
        """.trimIndent()

        val body = JSONObject().apply {
            put("model", model)
            put("messages", JSONArray().apply {
                put(JSONObject().put("role", "system").put("content", system))
                put(JSONObject().put("role", "user").put("content", goal))
            })
            put("temperature", 0.3)
        }

        val request = Request.Builder()
            .url(baseUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            val text = response.body?.string() ?: "{}"
            if (!response.isSuccessful) return@withContext "[]"
            val json = JSONObject(text)
            json.optJSONArray("choices")
                ?.optJSONObject(0)
                ?.optJSONObject("message")
                ?.optString("content")
                ?: "[]"
        }
    }
}
