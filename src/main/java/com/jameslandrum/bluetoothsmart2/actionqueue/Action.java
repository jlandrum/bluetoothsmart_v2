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

public abstract class Action {
    private ResultHandler mResultHandler = (code)->code == Result.OK;
    private int mTimeoutTime;
    private final Object mLock = new Object();
    private Result mResult = Result.UNKNOWN;

    public Action(ResultHandler handler, int timeoutTime) {
        if (handler!=null) mResultHandler = handler;
        mTimeoutTime = timeoutTime;
    }

    boolean handleResult(Result resultCode) {
        return mResultHandler.invoke(resultCode);
    }
    int getTimeout() { return mTimeoutTime; }

    void setResult(Result result) {
        mResult = result;
    }

    void finish() {
        synchronized (mLock) {
            mLock.notify();
        }
    }

    void waitForFinish() {
        synchronized (mLock) {
            try {
                mLock.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
                mResult = Result.TIMED_OUT;
            }
        }
    }

    void waitForFinish(int timeout) {
        synchronized (mLock) {
            try {
                if (timeout == -1) mLock.wait();
                else mLock.wait(timeout);
            } catch (InterruptedException e) {
                e.printStackTrace();
                mResult = Result.TIMED_OUT;
            }
        }
    }

    public Result getResult() {
        return mResult;
    }

    abstract Result execute(SmartDevice device, int maxWait);
    abstract boolean purge();

    public enum Result {
        /** An unusual error occured **/
        UNKNOWN,
        /** The action timed out before it could be completed. **/
        TIMED_OUT,
        /** The device is not in a state applicable to the action provided **/
        NOT_READY,
        /** The action completed successfully **/
        OK,
        /** The action failed to finish successfully **/
        FAILED,
        /** The action failed due to the device requiring bonding to access the given resource **/
        BONDING_REQUIRED,
    }
}
