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
import android.support.annotation.Nullable;
import com.jameslandrum.bluetoothsmart2.Characteristic;
import com.jameslandrum.bluetoothsmart2.DeviceUpdateListener;
import com.jameslandrum.bluetoothsmart2.SmartDevice;

final class ReadCharacteristicAction extends Action {
    private final int mCharId;
    private final int mWait;

    ReadCharacteristicAction(int characteristicId, int wait, @Nullable ResultHandler handler) {
        super(handler);
        mCharId = characteristicId;
        mWait = wait;
    }

    @Override
    public Result execute(SmartDevice device) {
        if (!device.isReady()) {
            setResult(Result.NOT_READY);
        } else {
            device.subscribeToUpdates(mListener);

            try {
                Characteristic characteristic = device.getCharacteristic(mCharId);
                BluetoothGattCharacteristic gattCharacteristic = characteristic.getNativeCharacteristic();
                device.getActiveConnection().readCharacteristic(gattCharacteristic);
                waitForFinish(mWait);
            } catch (Exception e) {
                setResult(Result.UNKNOWN);
            }
        }

        device.unsubscribeToUpdates(mListener);
        return getResult();
    }

    private final DeviceUpdateListener mListener = (action) -> {
        switch (action) {
            case SmartDevice.EVENT_SECURITY_FAILURE:
                setResult(Result.BONDING_REQUIRED);
                finish();
                break;
            case SmartDevice.EVENT_CHARACTERISTIC_READ_FAILURE:
            case SmartDevice.EVENT_CONNECTION_ERROR:
                setResult(Result.FAILED);
                finish();
                break;
            case SmartDevice.EVENT_CHARACTERISTIC_READ:
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
