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
import com.jameslandrum.bluetoothsmart2.OnConnectionStateListener;
import com.jameslandrum.bluetoothsmart2.SmartDevice;

/**
 * Explicitly disconnects from the device. This is ideally used when letting the built-in state management
 * will result in an unnecessary lingering connection.
 */
public final class Disconnect extends Action {
    public Disconnect(@Nullable ResultHandler handler) {
        super(handler);
    }

    @Override
    public int execute(SmartDevice device) {
        if (!device.isReady() || !device.isConnected()) {
            setResult(RESULT_OK);
        } else {
            device.subscribeToUpdates(mListener);
            device.disconnect();
            waitForFinish();
        }

        device.unsubscribeToUpdates(mListener);
        return getResult();
    }

    private final OnConnectionStateListener mListener = new OnConnectionStateListener() {
        @Override
        public void onConnected(SmartDevice device) {}

        @Override
        public void onDisconnected(SmartDevice device) {
            setResult(RESULT_OK);
            finish();
        }

        @Override
        public void onServicesDiscovered(SmartDevice device) {

        }
    };
}
