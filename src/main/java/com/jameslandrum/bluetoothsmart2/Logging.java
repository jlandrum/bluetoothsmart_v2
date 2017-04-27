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

import android.util.Log;

/**
 * Handles logging for BluetoothSmart specific events.
 */
final public class Logging {
    public static void notice(String s, Object ... data) {
        Log.d("BluetoothSmart", String.format(s,data));
    }
    public static void error(String s, Object ... data) {
        Log.e("BluetoothSmart", String.format(s,data));
    }
}
