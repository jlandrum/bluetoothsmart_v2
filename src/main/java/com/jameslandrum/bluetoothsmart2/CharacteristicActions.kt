package com.jameslandrum.bluetoothsmart2

import android.bluetooth.BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
import android.bluetooth.BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
import android.content.Context
import android.util.Log
import kotlinx.coroutines.experimental.*
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.locks.Lock
import kotlin.collections.HashSet

interface CharAction {
    val response: (Boolean) -> Unit
    suspend fun invoke(smartDevice: SmartDevice): Boolean

    class CharWrite(val handle: SmartDevice.CharHandle,
                    val data: ByteArray,
                    override val response: (Boolean) -> Unit) : CharAction {

        override suspend fun invoke(smartDevice: SmartDevice) : Boolean {
            val char = smartDevice.activeConnection
                    ?.services?.find { it.uuid == handle.serviceUuid.uuid }
                    ?.characteristics?.find { it.uuid == handle.charUuid.uuid }
            if (char == null) {
                Log.d("BluetoothSmart", "Writing Char $handle failed - No such characteristic")
                return false
            } else {
                Log.d("BluetoothSmart", "Writing Char $handle with $data")
                char.value = data
                smartDevice.activeConnection!!.writeCharacteristic(char)
                return smartDevice.channel.receive()
            }
        }
    }

    class CharRegister(val handle: SmartDevice.CharHandle,
                       val uuid: Uuid,
                    val register: Boolean,
                    override val response: (Boolean) -> Unit) : CharAction {

        override suspend fun invoke(smartDevice: SmartDevice) : Boolean {
            val desc = smartDevice.activeConnection
                    ?.services?.find { it.uuid == handle.serviceUuid.uuid }
                    ?.characteristics?.find { it.uuid == handle.charUuid.uuid }?.descriptors?.find { it.uuid == uuid.uuid }

            if (desc == null) {
                Log.d("BluetoothSmart", "Registering Char $handle failed - No such characteristic")
                return false
            } else {
                Log.d("BluetoothSmart", "Registering Char $handle")
                desc.setValue(ENABLE_NOTIFICATION_VALUE)
                smartDevice.activeConnection!!.writeDescriptor(desc)
                smartDevice.channel.receive()
                smartDevice.activeConnection!!.setCharacteristicNotification(desc.characteristic, true)
                return smartDevice.channel.receive()
            }
        }
    }

    class Connect(val ctx: Context, override val response: (Boolean) -> Unit) : CharAction {
        override suspend fun invoke(smartDevice: SmartDevice) : Boolean {
            Log.d("BluetoothSmart", "Connecting")
            return smartDevice.connect(ctx, false)
        }
    }
}

