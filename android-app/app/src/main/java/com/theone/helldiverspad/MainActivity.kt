package com.theone.helldiverspad

import android.content.Context
import android.content.Intent
import android.graphics.PorterDuff
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.net.Socket

class MainActivity : AppCompatActivity() {

    companion object {
        const val PREFS_NAME = "helldivers_pad_prefs"
        const val KEY_HOST = "pc_host"
        const val KEY_PORT = "pc_port"
        const val KEY_HAPTIC = "haptic_strength" // 0=off,1=light,2=medium,3=strong
        const val KEY_HAPTIC_ENABLED = "haptic_enabled" // true/false for haptic toggle
        const val KEY_SOUND_INDEX = "sound_index" // 0-9 for which sound file to use
        const val DEFAULT_PORT = 50555
        
        // Sound file names (shared with SettingsActivity)
        val SOUND_FILE_NAMES = listOf(
            "sound_021746_sfx_cellar_ui_button_01_c2e05",
            "sound_021747_sfx_cellar_ui_button_02_2c87a",
            "sound_021748_sfx_cellar_ui_button_03_bbe36",
            "sound_021749_sfx_cellar_ui_button_04_cec0a",
            "sound_021750_sfx_cellar_ui_button_05_4dace",
            "sound_021751_sfx_cellar_ui_button_06_e8712_2",
            "sound_021752_sfx_cellar_ui_button_07_bd677",
            "sound_021753_sfx_cellar_ui_button_08_cf00e",
            "sound_021754_sfx_cellar_ui_button_09_2d16a",
            "sound_021755_sfx_cellar_ui_button_10_26767"
        )
    }

    private lateinit var statusView: TextView
    private lateinit var overlayStrategemsOff: FrameLayout
    private var host: String = ""
    private var port: Int = DEFAULT_PORT
    private var hapticStrength: Int = 2
    private var hapticEnabled: Boolean = true
    private var selectedSoundIndex: Int = 0
    private var ctrlIsHeld: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Fullscreen immersive mode
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION

        statusView = findViewById(R.id.tvStatus)
        overlayStrategemsOff = findViewById(R.id.overlayStrategemsOff)

        // Load host/port/haptics/sound from preferences
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        host = prefs.getString(KEY_HOST, "") ?: ""
        port = prefs.getInt(KEY_PORT, DEFAULT_PORT)
        hapticStrength = prefs.getInt(KEY_HAPTIC, 2)
        hapticEnabled = prefs.getBoolean(KEY_HAPTIC_ENABLED, true)
        selectedSoundIndex = prefs.getInt(KEY_SOUND_INDEX, 0).coerceIn(0, SOUND_FILE_NAMES.size - 1)

        // Start with strategems off (overlay visible)
        updateStrategemsState(false)

        // Arrow pad: hold while pressed (DOWN on press, UP on release)
        initArrowHoldBehaviour()

        // Toggle Left Ctrl on each tap (press on first tap, release on second)
        findViewById<ImageButton?>(R.id.btnCtrlHold)?.setOnClickListener {
            try {
                performHaptic()
                ctrlIsHeld = !ctrlIsHeld
                updateStrategemsState(ctrlIsHeld)
                sendJson("""{"type":"toggle_left_ctrl"}""", "toggle ctrl")
            } catch (e: Exception) {
                e.printStackTrace()
                android.util.Log.e("MainActivity", "Error in skull button click", e)
            }
        }

        // Settings (gear) button in main layout
        findViewById<ImageButton?>(R.id.btnSettings)?.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        // Reload preferences in case they were changed in settings
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        host = prefs.getString(KEY_HOST, "") ?: ""
        port = prefs.getInt(KEY_PORT, DEFAULT_PORT)
        hapticStrength = prefs.getInt(KEY_HAPTIC, 2)
        hapticEnabled = prefs.getBoolean(KEY_HAPTIC_ENABLED, true)
        selectedSoundIndex = prefs.getInt(KEY_SOUND_INDEX, 0).coerceIn(0, SOUND_FILE_NAMES.size - 1)
    }

    private fun initArrowHoldBehaviour() {
        val yellowColor = getColor(R.color.pad_ctrl) // Yellow color from colors.xml
        
        fun attachHold(buttonId: Int, direction: String) {
            val btn = findViewById<ImageButton?>(buttonId)
            btn?.setOnTouchListener { view, event ->
                // Only process if strategems are enabled (CTRL is held)
                if (!ctrlIsHeld) return@setOnTouchListener false
                
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        // Tint arrow icon yellow when pressed
                        (view as? ImageButton)?.setColorFilter(yellowColor, PorterDuff.Mode.SRC_ATOP)
                        performHaptic()
                        playSound()
                        sendDirectionEvent(direction, true)
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        // Remove tint when released
                        (view as? ImageButton)?.clearColorFilter()
                        sendDirectionEvent(direction, false)
                        true
                    }
                    else -> false
                }
            }
        }

        attachHold(R.id.btnUp, "up")
        attachHold(R.id.btnDown, "down")
        attachHold(R.id.btnLeft, "left")
        attachHold(R.id.btnRight, "right")
    }

    private fun sendDirectionEvent(direction: String, isDown: Boolean) {
        val type = if (isDown) "direction_down" else "direction_up"
        val json = """{"type":"$type","direction":"$direction"}"""
        sendJson(json, "$type $direction")
    }

    private fun sendJson(json: String, label: String) {
        statusView.text = "Status: sending $label..."

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                Socket(host, port).use { socket ->
                    val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))
                    writer.write(json)
                    writer.write("\n")
                    writer.flush()
                }
                runOnUiThread {
                    statusView.text = "Status: sent $label"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    statusView.text = "Status: error sending $label (${e.message})"
                }
            }
        }
    }

    private fun performHaptic() {
        if (!hapticEnabled || hapticStrength <= 0) return
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        val duration = when (hapticStrength) {
            1 -> 10L
            2 -> 30L
            3 -> 150L  // Much stronger max haptic
            else -> 0L
        }
        if (duration <= 0L) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    duration,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(duration)
        }
    }

    private fun updateStrategemsState(enabled: Boolean) {
        ctrlIsHeld = enabled
        try {
            overlayStrategemsOff.visibility = if (enabled) View.GONE else View.VISIBLE
            
            // Enable/disable arrow buttons (with null safety)
            findViewById<ImageButton?>(R.id.btnUp)?.isEnabled = enabled
            findViewById<ImageButton?>(R.id.btnDown)?.isEnabled = enabled
            findViewById<ImageButton?>(R.id.btnLeft)?.isEnabled = enabled
            findViewById<ImageButton?>(R.id.btnRight)?.isEnabled = enabled
        } catch (e: Exception) {
            e.printStackTrace()
            // Log the error but don't crash
            android.util.Log.e("MainActivity", "Error updating strategems state", e)
        }
    }

    private fun playSound() {
        // Use the selected sound index from preferences
        val soundName = SOUND_FILE_NAMES[selectedSoundIndex.coerceIn(0, SOUND_FILE_NAMES.size - 1)]
        
        try {
            val soundResId = resources.getIdentifier(soundName, "raw", packageName)
            if (soundResId != 0) {
                val mediaPlayer = MediaPlayer.create(this, soundResId)
                mediaPlayer?.setOnCompletionListener { it.release() }
                mediaPlayer?.start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

