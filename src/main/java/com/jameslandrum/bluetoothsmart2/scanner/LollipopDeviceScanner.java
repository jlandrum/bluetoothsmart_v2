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

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Build;
import android.os.ParcelUuid;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Scanner for API 21 and above.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class LollipopDeviceScanner extends DeviceScanner {
    private BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();
    private BluetoothLeScanner mScanner;
    private HashMap<String, List<UUID>> mUuidMap = new HashMap<>();
    private boolean mIsScanning;

    @Override
    public void startScan(@ScanMode int scanMode, int interval) {
        mScanMode = scanMode;
        if (mScanner == null) mScanner = mAdapter.getBluetoothLeScanner();
        if (mAdapter.isEnabled() && mScanner != null) {
            ScanSettings.Builder settings = new ScanSettings.Builder();
            Log.d("LollipopDeviceScanner", "Setting scan mode to " + scanMode);
            settings.setScanMode(scanMode);
            if (interval > 0) settings.setReportDelay(interval);
            mScanner.startScan(null, settings.build(), callback);
            mIsScanning = true;
        }
    }

    @Override
    public void stopScan() {
        if (mScanner != null && mAdapter.isEnabled()) {
            mScanner.stopScan(callback);
            mIsScanning = false;
        }
    }

    @Override
    public boolean isScanning() {
        return mIsScanning;
    }

    private ScanCallback callback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, final ScanResult result) {
            try {
                if (result.getScanRecord() != null) {
                    List<UUID> uuids = mUuidMap.get(result.getDevice().getAddress());

                    if (uuids == null) {
                        if (result.getScanRecord().getServiceUuids() != null) {
                            uuids = new ArrayList<>();

                            for (ParcelUuid uuid : result.getScanRecord().getServiceUuids()) {
                                uuids.add(uuid.getUuid());
                            }
                        } else {
                            uuids = new ArrayList<>();
                        }
                        mUuidMap.put(result.getDevice().getAddress(), uuids);
                    }

                    processAdvertisement(result.getScanRecord().getBytes(), result.getDevice(), uuids, result.getRssi());
                }
            } catch (Exception ignored) {
                ignored.printStackTrace();
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    };
}