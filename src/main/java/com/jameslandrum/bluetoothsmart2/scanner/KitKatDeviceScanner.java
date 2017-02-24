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

package com.jameslandrum.bluetoothsmart2.scanner;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Scanner for API 19
 */
public class KitKatDeviceScanner extends DeviceScanner implements BluetoothAdapter.LeScanCallback {
    private BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();
    private boolean mIsScanning;

    KitKatDeviceScanner() {
        super();
    }

    @Override
    public void startScan(@ScanMode int scanMode, int interval) {
        if (!mAdapter.isEnabled()) return;
        mAdapter.startLeScan(this);
        mIsScanning = true;
    }

    @Override
    public void stopScan() {
        if (!mAdapter.isEnabled()) return;
        mAdapter.stopLeScan(this);
        mIsScanning = false;
    }

    @Override
    public boolean isScanning() {
        return mAdapter.isEnabled() && mIsScanning;
    }

    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        // TODO: This needs to somehow extract the UUIDs for processing.
        processAdvertisement(scanRecord, device, new ArrayList<>(), rssi);
    }
}