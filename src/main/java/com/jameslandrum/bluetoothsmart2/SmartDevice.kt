package com.jameslandrum.bluetoothsmart2

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
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

    val address : String get() = nativeDevice.address
    val advertisement = ByteArray(62)
    val lastSeen : Long = System.currentTimeMillis()
    var connecting = false
    var connected = false
    val connectedOrConnecting : Boolean get() = connected || connecting

    var rssi : Int = -120
    var updateListeners = ConcurrentSkipListSet<UpdateListener>()

    var executionThread : ExecutionThread? = null

    fun connect(context: Context, autoConnect: Boolean = false) = nativeDevice.connectGatt(context, autoConnect, this)
    fun diconnect() = activeConnection?.disconnect()
//
//    fun characteristic(handle : CharHandle, with: Characteristic.()->Unit = {}) : Unit
//            = characteristic(handle.serviceUuid, handle.charUuid, with)
//    fun characteristic(service: String, id : String, with: Characteristic.()->Unit = {}) : Unit
//            = characteristic(Uuid(id),Uuid(service,id), with)
//    fun characteristic(service : Uuid, id : Uuid, with: Characteristic.()->Unit = {}) : Unit {
//        val char = characteristics.find { it.id.uuid == service.uuid && it.service.uuid == id.uuid }
//        with(char!!)
//    }

    override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
        when (newState) {
            BluetoothGatt.STATE_CONNECTED -> {
                activeConnection = gatt
                activeConnection!!.discoverServices()
                executionThread?.kick()
            }
        }
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        connected = true
        super.onServicesDiscovered(gatt, status)
    }

    fun updateAdvertisement(data : ByteArray, rssi : Int) {
        System.arraycopy(data, 0, advertisement, 0, data.size)
        this.rssi = rssi
        onUpdate()
    }

    /**
     * Creates a queue of actions to be executed in a timely manner.
     * This is the recommended way to invoke characteristic changes.
     */
    protected fun post(context: Context, function: QueueBuilder.() -> Unit)  {
        val builder = QueueBuilder()
        builder.function()

        if (executionThread?.isInterrupted?:false) {
            executionThread = ExecutionThread(this,context)
            executionThread?.actions?.add(builder.queue)
        }
    }

    fun postUpdate() {
        onUpdate()
    }

    abstract fun onUpdate()

    fun onBeacon() {}
}

class QueueBuilder {
    val queue = ConcurrentLinkedQueue<CharAction>()

    fun write(charHandle: CharHandle, value: ByteArray, result: (Boolean) -> Unit) {
        queue.add(CharWrite(charHandle, value, result))
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
