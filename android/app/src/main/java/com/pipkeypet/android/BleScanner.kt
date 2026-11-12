
package com.pipkeypet.android

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context

class BleScanner(
    context: Context,
    private val onDeviceFound: (String, String) -> Unit
) {
    private val manager =
        context.getSystemService(Context.BLUETOOTH_SERVICE)
            as BluetoothManager

    private val adapter: BluetoothAdapter?
        get() = manager.adapter

    private val callback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(
            callbackType: Int,
            result: ScanResult
        ) {
            val name = result.device.name ?: "Unknown device"
            val address = result.device.address
            onDeviceFound(name, address)
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan(): Boolean {
        val bluetooth = adapter ?: return false
        if (!bluetooth.isEnabled) return false

        val scanner = bluetooth.bluetoothLeScanner
            ?: return false

        scanner.startScan(callback)
        return true
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        adapter?.bluetoothLeScanner?.stopScan(callback)
    }
}
