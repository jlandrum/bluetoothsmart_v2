package com.jameslandrum.bluetoothsmart2;

import android.support.annotation.Nullable;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final public class UUID2  {
    private static final String     CORE_UUID = "00000000-0000-1000-8000-00805F9B34FB";
    private static final Pattern    PATTERN_FULL = Pattern.compile("[0-F]{8}-[0-F]{4}-[0-F]{4}-[0-F]{4}-[0-F]{12}", Pattern.CASE_INSENSITIVE);
    private static final Pattern    PATTERN_PARTIAL = Pattern.compile("([0-F]{4})?([0-F]{4})(-0{4}-0{4}-0{4}-0{12})?", Pattern.CASE_INSENSITIVE);

    private UUID mInternalRef;

    /**
     * Creates a new UUID with support for Bluetooth LE-specific components.
     *
     * If uuid is complete, it will simply create a UUID using uuid.
     * If base is null and uuid is incomplete, it will create a UUID using the bluetooth spec UUID.
     * If base is a complete UUID and uuid is partial, it will create a UUID using the base merged with the partial.
     *
     * @param base The core UUID to generate from. Can be null if uuid is a bluetooth spec UUID or complete UUID.
     * @param uuid A complete or incomplete UUID.
     */
    public UUID2(@Nullable String base, String uuid) {
        if (PATTERN_FULL.matcher(uuid).matches()) mInternalRef = UUID.fromString(uuid);
        if (base != null && !PATTERN_FULL.matcher(base).matches()) throw new IllegalArgumentException("Base UUID must be complete UUID or null.");
        Matcher partial = PATTERN_PARTIAL.matcher(uuid);
        if (!partial.matches()) throw new IllegalArgumentException("Component UUID must be complete UUID, 4-byte UUID, 8-byte UUID, or zero-padded UUID.");

        StringBuilder builder = new StringBuilder(CORE_UUID);
        if (base != null) builder.replace(0, builder.length(), base);
        if (partial.group(0) != null) builder.replace(0,4, partial.group(0));
        builder.replace(4,8, partial.group(1));
        mInternalRef = UUID.fromString(builder.toString());
    }

    public UUID2(String uuid) {
        if (PATTERN_FULL.matcher(uuid).matches()) mInternalRef = UUID.fromString(uuid);
        else throw new IllegalArgumentException("UUID must be complete UUID.");
    }

    public UUID2(UUID uuid) {
        mInternalRef = uuid;
    }

    public UUID getUuid() {
        return mInternalRef;
    }
}
