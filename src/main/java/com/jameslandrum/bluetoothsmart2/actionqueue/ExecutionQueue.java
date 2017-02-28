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

import android.annotation.SuppressLint;
import com.jameslandrum.bluetoothsmart2.Logging;
import com.jameslandrum.bluetoothsmart2.SmartDevice;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Represents a queue for an action to be applied to a connected device
 */
@SuppressLint("NewApi")
public class ExecutionQueue {
    private ConcurrentLinkedQueue<Action> mPendingActions = new ConcurrentLinkedQueue<>();
    private int mWaitLimit;

    public ExecutionQueue(Intentions intention) {
        mPendingActions.addAll(intention.getActions());
        mWaitLimit = intention.getWaitLimit();
    }

    boolean completed() {
        return mPendingActions.size() == 0;
    }

    boolean step(SmartDevice mDevice) {
        Action action = mPendingActions.remove();
        int result = action.execute(mDevice, mWaitLimit);
        Logging.notice("Action %s completed with return code: %d ", action.getClass().getSimpleName(), result);
        return result == ActionResult.ERROR_OK;
    }
}
