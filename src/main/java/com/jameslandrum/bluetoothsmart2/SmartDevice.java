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

import android.bluetooth.*;
import android.content.Context;
import com.jameslandrum.bluetoothsmart2.actions.Action;
import com.jameslandrum.bluetoothsmart2.actions.ActionRunner;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

@SuppressWarnings("unused")
public abstract class SmartDevice extends BluetoothGattCallback {
    private BluetoothDevice mDevice;
    private ActionRunner mActionRunner = new ActionRunner(this);

    private boolean mIsRegistered;

    private HashSet<Characteristic> mCharacteristics = new HashSet<>();

    private ConcurrentLinkedQueue<OnConnectionStateListener> mConnectionListeners = new ConcurrentLinkedQueue<>();
    private ConcurrentLinkedQueue<OnDeviceUpdateListener> mUpdateListeners = new ConcurrentLinkedQueue<>();

    private BluetoothGatt mActiveConnection;
    private boolean mServicesDiscovered;
    private byte[] mAdvertisement = new byte[62];
    private int mRssi = -120;
    private long mLastSeen;
    private long mConnectionTimeout = 15000;
    private boolean mConnected;

    private Timer mRssiAdjuster = new Timer();

    public void init(BluetoothDevice device) {
        mDevice = device;
        mRssiAdjuster.schedule(new TimerTask() {
            @Override
            public void run() {
                if (mLastSeen < System.currentTimeMillis() - 15000) {
                    mRssi = (mRssi * 5 - 120) / 5;
                    for (OnDeviceUpdateListener l : mUpdateListeners) {
                        l.onDeviceUpdated(SmartDevice.this);
                    }
                }
            }
        }, 5000, 5000);
        mCharacteristics.addAll(allocateCharacteristics());
    }

    final public BluetoothGatt getActiveConnection() {
        return mActiveConnection;
    }

    public void connect(Context context) {
        mDevice.connectGatt(context, false, this);
    }

    public void disconnect() {
        if (mActiveConnection != null) mActiveConnection.disconnect();
    }

    final protected void startAction(Action action, Context context) {
        mActionRunner.addAction(action, context);
    }

    public long getConnectionTimeout() { return mConnectionTimeout; }
    public void setConnectionTimeout(long time) { mConnectionTimeout = time; }

    @Override
    final public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);
        if (newState == BluetoothAdapter.STATE_CONNECTED) {
            Logging.notice("Device %s connected.", this.getClass().getSimpleName());
            mActiveConnection = gatt;
            mActiveConnection.discoverServices();
            for (OnConnectionStateListener d : mConnectionListeners) {
                d.onConnected(this);
            }
        } else {
            for (Characteristic c : mCharacteristics) {
                c.clearAllCallbacks();
            }
            Logging.notice("Device %s disconnected.", this.getClass().getSimpleName());
            for (OnConnectionStateListener d : mConnectionListeners) {
                d.onDisconnected(this);
            }
            for (Characteristic characteristic : mCharacteristics) characteristic.reset();
            mActiveConnection = null;
            gatt.close();
        }
        mConnected = newState == BluetoothAdapter.STATE_CONNECTED;
    }

    @Override
    final public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        Logging.notice("Device %s services discovered.", this.getClass().getSimpleName());

        try {
            for (Characteristic c : mCharacteristics) {
                BluetoothGattService service = gatt.getService(c.getServiceUuid());
                if (service == null) {
                    for (BluetoothGattService gattService : gatt.getServices()) {
                        if (gattService.getCharacteristic(c.getHandleUuid()) != null) {
                            service = gattService;
                            break;
                        }
                    }
                }
                if (service != null) {
                    c.prepare(service.getCharacteristic(c.getHandleUuid()));
                }
            }

            mServicesDiscovered = true;
            for (OnConnectionStateListener c : mConnectionListeners) {
                c.onServicesDiscovered(this);
            }
        } catch (Exception e) {
            e.printStackTrace();
            disconnect();
        }
    }

    @Override
    @SuppressWarnings("Duplicates")
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        for (Characteristic c : mCharacteristics) {
            if (c.getNativeCharacteristic() == characteristic) {
                c.callEvent(status == BluetoothGatt.GATT_SUCCESS ?
                        CharacteristicCallback.EVENT_CHARACTERISTIC_WRITE :
                        CharacteristicCallback.EVENT_CHARACTERISTIC_WRITE_FAILURE);
                break;
            }
        }
        Logging.notice("Write result: %d", status);
    }

    @Override
    @SuppressWarnings("Duplicates")
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        for (Characteristic c : mCharacteristics) {
            if (c.getNativeCharacteristic() == characteristic) {
                c.callEvent(status == BluetoothGatt.GATT_SUCCESS ?
                        CharacteristicCallback.EVENT_CHARACTERISTIC_READ :
                        CharacteristicCallback.EVENT_CHARACTERISTIC_READ_FAILURE);
                break;
            }
        }
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        for (Characteristic c : mCharacteristics) {
            if (c.getNativeCharacteristic().getDescriptors().contains(descriptor)) {
                c.callEvent(status == BluetoothGatt.GATT_SUCCESS ?
                        CharacteristicCallback.EVENT_DESCRIPTOR_WRITE :
                        CharacteristicCallback.EVENT_DESCRIPTOR_WRITE_FAILURE);
                break;
            }
        }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        for (Characteristic c : mCharacteristics) {
            if (c.getNativeCharacteristic() == characteristic) {
                c.notifyUpdate();
                break;
            }
        }
    }

    public boolean isReady() {
        return mServicesDiscovered && mActiveConnection != null;
    }

    public void subscribeToUpdates(OnConnectionStateListener listener) {
        mConnectionListeners.add(listener);
    }

    public void unsubscribeToUpdates(OnConnectionStateListener listener) {
        mConnectionListeners.remove(listener);
    }

    public String getAddress() {
        return mDevice.getAddress();
    }

    public BluetoothDevice getDevice() {
        return mDevice;
    }

    public void onAdvertisement(byte[] data, int rssi) {
        mLastSeen = System.currentTimeMillis();
        mAdvertisement = data;
        System.arraycopy(data, 0, mAdvertisement, 0, mAdvertisement.length);
        mRssi = rssi;
        for (OnDeviceUpdateListener updateListener : mUpdateListeners) {
            updateListener.onDeviceUpdated(this);
        }
    }

    public int getRssi() {
        return mRssi;
    }

    public byte[] getAdvertisement() {
        return mAdvertisement;
    }

    @SuppressWarnings("WeakerAccess")
    public long getLastSeen() {
        return mLastSeen;
    }

    public boolean isConnected() {
        return mConnected;
    }

    public void onBeacon() {
    }

    public void addOnUpdateListener(OnDeviceUpdateListener listener) {
        if (!mUpdateListeners.contains(listener)) {
            mUpdateListeners.add(listener);
        }
    }

    public void removeOnUpdateListener(OnDeviceUpdateListener listener) {
        mUpdateListeners.remove(listener);
    }

    private void setRssi(int rssi) {
        this.mRssi = rssi;
    }

    public abstract List<Characteristic> allocateCharacteristics();
}
