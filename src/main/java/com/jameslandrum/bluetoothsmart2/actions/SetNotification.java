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

package com.jameslandrum.bluetoothsmart2.actions;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import com.jameslandrum.bluetoothsmart2.*;

public final class SetNotification extends Action {
    private static final int RESULT_BONDING_REQUIRED = 0x10;
    private static final int RESULT_FAILED = 0x11;

    private final Characteristic mCharacteristic;
    private final NotificationCallback mNotifyCallback;
    private final int mDescriptorId;
    private final boolean mEnable;

    SetNotification(Characteristic characteristic, int descriptorId, boolean enable, ResultHandler handler, NotificationCallback subscription) {
        super(handler);
        mCharacteristic = characteristic;
        mNotifyCallback = subscription;
        mDescriptorId = descriptorId;
        mEnable = enable;
    }

    @Override
    public int execute(SmartDevice device) {
        BluetoothGattCharacteristic characteristic = mCharacteristic.getNativeCharacteristic();
        if (!device.isReady() || !mCharacteristic.isReady()) {
            setResult(RESULT_NOT_READY);
        } else {
            mCharacteristic.addCallback(mListener);
            BluetoothGattDescriptor descriptor = characteristic.getDescriptors().get(mDescriptorId);
            descriptor.setValue( mEnable ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE );
            device.getActiveConnection().writeDescriptor(descriptor);
            waitForFinish(mCharacteristic.getTimeout());
            mCharacteristic.addCallback(mListener);

            device.getActiveConnection().setCharacteristicNotification(characteristic, mEnable);
            if (mEnable) {
                mCharacteristic.addNotificationListener(mNotifyCallback);
            } else {
                mCharacteristic.removeNotificationListener(mNotifyCallback);
            }

        }

        return getResult();
    }

    private final CharacteristicCallback mListener = (action) -> {
        switch (action) {
            case CharacteristicCallback.EVENT_SECURITY_FAILURE:
                setResult(RESULT_BONDING_REQUIRED);
                finish();
                break;
            case CharacteristicCallback.EVENT_CHARACTERISTIC_WRITE_FAILURE:
                setResult(RESULT_FAILED);
                finish();
                break;
            case CharacteristicCallback.EVENT_CHARACTERISTIC_WRITE:
                setResult(RESULT_OK);
                finish();
                break;
            default:
                break;
        }
    };
}
