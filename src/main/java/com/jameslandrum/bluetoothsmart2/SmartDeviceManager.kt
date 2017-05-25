/*
  Copyright 2017 James Landrum

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
package com.jameslandrum.bluetoothsmart2;

import com.jameslandrum.bluetoothsmart2.scanner.DeviceScanner;
import java.util.*
import kotlin.concurrent.timerTask

@SuppressWarnings("ALL")
enum class SmartDeviceManager {
    instance;

    var scanMode : Int = DeviceScanner.SCAN_MODE_LOW_LATENCY
        set(value) { field = value; scanParametersChanged() }
    var batchInterval = 0
        set(value) { field = value; scanParametersChanged() }

    private var isRunning = false
    private var scanner = DeviceScanner.getInstance()

    /**
     * Attemps to start scanning, if permissions are properly granted and Bluetooth is enabled.
     */
    fun startScan() {
        scanner.startScan(scanMode, batchInterval)
        Logging.notice("Starting Scan")
    }

    /**
     * Stops scanning.
     */
    fun stopScan() {
        scanner.stopScan()
        Logging.notice("Stopping Scan")
    }

    /**
     * Adds an identifier, which is an object used to determine the Java object type of a given device.
     * @param identifier An identifier build using Identifier.Builder()
     */
    fun addIdentifier(identifier: Identifier) {
        scanner.addIdentifier(identifier)
    }

    /**
     * Adds a listener that watches for new devices, as well as devices that are out of range.
     * @param scannerListener
     */
    fun addScanListener(scannerListener: ScannerCallback) {
        scanner.addScanListener(scannerListener)
    }

    fun removeScanListener(scannerListener: ScannerCallback) {
        scanner.removeScanListener(scannerListener)
    }

    fun getAllDevices() : List<SmartDevice> {
        return scanner.allDevices
    }

    fun cleanup(staleTime: Int) {
        scanner.allDevices
                .filter { System.currentTimeMillis() - it.lastSeen > staleTime; }
                .forEach { scanner.forgetDevice(it) }
    }

    fun getDeviceByMac(address: String) = scanner.allDevices.find { it.address == address }

    fun enableDiscovery(b : Boolean) {
        scanner.enableDiscovery(b)
    }

    fun <T: SmartDevice> injectDevice(type: Class<T>, address: String) : T = scanner.injectDevice<T>(type, address)
    fun <T: SmartDevice> injectDevice(device: T) : T = scanner.injectDevice<T>(device)

    fun scanParametersChanged() {
        Logging.notice("Scan Parameters Changed, Restarting Scan If Necessary")
        if (isRunning) startScan()
    }
}
