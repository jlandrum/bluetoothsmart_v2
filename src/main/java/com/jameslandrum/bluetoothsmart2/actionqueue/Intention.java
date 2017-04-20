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
import com.jameslandrum.bluetoothsmart2.Characteristic;
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
         * @return The builder.
         */
        public Builder connect() {
            return connect(null);
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
         * @param characteristic The characteristic to update.
         * @param timeout How long before the action should be cancelled and a timeout error thrown.
         *                Note that this does NOT cancel the actual action, just the queue - the Bluetooth stack
         *                will still attempt a best-effort to complete the action. Use -1 to wait indefinitely.
         * @param resultHandler An optional handler to be called once the action completes or fails.
         * @param data The byte data to write
         * @return The builder.
         */
        @SuppressWarnings("SameParameterValue")
        @Sequential
        public Builder changeCharacteristic(Characteristic characteristic, int timeout, @Nullable ResultHandler resultHandler, byte ... data) {
            return changeCharacteristic(characteristic, timeout, resultHandler, -1, data);
        }

        /**
         * Changes the value of a characteristic with a given invoke handler to allow errors to be ignored or otherwise
         * resolved.
         * @param characteristic The characteristic to update.
         * @param timeout How long before the action should be cancelled and a timeout error thrown.
         *                Note that this does NOT cancel the actual action, just the queue - the Bluetooth stack
         *                will still attempt a best-effort to complete the action. Use -1 to wait indefinitely.
         * @param resultHandler An optional handler to be called once the action completes or fails.
         * @param writeMode The BluetoothGattCharacteristic WRITE_TYPE to write with.
         * @param data The byte data to write
         * @return The builder.
         */
        @Sequential
        public Builder changeCharacteristic(Characteristic characteristic, int timeout, @Nullable ResultHandler resultHandler, int writeMode, byte ... data) {
            mIntentions.mActions.add(new WriteCharacteristicAction(characteristic, timeout, resultHandler, writeMode, data));
            return this;
        }

        /**
         * Reads the value of a characteristic with a given invoke handler to allow errors to be ignored or otherwise
         * resolved.
         * @param characteristic The characteristic to read.
         * @param timeout How long before the action should be cancelled and a timeout error thrown.
         *                Note that this does NOT cancel the actual action, just the queue - the Bluetooth stack
         *                will still attempt a best-effort to complete the action. Use -1 to wait indefinitely.
         * @param resultHandler An optional handler to be called once the action completes or fails.
         * @return The builder.
         */
        @Sequential
        public Builder readCharacteristic(Characteristic characteristic, int timeout, @Nullable ResultHandler resultHandler) {
            mIntentions.mActions.add(new ReadCharacteristicAction(characteristic, timeout, resultHandler));
            return this;
        }

        /**
         * Registers a callback for characteristic notifications.
         * @param characteristic The characteristic to watch.
         * @param timeout How long before the action should be cancelled and a timeout error thrown.
         *                Note that this does NOT cancel the actual action, just the queue - the Bluetooth stack
         *                will still attempt a best-effort to complete the action. Use -1 to wait indefinitely.
         * @param resultHandler An optional handler to be called once the action completes or fails.
         * @param callback the callback to add for notifications.
         * @return The builder.
         */
        @Sequential
        public Builder registerForNotifications(Characteristic characteristic, int timeout,
                                                @Nullable ResultHandler resultHandler, NotificationCallback callback) {
            mIntentions.mActions.add(new SetNotificationAction(characteristic, timeout, 0,
                    true, resultHandler, callback));
            return this;
        }

        /**
         * Unregisters a callback for characteristic notifications.
         * @param characteristic The characteristic to update.
         * @param timeout How long before the action should be cancelled and a timeout error thrown.
         *                Note that this does NOT cancel the actual action, just the queue - the Bluetooth stack
         *                will still attempt a best-effort to complete the action. Use -1 to wait indefinitely.
         * @param resultHandler An optional handler to be called once the action completes or fails.
         * @param callback the callback to remove from notifications.
         * @return The builder.
         */
        @Sequential
        public Builder unregisterForNotifications(Characteristic characteristic, int timeout,
                                                @Nullable ResultHandler resultHandler, NotificationCallback callback) {
            mIntentions.mActions.add(new SetNotificationAction(characteristic, timeout, 0,
                    false, resultHandler, callback));
            return this;
        }

        /**
         * Disconnects from the target device.
         * @return The builder.
         */
        @Sequential
        public Builder disconnect() {
            return disconnect(null);
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
         * @return
         */
        @Sequential
        public Builder then(ExecuteAction.Execute execute) {
            return then(execute,null);
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

    public ArrayList<Action> getActions() {
        return new ArrayList<>(mActions);
    }
}
