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

import android.support.annotation.Nullable;
import com.jameslandrum.bluetoothsmart2.SmartDevice;

/**
 * Executes code in an intent pipeline.
 * The executed action may throw an error, and if it does, should handle the result.
 */
final class ExecuteAction extends Action {
    private Execute mExecutor;

    ExecuteAction(Execute executor, @Nullable ResultHandler handler) {
        super(handler,-1);
        mExecutor = executor;
    }

    @Override
    public Result execute(SmartDevice device, int maxWait) {
        return mExecutor.execute();
    }

    @Override
    public boolean purge() {
        return true;
    }

    public interface Execute {
        Result execute();
    }
}
