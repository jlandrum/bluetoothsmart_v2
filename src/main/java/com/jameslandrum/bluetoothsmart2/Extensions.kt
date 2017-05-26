package com.jameslandrum.bluetoothsmart2

import java.nio.ByteBuffer
import java.nio.ByteOrder

fun Number.asBytes(length: Int, littleEndian : Boolean = false) : ByteArray {
    val buff = ByteBuffer.allocate(length)
            .order(if (littleEndian) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN)
    when (length) {
        1 -> buff.put(this.toByte())
        2 -> buff.putShort(this.toShort())
        4 -> buff.putInt(this.toInt())
        8 -> buff.putLong(this.toLong())
        else -> throw RuntimeException("Length must be power of 2")
    }
    return buff.array()
}
fun Byte.asBytes(littleEndian : Boolean = false) = this.asBytes(1,littleEndian)
fun Short.asBytes(littleEndian : Boolean = false) = this.asBytes(2,littleEndian)
fun Int.asBytes(littleEndian : Boolean = false) = this.asBytes(4,littleEndian)
fun Long.asBytes(littleEndian : Boolean = false) = this.asBytes(8,littleEndian)
fun String.asBytes(encoding: String = "UTF-8") = this.toByteArray(java.nio.charset.Charset.forName(encoding))
fun Boolean.asBytes() = (if (this) 1 else 0).asBytes(1)

infix fun ByteArray.byte(id : Int) = this[id]
infix fun Byte.bit(i : Int) = 0b1.shl(i).and(this.toInt()) == 0b1.shl(i)
