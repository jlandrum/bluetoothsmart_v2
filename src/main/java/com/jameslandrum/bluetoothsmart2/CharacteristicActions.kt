package com.jameslandrum.bluetoothsmart2

import android.content.Context
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.locks.Lock
import kotlin.collections.HashSet


class ExecutionThread(val smartDevice : SmartDevice,
                      val context : Context, var failureCallback: (()->Unit)?={}) : Thread(), (Boolean) -> Unit {
    override fun invoke(it: Boolean) {
        if (it) {
            this.start()
        } else {
            failureCallback?.invoke()
        }
        smartDevice.connectionCallbacks.remove(this)
    }

    val queues = ConcurrentLinkedQueue<ConcurrentLinkedQueue<CharAction>>()

    private var activeQueue : ConcurrentLinkedQueue<CharAction>? = null
    private var activeAction : CharAction? = null
    private var lock : Object = Object()

    init {
        smartDevice.connectionCallbacks.add(this)
        smartDevice.connect(context, false)
    }

    override fun run() {
        while (!interrupted() && queues.isNotEmpty()) {
            if (!smartDevice.connected) interrupt()
            else {
                activeQueue = queues.remove()
                while (activeQueue!!.isNotEmpty()) {
                    activeAction = activeQueue!!.remove()
                    activeAction!!.invoke(smartDevice) {
                        activeAction!!.response(it)
                        if (!it) {
                            activeQueue!!.clear()
                        }
                        synchronized(lock, {lock.notify()})
                    }
                    synchronized(lock, {lock.wait(5000)})
                }
            }
        }
        if (!interrupted() && queues.isNotEmpty()) {
            failureCallback?.invoke()
        }
        interrupt()
    }
}

interface CharAction {
    val handle : CharHandle
    val response : (Boolean)->Unit
    fun invoke(smartDevice: SmartDevice, result: (Boolean)->Unit)
}

class CharWrite(override val handle : CharHandle,
                val data : ByteArray,
                override val response : (Boolean)->Unit) : CharAction {

    override fun invoke(smartDevice: SmartDevice, result: (Boolean)->Unit) {
        val char = smartDevice.activeConnection
                ?.services?.find { it.uuid == handle.serviceUuid.uuid }
                ?.characteristics?.find { it.uuid == handle.charUuid.uuid }
        if (char == null) response(false)
        else {
            char.value = data
            smartDevice.writeCharacteristic(char) { result(it) }
        }
    }
}
