package com.cristi.subtitrari

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import org.json.JSONArray
import org.json.JSONObject

private const val MODEL = "gemini-3.1-flash-lite"
private const val URL_BASE = "https://generativelanguage.googleapis.com/v1beta/models"
private const val CONNECT_TIMEOUT_MS = 1500
private const val READ_TIMEOUT_MS = 3500
private const val RATE_LIMIT_PAUSE_MS = 30_000L

fun captionPrompt(language: String, earlier: List<String>, current: String): String = """
    Translate YouTube captions into natural $language, for an older viewer.
    Captions may have no punctuation and may be cut mid-sentence. They may contain speech-recognition errors; infer the intended words from context.
    Translate only CURRENT, all of it. Translate fragments as fragments, without completing them. Keep names, numbers, units and acronyms. Translate sound labels such as [Music]. Add no quotation marks, notes or information that is not there.
    If CURRENT is already $language, return it unchanged. Reply with the $language text only.

    EARLIER: ${earlier.joinToString(" ")}
    CURRENT: $current
""".trimIndent()

/** Returns the reply text, or null when the model gave none (a blocked or empty answer). */
fun parseReply(json: String): String? {
    val parts = JSONObject(json).optJSONArray("candidates")?.optJSONObject(0)
        ?.optJSONObject("content")?.optJSONArray("parts") ?: return null
    return parts.optJSONObject(0)?.optString("text")?.trim()?.takeIf { it.isNotEmpty() }
}

/** How long Gemini asks to wait, from the "retryDelay" in its error body, or null if it doesn't say. */
fun parseRetryDelayMs(errorBody: String): Long? {
    val details = runCatching { JSONObject(errorBody).getJSONObject("error").getJSONArray("details") }
        .getOrNull() ?: return null
    for (i in 0 until details.length()) {
        val delay = details.optJSONObject(i)?.optString("retryDelay").orEmpty()
        val seconds = delay.removeSuffix("s").toDoubleOrNull() ?: continue
        return (seconds * 1000).toLong()
    }
    return null
}

/** After a rate limit, stays out of the way instead of failing on each caption. */
class Cooldown {
    @Volatile private var until = 0L
    fun trip(now: Long, pauseMs: Long) {
        until = now + pauseMs
    }
    fun active(now: Long) = now < until
}

/** Calls Gemini off the main thread. Timeouts are short: a late caption is a useless caption. */
class GeminiClient(private val apiKey: String) {
    private val worker = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private val cooldown = Cooldown()

    fun translate(
        language: String,
        current: String,
        earlier: List<String>,
        now: Long,
        onResult: (String) -> Unit,
        onFailure: () -> Unit,
    ) {
        if (apiKey.isEmpty() || cooldown.active(now)) {
            onFailure()
            return
        }
        worker.execute {
            val reply = try {
                request(captionPrompt(language, earlier, current))
            } catch (e: IOException) {
                Log.w("Subtitrari", "gemini: ${e.javaClass.simpleName}")
                null
            } catch (_: org.json.JSONException) {
                null
            }
            main.post { if (reply != null) onResult(reply) else onFailure() }
        }
    }

    fun close() = worker.shutdownNow().let { }

    private fun request(prompt: String): String? {
        val connection = URL("$URL_BASE/$MODEL:generateContent").openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("x-goog-api-key", apiKey)
            connection.outputStream.use { it.write(requestBody(prompt).toByteArray()) }
            if (connection.responseCode != 200) {
                Log.w("Subtitrari", "gemini: HTTP ${connection.responseCode}")
                if (connection.responseCode == 429) tripCooldown(connection)
                return null
            }
            return parseReply(connection.inputStream.bufferedReader().use { it.readText() })
        } finally {
            connection.disconnect()
        }
    }

    private fun tripCooldown(connection: HttpURLConnection) {
        val body = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
        cooldown.trip(SystemClock.uptimeMillis(), parseRetryDelayMs(body) ?: RATE_LIMIT_PAUSE_MS)
    }

    private fun requestBody(prompt: String): String = JSONObject()
        .put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))))
        .put(
            "generationConfig",
            JSONObject().put("temperature", 0.2).put("thinkingConfig", JSONObject().put("thinkingLevel", "minimal")),
        )
        .toString()
}
