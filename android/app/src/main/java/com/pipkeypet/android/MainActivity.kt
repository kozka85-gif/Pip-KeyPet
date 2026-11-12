
package com.pipkeypet.android

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothManager
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var scanner: BleScanner
    private lateinit var status: TextView
    private lateinit var button: Button

    private val handler = Handler(Looper.getMainLooper())
    private var scanning = false
    private val devices = mutableSetOf<String>()

    private val scanTimeout = Runnable {
        stopScanning()
    }

    private fun requiredPermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= 31) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
            setBackgroundColor(Color.rgb(20, 24, 35))
        }

        val title = TextView(this).apply {
            text = "PIP KEY PET"
            textSize = 28f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        status = TextView(this).apply {
            text = "Android Companion v0.2\n\nReady to scan"
            textSize = 17f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 40, 0, 40)
        }

        button = Button(this).apply {
            text = "SCAN BLUETOOTH"
            setOnClickListener { checkAndScan() }
        }

        layout.addView(title)
        layout.addView(status)
        layout.addView(button)
        setContentView(layout)

        scanner = BleScanner(this) { name, address ->
            runOnUiThread {
                if (scanning && devices.add(address)) {
                    status.text =
                        "Devices found: ${devices.size}\n\n" +
                        "$name\n$address"
                }
            }
        }
    }

    private fun checkAndScan() {
        if (scanning) {
            stopScanning()
            return
        }

        val missing = requiredPermissions().filter {
            checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            requestPermissions(missing.toTypedArray(), 100)
        } else {
            startScanning()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode, permissions, grantResults
        )

        if (requestCode == 100) {
            if (grantResults.isNotEmpty() &&
                grantResults.all {
                    it == PackageManager.PERMISSION_GRANTED
                }
            ) {
                startScanning()
            } else {
                status.text = "Bluetooth permission denied"
            }
        }
    }

    private fun startScanning() {
        val manager = getSystemService(
            BLUETOOTH_SERVICE
        ) as BluetoothManager

        if (manager.adapter?.isEnabled != true) {
            status.text = "Please enable Bluetooth"
            return
        }

        devices.clear()

        try {
            if (scanner.startScan()) {
                scanning = true
                status.text = "Scanning Bluetooth devices..."
                button.text = "STOP SCAN"
                handler.postDelayed(scanTimeout, 10000)
            } else {
                status.text = "Unable to start BLE scan"
            }
        } catch (e: SecurityException) {
            status.text = "Bluetooth permission error"
        } catch (e: Exception) {
            status.text = "BLE scan failed"
        }
    }

    private fun stopScanning() {
        handler.removeCallbacks(scanTimeout)

        if (scanning) {
            try {
                scanner.stopScan()
            } catch (_: SecurityException) {
            }
        }

        scanning = false
        button.text = "SCAN BLUETOOTH"

        if (devices.isEmpty()) {
            status.text = "Scan finished. No devices found."
        } else {
            Toast.makeText(
                this,
                "Found ${devices.size} BLE devices",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDestroy() {
        stopScanning()
        super.onDestroy()
    }
}
