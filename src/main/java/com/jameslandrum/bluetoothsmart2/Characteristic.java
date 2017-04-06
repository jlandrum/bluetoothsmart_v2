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
import com.annimon.stream.Stream;
import com.jameslandrum.bluetoothsmart2.actionqueue.NotificationCallback;

import java.util.concurrent.ConcurrentLinkedQueue;

public class Characteristic {
    private BluetoothGattCharacteristic mCharacteristic;
    private int mIdentifier;
    private final ConcurrentLinkedQueue<NotificationCallback> mChangeCallbacks = new ConcurrentLinkedQueue<>();

    Characteristic(BluetoothGattCharacteristic nativeChar, int identifier) {
        mCharacteristic = nativeChar;
        mIdentifier = identifier;
    }

    public BluetoothGattCharacteristic getNativeCharacteristic() {
        return mCharacteristic;
    }

    public byte[] getValue() {
        if (mCharacteristic != null) return mCharacteristic.getValue();
        return null;
    }

    public int getId() {
        return mIdentifier;
    }

    void addCallback(NotificationCallback callback) {
        if (!mChangeCallbacks.contains(callback)) mChangeCallbacks.add(callback);
    }

    void removeCallback(NotificationCallback callback) {
        mChangeCallbacks.remove(callback);
    }

    void notifyUpdate() {
        Stream.of(mChangeCallbacks).forEach(NotificationCallback::onCharacteristicChange);
    }

    void clearAllCallbacks() {
        mChangeCallbacks.clear();
    }
}
