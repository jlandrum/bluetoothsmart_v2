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

import android.app.Application;
import android.content.Context;
import com.jameslandrum.bluetoothsmart2.actions.Identifier;
import com.jameslandrum.bluetoothsmart2.scanner.DeviceScanner;

import java.lang.ref.WeakReference;
import java.util.List;

@SuppressWarnings("ALL")
public enum SmartDeviceManager {
    INSTANCE;

    private int mActiveMode = DeviceScanner.SCAN_MODE_LOW_LATENCY;
    private int mActiveBatchInterval = 0;
    private int mPauseMode = DeviceScanner.SCAN_MODE_LOW_POWER;
    private int mPauseBatchInterval = 15000;

    private boolean mIsForeground = true;
    private boolean mIsRunning = false;
    private DeviceScanner mScanner = DeviceScanner.getInstance();
    private static WeakReference<Application> mActiveContext;

    /**
     * Gets the SmartDeviceManager instance.
     * @return The singleton instance to SmartDeviceManager.
     */
    public static SmartDeviceManager getInstance()
    {
        return INSTANCE;
    }

    /**
     * Attemps to start scanning, if permissions are properly granted and Bluetooth is enabled.
     */
    public void startScan()
    {
        if (mIsForeground) {
            mScanner.startScan(mActiveMode, mActiveBatchInterval);
            Logging.notice("Starting scan in Active mode.");
        } else {
            mScanner.startScan(mPauseMode, mPauseBatchInterval);
            Logging.notice("Starting scan in Passive mode.");
        }
    }

    /**
     * Stops scanning.
     */
    public void stopScan() {
        mScanner.stopScan();
    }

    /**
     * Sets the configuration to use when pause is called.
     * @param mode The scan mode to use. Defaults to DeviceScanner.SCAN_MODE_LOW_POWER
     */
    public void setPauseMode(int mode) {
        setPauseMode(mode, 0);
    }

    /**
     * Sets the configuration to use when pause is called.
     * @param mode The scan mode to use. Defaults to DeviceScanner.SCAN_MODE_LOW_POWER
     * @param batchInterval How long to delay before reporting advertisements.
     */
    public void setPauseMode(int mode, int batchInterval) {
        mPauseMode = mode;
        mPauseBatchInterval = batchInterval;
        if (!mIsForeground && mIsRunning) {
            scanParametersChanged();
        }
    }

    /**
     * Sets the mode to use in active mode.
     * @param mode The scan mode to use. Defaults to DeviceScanner.SCAN_MODE_LOW_LATENCY
     */
    public void setActiveMode(int mode) {
        setActiveMode(mode, 0);
    }

    /**
     * Sets the mode to use in active mode.
     * @param mode The scan mode to use. Defaults to DeviceScanner.SCAN_MODE_LOW_LATENCY
     * @param batchInterval How long to delay before reporting advertisements.
     */
    public void setActiveMode(int mode, int batchInterval) {
        mActiveMode = mode;
        mActiveBatchInterval = batchInterval;
        if (mIsForeground && mIsRunning) {
            scanParametersChanged();
        }
    }

    /**
     * Adds an identifier, which is an object used to determine the Java object type of a given device.
     * @param identifier An identifier build using Identifier.Builder()
     */
    public void addIdentifier(Identifier identifier) {
        mScanner.addIdentifier(identifier);
    }

    /**
     * Adds a listener that watches for new devices, as well as devices that are out of range.
     * @param scannerListener
     */
    public void addScanListener(ScannerCallback scannerListener) {
        mScanner.addScanListener(scannerListener);
    }

    public void removeScanListener(ScannerCallback scannerListener) {
        mScanner.removeScanListener(scannerListener);
    }

    public void wake() {
        Logging.notice("Resuming Scanning");
        mIsForeground = true;
        scanParametersChanged();
    }

    public void sleep() {
        Logging.notice("Yielding Scanning");
        mIsForeground = false;
        scanParametersChanged();
    }

    public List<SmartDevice> getAllDevices() {
        return mScanner.getAllDevices();
    }

    public void cleanup(int staleTime) {
        for (SmartDevice d : mScanner.getAllDevices()) {
            if (System.currentTimeMillis() - d.getLastSeen() > staleTime) mScanner.forgetDevice(d);
        }
    }

    public SmartDevice getDeviceByMac(String macAddress) {

        return mScanner.getDeviceByMacAddress(macAddress);
    }

    public void enableDiscovery(boolean b) {
        mScanner.enableDiscovery(b);
    }

    public <T extends SmartDevice> T injectDevice(Class<T> deviceType, String address) throws InstantiationException, IllegalAccessException {
        return mScanner.injectDevice(deviceType, address);
    }

    private void scanParametersChanged() {
        Logging.notice("Scan parameters have changed. Restarting scanning.");
        startScan();
    }

}
