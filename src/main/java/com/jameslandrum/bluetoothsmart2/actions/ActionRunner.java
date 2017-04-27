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

import android.content.Context;
import com.jameslandrum.bluetoothsmart2.Logging;
import com.jameslandrum.bluetoothsmart2.OnConnectionStateListener;
import com.jameslandrum.bluetoothsmart2.OnDeviceUpdateListener;
import com.jameslandrum.bluetoothsmart2.SmartDevice;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class ActionRunner extends Thread implements OnConnectionStateListener {
    private final Object mLock = new Object();
    private Executor mExecutor;
    private SmartDevice mDevice;
    private ConcurrentLinkedQueue<Action> mActions = new ConcurrentLinkedQueue<>();


    public ActionRunner(SmartDevice parent) {
        mDevice = parent;
        mDevice.subscribeToUpdates(this);
    }

    private class Executor extends Thread {
        @Override
        public void run() {
            Logging.notice("ActionRunner thread has started.");
            while (!interrupted() && mDevice.isReady()) {
                try {
                    if (!mActions.isEmpty()) {
                        Logging.notice("ActionRunner action started.");
                        Action mActiveAction = mActions.remove();
                        if (!mActiveAction.handleResult(mActiveAction.execute(mDevice))) {
                            mActions.clear();
                        }
                    } else {
                        Logging.notice("ActionRunner queue empty.");
                        synchronized (mLock) {
                            mLock.wait(mDevice.getConnectionTimeout());
                        }
                        if (mActions.isEmpty()) interrupt();
                    }
                } catch (InterruptedException ignored) {
                }
            }
            Logging.notice("ActionRunner thread has terminated.");
            mDevice.disconnect();
            mActions.clear();
            mExecutor = null;
        }
    }
    @Override
    public void onConnected(SmartDevice device) {
    }

    @Override
    public void onDisconnected(SmartDevice device) {
        if (mExecutor != null) {
            mExecutor.interrupt();
            synchronized (mLock) {
                mLock.notify();
            }
        }
    }

    @Override
    public void onServicesDiscovered(SmartDevice device) {
        if (mExecutor != null) {
            mExecutor.start();
        }
    }

    public void addAction(Action action, Context context)
    {
        mActions.add(action);

        if (mExecutor == null) {
            mExecutor = new Executor();
            if (mDevice.isConnected()) mExecutor.start();
            else mDevice.connect(context);
        } else {
            synchronized (mLock) {
                mLock.notify();
            }
        }
    }
}
