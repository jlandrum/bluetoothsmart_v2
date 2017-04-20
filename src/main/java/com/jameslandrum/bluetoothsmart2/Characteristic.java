package com.jameslandrum.bluetoothsmart2;

import android.bluetooth.BluetoothGattCharacteristic;
import android.support.annotation.Nullable;
import android.util.Log;
import com.jameslandrum.bluetoothsmart2.actionqueue.NotificationCallback;

import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Characteristic {
    private BluetoothGattCharacteristic mCharacteristic;
    private ConcurrentLinkedQueue<NotificationCallback> mCallbacks;

    private UUID mService;
    private UUID mHandle;

    private Characteristic(@Nullable UUID2 service, UUID2 handle) {
        if (service == null) Log.w(this.getClass().getCanonicalName(),
                "Not supplying a service may decrease performance. Use with caution.");
        if (service!=null) mService = service.getUuid();
        mHandle = handle.getUuid();
    };

    public Characteristic(@Nullable String service, String handle) {
        this(new UUID2(service), new UUID2(service, handle));
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
        // TODO: Remove notification listeners
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
        if (!mCallbacks.contains(notificationCallback)) mCallbacks.add(notificationCallback);
    }

    public void removeNotificationListener(NotificationCallback notificationCallback) {
        mCallbacks.remove(notificationCallback);
    }
}
