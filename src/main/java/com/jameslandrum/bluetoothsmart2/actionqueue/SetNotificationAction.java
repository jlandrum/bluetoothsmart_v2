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

package com.jameslandrum.bluetoothsmart2.actionqueue;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import com.jameslandrum.bluetoothsmart2.Characteristic;
import com.jameslandrum.bluetoothsmart2.Logging;
import com.jameslandrum.bluetoothsmart2.SmartDevice;

final class SetNotificationAction extends Action {
    private final int mCharId;
    private final NotificationCallback mNotifyCallback;
    private final int mTimeout;
    private final int mDescriptorId;
    private final boolean mEnable;

    SetNotificationAction(int characteristicId, int timeout, int descriptorId, boolean enable, ResultHandler handler, NotificationCallback subscription) {
        super(handler);
        mCharId = characteristicId;
        mNotifyCallback = subscription;
        mTimeout = timeout;
        mDescriptorId = descriptorId;
        mEnable = enable;
    }

    @Override
    public Result execute(SmartDevice device) {
        Characteristic characteristic = null;

        if (!device.isConnected()) {
            setResult(Result.NOT_READY);
        } else {
            device.subscribeToUpdates(this::onDeviceUpdateEvent);
            try {
                characteristic = device.getCharacteristic(mCharId);
                BluetoothGattCharacteristic gattCharacteristic = characteristic.getNativeCharacteristic();
                BluetoothGattDescriptor descriptor = gattCharacteristic.getDescriptors().get(mDescriptorId);
                descriptor.setValue( mEnable ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE );
                device.getActiveConnection().writeDescriptor(descriptor);
                waitForFinish(mTimeout);
            } catch (Exception e) {
                e.printStackTrace();
                setResult(Result.UNKNOWN);
            }

            device.unsubscribeToUpdates(this::onDeviceUpdateEvent);
        }

        Result result = getResult();
        if (result == Result.OK && characteristic != null) {
            device.getActiveConnection().setCharacteristicNotification(characteristic.getNativeCharacteristic(), mEnable);
            if (mEnable) {
                device.addNotificationListener(mCharId, mNotifyCallback);
            } else {
                device.removeNotificationListener(mCharId, mNotifyCallback);
            }
        } else {
            setResult(Result.FAILED);
        }

        return result;
    }

    private void onDeviceUpdateEvent(int action) {
        switch (action) {
            case SmartDevice.EVENT_CHARACTERISTIC_WRITE_FAILURE:
            case SmartDevice.EVENT_CONNECTION_ERROR:
                setResult(Result.FAILED);
                finish();
                break;
            case SmartDevice.EVENT_CHARACTERISTIC_WRITTEN:
                setResult(Result.OK);
                finish();
                break;
            default:
                break;
        }
    }

    @Override
    public boolean purge() {
        return true;
    }
}
