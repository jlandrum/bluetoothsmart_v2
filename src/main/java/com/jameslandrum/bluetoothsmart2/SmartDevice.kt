package com.jameslandrum.bluetoothsmart2

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.content.Context
import java.util.*

abstract class SmartDevice(var nativeDevice: BluetoothDevice) : BluetoothGattCallback() {
    private var activeConnection : BluetoothGatt? = null
    private var servicesDiscovered : Boolean = false

    val rssi : Int = -120
    val lastSeen : Long = System.currentTimeMillis()

    fun connect(context: Context, autoConnect: Boolean = false) = nativeDevice.connectGatt(context, autoConnect, this)
    fun diconnect() = activeConnection?.disconnect()

    fun characteristic(id : String) = characteristic(Uuid(id))
    fun characteristic(id : Uuid) {}
}

class CharID {

}
