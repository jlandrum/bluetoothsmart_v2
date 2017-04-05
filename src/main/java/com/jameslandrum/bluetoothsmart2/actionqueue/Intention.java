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

import android.bluetooth.BluetoothGattCharacteristic;
import android.support.annotation.Nullable;
import com.jameslandrum.bluetoothsmart2.SmartDeviceManager;
import com.jameslandrum.bluetoothsmart2.annotations.Sequential;

import java.util.ArrayList;

/**
 * Intention are descriptors for the series of actions that are to be taken on a Bluetooth device.
 * These are device agnostic, meaning you could define a series of intents such as to read the current value
 * from a Bluetooth Sig defined service, and issue that command on any device that offers those defined services,
 * even if the two devices are manufactured by separate entities.
 */
public final class Intention {
    private int mWaitLimit = 300;                               // How long an action may take before being considered "Failed"
    private ArrayList<Action> mActions = new ArrayList<>();     // The list of actions to be taken.

    /**
     * Builder class for creating an intention.
     * The
     */
    @SuppressWarnings("unused")
    public static class Builder {
        Intention mIntentions;

        /**
         * Builder for creating a new intention.
         */
        public Builder() {
            mIntentions = new Intention();
        }

        /**
         * Connects to the target device.
         * @param resultHandler An optional handler to be called once the action completes or fails.
         * @return The builder.
         */
        public Builder connect(@Nullable ResultHandler resultHandler) {
            mIntentions.mActions.add(0,new ConnectAction(SmartDeviceManager.getActiveContext(), resultHandler));
            return this;
        }

        /**
         * Changes the value of a characteristic with a given invoke handler to allow errors to be ignored or otherwise
         * resolved.
         * @param characteristicId The identifier for the characteristic, defined by @DeviceParameters
         * @param resultHandler An optional handler to be called once the action completes or fails.
         * @param data The byte data to write
         * @return The builder.
         */
        @SuppressWarnings("SameParameterValue")
        @Sequential
        public Builder changeCharacteristic(int characteristicId, @Nullable ResultHandler resultHandler, byte ... data) {
            changeCharacteristic(characteristicId, resultHandler,BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT, data);
            return this;
        }

        /**
         * Changes the value of a characteristic with a given invoke handler to allow errors to be ignored or otherwise
         * resolved.
         * @param characteristicId The identifier for the characteristic, defined by @DeviceParameters
         * @param resultHandler An optional handler to be called once the action completes or fails.
         * @param writeMode The BluetoothGattCharacteristic WRITE_TYPE to write with.
         * @param data The byte data to write
         * @return The builder.
         */
        @Sequential
        public Builder changeCharacteristic(int characteristicId, @Nullable ResultHandler resultHandler, int writeMode, byte ... data) {
            mIntentions.mActions.add(new WriteCharacteristicAction(characteristicId, resultHandler, writeMode, data));
            return this;
        }

        /**
         * Disconnects from the target device.
         * @param handler An optional handler to be called once the action completes or fails.
         * @return The builder.
         */
        @Sequential
        public Builder disconnect(@Nullable ResultHandler handler) {
            mIntentions.mActions.add(new DisconnectAction(handler));
            return this;
        }

        /**
         * Appends the actions of the given intention to this one.
         * @param intention The intention object to append.
         * @return The builder.
         */
        @Sequential
        public Builder appendIntention(Intention intention) {
            mIntentions.mActions.addAll(intention.getActions());
            return this;
        }

        /**
         * Executes an action at the given time in the queue.
         * If the event should not block future events in the queue, it should ensure that it explicitly
         * returns Result.OK;
         * @param execute The method to execute.
         * @param resultHandler An optional handler to be called once the action completes or fails.
         * @return
         */
        @Sequential
        public Builder then(ExecuteAction.Execute execute, @Nullable ResultHandler resultHandler) {
            mIntentions.mActions.add(new ExecuteAction(execute, resultHandler));
            return this;
        }

        public Intention build() {
            return mIntentions;
        }

    }

    public int getWaitLimit() {
        return mWaitLimit;
    }

    public ArrayList<Action> getActions() {
        return new ArrayList<>(mActions);
    }
}
