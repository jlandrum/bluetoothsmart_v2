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
import com.jameslandrum.bluetoothsmart2.SmartDevice;

/**
 * Executes code in an action pipeline.
 * The executed action may throw an error, and if it does, should handle the result.
 * This can be used to take action between characteristic reads and writes.
 */
final class ExecuteAction extends Action {
    private Execute mExecutor;

    ExecuteAction(Execute executor, @Nullable ResultHandler handler) {
        super(handler);
        mExecutor = executor;
    }

    @Override
    public int execute(SmartDevice ignored) {
        return mExecutor.execute();
    }

    public interface Execute {
        int execute();
    }
}
