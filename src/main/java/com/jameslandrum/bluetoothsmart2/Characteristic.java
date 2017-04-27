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

import android.bluetooth.BluetoothGattCharacteristic;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Characteristic {
    private BluetoothGattCharacteristic mCharacteristic;
    private LinkedBlockingQueue<NotificationCallback> mNotifications = new LinkedBlockingQueue<>();
    private LinkedBlockingQueue<CharacteristicCallback> mCallbacks = new LinkedBlockingQueue<>();

    private UUID mService;
    private UUID mHandle;
    private int mTimeout = 5000;
    private int mWriteMode = -1;

    private Characteristic(UUID2 service, UUID2 handle, int timeout) {
        mService = service.getUuid();
        mHandle = handle.getUuid();
        mTimeout = timeout;
    }

    public Characteristic(String service, String handle) {
        this(service,handle,5000);
    }

    public Characteristic(String service, String handle, int timeout) {
        this(new UUID2(service), new UUID2(service, handle), timeout);
    }

    void reset() {
        mCharacteristic = null;
    }

    public boolean isReady() {
        return mCharacteristic != null;
    }

    UUID getServiceUuid() {
        return mService;
    }

    UUID getHandleUuid() {
        return mHandle;
    }

    void prepare(@Nullable BluetoothGattCharacteristic characteristic) {
        if (characteristic == null) return;
        mCharacteristic = characteristic;
    }

    void clearAllCallbacks() {
        mNotifications.clear();
    }

    boolean equalsCharacteristic(BluetoothGattCharacteristic characteristic) {
        return mCharacteristic == characteristic;
    }

    void notifyUpdate() {
    }

    public BluetoothGattCharacteristic getNativeCharacteristic() {
        return mCharacteristic;
    }

    public void addNotificationListener(NotificationCallback notificationCallback) {
        if (!mNotifications.contains(notificationCallback)) mNotifications.add(notificationCallback);
    }

    public void removeNotificationListener(NotificationCallback notificationCallback) {
        mNotifications.remove(notificationCallback);
    }

    public void addCallback(CharacteristicCallback listener) {
        if (!mCallbacks.contains(listener)) mCallbacks.add(listener);
    }

    public void removeCallback(CharacteristicCallback listener) {
        mCallbacks.remove(listener);
    }

    void callEvent(int event) {
        for (CharacteristicCallback c : mCallbacks) { c.onEvent(event); }
    }

    public int getTimeout() {
        return mTimeout;
    }

    public int getWriteMode() {
        return mWriteMode;
    }

    public byte[] getValue() {
        if (getNativeCharacteristic() == null) return null;
        return getNativeCharacteristic().getValue();
    }

    @Override
    public int hashCode() {
        return mHandle.hashCode();
    }
}
