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

public interface CharacteristicCallback {
    int EVENT_CHARACTERISTIC_WRITE = 0x01;
    int EVENT_CHARACTERISTIC_WRITE_FAILURE = 0xF1;
    int EVENT_CHARACTERISTIC_READ = 0x02;
    int EVENT_CHARACTERISTIC_READ_FAILURE = 0xF2;
    int EVENT_DESCRIPTOR_WRITE = 0x03;
    int EVENT_DESCRIPTOR_WRITE_FAILURE = 0xF3;
    int EVENT_SECURITY_FAILURE = 0xFF;

    void onEvent(int event);
}
