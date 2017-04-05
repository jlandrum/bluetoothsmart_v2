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
import com.jameslandrum.bluetoothsmart2.SmartDevice;

final class ConnectAction extends Action {
    private Context mContext;

    ConnectAction(Context context, @Nullable ResultHandler handler) {
        super(handler,-1);
        mContext = context;
    }

    @Override
    public Result execute(SmartDevice device, int maxWait) {
        if (device.isConnected()) {
            setResult(Result.OK);
        } else {
            device.subscribeToUpdates((event) -> {
                switch (event) {
                    case SmartDevice.EVENT_DISCONNECTED:
                    case SmartDevice.EVENT_CONNECTION_ERROR:
                        setResult(Result.FAILED);
                        finish();
                        break;
                    case SmartDevice.EVENT_SERVICES_DISCOVERED:
                        setResult(Result.OK);
                        finish();
                        break;
                    default:
                        break;
                }
            });
            device.connect(mContext);
            waitForFinish();
        }

        return getResult();
    }

    @Override
    public boolean purge() {
        return true;
    }
}
