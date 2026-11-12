
package com.pipkeypet.android

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
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
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {

    private data class DeviceInfo(
        var name: String,
        var rssi: Int
    )

    private lateinit var scanner: BleScanner
    private lateinit var connection: BleConnection
    private lateinit var status: TextView
    private lateinit var deviceList: LinearLayout
    private lateinit var scanButton: Button
    private lateinit var disconnectButton: Button

    private val handler = Handler(Looper.getMainLooper())
    private val devices = linkedMapOf<String, DeviceInfo>()
    private var scanning = false

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
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 32, 24, 24)
            setBackgroundColor(Color.rgb(20, 24, 35))
        }

        val title = TextView(this).apply {
            text = "PIP KEY PET"
            textSize = 27f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        status = TextView(this).apply {
            text = "Android Companion v0.5\nReady to scan"
            textSize = 16f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 24)
        }

        scanButton = Button(this).apply {
            text = "SCAN BLUETOOTH"
            setOnClickListener {
                if (scanning) {
                    stopScanning()
                } else {
                    checkAndScan()
                }
            }
        }

        disconnectButton = Button(this).apply {
            text = "DISCONNECT"
            isEnabled = false
            setOnClickListener {
                connection.disconnect()
                status.text = "Disconnected"
                isEnabled = false
            }
        }

        deviceList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val scroll = ScrollView(this).apply {
            addView(deviceList)
        }

        root.addView(title)
        root.addView(status)
        root.addView(scanButton)
        root.addView(disconnectButton)
        root.addView(
            scroll,
            LinearLayout.LayoutParams(-1, 0, 1f)
        )

        setContentView(root)

        scanner = BleScanner(this) { name, address, rssi ->
            runOnUiThread {
                if (scanning) {
                    val existing = devices[address]

                    if (existing == null) {
                        devices[address] = DeviceInfo(name, rssi)
                    } else {
                        existing.rssi = rssi
                        if (name != "Unknown device") {
                            existing.name = name
                        }
                    }

                    renderDevices()
                }
            }
        }

        connection = BleConnection(this) { message ->
            runOnUiThread {
                status.text = message
                disconnectButton.isEnabled =
                    message == "Connected" ||
                    message.startsWith("Connected.")
            }
        }
    }

    private fun renderDevices() {
        status.text = if (scanning) {
            "Scanning... Found ${devices.size} devices"
        } else {
            "Found ${devices.size} devices"
        }

        deviceList.removeAllViews()

        for ((address, device) in devices) {
            val item = TextView(this).apply {
                text = "${device.name}\n$address\nSignal: ${device.rssi} dBm"
                textSize = 15f
                setTextColor(Color.WHITE)
                setPadding(16, 18, 16, 18)
                setBackgroundColor(Color.rgb(35, 40, 55))

                setOnClickListener {
                    showDeviceDialog(
                        device.name,
                        address,
                        device.rssi
                    )
                }
            }

            deviceList.addView(
                item,
                LinearLayout.LayoutParams(-1, -2).apply {
                    bottomMargin = 8
                }
            )
        }
    }

    private fun showDeviceDialog(
        name: String,
        address: String,
        rssi: Int
    ) {
        AlertDialog.Builder(this)
            .setTitle(name)
            .setMessage(
                "Bluetooth address:\n$address\n\n" +
                "Signal: $rssi dBm\n\n" +
                "Connect only to a device you own."
            )
            .setNegativeButton("CANCEL", null)
            .setPositiveButton("CONNECT") { _, _ ->
                connectToDevice(address)
            }
            .show()
    }

    private fun connectToDevice(address: String) {
        val manager = getSystemService(
            BLUETOOTH_SERVICE
        ) as BluetoothManager

        val adapter = manager.adapter

        if (adapter == null || !adapter.isEnabled) {
            status.text = "Please enable Bluetooth"
            return
        }

        if (Build.VERSION.SDK_INT >= 31 &&
            checkSelfPermission(
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            status.text = "Bluetooth permission required"
            return
        }

        try {
            stopScanning()
            val device = adapter.getRemoteDevice(address)
            connection.connect(device)
        } catch (e: IllegalArgumentException) {
            status.text = "Invalid Bluetooth address"
        } catch (e: SecurityException) {
            status.text = "Bluetooth permission error"
        }
    }

    private fun checkAndScan() {
        val missing = requiredPermissions().filter {
            checkSelfPermission(it) !=
                PackageManager.PERMISSION_GRANTED
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
        deviceList.removeAllViews()

        try {
            if (scanner.startScan()) {
                scanning = true
                scanButton.text = "STOP SCAN"
                status.text = "Scanning..."
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

        if (::scanButton.isInitialized) {
            scanButton.text = "SCAN BLUETOOTH"
        }

        if (::status.isInitialized) {
            status.text =
                "Scan finished. Found ${devices.size} devices"
        }
    }

    override fun onDestroy() {
        stopScanning()
        connection.disconnect()
        super.onDestroy()
    }
}
