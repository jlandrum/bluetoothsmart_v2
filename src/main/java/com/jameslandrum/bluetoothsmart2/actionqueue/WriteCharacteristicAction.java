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

class WriteCharacteristicAction extends BluetoothAction {
    private final int mCharId;
    private final byte[] mData;
    private final int mWriteMode;
    private ErrorHandler mErrorHandler;
    private final Object mLock = new Object();
    private int mError = ActionResult.ERROR_UNKNOWN;

    WriteCharacteristicAction(int characteristicId, ErrorHandler handler, int writeMode, byte[] data) {
        mCharId = characteristicId;
        mErrorHandler = handler;
        mData = data;
        mWriteMode = writeMode;
    }

    @Override
    public int execute(SmartDevice device, int maxWait) {
        if (!device.isConnected()) return ActionResult.ERROR_NOT_READY;

        device.subscribeToUpdates((event)-> {
            switch (event) {
                case SmartDevice.EVENT_CONNECTION_ERROR:
                    mError = ConnectAction.ERROR_CONNECTION_TIMEOUT;
                    synchronized (mLock) {
                        mLock.notify();
                    }
                    break;
                case SmartDevice.EVENT_CHARACTERISTIC_WRITTEN:
                    mError = ActionResult.ERROR_OK;
                    synchronized (mLock) {
                        mLock.notify();
                    }
                    break;
                default:
                    break;
            }
        });

        try {
            Characteristic characteristic = device.getCharacteristic(mCharId);
            BluetoothGattCharacteristic gattCharacteristic = characteristic.getCharacteristic();
            gattCharacteristic.setValue(mData);
            gattCharacteristic.setWriteType(mWriteMode);
            device.getActiveConnection().writeCharacteristic(gattCharacteristic);
        } catch (Exception e) {
            return ActionResult.ERROR_UNKNOWN;
        }

        synchronized (mLock) {
            try {
                mLock.wait(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                mError = ActionResult.ERROR_TIMED_OUT;
            }
        }

        return mError;
    }

    @Override
    public boolean purge() {
        return true;
    }

    @Override
    public boolean handleError(int error) {
        return mErrorHandler != null && mErrorHandler.error(error);
    }
}
