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

import com.jameslandrum.bluetoothsmart2.Logging;
import com.jameslandrum.bluetoothsmart2.SmartDevice;

import java.util.ArrayList;

@SuppressWarnings("unused")
public class ActionRunner extends Thread {
    private final Object mLock = new Object();
    private Executor mExecutor;
    private SmartDevice mDevice;

    public ActionRunner(SmartDevice parent) {
        mDevice = parent;
    }

    private class Executor extends Thread {
        private ArrayList<ExecutionQueue> mQueues = new ArrayList<>();

        @Override
        public void run() {
            Logging.notice("ActionRunner thread has started.");
            while (!interrupted()) {
                try {
                    if (!mQueues.isEmpty()) {
                        ExecutionQueue mActiveQueue = mQueues.remove(0);
                        while (!mActiveQueue.completed()) {
                            if (!mActiveQueue.step(mDevice)) {
                                break;
                            }
                        }
                    } else {
                        synchronized (mLock) {
                            mLock.wait();
                        }
                    }
                } catch (InterruptedException ignored) {}
            }
            Logging.notice("ActionRunner thread has terminated.");
            mExecutor = null;
        }

        void insertQueue(ExecutionQueue queue) {
            mQueues.add(queue);
            synchronized (mLock) {
                mLock.notify();
            }
        }
    }

    public void addQueue(ExecutionQueue queue)
    {
        if (mExecutor == null) {
            mExecutor = new Executor();
            mExecutor.start();
            mExecutor.insertQueue(queue);
        } else {
            mExecutor.insertQueue(queue);
        }
    }
}
