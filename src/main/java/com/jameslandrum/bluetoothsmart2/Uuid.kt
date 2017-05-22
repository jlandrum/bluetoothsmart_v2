package com.jameslandrum.bluetoothsmart2

import java.util.*
import java.util.regex.Pattern

class Uuid(val uuid: UUID) {
    companion object {
        private val CORE_UUID = "00000000-0000-1000-8000-00805F9B34FB"
        private val PATTERN_FULL = Pattern.compile("[0-F]{8}-[0-F]{4}-[0-F]{4}-[0-F]{4}-[0-F]{12}", Pattern.CASE_INSENSITIVE)
        private val PATTERN_PARTIAL = Pattern.compile("([0-F]{4})?([0-F]{4})(-0{4}-0{4}-0{4}-0{12})?", Pattern.CASE_INSENSITIVE)

        private fun  uuidFromPair(base: String?, uuid: String): UUID {
            if (PATTERN_FULL.matcher(uuid).matches()) {
                return UUID.fromString(uuid)
            }
            if (base != null && !PATTERN_FULL.matcher(base).matches()) throw IllegalArgumentException("Base UUID must be complete UUID or null.")
            val partial = PATTERN_PARTIAL.matcher(uuid)
            if (!partial.matches()) throw IllegalArgumentException("Component UUID must be complete UUID, 4-byte UUID, 8-byte UUID, or zero-padded UUID.")

            val builder = StringBuilder(CORE_UUID)
            if (base != null) builder.replace(0, builder.length, base)
            if (partial.group(1) != null) builder.replace(0, 4, partial.group(0))
            builder.replace(4, 8, partial.group(2))
            return UUID.fromString(builder.toString())
        }
    }

    constructor(char: String) : this(CORE_UUID, char)
    constructor(service: String, char: String) : this(uuidFromPair(service,char))
}