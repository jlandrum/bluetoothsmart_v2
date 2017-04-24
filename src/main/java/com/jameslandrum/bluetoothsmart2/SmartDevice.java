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
import com.jameslandrum.bluetoothsmart2.actionqueue.ActionRunner;
import com.jameslandrum.bluetoothsmart2.actionqueue.ExecutionQueue;
import com.jameslandrum.bluetoothsmart2.actionqueue.Intention;

import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.ConcurrentLinkedQueue;

@SuppressWarnings("unused")
public abstract class SmartDevice extends BluetoothGattCallback {
    private BluetoothDevice mDevice;
    private ActionRunner mActionRunner = new ActionRunner(this);

    private boolean mIsRegistered;

    private HashSet<Characteristic> mCharacteristics = new HashSet<>();

    private ConcurrentLinkedQueue<SmartDeviceCallback> mListeners = new ConcurrentLinkedQueue<>();
    private BluetoothGatt mActiveConnection;
    private boolean mServicesDiscovered;
    private byte[] mAdvertisement = new byte[62];
    private int mRssi;
    private long mLastSeen;
    private boolean mConnected;

    public void init(BluetoothDevice device) {
        mDevice = device;
    }

    final public void registerCharacteristics(Characteristic ... characteristics) {
        if (isConnected()) throw new RuntimeException("Cannot modify characteristics of connected device.");
        mCharacteristics.clear();
        Collections.addAll(mCharacteristics, characteristics);
    }

    final public BluetoothGatt getActiveConnection() {
        return mActiveConnection;
    }

    public void connect(Context context) {
        mDevice.connectGatt(context, false, this);
    }

    public void disconnect() {
        if (mActiveConnection!=null) mActiveConnection.disconnect();
    }

    final protected void startIntentions(Intention queue) {
        mActionRunner.addQueue(new ExecutionQueue(queue));
    }

    @Override
    final public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);
        if (newState == BluetoothAdapter.STATE_CONNECTED) {
            Logging.notice("Device %s connected.", this.getClass().getSimpleName());
            mActiveConnection = gatt;
            mActiveConnection.discoverServices();
            for (SmartDeviceCallback d : mListeners) {
                d.onEvent(SmartDeviceCallback.EVENT_CONNECTED);
            }
        } else {
            for (Characteristic c : mCharacteristics) { c.clearAllCallbacks(); }
            Logging.notice("Device %s disconnected.", this.getClass().getSimpleName());
            for (SmartDeviceCallback d : mListeners) { d.onEvent(SmartDeviceCallback.EVENT_DISCONNECTED); }
            for (Characteristic characteristic : mCharacteristics) characteristic.reset();
            mActiveConnection = null;
            gatt.close();
        }
        mConnected = newState==BluetoothAdapter.STATE_CONNECTED;
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
            for (SmartDeviceCallback c : mListeners) { c.onEvent(SmartDeviceCallback.EVENT_SERVICES_DISCOVERED); }
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

    public void subscribeToUpdates(SmartDeviceCallback listener) {
        mListeners.add(listener);
    }

    public void unsubscribeToUpdates(SmartDeviceCallback listener) {
        mListeners.remove(listener);
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

    public abstract void onBeacon();
}
