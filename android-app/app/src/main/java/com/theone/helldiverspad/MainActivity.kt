package com.theone.helldiverspad

import android.os.Bundle
import android.widget.Button
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
        // PC's LAN IP address
        private const val PC_HOST = "192.168.1.158"
        private const val PC_PORT = 50555
    }

    private lateinit var statusView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusView = findViewById(R.id.tvStatus)

        // Arrow pad: send one direction per tap.
        // Default mapping assumes Helldivers uses WASD for strategem directions:
        // Up=W, Down=S, Left=A, Right=D
        findViewById<ImageButton>(R.id.btnUp).setOnClickListener { sendDirection("up") }
        findViewById<ImageButton>(R.id.btnDown).setOnClickListener { sendDirection("down") }
        findViewById<ImageButton>(R.id.btnLeft).setOnClickListener { sendDirection("left") }
        findViewById<ImageButton>(R.id.btnRight).setOnClickListener { sendDirection("right") }

        // Toggle Left Ctrl on each tap (press on first tap, release on second)
        findViewById<ImageButton>(R.id.btnCtrlHold).setOnClickListener {
            sendJson("""{"type":"toggle_left_ctrl"}""", "toggle ctrl")
        }
    }

    private fun sendDirection(direction: String) {
        val key = when (direction) {
            "up" -> "w"
            "down" -> "s"
            "left" -> "a"
            "right" -> "d"
            else -> return
        }

        val json = """{"type":"strategem","name":"dir_$direction","sequence":["$key"]}"""
        sendJson(json, "dir $direction")
    }

    private fun sendJson(json: String, label: String) {
        statusView.text = "Status: sending $label..."

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                Socket(PC_HOST, PC_PORT).use { socket ->
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
}

