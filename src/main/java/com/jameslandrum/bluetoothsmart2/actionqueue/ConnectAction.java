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

import android.content.Context;
import android.support.annotation.Nullable;
import com.jameslandrum.bluetoothsmart2.OnConnectionStateListener;
import com.jameslandrum.bluetoothsmart2.SmartDeviceCallback;
import com.jameslandrum.bluetoothsmart2.SmartDevice;

final class ConnectAction extends Action {
    private Context mContext;

    ConnectAction(Context context, @Nullable ResultHandler handler) {
        super(handler);
        mContext = context;
    }

    @Override
    public Result execute(SmartDevice device) {
        if (device.isConnected()) {
            setResult(Result.OK);
        } else {
            device.subscribeToUpdates(mListener);
            device.connect(mContext);
            waitForFinish(60000);
        }

        device.unsubscribeToUpdates(mListener);
        return getResult();
    }

    @Override
    public boolean purge() {
        return true;
    }

    private final OnConnectionStateListener mListener = new OnConnectionStateListener() {
        @Override
        public void onConnected(SmartDevice device) {

        }

        @Override
        public void onDisconnected(SmartDevice device) {
            setResult(Result.FAILED);
            finish();
        }

        @Override
        public void onServicesDiscovered(SmartDevice device) {
            setResult(Result.OK);
            finish();
        }
    };
}
