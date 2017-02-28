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

import android.bluetooth.BluetoothDevice;
import android.os.Build;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import com.annimon.stream.Optional;
import com.annimon.stream.Stream;
import com.jameslandrum.bluetoothsmart2.ScannerCallback;
import com.jameslandrum.bluetoothsmart2.SmartDevice;
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

    protected static int mScanMode;
    protected static int mScanInterval;
    private static DeviceScanner mInstance;
    protected static final ArrayList<ScannerCallback> mListeners = new ArrayList<>();
    protected static final ArrayList<String> mInvalidDevices = new ArrayList<>();
    protected static final ConcurrentHashMap<String, SmartDevice> mDevices = new ConcurrentHashMap<>();
    protected static final HashSet<Identifier> mIdentifiers = new HashSet<>();

    public void forgetDevice(SmartDevice device) {
        mInvalidDevices.remove(device.getAddress());
        mDevices.remove(device.getAddress());
    }

    public void injectDevice(SmartDevice device) {
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
    }

    @SuppressWarnings("RedundantIfStatement")
    void processAdvertisement(byte[] data, BluetoothDevice device, List<UUID> uuids, int rssi) {
        if (mInvalidDevices.contains(device.getAddress())) return;

        boolean isBeacon = Arrays.equals(Arrays.copyOfRange(data, 5, 7), APPLE_PREFIX);

        if (mDevices.containsKey(device.getAddress())) {
            SmartDevice target = mDevices.get(device.getAddress());
            if (isBeacon) {
                target.notifyEvent(SmartDevice.EVENT_NEW_BEACON);
                Stream.of(mListeners).forEach(e->e.onDeviceEvent(ScannerCallback.DEVICE_BEACONED, target));
            } else {
                target.newAdvertisement(data,rssi);
                Stream.of(mListeners).forEach(e->e.onDeviceEvent(ScannerCallback.DEVICE_UPDATED, target));
            }
        } else if (!isBeacon) {
            Optional<Identifier> identifier = Stream.of(mIdentifiers).filter(id -> {
                if (id.getName() != null && !id.getName().equals(device.getName())) return false;
                if (!Stream.of(id.getUuids()).allMatch(uuid->
                     Stream.of(uuids).anyMatch(duuid-> duuid != null && duuid.equals(uuid)))) {
                    return false;
                }
                if (id.getByteId() != null && !id.getByteId().checkBytes(data)) return false;
                return true;
            }).findFirst();

            if (identifier.isPresent()) {
                try {
                    Class c = identifier.get().getDeviceClass();
                    SmartDevice target = (SmartDevice) c.newInstance();
                    target.init(device);
                    target.newAdvertisement(data,rssi);
                    mDevices.put(device.getAddress(), target);

                    Stream.of(mListeners).forEach(e->e.onDeviceEvent(ScannerCallback.DEVICE_DISCOVERED, target));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

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

