package com.jameslandrum.bluetoothsmart2

import android.bluetooth.BluetoothGattCharacteristic

class Characteristic(val service : Uuid, val id : Uuid) {
    var nativeCharacteristic : BluetoothGattCharacteristic? = null

    fun set(data : ByteArray, success: ()->Unit={}, failed: ()->Unit={}) {
        if (nativeCharacteristic == null) failed()
        val char = nativeCharacteristic!!
        char.value = data
    }

    override fun hashCode(): Int {
        return id.hashCode() + service.hashCode()
    }

    override fun equals(other: Any?): Boolean = other is Characteristic && other.id == id && other.service == service
}