package com.tencent.bkrepo.common.bksync.checksum

import java.util.zip.Adler32

/**
 * 基于Adler32的滚动哈希
 * */
class Adler32RollingHash(val blockSize: Int) {

    private var s1: Int = 0
    private var s2: Int = 0
    private val adler32 = Adler32()

    /**
     * 更新data
     * 由于jdk Adler32未提供设置adler的变量值，
     * 所以在滚动计算哈希后，需要reset才能正确使用update
     * */
    fun update(data: ByteArray) {
        adler32.update(data)
        val value = adler32.value
        s1 = (value and 0xFFFF).toInt()
        s2 = (value shr 16 and 0xFFFF).toInt()
    }

    /**
     * 滚动计算哈希
     * @param remove 移除的字节
     * @param enter 加入的字节
     * */
    fun rotate(remove: Byte, enter: Byte) {
        val ix1 = toInt(remove)
        val ixn = toInt(enter)
        s1 = (s1 - ix1 + ixn) % BASE
        if (s1 < 0) {
            s1 += BASE
        }
        val sx1 = (blockSize * ix1) % BASE
        s2 = (s2 - sx1 + s1 - OFFS) % BASE
        if (s2 < 0) {
            s2 += BASE
        }
    }

    fun digest(): Long {
        return (s2 shl 16 or s1).toLong() and 0xffffffffL
    }

    fun reset() {
        adler32.reset()
    }

    private fun toInt(byte: Byte): Int {
        return byte.toInt() and 0xFF
    }

    companion object {
        const val BASE = 65521
        const val OFFS = 1
    }
}
