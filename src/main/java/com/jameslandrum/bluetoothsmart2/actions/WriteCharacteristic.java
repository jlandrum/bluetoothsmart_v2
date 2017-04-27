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
import com.jameslandrum.bluetoothsmart2.*;

import java.math.BigInteger;

public final class WriteCharacteristic extends Action {
    private static final int RESULT_BONDING_REQUIRED = 0x10;
    private static final int RESULT_FAILED = 0x11;

    private final Characteristic mCharacteristic;
    private byte[] mData;

    public WriteCharacteristic(Characteristic characteristic) {
        this(characteristic, null, new byte[]{});
    }

    public WriteCharacteristic(Characteristic characteristic, byte[] data) {
        this(characteristic, null, data);
    }

    public WriteCharacteristic(Characteristic characteristic, ResultHandler handler, byte[] data) {
        super(handler);
        mCharacteristic = characteristic;
        mData = data;
    }

    @Override
    public int execute(SmartDevice device) {
        if (!device.isReady()) {
            setResult(RESULT_NOT_READY);
        } else {
            mCharacteristic.addCallback(mListener);

            try {
                BluetoothGattCharacteristic gattCharacteristic = mCharacteristic.getNativeCharacteristic();
                gattCharacteristic.setValue(mData);
                if (mCharacteristic.getWriteMode() != -1) gattCharacteristic.setWriteType(mCharacteristic.getWriteMode());
                device.getActiveConnection().writeCharacteristic(gattCharacteristic);
                Logging.notice("Write sent to %s, %s, type %d.",
                        mCharacteristic.getNativeCharacteristic().getUuid(),
                        new BigInteger(1, mData).toString(16),
                        mCharacteristic.getWriteMode());
                waitForFinish(mCharacteristic.getTimeout());
            } catch (Exception e) {
                Logging.notice("Write error: %s", e.getMessage());
                e.printStackTrace();
                setResult(RESULT_UNKNOWN);
            }

            mCharacteristic.removeCallback(mListener);
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

    @Override
    public boolean purge() {
        return true;
    }

    public void setValue(byte[] value) {
        mData = value;
    }
}
