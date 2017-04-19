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

import android.app.Service;
import android.bluetooth.*;
import android.content.Context;
import com.annimon.stream.Stream;
import com.jameslandrum.bluetoothsmart2.actionqueue.ActionRunner;
import com.jameslandrum.bluetoothsmart2.actionqueue.ExecutionQueue;
import com.jameslandrum.bluetoothsmart2.actionqueue.Intention;
import com.jameslandrum.bluetoothsmart2.actionqueue.NotificationCallback;

import java.util.Collections;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

@SuppressWarnings("unused")
public abstract class SmartDevice extends BluetoothGattCallback {
    public static final int EVENT_CONNECTED = 0x01;
    public static final int EVENT_DISCONNECTED = 0x02;
    public static final int EVENT_SERVICES_DISCOVERED = 0x03;
    public static final int EVENT_CONNECTION_ERROR = 0x04;
    public static final int EVENT_NEW_BEACON = 0x05;
    public static final int EVENT_NEW_ADVERTISEMENT = 0x06;
    public static final int EVENT_SECURITY_FAILURE = 0x07;

    public static final int EVENT_CHARACTERISTIC_WRITTEN = 0x10;
    public static final int EVENT_CHARACTERISTIC_WRITE_FAILURE = 0x9010;
    public static final int EVENT_CHARACTERISTIC_READ = 0x11;
    public static final int EVENT_CHARACTERISTIC_READ_FAILURE = 0x9011;
    public static final int EVENT_DESCRIPTOR_WRITTEN = 0x10;
    public static final int EVENT_DESCRIPTOR_WRITE_FAILURE = 0x9010;

    private BluetoothDevice mDevice;
    private ActionRunner mActionRunner = new ActionRunner(this);

    private boolean mIsRegistered;

    private HashSet<Characteristic> mCharacteristics = new HashSet<>();
    private ConcurrentLinkedQueue<DeviceUpdateListener> mListeners = new ConcurrentLinkedQueue<>();
    private BluetoothGatt mActiveConnection;
    private boolean mServicesDiscovered;
    private byte[] mAdvertisement = new byte[62];
    private int mRssi;
    private long mLastSeen;
    private boolean mConnected;

    public void init(BluetoothDevice device) {
        mDevice = device;
    }

    public void registerCharacteristics(Characteristic ... characteristics) {
        if (isConnected()) throw new RuntimeException("Cannot modify characteristics of connected device.");
        mCharacteristics.clear();
        Collections.addAll(mCharacteristics, characteristics);
    }

    public BluetoothGatt getActiveConnection() {
        return mActiveConnection;
    }

    public void connect(Context context) {
        mDevice.connectGatt(context, false, this);
    }

    public void disconnect() {
        if (mActiveConnection!=null) mActiveConnection.disconnect();
    }

    protected void startIntentions(Intention queue) {
        mActionRunner.addQueue(new ExecutionQueue(queue));
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);
        if (newState == BluetoothAdapter.STATE_CONNECTED) {
            Logging.notice("Device %s connected.", this.getClass().getSimpleName());
            mActiveConnection = gatt;
            mActiveConnection.discoverServices();
            Stream.of(mListeners).forEach(l->l.onDeviceUpdateEvent(EVENT_CONNECTED));
        } else {
            Stream.of(mCharacteristics).forEach(Characteristic::clearAllCallbacks);
            Logging.notice("Device %s disconnected.", this.getClass().getSimpleName());
            Stream.of(mListeners).forEach(l->l.onDeviceUpdateEvent(EVENT_DISCONNECTED));
            for (Characteristic characteristic : mCharacteristics) characteristic.reset();
            mActiveConnection = null;
            gatt.close();
        }
        mConnected = newState==BluetoothAdapter.STATE_CONNECTED;
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
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
            Stream.of(mListeners).forEach(l->l.onDeviceUpdateEvent(EVENT_SERVICES_DISCOVERED));
        } catch (Exception e) {
            e.printStackTrace();
            disconnect();
        }
    }

    @Override
    @SuppressWarnings("Duplicates")
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        Logging.notice("Write result: %d", status);
        switch (status) {
            case BluetoothGatt.GATT_SUCCESS:
                Stream.of(mListeners).forEach(l->l.onDeviceUpdateEvent(EVENT_CHARACTERISTIC_WRITTEN));
                return;
            case BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION:
            case BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION:
                Stream.of(mListeners).forEach(l->l.onDeviceUpdateEvent(EVENT_SECURITY_FAILURE));
                return;
            default:
                Stream.of(mListeners).forEach(l->l.onDeviceUpdateEvent(EVENT_CHARACTERISTIC_WRITE_FAILURE));
        }
    }

    @Override
    @SuppressWarnings("Duplicates")
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        switch (status) {
            case BluetoothGatt.GATT_SUCCESS:
                Stream.of(mListeners).forEach(l->l.onDeviceUpdateEvent(EVENT_CHARACTERISTIC_READ));
                return;
            case BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION:
            case BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION:
                Stream.of(mListeners).forEach(l->l.onDeviceUpdateEvent(EVENT_SECURITY_FAILURE));
                return;
            default:
                Stream.of(mListeners).forEach(l->l.onDeviceUpdateEvent(EVENT_CHARACTERISTIC_READ_FAILURE));
        }
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        switch (status) {
            case BluetoothGatt.GATT_SUCCESS:
                Stream.of(mListeners).forEach(l->l.onDeviceUpdateEvent(EVENT_DESCRIPTOR_WRITTEN));
                return;
            default:
                Stream.of(mListeners).forEach(l->l.onDeviceUpdateEvent(EVENT_DESCRIPTOR_WRITE_FAILURE));
        }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        Stream.of(mCharacteristics)
                .filter(n->n.equalsCharacteristic(characteristic))
                .forEach(Characteristic::notifyUpdate);
    }

    public boolean isReady() {
        return mServicesDiscovered && mActiveConnection != null;
    }

    public void subscribeToUpdates(DeviceUpdateListener listener) {
        mListeners.add(listener);
    }

    public void unsubscribeToUpdates(DeviceUpdateListener listener) {
        mListeners.remove(listener);
    }

    public String getAddress() {
        return mDevice.getAddress();
    }

    public void notifyEvent(int event) {

    }

    public BluetoothDevice getDevice() {
        return mDevice;
    }

    public void newAdvertisement(byte[] data, int rssi) {
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
}
