package com.jameslandrum.bluetoothsmart2

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.Channel
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

abstract class SmartDevice(val nativeDevice: BluetoothDevice) : BluetoothGattCallback() {
    internal var activeConnection : BluetoothGatt? = null
    private var servicesDiscovered : Boolean = false
    private var characteristicCallbacks = ConcurrentHashMap<CharHandle,(ByteArray)->Unit>()
    private var queue = ConcurrentLinkedQueue<()->Unit>()

    val address : String get() = nativeDevice.address
    val advertisement = ByteArray(62)
    var lastSeen : Long = 0L
    var connecting = false
    var connected = false
    private val connectionCallbacks = ArrayList<((Boolean) -> Unit)>()

    val connectedOrConnecting : Boolean get() = connected || connecting

    var rssi : Int = -120
    var updateListeners = ConcurrentLinkedQueue<UpdateListener>()

    private var actionQueue = ActionQueue()
    internal var channel : Channel<Boolean> = Channel<Boolean>()
    var thread = newSingleThreadContext("ActionThread_" + nativeDevice.address)

    fun connect(context: Context, autoConnect: Boolean = false, callback: (Boolean) -> Unit) {
        if (connected) {
            callback.invoke(true)
        } else {
            connectionCallbacks += callback
            nativeDevice.connectGatt(context, autoConnect, this)
        }
    }

    fun diconnect() = activeConnection?.disconnect()

    override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
        when (newState) {
            BluetoothGatt.STATE_CONNECTED -> {
                activeConnection = gatt
                activeConnection!!.discoverServices()
                connecting = true
            }
            BluetoothGatt.STATE_DISCONNECTED -> {
                connectionCallbacks.forEach { it.invoke(false); connectionCallbacks -= it }
                onDisconnect()
                connected = false
                connecting = false
            }
        }
        when (status) {
            133 -> {
                connected = false
                connecting = false
                connectionCallbacks.forEach { it.invoke(false); connectionCallbacks -= it  }
            }
        }
    }

    override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
        launch(thread) {
            channel.send(status == BluetoothGatt.GATT_SUCCESS)
        }
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        connected = true
        connecting = false
        super.onServicesDiscovered(gatt, status)
        onConnect()
        connectionCallbacks.forEach { it.invoke(true) }
    }

    open fun onConnect() {}
    open fun onDisconnect() {}

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
    protected fun post(function: QueueBuilder.() -> Unit)  {
        val builder = QueueBuilder()
        builder.function()
        actionQueue.addAll(builder.queue)
        executeQueue()
    }

    fun executeQueue() = async(thread) {
        while (actionQueue.isNotEmpty()) {
            actionQueue.remove().invoke(this@SmartDevice).join()
        }
    }


    fun postUpdate() {
        updateListeners.forEach { it.onDeviceUpdated(this) }
        onUpdate()
    }

    abstract fun onUpdate()

    fun onBeacon() {}

    fun disconnect() {
        activeConnection?.disconnect()
    }
}

class QueueBuilder {
    val queue = ActionQueue()

    fun write(charHandle: CharHandle, value: ByteArray, result: (Boolean) -> Unit = {}) {
        queue.add(CharWrite(charHandle, value, result))
    }

    fun connect(context: Context, result: (Boolean) -> Unit = {}) {
        queue.add(Connect(context, result))
    }

    fun read(charHandle: CharHandle, result: (Boolean, ByteArray) -> Unit ) {

    }
}

interface UpdateListener {
    fun onDeviceUpdated(device: SmartDevice)
}

interface CharHandle {
    var charUuid : Uuid
    var serviceUuid : Uuid
}

typealias ActionQueue = ConcurrentLinkedQueue<CharAction>

