package com.jameslandrum.bluetoothsmart2

import android.content.Context
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.locks.Lock
import kotlin.collections.HashSet


class ExecutionThread(val smartDevice : SmartDevice,
                      val context : Context) : Thread() {
    val actions = ArrayList<ConcurrentLinkedQueue<CharAction>>()

    private val lock = Object()
    private var activeQueue : ConcurrentLinkedQueue<CharAction>? = null
    private var activeAction : CharAction? = null

    override fun run() {
        while (!interrupted()) {
            if (activeQueue?.isNotEmpty()?:false) {
                activeAction = activeQueue!!.remove()
                activeAction!!.invoke(smartDevice)
            } else if (actions.isNotEmpty()) {
                if (!smartDevice.connectedOrConnecting) {
                    smartDevice.connect(context, false)
                    lock.wait()
                    if (!smartDevice.connectedOrConnecting) interrupt()
                } else {

                }
            } else {

            }
        }
    }

    fun kick() {

    }
}

interface CharAction {
    val handle : CharHandle
    val response : (Boolean)->Unit
    fun invoke(smartDevice: SmartDevice)
}

interface ActionResult {
}

class CharWrite(override val handle : CharHandle,
                val data : ByteArray,
                override val response : (Boolean)->Unit) : CharAction {
    override fun invoke(smartDevice: SmartDevice) {
        val char = smartDevice.activeConnection
                ?.services?.find { it.uuid == handle.serviceUuid.uuid }
                ?.characteristics?.find { it.uuid == handle.charUuid.uuid }
        if (char==null) response(false)
        else {
            char.value = data
        }
    }
}
