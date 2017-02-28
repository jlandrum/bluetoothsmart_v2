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
import com.jameslandrum.bluetoothsmart2.SmartDevice;

@SuppressWarnings({"unused", "WeakerAccess"})
class ConnectAction implements Action {
    public static final int ERROR_CONNECTION_TIMEOUT = -16;

    private final Object mLock = new Object();
    private int mError = ActionResult.ERROR_UNKNOWN;
    private Context mContext;

    public ConnectAction(Context context) {
        mContext = context;
    }

    @Override
    public int execute(SmartDevice device, int maxWait) {
        if (device.isConnected()) return ActionResult.ERROR_OK;
        device.subscribeToUpdates((event)-> {
            switch (event) {
                case SmartDevice.EVENT_DISCONNECTED:
                case SmartDevice.EVENT_CONNECTION_ERROR:
                    mError = ConnectAction.ERROR_CONNECTION_TIMEOUT;
                    synchronized (mLock) {
                        mLock.notify();
                    }
                    break;
                case SmartDevice.EVENT_SERVICES_DISCOVERED:
                    mError = ActionResult.ERROR_OK;
                    synchronized (mLock) {
                        mLock.notify();
                    }
                    break;
                default:
                    break;
            }
        });

        device.connect(mContext);

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
    public boolean handleError(int mError) {
        return false;
    }
}
