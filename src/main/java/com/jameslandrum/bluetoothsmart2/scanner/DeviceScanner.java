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
import android.os.Build;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import com.jameslandrum.bluetoothsmart2.ScannerCallback;
import com.jameslandrum.bluetoothsmart2.SmartDevice;
import com.jameslandrum.bluetoothsmart2.SmartDeviceCallback;
import com.jameslandrum.bluetoothsmart2.actionqueue.Identifier;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Interface class that connects to the proper device scanner for Android
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public abstract class DeviceScanner {
    private static final byte[] APPLE_PREFIX =
            new byte[]{0x4C, 0x00};

    @IntDef({SCAN_MODE_LOW_LATENCY, SCAN_MODE_LOW_POWER, SCAN_MODE_NORMAL, SCAN_MODE_PASSIVE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ScanMode {}
    public static final int SCAN_MODE_PASSIVE =     -1;
    public static final int SCAN_MODE_LOW_POWER =   0;
    public static final int SCAN_MODE_NORMAL =      1;
    public static final int SCAN_MODE_LOW_LATENCY = 2;

    private boolean mEnableDiscovery = false;
    protected static int mScanMode;
    protected static int mScanInterval;
    private static DeviceScanner mInstance;
    protected static final ArrayList<ScannerCallback> mListeners = new ArrayList<>();
    protected static final ArrayList<String> mInvalidDevices = new ArrayList<>();
    protected static final ConcurrentHashMap<String, SmartDevice> mDevices = new ConcurrentHashMap<>();
    protected static final HashSet<Identifier> mIdentifiers = new HashSet<>();

    public <T extends SmartDevice> void forgetDevice(T device) {
        mInvalidDevices.remove(device.getAddress());
        mDevices.remove(device.getAddress());
    }

    public <T extends SmartDevice> void injectDevice(T device) {
        mDevices.remove(device.getAddress());
        mInvalidDevices.remove(device.getAddress());
        mDevices.put(device.getAddress(),device);
    }

    /**
     * Returns an instance of the DeviceScanner
     * @return The relevant device scanner for this device.
     */
    public static DeviceScanner getInstance() {
        if (mInstance != null) return mInstance;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mInstance = new LollipopDeviceScanner();
        } else {
            mInstance = new KitKatDeviceScanner();
        }
        return mInstance;
    }

    DeviceScanner() {
    }

    public abstract void startScan(@ScanMode int scanMode, int interval);
    public abstract void stopScan();
    public abstract boolean isScanning();

    public void addIdentifier(Identifier identifier) {
        mIdentifiers.add(identifier);
        mInvalidDevices.clear();
    }

    @SuppressWarnings("RedundantIfStatement")
    void processAdvertisement(byte[] data, BluetoothDevice device, List<UUID> uuids, int rssi) {
        if (mInvalidDevices.contains(device.getAddress())) return;

        boolean isBeacon = data[5] == APPLE_PREFIX[0] &&
                            data[6] == APPLE_PREFIX[1];

        if (mDevices.containsKey(device.getAddress())) {
            SmartDevice target = mDevices.get(device.getAddress());
            if (isBeacon) {
                target.onBeacon();
            } else {
                target.onAdvertisement(data,rssi);
            }
        } else if (mEnableDiscovery && !isBeacon) {
            Identifier identifier = null;

            getId: for (Identifier i : mIdentifiers) {
                if (i.getName() != null && !i.getName().equals(device.getName())) continue;
                for (UUID uuid: i.getUuids()) {
                    if (!uuids.contains(uuid)) continue getId;
                }
                if (i.getByteId() != null && !i.getByteId().checkBytes(data)) continue;
                identifier = i;
            }

            if (identifier != null) {
                try {
                    Class<? extends SmartDevice> k = identifier.getDeviceClass();

                    SmartDevice target = injectDevice(k, device);
                    target.onAdvertisement(data,rssi);

                    for (ScannerCallback c : mListeners) {
                        c.onDeviceDiscovered(target);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                mInvalidDevices.add(device.getAddress());
            }
        }

    }

    public <T extends SmartDevice> T injectDevice(Class<T> k, BluetoothDevice device) throws IllegalAccessException, InstantiationException {
        if (mDevices.containsKey(device.getAddress())) {
            //noinspection unchecked
            return (T) mDevices.get(device.getAddress());
        }

        T target = k.newInstance();
        target.init(device);
        mDevices.put(device.getAddress(), target);
        return target;
    }

    public <T extends SmartDevice> T injectDevice(Class<T> k, String address) throws IllegalAccessException, InstantiationException
    {
        if (mDevices.containsKey(address)) {
            //noinspection unchecked
            return (T) mDevices.get(address);
        }

        T target = k.newInstance();
        target.init(BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address));
        mDevices.put(address, target);
        return target;
    }

    public void enableDiscovery(boolean enableDiscovery) {
        mEnableDiscovery = enableDiscovery;
    }

    public List<SmartDevice> getAllDevices() {
        return Collections.unmodifiableList(new ArrayList<>(mDevices.values()));
    }

    public void addScanListener(@NonNull ScannerCallback listener) {
        mListeners.add(listener);
    }

    public void removeScanListener(@NonNull ScannerCallback listener) {
        mListeners.remove(listener);
    }

    public SmartDevice getDeviceByMacAddress(String macAddress) {
        return mDevices.get(macAddress);
    }
}

