/*
  Copyright 2016 James Landrum

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */

package com.jameslandrum.bluetoothsmart2.scanner

import android.annotation.TargetApi
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.Build
import android.util.Log

import java.util.ArrayList
import java.util.HashMap
import java.util.UUID

/**
 * Scanner for API 21 and above.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class LollipopDeviceScanner : DeviceScanner() {
    private val adapter = BluetoothAdapter.getDefaultAdapter()
    private var scanner: BluetoothLeScanner? = null
    private val uuidMap = HashMap<String, List<UUID>>()
    private var isScanning: Boolean = false

    override fun startScan(@ScanMode scanMode: Int, interval: Int) {
        this.scanMode = scanMode
        if (scanner == null) scanner = adapter.bluetoothLeScanner
        if (adapter.isEnabled && scanner != null) {
            val settings = ScanSettings.Builder()
            Log.d("LollipopDeviceScanner", "Setting scan mode to " + scanMode)
            settings.setScanMode(scanMode)
            if (interval > 0) settings.setReportDelay(interval.toLong())
            scanner!!.startScan(null, settings.build(), callback)
            isScanning = true
        }
    }

    override fun stopScan() {
        if (scanner != null && adapter.isEnabled) {
            scanner!!.stopScan(callback)
            isScanning = false
        }
    }

    override fun isScanning(): Boolean {
        return isScanning
    }

    private val callback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            try {
                if (result.scanRecord != null) {
                    var uuids: MutableList<UUID>? = uuidMap[result.device.address]?.toMutableList()

                    if (uuids == null) {
                        if (result.scanRecord.serviceUuids != null) {
                            uuids = ArrayList<UUID>()

                            for (uuid in result.scanRecord.serviceUuids) {
                                uuids.add(uuid.uuid)
                            }
                        } else {
                            uuids = ArrayList<UUID>()
                        }
                        uuidMap.put(result.device.address, uuids)
                    }

                    processAdvertisement(result.scanRecord.bytes, result.device, uuids, result.rssi)
                }
            } catch (ignored: Exception) {
                ignored.printStackTrace()
            }

        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
        }
    }
}