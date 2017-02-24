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
import com.jameslandrum.bluetoothsmart2.Utils;

import java.util.ArrayList;
import java.util.UUID;

@SuppressWarnings("ALL")
public class Identifier {
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

        public Builder(Class<? extends SmartDevice> klass) {
            identifier = new Identifier(klass);
        }

        public Builder name(String name) {
            identifier.mName = name;
            return this;
        }

        @SuppressWarnings("SameParameterValue")
        public Builder uuid(String uuid) {
            identifier.mUuids.add(Utils.uuidFromString(uuid));
            return this;
        }

        public Builder byteId(ByteId byteId) {
            identifier.mByteId = byteId;
            return this;
        }

        public Identifier build() {
            return identifier;
        }
    }

    public interface ByteId {
        boolean checkBytes(byte[] data);
    }
}
