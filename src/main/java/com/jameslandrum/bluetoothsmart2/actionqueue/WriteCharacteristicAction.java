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

    WriteCharacteristicAction(Characteristic characteristic, ResultHandler handler, byte[] data) {
        super(handler);
        mCharacteristic = characteristic;
        mData = data;
    }

    @Override
    public Result execute(SmartDevice device) {
        if (!device.isReady()) {
            setResult(Result.NOT_READY);
        } else {
            mCharacteristic.addCallback(mListener);

            try {
                BluetoothGattCharacteristic gattCharacteristic = mCharacteristic.getNativeCharacteristic();
                gattCharacteristic.setValue(mData);
                if (mCharacteristic.getWriteMode() != -1) gattCharacteristic.setWriteType(mCharacteristic.getWriteMode());
                device.getActiveConnection().writeCharacteristic(gattCharacteristic);
                Logging.notice("Write sent with type %d.", mCharacteristic.getWriteMode());
                waitForFinish(mCharacteristic.getTimeout());
            } catch (Exception e) {
                Logging.notice("Write error: %s", e.getMessage());
                e.printStackTrace();
                setResult(Result.UNKNOWN);
            }

            mCharacteristic.removeCallback(mListener);
        }

        return getResult();
    }

    private final CharacteristicCallback mListener = (action) -> {
        switch (action) {
            case CharacteristicCallback.EVENT_SECURITY_FAILURE:
                setResult(Result.BONDING_REQUIRED);
                finish();
                break;
            case CharacteristicCallback.EVENT_CHARACTERISTIC_WRITE_FAILURE:
                setResult(Result.FAILED);
                finish();
                break;
            case CharacteristicCallback.EVENT_CHARACTERISTIC_WRITE:
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
