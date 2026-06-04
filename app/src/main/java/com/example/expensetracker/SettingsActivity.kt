package com.example.expensetracker

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val btnBack       = findViewById<android.widget.ImageView>(R.id.btnBack)
        val etUsername    = findViewById<EditText>(R.id.etUsername)
        val switchNight   = findViewById<Switch>(R.id.switchNightMode)
        val etMin         = findViewById<EditText>(R.id.etMinAmount)
        val etMax         = findViewById<EditText>(R.id.etMaxAmount)

        // Load saved values
        etUsername.setText(AppPreferences.getUsername(this))
        switchNight.isChecked = AppPreferences.isNightMode(this)
        etMin.setText(AppPreferences.getMinAmount(this).toInt().toString())
        val maxAmt = AppPreferences.getMaxAmount(this)
        if (maxAmt > 0) etMax.setText(maxAmt.toInt().toString())

        btnBack.setOnClickListener { finish() }

        // Username save on focus loss
        etUsername.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val name = etUsername.text.toString().trim()
                if (name.isNotEmpty()) AppPreferences.setUsername(this, name)
            }
        }

        // Night mode toggle
        switchNight.setOnCheckedChangeListener { _, isChecked ->
            AppPreferences.setNightMode(this, isChecked)
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                else           AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        // Min/Max save on focus loss
        etMin.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val v = etMin.text.toString().toFloatOrNull() ?: 0f
                AppPreferences.setMinAmount(this, v)
            }
        }
        etMax.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val v = etMax.text.toString().toFloatOrNull() ?: -1f
                AppPreferences.setMaxAmount(this, v)
            }
        }
    }
}
