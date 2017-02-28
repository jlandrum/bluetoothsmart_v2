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
import com.jameslandrum.bluetoothsmart2.Characteristic;
import com.jameslandrum.bluetoothsmart2.SmartDevice;

class WriteCharacteristicAction implements BluetoothAction {
    private final int mCharId;
    private final byte[] mData;
    private final int mWriteMode;
    private ErrorHandler mErrorHandler;

    WriteCharacteristicAction(int characteristicId, ErrorHandler handler, int writeMode, byte[] data) {
        mCharId = characteristicId;
        mErrorHandler = handler;
        mData = data;
        mWriteMode = writeMode;
    }

    @Override
    public int execute(SmartDevice device, int maxWait) {
        if (!device.isConnected()) return ActionResult.ERROR_NOT_READY;
        Characteristic characteristic = device.getCharacteristic(mCharId);
        BluetoothGattCharacteristic gattCharacteristic = characteristic.getCharacteristic();
        gattCharacteristic.setValue(mData);
        gattCharacteristic.setWriteType(mWriteMode);
        return device.getActiveConnection().writeCharacteristic(gattCharacteristic)?ActionResult.ERROR_OK:ActionResult.ERROR_UNKNOWN;
    }

    @Override
    public boolean handleError(int error) {
        return mErrorHandler != null && mErrorHandler.error(error);
    }
}
