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

@SuppressWarnings({"unused", "WeakerAccess"})
public class ActionResult {
    public static final int ERROR_CONDITION_NOT_MET = -4;
    public static final int ERROR_UNKNOWN = -3;
    public static final int ERROR_TIMED_OUT = -2;
    public static final int ERROR_NOT_READY = -1;
    public static final int ERROR_OK = 0;
}
