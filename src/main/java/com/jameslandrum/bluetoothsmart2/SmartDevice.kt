package com.jameslandrum.bluetoothsmart2

import android.bluetooth.*
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
    private var queue = ConcurrentLinkedQueue<()->Unit>()
    private var characteristicCallbacks = ConcurrentHashMap<CharHandle,(ByteArray)->Unit>()

    val address : String get() = nativeDevice.address
    val advertisement = ByteArray(62)
    var lastSeen : Long = 0L
    var connecting = false
    var connected = false

    val connectedOrConnecting : Boolean get() = connected || connecting

    var rssi : Int = -120
    var updateListeners = ConcurrentLinkedQueue<UpdateListener>()

    internal var channel : Channel<Boolean> = Channel<Boolean>()
    internal var thread = newSingleThreadContext("ActionThread_" + nativeDevice.address)

    suspend fun connect(context: Context, autoConnect: Boolean = false) : Boolean {
        if (connected) {
            return true
        } else {
            if (!connecting) {
                nativeDevice.connectGatt(context, autoConnect, this)
                connecting = true
            }
            return channel.receive()
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
                onDisconnect()
                connected = false
                launch(thread) {
                    channel.send(status == BluetoothGatt.GATT_SUCCESS)
                }
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
        onConnect()
        launch(thread) {
            channel.send(status == BluetoothGatt.GATT_SUCCESS)
        }
    }

    override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
        launch(thread) {
            channel.send(status == BluetoothGatt.GATT_SUCCESS)
        }
    }

    override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        characteristicCallbacks.filter { it.key.like(characteristic) }
                .forEach { _, method -> method.invoke(characteristic.value) }
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
            val result = action.invoke(this@SmartDevice)
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


    class QueueBuilder(val error : (Int)->Unit, val completed : ()->Unit) {
        val actions = ActionQueue()

        fun write(charHandle: CharHandle, value: ByteArray, result: (Boolean) -> Unit = {}) {
            actions.add(CharAction.CharWrite(charHandle, value, result))
        }

        fun connect(context: Context, result: (Boolean) -> Unit = {}) {
            actions.add(CharAction.Connect(context, result))
        }

        fun read(charHandle: CharHandle, result: (Boolean, ByteArray) -> Unit ) {

        }

        fun enableNotifications(charHandle: CharHandle, descriptor: String, result: (Boolean) -> Unit = {}) =
                enableNotifications(charHandle,Uuid(descriptor),result)

        fun enableNotifications(charHandle: CharHandle, descriptor: Uuid, result: (Boolean) -> Unit = {}) {
            actions.add(CharAction.CharRegister(charHandle, descriptor, true, result))
        }
    }

    fun registerNotify(handle: CharHandle, notify: (ByteArray) -> Unit) {
        characteristicCallbacks.put(handle,notify)
    }

    interface UpdateListener {
        fun onDeviceUpdated(device: SmartDevice)
    }

    interface CharHandle {
        var charUuid : Uuid
        var serviceUuid : Uuid
    }
}

fun SmartDevice.CharHandle.like(other: Any?): Boolean = when(other) {
    is BluetoothGattCharacteristic -> this.serviceUuid.uuid == other.service.uuid && this.charUuid.uuid == other.uuid
    is SmartDevice.CharHandle -> this.charUuid == other.charUuid && this.serviceUuid == other.serviceUuid
    else -> false
}

