package com.jameslandrum.bluetoothsmart2;

import android.bluetooth.BluetoothGattCharacteristic;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Characteristic {
    private BluetoothGattCharacteristic mCharacteristic;
    private ConcurrentLinkedQueue<NotificationCallback> mNotifications = new ConcurrentLinkedQueue<>();
    private ConcurrentLinkedQueue<CharacteristicCallback> mCallbacks = new ConcurrentLinkedQueue<>();

    private UUID mService;
    private UUID mHandle;
    private int mTimeout = 5000;
    private int mWriteMode = -1;

    private Characteristic(@Nullable UUID2 service, UUID2 handle, int timeout) {
        if (service == null) Log.w(this.getClass().getCanonicalName(),
                "Not supplying a service may decrease performance. Use with caution.");
        if (service!=null) mService = service.getUuid();
        mHandle = handle.getUuid();
    };

    public Characteristic(@Nullable String service, String handle) {
        this(service,handle,5000);
    }

    public Characteristic(@Nullable String service, String handle, int timeout) {
        this(new UUID2(service), new UUID2(service, handle), timeout);
    }

    @Override
    public int hashCode() {
        return mHandle.hashCode();
    }

    void reset() {
        mCharacteristic = null;
    }

    public boolean isReady() {
        return mCharacteristic != null;
    }

    UUID getServiceUuid() {
        return mService;
    }

    UUID getHandleUuid() {
        return mHandle;
    }

    void prepare(@Nullable BluetoothGattCharacteristic characteristic) {
        if (characteristic == null) return;
        mCharacteristic = characteristic;
    }

    void clearAllCallbacks() {
        mNotifications.clear();
    }

    boolean equalsCharacteristic(BluetoothGattCharacteristic characteristic) {
        return mCharacteristic == characteristic;
    }

    void notifyUpdate() {
    }

    public BluetoothGattCharacteristic getNativeCharacteristic() {
        return mCharacteristic;
    }

    public void addNotificationListener(NotificationCallback notificationCallback) {
        if (!mNotifications.contains(notificationCallback)) mNotifications.add(notificationCallback);
    }

    public void removeNotificationListener(NotificationCallback notificationCallback) {
        mNotifications.remove(notificationCallback);
    }

    public void addCallback(CharacteristicCallback listener) {
        if (!mCallbacks.contains(listener)) mCallbacks.add(listener);
    }

    public void removeCallback(CharacteristicCallback listener) {
        mCallbacks.remove(listener);
    }

    void callEvent(int event) {
        for (CharacteristicCallback c : mCallbacks) { c.onEvent(event); }
    }

    public int getTimeout() {
        return mTimeout;
    }

    public int getWriteMode() {
        return mWriteMode;
    }

    public byte[] getValue() {
        if (getNativeCharacteristic() == null) return null;
        return getNativeCharacteristic().getValue();
    }
}
