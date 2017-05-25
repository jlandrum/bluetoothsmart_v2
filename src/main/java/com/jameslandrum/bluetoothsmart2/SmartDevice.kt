package com.jameslandrum.bluetoothsmart2

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ConcurrentSkipListSet

abstract class SmartDevice(val nativeDevice: BluetoothDevice) : BluetoothGattCallback() {
    internal var activeConnection : BluetoothGatt? = null
    private var servicesDiscovered : Boolean = false
    private var characteristicCallbacks = ConcurrentHashMap<CharHandle,(ByteArray)->Unit>()
    private var queue = ConcurrentLinkedQueue<()->Unit>()
    private var actionCallback : ((Boolean)->Unit)? = null

    val address : String get() = nativeDevice.address
    val advertisement = ByteArray(62)
    var lastSeen : Long = 0L
    var connecting = false
    var connected = false
    val connectionCallbacks = ArrayList<((Boolean) -> Unit)>()

    val connectedOrConnecting : Boolean get() = connected || connecting

    var rssi : Int = -120
    var updateListeners = ConcurrentLinkedQueue<UpdateListener>()

    var executionThread : ExecutionThread? = null

    fun connect(context: Context, autoConnect: Boolean = false) {
        nativeDevice.connectGatt(context, autoConnect, this)
    }

    fun diconnect() = activeConnection?.disconnect()

    override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
        when (newState) {
            BluetoothGatt.STATE_CONNECTED -> {
                activeConnection = gatt
                activeConnection!!.discoverServices()
                connecting = false
            }
            BluetoothGatt.STATE_DISCONNECTED -> {
                connectionCallbacks.forEach { it.invoke(false) }
                connected = false
            }
        }
        when (status) {
            133 -> {
                connected = false
                connectionCallbacks.forEach { it.invoke(false) }
            }
        }
    }

    override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
        invokeActionCallback(status == BluetoothGatt.GATT_SUCCESS)
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        connected = true
        super.onServicesDiscovered(gatt, status)
        connectionCallbacks.forEach { it.invoke(true) }
    }

    fun updateAdvertisement(data : ByteArray, rssi : Int) {
        System.arraycopy(data, 0, advertisement, 0, data.size)
        this.rssi = rssi
        this.lastSeen = System.currentTimeMillis()
        postUpdate()
    }

    /**
     * Creates a queue of actions to be executed in a timely manner.
     * This is the recommended way to invoke characteristic changes.
     */
    protected fun post(context: Context, function: QueueBuilder.() -> Unit)  {
        val builder = QueueBuilder()
        builder.function()

        if (!(executionThread?.isAlive?:false)) {
            executionThread = ExecutionThread(this,context, builder.failure)
        }
        executionThread?.queues?.add(builder.queue)
    }

    fun postUpdate() {
        updateListeners.forEach { it.onDeviceUpdated(this) }
        onUpdate()
    }

    abstract fun onUpdate()

    fun onBeacon() {}

    fun writeCharacteristic(char: BluetoothGattCharacteristic, callback: ((Boolean)->Unit)) {
        actionCallback = callback
        activeConnection?.writeCharacteristic(char)?:invokeActionCallback(false)
    }

    private fun invokeActionCallback(res:Boolean) {
        actionCallback?.invoke(res)
        actionCallback = null
    }
}

class QueueBuilder {
    val queue = ConcurrentLinkedQueue<CharAction>()

    fun write(charHandle: CharHandle, value: ByteArray, result: (Boolean) -> Unit = {}) {
        queue.add(CharWrite(charHandle, value, result))
    }

    fun read(charHandle: CharHandle, result: (Boolean, ByteArray) -> Unit ) {

    }

    var failure: (()->Unit)? = null
    infix fun failure(g: ()->Unit) { failure = g }
}

interface UpdateListener {
    fun onDeviceUpdated(device: SmartDevice)
}

interface CharHandle {
    var charUuid : Uuid
    var serviceUuid : Uuid
}
