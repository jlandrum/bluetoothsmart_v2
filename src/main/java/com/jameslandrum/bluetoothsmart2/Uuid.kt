package com.jameslandrum.bluetoothsmart2

import java.util.regex.Pattern

class Uuid {
    companion object {
        private val CORE_UUID = "00000000-0000-1000-8000-00805F9B34FB"
        private val PATTERN_FULL = Pattern.compile("[0-F]{8}-[0-F]{4}-[0-F]{4}-[0-F]{4}-[0-F]{12}", Pattern.CASE_INSENSITIVE)
        private val PATTERN_PARTIAL = Pattern.compile("([0-F]{4})?([0-F]{4})(-0{4}-0{4}-0{4}-0{12})?", Pattern.CASE_INSENSITIVE)
    }

    constructor(service: String, char: String) {}
    constructor(char: String) : this(CORE_UUID, char)

}