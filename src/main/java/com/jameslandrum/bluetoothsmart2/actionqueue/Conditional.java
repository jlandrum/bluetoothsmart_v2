package com.jameslandrum.bluetoothsmart2.actionqueue;

import com.jameslandrum.bluetoothsmart2.SmartDevice;

import java.util.ArrayList;

/**
 * Represents a conditional block.
 * Actions within this conditional will only be executed if the conditions are met.
 */
public interface Conditional {
    boolean condition();
}
