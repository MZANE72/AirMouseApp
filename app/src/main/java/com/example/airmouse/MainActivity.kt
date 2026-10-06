package com.example.airmouse

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.MotionEvent
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlin.math.abs

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var gyroscope: Sensor? = null

    private var socket: DatagramSocket? = null
    private var targetAddress: InetAddress? = null
    private val targetPort = 8888

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val ipInput = findViewById<EditText>(R.id.ipInput)
        val connectButton = findViewById<Button>(R.id.connectButton)
        val clickButton = findViewById<Button>(R.id.clickButton)
        val infoText = findViewById<TextView>(R.id.infoText)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        connectButton.setOnClickListener {
            try {
                val ip = ipInput.text.toString().trim()
                targetAddress = InetAddress.getByName(ip)
                socket = DatagramSocket()
                infoText.text = "Bağlandı: $ip:$targetPort"
            } catch (e: Exception) {
                infoText.text = "Hata: ${e.message}"
            }
        }

        clickButton.setOnClickListener {
            send("CLICK")
        }

        clickButton.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                send("RIGHT_CLICK")
            }
            false
        }
    }

    override fun onResume() {
        super.onResume()
        gyroscope?.let {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_GAME
            )
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_GYROSCOPE) return

        val x = event.values[0]
        val y = event.values[1]

        val dx = (-y * 100).toInt()
        val dy = (-x * 100).toInt()

        if (targetAddress != null && socket != null) {
            if (abs(dx) > 2 || abs(dy) > 2) {
                send("MOVE:$dx:$dy")
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun send(message: String) {
        try {
            val address = targetAddress ?: return
            val data = message.toByteArray()
            socket?.send(DatagramPacket(data, data.size, address, targetPort))
        } catch (_: Exception) {}
    }
}
