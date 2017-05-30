package com.jameslandrum.bluetoothsmart2

import android.content.Context
import android.util.Log
import kotlinx.coroutines.experimental.*
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.locks.Lock
import kotlin.collections.HashSet

interface CharAction {
    val response: (Boolean) -> Unit
    fun invoke(smartDevice: SmartDevice): Job
}

class CharWrite(val handle: CharHandle,
                val data: ByteArray,
                override val response: (Boolean) -> Unit) : CharAction {

    override fun invoke(smartDevice: SmartDevice) = launch(smartDevice.thread) {
        val char = smartDevice.activeConnection
                ?.services?.find { it.uuid == handle.serviceUuid.uuid }
                ?.characteristics?.find { it.uuid == handle.charUuid.uuid }
        if (char == null) {
            Log.d("BluetoothSmart", "Writing Char $handle failed - No such characteristic")
            launch(smartDevice.thread) { smartDevice.channel.send(false) }
        } else {
            Log.d("BluetoothSmart", "Writing Char $handle with $data")
            char.value = data
            smartDevice.activeConnection!!.writeCharacteristic(char)
        }
    }
}

class Connect(val ctx: Context, override val response: (Boolean) -> Unit) : CharAction {
    override fun invoke(smartDevice: SmartDevice) = launch(smartDevice.thread) {
        Log.d("BluetoothSmart", "Connecting")
        smartDevice.connect(ctx, false) {
            launch(smartDevice.thread) {
                smartDevice.channel.send(it)
                Log.d("BluetoothSmart", "Connected")
            }
        }
    }
}