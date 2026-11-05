package com.cristi.subtitrari

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import com.google.mlkit.nl.translate.Translator

/** Choices (language, colour, size) with a live preview, then the one-time setup. */
class MainActivity : Activity() {
    private lateinit var settings: CaptionSettings
    private lateinit var serviceState: TextView
    private lateinit var modelState: TextView
    private lateinit var preview: TextView
    private val swatches = mutableListOf<Pair<ColorChoice, View>>()
    private var translator: Translator? = null

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = CaptionSettings(this)

        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(64), dp(16), dp(32))
        }
        fun label(value: String = "", size: Float = 17f) = TextView(this).apply {
            text = value
            setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
            setPadding(0, dp(8), 0, dp(8))
            page.addView(this)
        }
        fun heading(value: String) = label(value, 22f).apply { setPadding(0, dp(24), 0, dp(4)) }

        label(getString(R.string.app_name), 28f)
        label("Traduce subtitrările din aplicația YouTube.")

        preview = TextView(this).apply {
            text = "Așa va arăta subtitrarea"
            gravity = Gravity.CENTER
            setBackgroundColor(0xFF000000.toInt())
            setPadding(dp(8), dp(12), dp(8), dp(12))
            page.addView(this)
        }

        heading("Limba")
        page.addView(languagePicker())

        heading("Culoarea textului")
        page.addView(colorPicker())

        heading("Mărimea textului")
        page.addView(sizePicker())

        heading("Permisiunea")
        serviceState = label()
        Button(this).apply {
            text = "Deschide Accesibilitate"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
            page.addView(this)
        }

        heading("Traducerea de rezervă")
        label("Se folosește dacă nu e internet sau traducerea online nu răspunde. Funcționează doar pentru clipuri în engleză și e mai slabă.", 15f)
        modelState = label()
        Button(this).apply {
            text = "Descarcă din nou"
            setOnClickListener { downloadModel() }
            page.addView(this)
        }

        heading("Folosire")
        label("Deschide un clip în YouTube și pornește subtitrările de la butonul CC.")

        setContentView(ScrollView(this).apply { addView(page) })
        refreshStyle()
        downloadModel()
    }

    override fun onResume() {
        super.onResume()
        serviceState.text =
            if (serviceEnabled()) "Pornită." else "Oprită. Pornește „Subtitrări” din Accesibilitate."
    }

    override fun onDestroy() {
        translator?.close()
        super.onDestroy()
    }

    private fun languagePicker() = Spinner(this).apply {
        adapter = ArrayAdapter(
            this@MainActivity,
            android.R.layout.simple_spinner_dropdown_item,
            LANGUAGES.map { it.label },
        )
        setSelection(LANGUAGES.indexOf(settings.language))
        onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Also fires once when the screen opens, with the language already saved.
                if (LANGUAGES[position] == settings.language) return
                settings.language = LANGUAGES[position]
                downloadModel()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun colorPicker() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        COLORS.forEach { choice ->
            val swatch = View(this@MainActivity).apply {
                contentDescription = choice.name
                layoutParams = LinearLayout.LayoutParams(dp(44), dp(44)).apply { marginEnd = dp(10) }
                setOnClickListener {
                    settings.color = choice
                    refreshStyle()
                }
            }
            swatches += choice to swatch
            addView(swatch)
        }
    }

    private fun sizePicker() = RadioGroup(this).apply {
        orientation = RadioGroup.HORIZONTAL
        SIZES.forEach { choice ->
            val button = RadioButton(this@MainActivity).apply {
                text = choice.name
                setPadding(dp(4), 0, dp(16), 0)
                setOnClickListener {
                    settings.size = choice
                    refreshStyle()
                }
            }
            addView(button)
            button.isChecked = choice == settings.size
        }
    }

    private fun refreshStyle() {
        preview.setTextColor(settings.color.argb)
        preview.setTextSize(TypedValue.COMPLEX_UNIT_SP, settings.size.sp)
        swatches.forEach { (choice, view) ->
            view.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(choice.argb)
                // The selected colour gets a ring in a contrasting tone, so white stays visible too.
                if (choice == settings.color) setStroke(dp(4), 0xFF2196F3.toInt())
                else setStroke(dp(1), 0xFF808080.toInt())
            }
        }
    }

    private fun downloadModel() {
        translator?.close()
        translator = newTranslator(settings.language)
        val client = translator
        if (client == null) {
            modelState.text = "Pentru engleză nu e nevoie."
            return
        }
        modelState.text = "Se descarcă pe WiFi…"
        client.downloadModelIfNeeded(wifiOnly)
            .addOnSuccessListener { if (translator === client) modelState.text = "Gata." }
            .addOnFailureListener {
                if (translator === client) modelState.text = "Nu s-a descărcat. Verifică WiFi-ul și încearcă din nou."
            }
    }

    private fun serviceEnabled(): Boolean {
        val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?: return false
        val mine = ComponentName(this, CaptionService::class.java)
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == mine }
    }
}
