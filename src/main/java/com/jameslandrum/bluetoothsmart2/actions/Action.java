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

import com.jameslandrum.bluetoothsmart2.SmartDevice;

/**
 * Represents an actionable item.
 */
public abstract class Action {
    public static final int RESULT_OK           = 0x01;
    public static final int RESULT_UNKNOWN      = 0x00;
    public static final int RESULT_TIMED_OUT    = 0xFF;
    public static final int RESULT_NOT_READY    = 0xFE;
    public static final int RESULT_CANCELLED    = 0xFD;

    private ResultHandler mResultHandler = (code)->code==RESULT_OK;
    private final Object mLock = new Object();
    private int mResult = RESULT_UNKNOWN;
    private boolean mCancelled = false;

    /**
     * Creates a new instanc
     * @param handler
     */
    public Action(ResultHandler handler) {
        if (handler!=null) mResultHandler = handler;
    }

    boolean handleResult(int resultCode) {
        return mResultHandler.invoke(resultCode);
    }

    void setResultHandler(ResultHandler resultHandler) {
        mResultHandler = resultHandler;
    }

    void setResult(int result) {
        mResult = result;
    }

    void hold(int i) {
        synchronized (mLock) {
            try {
                mLock.wait(i);
            } catch (InterruptedException ignored) {}
        }
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
                mResult = RESULT_TIMED_OUT;
            }
        }
    }

    void waitForFinish(int timeout) {
        synchronized (mLock) {
            try {
                if (timeout == -1) mLock.wait();
                else mLock.wait(timeout);
            } catch (InterruptedException ignored) {}
            if (mResult != RESULT_OK) setResult(RESULT_TIMED_OUT);
        }
    }

    public int getResult() {
        return mResult;
    }

    void cancel() {
        setResult(RESULT_CANCELLED);
        mCancelled = true;
        finish();
    }

    abstract int execute(SmartDevice device);

    public boolean cancelled() {
        return mCancelled;
    }
}
