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
import com.jameslandrum.bluetoothsmart2.*;

final class WriteCharacteristicAction extends Action {
    private final Characteristic mCharacteristic;
    private final byte[] mData;
    private final int mWriteMode;
    private final int mTimeout;

    WriteCharacteristicAction(Characteristic characteristic, int timeout, ResultHandler handler, int writeMode, byte[] data) {
        super(handler);
        mCharacteristic = characteristic;
        mData = data;
        mWriteMode = writeMode;
        mTimeout = timeout;
    }

    @Override
    public Result execute(SmartDevice device) {
        if (!device.isReady()) {
            setResult(Result.NOT_READY);
        } else {
            device.subscribeToUpdates(mListener);

            try {
                BluetoothGattCharacteristic gattCharacteristic = mCharacteristic.getNativeCharacteristic();
                gattCharacteristic.setValue(mData);
                if (mWriteMode != -1) gattCharacteristic.setWriteType(mWriteMode);
                device.getActiveConnection().writeCharacteristic(gattCharacteristic);
                Logging.notice("Write sent with type %d.", mWriteMode);
                waitForFinish(mTimeout);
            } catch (Exception e) {
                Logging.notice("Write error: %s", e.getMessage());
                e.printStackTrace();
                setResult(Result.UNKNOWN);
            }

            device.unsubscribeToUpdates(mListener);
        }

        return getResult();
    }

    private final DeviceUpdateListener mListener = (action) -> {
        switch (action) {
            case SmartDevice.EVENT_SECURITY_FAILURE:
                setResult(Result.BONDING_REQUIRED);
                finish();
                break;
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
