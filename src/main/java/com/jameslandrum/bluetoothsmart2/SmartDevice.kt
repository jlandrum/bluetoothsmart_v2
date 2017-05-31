package com.jameslandrum.bluetoothsmart2

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.util.Log
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.Channel
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

typealias ActionQueue = ConcurrentLinkedQueue<CharAction>

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

    internal var channel : Channel<Boolean> = Channel<Boolean>()
    internal var thread = newSingleThreadContext("ActionThread_" + nativeDevice.address)

    fun connect(context: Context, autoConnect: Boolean = false, callback: (Boolean) -> Unit) {
        if (connected) {
            callback.invoke(true)
        } else {
            connectionCallbacks += callback
            if (!connecting) {
                nativeDevice.connectGatt(context, autoConnect, this)
                connecting = true
            }
        }
    }

    fun diconnect() = activeConnection?.disconnect()

    override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
        connecting = false
        when (newState) {
            BluetoothGatt.STATE_CONNECTED -> {
                activeConnection = gatt
                activeConnection!!.discoverServices()
            }
            BluetoothGatt.STATE_DISCONNECTED -> {
                connectionCallbacks.forEach { it.invoke(false); }
                connectionCallbacks.clear()
                onDisconnect()
                connected = false
            }
        }
    }

    override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
        launch(thread) {
            channel.send(status == BluetoothGatt.GATT_SUCCESS)
        }
        Log.d("BluetoothSmart", "Char written.")
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        connected = true
        connecting = false
        super.onServicesDiscovered(gatt, status)
        onConnect()
        connectionCallbacks.forEach { it.invoke(true) }
        connectionCallbacks.clear()
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
     * Creates a actions of actions to be executed in a timely manner.
     * This is the recommended way to invoke characteristic changes.
     */
    protected fun post(completed: ()->Unit = {}, error: (Int)->Unit = {}, function: QueueBuilder.() -> Unit)  {
        val builder = QueueBuilder(error,completed)
        builder.function()
        executeQueue(builder)
    }

    fun executeQueue(queue:QueueBuilder) = async(thread) {
        while (queue.actions.isNotEmpty()) {
            val action = queue.actions.remove()
            Log.d("BluetoothSmart", "Invoking Action " + action.toString())
            action.invoke(this@SmartDevice).join()
            val result = channel.receive()
            Log.d("BluetoothSmart", "Action Complete" + action.toString() + " " + result)
            if (!result) {
                queue.actions.clear()
                queue.error.invoke(0)
                return@async
            } else {
                action.response(result)
            }
        }
        queue.completed.invoke()
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

class QueueBuilder(val error : (Int)->Unit, val completed : ()->Unit) {
    val actions = ActionQueue()

    fun write(charHandle: CharHandle, value: ByteArray, result: (Boolean) -> Unit = {}) {
        actions.add(CharWrite(charHandle, value, result))
    }

    fun connect(context: Context, result: (Boolean) -> Unit = {}) {
        actions.add(Connect(context, result))
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

