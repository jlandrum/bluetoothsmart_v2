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
import com.jameslandrum.bluetoothsmart2.DeviceUpdateListener;
import com.jameslandrum.bluetoothsmart2.SmartDevice;

final class SetNotificationAction extends Action {
    private final Characteristic mCharacteristic;
    private final NotificationCallback mNotifyCallback;
    private final int mTimeout;
    private final int mDescriptorId;
    private final boolean mEnable;

    SetNotificationAction(Characteristic characteristic, int timeout, int descriptorId, boolean enable, ResultHandler handler, NotificationCallback subscription) {
        super(handler);
        mCharacteristic = characteristic;
        mNotifyCallback = subscription;
        mTimeout = timeout;
        mDescriptorId = descriptorId;
        mEnable = enable;
    }

    @Override
    public Result execute(SmartDevice device) {
        BluetoothGattCharacteristic characteristic = mCharacteristic.getNativeCharacteristic();
        if (!device.isReady() || !mCharacteristic.isReady()) {
            setResult(Result.NOT_READY);
        } else {
            device.subscribeToUpdates(mListener);
            BluetoothGattDescriptor descriptor = characteristic.getDescriptors().get(mDescriptorId);
            descriptor.setValue( mEnable ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE );
            device.getActiveConnection().writeDescriptor(descriptor);
            waitForFinish(mTimeout);
            device.unsubscribeToUpdates(mListener);

            device.getActiveConnection().setCharacteristicNotification(characteristic, mEnable);
            if (mEnable) {
                mCharacteristic.addNotificationListener(mNotifyCallback);
            } else {
                mCharacteristic.removeNotificationListener(mNotifyCallback);
            }

        }

        Result result = getResult();
        if (result == Result.OK && characteristic != null) {
        } else {
            setResult(Result.FAILED);
        }

        return result;
    }

    private final DeviceUpdateListener mListener = (action) -> {
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
    };

    @Override
    public boolean purge() {
        return true;
    }
}
