package com.jameslandrum.bluetoothsmart2.scanner

import android.annotation.TargetApi
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.os.Build
import android.support.annotation.IntDef
import android.support.annotation.RequiresApi
import com.jameslandrum.bluetoothsmart2.Identifier
import com.jameslandrum.bluetoothsmart2.SmartDevice
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.rxkotlin.toFlowable
import io.reactivex.rxkotlin.toObservable
import java.lang.ref.WeakReference
import java.lang.reflect.InvocationTargetException
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.HashSet

@TargetApi(Build.VERSION_CODES.KITKAT)
abstract class DeviceScanner {
    protected var scanMode: Int = 0
    protected var scanInterval: Int = 0
    protected val invalidDevices = ArrayList<String>()

    val identifiers = HashSet<Identifier>()
    var discovery = false
    val devices = DeviceSet()

    companion object {
        val APPLE_PREFIX = byteArrayOf(0x4C, 0x00)
        const val SCAN_MODE_PASSIVE = -1
        const val SCAN_MODE_LOW_POWER = 0
        const val SCAN_MODE_NORMAL = 1
        const val SCAN_MODE_LOW_LATENCY = 2
        var _instance : DeviceScanner? = null

        val instance: DeviceScanner
            get() {
                if (_instance != null) return _instance!!
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    _instance = LollipopDeviceScanner()
                } else {
                    _instance = KitKatDeviceScanner()
                }
                return _instance!!
            }

        fun startScan(scanMode: Int, interval: Int) = instance.startScan(scanMode,interval)

        @IntDef(SCAN_MODE_LOW_LATENCY.toLong(), SCAN_MODE_LOW_POWER.toLong(), SCAN_MODE_NORMAL.toLong(), SCAN_MODE_PASSIVE.toLong())
        @Retention(AnnotationRetention.SOURCE)
        annotation class ScanMode
    }

    fun <T : SmartDevice> forgetDevice(device: T) {
        invalidDevices.remove(device.address)
        devices.removeIf { it.address == device.address }
    }

    fun <T : SmartDevice> injectDevice(device: T): T {
        devices.removeIf { it.address == device.address }
        invalidDevices -= device.address
        devices += device
        return device
    }

    abstract fun startScan(@ScanMode scanMode: Int, interval: Int)
    abstract fun stopScan()
    abstract fun isScanning(): Boolean

    fun addIdentifier(identifier: Identifier) {
        identifiers += identifier
        invalidDevices.clear()
    }

    @RequiresApi(Build.VERSION_CODES.ECLAIR)
    internal fun processAdvertisement(data: ByteArray, device: BluetoothDevice, uuids: List<UUID>, rssi: Int) {
        if (invalidDevices.contains(device.address)) return

        val isBeacon = data[5] == APPLE_PREFIX[0] && data[6] == APPLE_PREFIX[1]

        if (devices.any { it.address == device.address }) {
            val target = devices[device.address]
            if (isBeacon) {
                target?.onBeacon()
            } else {
                target?.updateAdvertisement(data, rssi)
            }
        } else if (discovery && !isBeacon) {
            var identifier: Identifier? = null

            getId@ for (i in identifiers) {
                if (i.name != null && i.name != device.name) continue
                for (uuid in i.uuids) {
                    if (!uuids.contains(uuid)) continue@getId
                }
                if (!i.byteId.invoke(data)) continue
                identifier = i
            }

            if (identifier != null) {
                try {
                    val k = identifier.deviceClass
                    val target = injectDevice(k, device)
                    target.updateAdvertisement(data, rssi)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            } else {
                invalidDevices.add(device.address)
            }
        }

    }

    @RequiresApi(Build.VERSION_CODES.ECLAIR)
    @Throws(IllegalAccessException::class, InstantiationException::class, NoSuchMethodException::class, InvocationTargetException::class)
    fun <T : SmartDevice> injectDevice(k: Class<T>, device: BluetoothDevice): SmartDevice {
        if (devices.available(device.address)) return devices[device.address]!!

        val target = k.getConstructor(BluetoothDevice::class.java).newInstance(device)
        devices += target
        return target
    }

    @RequiresApi(Build.VERSION_CODES.ECLAIR)
    @Throws(IllegalAccessException::class, InstantiationException::class, NoSuchMethodException::class, InvocationTargetException::class)
    fun <T : SmartDevice> injectDevice(k: Class<T>, address: String): SmartDevice {
        return this.injectDevice(k, BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address))
    }

    fun getDeviceByMacAddress(macAddress: String): SmartDevice? {
        return devices.get(macAddress)
    }
}

interface OnDeviceUpdateListener {
    fun onDeviceUpdated(device: SmartDevice)
}

interface ScannerCallback {
    fun <T : SmartDevice> onDeviceDiscovered(device: T)
}

class DeviceSet : HashSet<SmartDevice>() {
    private val listeners = WeakListeners()

    operator fun get(macAddress: String) : SmartDevice? = this.find { it.address == macAddress }
    operator fun get(index: Int) : SmartDevice? = this.elementAt(index)
    fun available(macAddress: String) = this.any { it.address == macAddress }
    override fun add(element: SmartDevice): Boolean {
        val success = super.add(element)
        if (success) {
            listeners.removeIf { it.get() == null }
            listeners.forEach { it.get()?.invoke() }
        }
        return success
    }
    fun onChange(call: ()->Unit) = listeners.add(WeakReference(call))
}

typealias WeakListeners = ArrayList<WeakReference<()->Unit>>

