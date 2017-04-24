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
import com.jameslandrum.bluetoothsmart2.actionqueue.Identifier;
import com.jameslandrum.bluetoothsmart2.scanner.DeviceScanner;

import java.lang.ref.WeakReference;
import java.util.List;

@SuppressWarnings("ALL")
public enum SmartDeviceManager {
    INSTANCE;

    private int mActiveMode = DeviceScanner.SCAN_MODE_LOW_LATENCY;
    private int mActiveBatchInterval = 0;
    private int mYieldMode = DeviceScanner.SCAN_MODE_LOW_POWER;
    private int mYieldBatchInterval = 15000;
    private boolean mIsForeground = true;
    private boolean mIsRunning = false;
    private DeviceScanner mScanner = DeviceScanner.getInstance();
    private static WeakReference<Application> mActiveContext;

    public static SmartDeviceManager getInstance()
    {
        return INSTANCE;
    }

    public static void setActiveContext(Application activeContext) {
        SmartDeviceManager.mActiveContext = new WeakReference<>(activeContext);
    }

    public static Context getActiveContext() {
        if (mActiveContext == null) throw new RuntimeException("Context must be supplied by application class.");
        return mActiveContext.get();
    }

    public void startScan()
    {
        if (mIsForeground) {
            mScanner.startScan(mActiveMode, mActiveBatchInterval);
            Logging.notice("Starting scan in Active mode.");
        } else {
            mScanner.startScan(mYieldMode, mYieldBatchInterval);
            Logging.notice("Starting scan in Passive mode.");
        }
    }

    public void stopScan() {
        mScanner.stopScan();
    }


    public void setYieldMode(int mode) {
        setYieldMode(mode, 0);
    }

    public void setYieldMode(int mode, int batchInterval) {
        mYieldMode = mode;
        mYieldBatchInterval = batchInterval;
        if (!mIsForeground && mIsRunning) {
            scanParametersChanged();
        }
    }

    private void scanParametersChanged() {
        Logging.notice("Scan parameters have changed. Restarting scanning.");
        startScan();
    }

    public void setActiveMode(int mode) {
        setActiveMode(mode, 0);
    }

    public void setActiveMode(int mode, int batchInterval) {
        mActiveMode = mode;
        mActiveBatchInterval = batchInterval;
        if (mIsForeground && mIsRunning) {
            scanParametersChanged();
        }
    }

    public void addIdentifier(Identifier identifier) {
        mScanner.addIdentifier(identifier);
    }

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
}
