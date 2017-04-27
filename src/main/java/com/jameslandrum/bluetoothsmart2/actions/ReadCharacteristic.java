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

import android.support.annotation.Nullable;
import com.jameslandrum.bluetoothsmart2.Characteristic;
import com.jameslandrum.bluetoothsmart2.CharacteristicCallback;
import com.jameslandrum.bluetoothsmart2.SmartDevice;

public final class ReadCharacteristic extends Action {
    private static final int RESULT_BONDING_REQUIRED = 0x10;
    private static final int RESULT_FAILED = 0x11;

    private Characteristic mCharacteristic;

    ReadCharacteristic(Characteristic characteristic, @Nullable ResultHandler handler) {
        super(handler);
        mCharacteristic = characteristic;
    }

    @Override
    public int execute(SmartDevice device) {
        if (!device.isReady() || !mCharacteristic.isReady()) {
            setResult(RESULT_NOT_READY);
        } else {
            mCharacteristic.addCallback(mListener);

            device.getActiveConnection().readCharacteristic(mCharacteristic.getNativeCharacteristic());
            waitForFinish(mCharacteristic.getTimeout());
            setResult(RESULT_UNKNOWN);
        }

        mCharacteristic.removeCallback(mListener);
        return getResult();
    }

    private final CharacteristicCallback mListener = (action) -> {
        switch (action) {
            case CharacteristicCallback.EVENT_SECURITY_FAILURE:
                setResult(RESULT_BONDING_REQUIRED);
                finish();
                break;
            case CharacteristicCallback.EVENT_CHARACTERISTIC_READ_FAILURE:
                setResult(RESULT_FAILED);
                finish();
                break;
            case CharacteristicCallback.EVENT_CHARACTERISTIC_READ:
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
}
