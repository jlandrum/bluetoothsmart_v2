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

import com.jameslandrum.bluetoothsmart2.annotations.UUIDRef;

import java.util.*;

@SuppressWarnings("WeakerAccess")
public class Utils {
    private static final UUID UUID_16 = UUID.fromString("00000000-0000-1000-8000-00805F9B34FB");

    public static UUID uuidFromRef(UUIDRef ref)
    {
        return uuidFromString(ref.value(), UUID_16);
    }

    public static UUID uuidFromString(String ref)
    {
        return uuidFromString(ref, UUID_16);
    }

    public static UUID uuidFromString(String ref, UUID base)
    {
        UUID result;

        if (ref.length() == 4) {
            String uuidres = base.toString().substring(0,4) + ref + base.toString().substring(8);
            result = UUID.fromString(uuidres);
        } else if (ref.length() == 8) {
            String uuidres = ref + base.toString().substring(8);
            result = UUID.fromString(uuidres);
        } else {
            result = UUID.fromString(ref);
        }

        if (result == null) throw new RuntimeException("UUID must be 16-bit, 32-bit, or 128-bit.");

        return result;
    }

    static UUID uuidFromRef(UUIDRef uuid, UUIDRef service) {
        return uuidFromString(uuid.value(), uuidFromRef(service));
    }
}
