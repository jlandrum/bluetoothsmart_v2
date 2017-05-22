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

package com.jameslandrum.bluetoothsmart2

import java.util.ArrayList
import java.util.UUID

/**
 * Allows the scanner to identify devices, as well as automatically applies filters to the Android BLE stack (5.0+)
 */
class Identifier private constructor(val deviceClass: Class<out SmartDevice>) {
    var name: String? = null
        private set
    val uuids = ArrayList<UUID>()
    var byteId: (ByteArray)->Boolean = {true}

    class Builder
    /**
     * Creates a new builder object to create identifiers.
     * @param klass the class of an object that extends SmartDevice that this identifier should use
     * *              to create an instance of in response to being detected.
     */
    (klass: Class<out SmartDevice>) {
        internal var identifier: Identifier

        init {
            identifier = Identifier(klass)
        }

        /**
         * Checks against the known name (for bonded devices) as well as the advertised name for a match.
         * Note: Devices that do not offer a name in the advertisement but do in the DIS will not match until a
         * connection has been established.
         * @param name The name to look for.
         * *
         * @return This builder.
         */
        fun name(name: String): Builder {
            identifier.name = name
            return this
        }

        /**
         * Checks for a partial or complete UUID. Partials will automatically expand based on the BLE specification.
         * @param uuid The UUID to look for.
         * *
         * @return This builder.
         */
        fun uuid(uuid: String): Builder {
            identifier.uuids.add(Uuid(uuid).uuid)
            return this
        }

        /**
         * Executes a byte identifier to compare against the advertisement. Typically used to validate against
         * manufacturer specific data.
         * @param byteId The byte identifier to execute that returns true on a match.
         * *
         * @return This builder.
         */
        fun byteId(byteId: (ByteArray)->Boolean): Builder {
            identifier.byteId = byteId
            return this
        }

        /**
         * Creates the identifier instance.
         * @return A new identifier based on the provided details.
         */
        fun build(): Identifier {
            return identifier
        }
    }
}
