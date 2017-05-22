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

package com.jameslandrum.bluetoothsmart2;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Allows the scanner to identify devices, as well as automatically applies filters to the Android BLE stack (5.0+)
 */
public final class Identifier {
    private String mName;
    private ArrayList<UUID> mUuids = new ArrayList<>();
    private ByteId mByteId;
    private Class<? extends SmartDevice> mClass;

    private Identifier(Class<? extends SmartDevice> klass) {
     this.mClass = klass;
    }

    public String getName() {
        return mName;
    }

    public ArrayList<UUID> getUuids() {
        return mUuids;
    }

    public ByteId getByteId() {
        return mByteId;
    }

    public Class<? extends SmartDevice>  getDeviceClass() {
        return mClass;
    }

    public static class Builder {
        Identifier identifier;

        /**
         * Creates a new builder object to create identifiers.
         * @param klass the class of an object that extends SmartDevice that this identifier should use
         *              to create an instance of in response to being detected.
         */
        public Builder(Class<? extends SmartDevice> klass) {
            identifier = new Identifier(klass);
        }

        /**
         * Checks against the known name (for bonded devices) as well as the advertised name for a match.
         * Note: Devices that do not offer a name in the advertisement but do in the DIS will not match until a
         * connection has been established.
         * @param name The name to look for.
         * @return This builder.
         */
        public Builder name(String name) {
            identifier.mName = name;
            return this;
        }

        /**
         * Checks for a partial or complete UUID. Partials will automatically expand based on the BLE specification.
         * @param uuid The UUID to look for.
         * @return This builder.
         */
        public Builder uuid(String uuid) {
            identifier.mUuids.add(new Uuid(uuid).getUuid());
            return this;
        }

        /**
         * Executes a byte identifier to compare against the advertisement. Typically used to validate against
         * manufacturer specific data.
         * @param byteId The byte identifier to execute that returns true on a match.
         * @return This builder.
         */
        public Builder byteId(ByteId byteId) {
            identifier.mByteId = byteId;
            return this;
        }

        /**
         * Creates the identifier instance.
         * @return A new identifier based on the provided details.
         */
        public Identifier build() {
            return identifier;
        }
    }

    public interface ByteId {
        /**
         * Checks the provided advertisement data for a match
         * @param data The complete advertisement. This will be 62 bytes on Android 5.0, and <31 bytes on 4.4
         * @return Return true to indicate that the provided advertisement matches an expected specification.
         */
        boolean checkBytes(byte[] data);
    }
}
