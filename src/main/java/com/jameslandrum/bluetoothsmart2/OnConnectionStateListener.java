package com.jameslandrum.bluetoothsmart2;

public interface OnConnectionStateListener {
    void onConnected(SmartDevice device);
    void onDisconnected(SmartDevice device);
    void onServicesDiscovered(SmartDevice device);
}
