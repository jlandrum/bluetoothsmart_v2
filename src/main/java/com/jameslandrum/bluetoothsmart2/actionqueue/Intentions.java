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
import android.os.Build;
import com.jameslandrum.bluetoothsmart2.SmartDeviceManager;
import com.jameslandrum.bluetoothsmart2.annotations.Sequential;

import java.util.ArrayList;

/**
 * Intentions are descriptors for the series of actions that are to be taken on a Bluetooth device.
 * These are device agnostic, meaning you could define a series of intents such as to read the current value
 * from a Bluetooth Sig defined service, and issue that command on any device that offers those defined services,
 * even if the two devices are manufactured by separate entities.
 */
@SuppressWarnings({"WeakerAccess", "FieldCanBeLocal"})
public class Intentions {
    private int mWaitLimit = 300;                               // How long an action may take before being considered "Failed"
    private ArrayList<Action> mActions = new ArrayList<>();     // The list of actions to be taken.

    /**
     * Builder class for creating an intention.
     * The
     */
    @SuppressWarnings("unused")
    public static class Builder {
        Intentions mIntentions;
        Conditional mConditional;

        /**
         * Builder for creating a new intention.
         */
        public Builder() {
            mIntentions = new Intentions();
        }

        public Builder connect() {
            mIntentions.mActions.add(0,new ConnectAction(SmartDeviceManager.getActiveContext()));
            return this;
        }

        /**
         * Begins a conditional block. If the check passes, then the following commands
         * will be executed, otherwise they will be skipped.
         * Notice:
         * @param check The check to execute.
         * @return The builder.
         */
        @Sequential
        public Builder onlyIf(Conditional check) {
            if (mIntentions.getActions().size() == 0) throw new RuntimeException("onlyIf can only be used after a step.");
            mIntentions.getActions().get(mIntentions.getActions().size()-1).addCondition(check);
            return this;
        }

        /**
         * Changes the value of a characteristic
         * @param characteristicId The identifier for the characteristic, defined by @DeviceParameters
         * @param data The byte data to write
         * @return The builder.
         */
        @Sequential
        public Builder changeCharacteristic(int characteristicId, byte ... data) {
            changeCharacteristic(characteristicId, null, data);
            return this;
        }

        /**
         * Changes the value of a characteristic with a given error handler to allow errors to be ignored or otherwise
         * resolved.
         * @param characteristicId The identifier for the characteristic, defined by @DeviceParameters
         * @param errorHandler The error handler to be called if the action fails
         * @param data The byte data to write
         * @return The builder.
         */
        @SuppressWarnings("SameParameterValue")
        @Sequential
        public Builder changeCharacteristic(int characteristicId, ErrorHandler errorHandler, byte ... data) {
            changeCharacteristic(characteristicId,errorHandler,BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT, data);
            return this;
        }

        /**
         * Changes the value of a characteristic with a given error handler to allow errors to be ignored or otherwise
         * resolved.
         * @param characteristicId The identifier for the characteristic, defined by @DeviceParameters
         * @param errorHandler The error handler to be called if the action fails
         * @param writeMode The BluetoothGattCharacteristic WRITE_TYPE to write with.
         * @param data The byte data to write
         * @return The builder.
         */
        @Sequential
        public Builder changeCharacteristic(int characteristicId, ErrorHandler errorHandler, int writeMode, byte ... data) {
            mIntentions.mActions.add(new WriteCharacteristicAction(characteristicId, errorHandler, writeMode, data));
            return this;
        }

        /**
         * Insurance allows a process to validate state before, during and after actions to avoid taking actions that
         * may otherwise be destructive or unecessary. An example of this would be a bluetooth device with a login
         * service. changeCharacteristic can be used to send the password to a device, readCharacteristic can be used
         * to then fetch the authentication state of a device, then ensure can be used to check the value of the
         * authentication state to ensure authentication was successful, updating the state of the device accordingly.
         * @param insurance The callback that executes and determines if the process has failed.
         * @return The builder.
         */
        @Sequential
        public Builder ensure(Ensure.Insurance insurance) {
            mIntentions.mActions.add(new Ensure(insurance));
            return this;
        }

        @Sequential
        public Builder disconnect() {
            mIntentions.mActions.add(new DisconnectAction());
            return this;
        }

        @Sequential
        public Builder appendIntention(Intentions intentions) {
            mIntentions.mActions.addAll(intentions.getActions());
            return this;
        }

        public Intentions build() {
            if (mConditional != null) throw new RuntimeException("Conditional block must be closed.");
            return mIntentions;
        }

        public Builder then(ExecuteAction.Execute execute) {
            mIntentions.mActions.add(new ExecuteAction(execute));
            return this;
        }
    }

    public int getWaitLimit() {
        return mWaitLimit;
    }

    public ArrayList<Action> getActions() {
        return new ArrayList<>(mActions);
    }
}
