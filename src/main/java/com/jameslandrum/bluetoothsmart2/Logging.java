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
