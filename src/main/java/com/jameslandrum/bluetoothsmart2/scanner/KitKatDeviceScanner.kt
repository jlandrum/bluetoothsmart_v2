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
import android.bluetooth.BluetoothDevice
import android.os.Build

import java.util.ArrayList
import java.util.UUID

/**
 * Scanner for API 19
 */
@TargetApi(Build.VERSION_CODES.KITKAT)
class KitKatDeviceScanner internal constructor() : DeviceScanner(), BluetoothAdapter.LeScanCallback {
    private val mAdapter = BluetoothAdapter.getDefaultAdapter()
    private var mIsScanning: Boolean = false

    override fun startScan(@ScanMode scanMode: Int, interval: Int) {
        if (!mAdapter.isEnabled) return
        mAdapter.startLeScan(this)
        mIsScanning = true
    }

    override fun stopScan() {
        if (!mAdapter.isEnabled) return
        mAdapter.stopLeScan(this)
        mIsScanning = false
    }

    override fun isScanning(): Boolean {
        return mAdapter.isEnabled && mIsScanning
    }

    override fun onLeScan(device: BluetoothDevice, rssi: Int, scanRecord: ByteArray) {
        // TODO: This needs to somehow extract the UUIDs for processing.
        processAdvertisement(scanRecord, device, listOf(), rssi)
    }
}