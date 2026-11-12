
package com.pipkeypet.android

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build

class BleConnection(
    private val context: Context,
    private val onStatusChanged: (String) -> Unit
) {
    private var gatt: BluetoothGatt? = null

    private val callback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(
            gatt: BluetoothGatt,
            status: Int,
            newState: Int
        ) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                onStatusChanged("Connection error: $status")
                closeGatt(gatt)
                return
            }

            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    onStatusChanged("Connected")
                    try {
                        @SuppressLint("MissingPermission")
                        gatt.discoverServices()
                    } catch (e: SecurityException) {
                        onStatusChanged("Bluetooth permission denied")
                        closeGatt(gatt)
                    }
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    onStatusChanged("Disconnected")
                    closeGatt(gatt)
                }
            }
        }

        override fun onServicesDiscovered(
            gatt: BluetoothGatt,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                onStatusChanged(
                    "Connected. Found ${gatt.services.size} GATT services"
                )
            } else {
                onStatusChanged("Service discovery failed: $status")
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice) {
        disconnect()
        onStatusChanged("Connecting...")

        try {
            gatt = device.connectGatt(
                context,
                false,
                callback,
                BluetoothDevice.TRANSPORT_LE
            )
        } catch (e: SecurityException) {
            onStatusChanged("Bluetooth permission denied")
        } catch (e: Exception) {
            onStatusChanged("Connection failed")
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        val oldGatt = gatt
        gatt = null

        if (oldGatt != null) {
            try {
                oldGatt.disconnect()
            } catch (_: SecurityException) {
            }
            oldGatt.close()
        }
    }

    private fun closeGatt(connection: BluetoothGatt) {
        if (gatt === connection) {
            gatt = null
        }
        connection.close()
    }
}
