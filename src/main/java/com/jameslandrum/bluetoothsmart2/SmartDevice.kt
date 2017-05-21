package com.jameslandrum.bluetoothsmart2

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.content.Context
import com.jameslandrum.bluetoothsmart2.actions.ActionRunner
import java.util.*

abstract class SmartDevice(var nativeDevice: BluetoothDevice) : BluetoothGattCallback() {
    private var activeConnection : BluetoothGatt? = null
    private var servicesDiscovered : Boolean = false
    private var characteristics = HashSet<Characteristic>()

    val rssi : Int = -120
    val lastSeen : Long = System.currentTimeMillis()

    fun connect(context: Context, autoConnect: Boolean = false) = nativeDevice.connectGatt(context, autoConnect, this)
    fun diconnect() = activeConnection?.disconnect()

    fun characteristic(id : String) = characteristic(Uuid(id))
    fun characteristic(id : Uuid) {

    }

    override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
        when (newState) {
            BluetoothGatt.STATE_CONNECTED -> {
                activeConnection = gatt
                activeConnection!!.discoverServices()
            }
        }
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        gatt.services.forEach { service ->
            service.characteristics.forEach {
                val newChar = Characteristic()
                val char = characteristics.
            }
        }
        super.onServicesDiscovered(gatt, status)
    }
}
