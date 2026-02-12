package com.theone.helldiverspad

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import org.json.JSONObject

class SettingsActivity : AppCompatActivity() {

    private var selectedSoundIndex: Int = 0
    private var currentMediaPlayer: MediaPlayer? = null

    private val scanLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result == null || result.contents.isNullOrBlank()) {
            return@registerForActivityResult
        }
        try {
            val json = JSONObject(result.contents)
            val host = json.optString("host", "")
            val port = json.optInt("port", MainActivity.DEFAULT_PORT)
            if (host.isNotBlank()) {
                // Preserve existing haptic strength, enabled state, and sound selection when syncing from QR
                val prefs = getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
                val hapticStrength = prefs.getInt(MainActivity.KEY_HAPTIC, 2)
                val hapticEnabled = prefs.getBoolean(MainActivity.KEY_HAPTIC_ENABLED, true)
                val soundIndex = prefs.getInt(MainActivity.KEY_SOUND_INDEX, 0)
                saveConfig(host, port, hapticStrength, hapticEnabled, soundIndex)
                findViewById<EditText>(R.id.etHost).setText(host)
                findViewById<EditText>(R.id.etPort).setText(port.toString())
                findViewById<SeekBar>(R.id.sbHaptic).progress = hapticStrength.coerceIn(0, 3)
                findViewById<SwitchCompat>(R.id.switchHapticEnabled).isChecked = hapticEnabled
                findViewById<SeekBar>(R.id.sbHaptic).isEnabled = hapticEnabled
                selectedSoundIndex = soundIndex.coerceIn(0, MainActivity.SOUND_FILE_NAMES.size - 1)
                // Update sound selection highlight
                val soundListLayout = findViewById<LinearLayout>(R.id.llSoundList)
                for (i in 0 until soundListLayout.childCount) {
                    (soundListLayout.getChildAt(i) as? TextView)?.setBackgroundColor(if (i == selectedSoundIndex) 0x33FFFFFF else 0x00000000)
                }
                Toast.makeText(this, "Synced from QR", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Invalid QR payload", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to parse QR", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val etHost = findViewById<EditText>(R.id.etHost)
        val etPort = findViewById<EditText>(R.id.etPort)
        val sbHaptic = findViewById<SeekBar>(R.id.sbHaptic)
        val switchHapticEnabled = findViewById<SwitchCompat>(R.id.switchHapticEnabled)
        val llSoundList = findViewById<LinearLayout>(R.id.llSoundList)

        // Load existing config
        val prefs = getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
        etHost.setText(prefs.getString(MainActivity.KEY_HOST, ""))
        val port = prefs.getInt(MainActivity.KEY_PORT, MainActivity.DEFAULT_PORT)
        etPort.setText(port.toString())
        val haptic = prefs.getInt(MainActivity.KEY_HAPTIC, 2)
        val hapticEnabled = prefs.getBoolean(MainActivity.KEY_HAPTIC_ENABLED, true)
        sbHaptic.progress = haptic.coerceIn(0, 3)
        switchHapticEnabled.isChecked = hapticEnabled
        sbHaptic.isEnabled = hapticEnabled
        
        // Toggle haptic seekbar based on switch
        switchHapticEnabled.setOnCheckedChangeListener { _, isChecked ->
            sbHaptic.isEnabled = isChecked
        }
        
        // Setup sound list with preview
        selectedSoundIndex = prefs.getInt(MainActivity.KEY_SOUND_INDEX, 0).coerceIn(0, MainActivity.SOUND_FILE_NAMES.size - 1)
        MainActivity.SOUND_FILE_NAMES.forEachIndexed { index, soundName ->
            val soundItem = TextView(this).apply {
                text = "Sound ${index + 1}"
                textSize = 16f
                setTextColor(getColor(android.R.color.white))
                setPadding(32, 24, 32, 24)
                setBackgroundColor(if (index == selectedSoundIndex) 0x33FFFFFF else 0x00000000)
                setOnClickListener {
                    selectedSoundIndex = index
                    previewSound(index)
                    // Update visual selection for all items
                    for (i in 0 until llSoundList.childCount) {
                        (llSoundList.getChildAt(i) as? TextView)?.setBackgroundColor(if (i == index) 0x33FFFFFF else 0x00000000)
                    }
                }
            }
            llSoundList.addView(soundItem)
        }

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            val host = etHost.text.toString().trim()
            val portText = etPort.text.toString().trim()
            val portValue = portText.toIntOrNull() ?: MainActivity.DEFAULT_PORT
            val hapticValue = sbHaptic.progress.coerceIn(0, 3)
            val hapticEnabledValue = switchHapticEnabled.isChecked
            if (host.isBlank()) {
                Toast.makeText(this, "Host cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            saveConfig(host, portValue, hapticValue, hapticEnabledValue, selectedSoundIndex)
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
            finish()
        }

        findViewById<Button>(R.id.btnScanQr).setOnClickListener {
            val options = ScanOptions().apply {
                setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                setPrompt("Scan PC helper QR code")
                setCameraId(0)
                setBeepEnabled(true)
                setBarcodeImageEnabled(false)
            }
            scanLauncher.launch(options)
        }

        findViewById<ImageButton>(R.id.btnClose).setOnClickListener {
            finish()
        }
    }

    private fun previewSound(index: Int) {
        // Stop any currently playing sound
        currentMediaPlayer?.release()
        currentMediaPlayer = null
        
        try {
            val soundName = MainActivity.SOUND_FILE_NAMES[index.coerceIn(0, MainActivity.SOUND_FILE_NAMES.size - 1)]
            val soundResId = resources.getIdentifier(soundName, "raw", packageName)
            if (soundResId != 0) {
                currentMediaPlayer = MediaPlayer.create(this, soundResId)
                currentMediaPlayer?.setOnCompletionListener {
                    it.release()
                    currentMediaPlayer = null
                }
                currentMediaPlayer?.start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        currentMediaPlayer?.release()
        currentMediaPlayer = null
    }

    private fun saveConfig(host: String, port: Int, hapticStrength: Int, hapticEnabled: Boolean, soundIndex: Int) {
        val prefs = getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(MainActivity.KEY_HOST, host)
            .putInt(MainActivity.KEY_PORT, port)
            .putInt(MainActivity.KEY_HAPTIC, hapticStrength)
            .putBoolean(MainActivity.KEY_HAPTIC_ENABLED, hapticEnabled)
            .putInt(MainActivity.KEY_SOUND_INDEX, soundIndex.coerceIn(0, MainActivity.SOUND_FILE_NAMES.size - 1))
            .apply()
    }
}

