package com.jameslandrum.bluetoothsmart2

import android.content.Context
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
            response(false)
        } else {
            char.value = data
            smartDevice.activeConnection!!.writeCharacteristic(char)
            response(smartDevice.channel.receive())
        }
    }
}

class Connect(val ctx: Context, override val response: (Boolean) -> Unit) : CharAction {
    override fun invoke(smartDevice: SmartDevice) = launch(smartDevice.thread) {
        smartDevice.connect(ctx, false) {
            launch(context) {
                smartDevice.channel.send(it)
            }
        }
        response(smartDevice.channel.receive())
    }
}