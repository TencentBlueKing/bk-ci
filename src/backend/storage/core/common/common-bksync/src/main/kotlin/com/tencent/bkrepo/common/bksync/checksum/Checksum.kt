package com.tencent.bkrepo.common.bksync.checksum

/**
 * bksync校验和
 * */
data class Checksum(
    val rollingHash: Int,
    val md5: ByteArray,
    val seq: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Checksum

        if (rollingHash != other.rollingHash) return false
        if (!md5.contentEquals(other.md5)) return false
        if (seq != other.seq) return false

        return true
    }

    override fun hashCode(): Int {
        var result = rollingHash
        result = 31 * result + md5.contentHashCode()
        result = 31 * result + seq
        return result
    }
}
