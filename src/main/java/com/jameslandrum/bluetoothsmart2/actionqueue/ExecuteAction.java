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

import com.jameslandrum.bluetoothsmart2.SmartDevice;

/**
 * Runs a method, always succeeds.
 */
public class ExecuteAction extends Action {
    private Execute mExecutor;

    ExecuteAction(Execute insurance) {
        mExecutor = insurance;
    }

    @Override
    public int execute(SmartDevice device, int maxWait) {
        mExecutor.execute();
        return ActionResult.ERROR_OK;
    }

    @Override
    public boolean handleError(int mError) {
        return false;
    }

    @Override
    public boolean purge() {
        return true;
    }

    public interface Execute {
        void execute();
    }
}
