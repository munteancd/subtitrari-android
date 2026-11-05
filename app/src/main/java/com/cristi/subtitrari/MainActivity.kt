package com.cristi.subtitrari

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.TypedValue
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.google.mlkit.nl.translate.Translator

/** Setup screen: the things that must be done once, each with its current state. */
class MainActivity : Activity() {
    private lateinit var serviceState: TextView
    private lateinit var modelState: TextView
    private lateinit var translator: Translator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        translator = newTranslator()

        val gap = (16 * resources.displayMetrics.density).toInt()
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(gap, gap * 4, gap, gap)
        }
        fun label(value: String = "", size: Float = 18f) = TextView(this).apply {
            text = value
            setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
            setPadding(0, gap / 2, 0, gap / 2)
            page.addView(this)
        }

        label(getString(R.string.app_name), 28f)
        label("Traduce în română subtitrările din aplicația YouTube.")

        label("1. Traducerea de rezervă", 22f)
        modelState = label()
        Button(this).apply {
            text = "Descarcă traducerea"
            setOnClickListener { downloadModel() }
            page.addView(this)
        }

        label("2. Permisiunea", 22f)
        serviceState = label()
        Button(this).apply {
            text = "Deschide Accesibilitate"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
            page.addView(this)
        }

        label("3. Folosire", 22f)
        label("Deschide un clip în YouTube și pornește subtitrările de la butonul CC.")

        setContentView(ScrollView(this).apply { addView(page) })
        downloadModel()
    }

    override fun onResume() {
        super.onResume()
        serviceState.text =
            if (serviceEnabled()) "Pornită." else "Oprită. Pornește „Subtitrări RO” din Accesibilitate."
    }

    override fun onDestroy() {
        translator.close()
        super.onDestroy()
    }

    private fun downloadModel() {
        modelState.text = "Se descarcă pe WiFi…"
        translator.downloadModelIfNeeded(wifiOnly)
            .addOnSuccessListener { modelState.text = "Gata." }
            .addOnFailureListener { modelState.text = "Nu s-a descărcat. Verifică WiFi-ul și încearcă din nou." }
    }

    private fun serviceEnabled(): Boolean {
        val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?: return false
        val mine = ComponentName(this, CaptionService::class.java)
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == mine }
    }
}
