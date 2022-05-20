package com.tencent.bkrepo.common.bksync

import com.google.common.primitives.Ints
import com.tencent.bkrepo.common.bksync.checksum.Checksum
import java.io.InputStream
import java.util.HashMap

/**
 * Checksum块内容索引
 * 根据校验和输入流构建checksum索引，为加快校验和查询速度，提供三级查询
 * level 1: rolling hash的hash是否存在
 * level 2: rolling hash是否相等
 * level 3: md5是否相等
 * */
class ChecksumIndex(inputStream: InputStream) {

    /**
     * checksum index
     * */
    private val index = HashMap<Key, Checksum>(INITIAL_CAPACITY)

    /**
     * key查询时的容器对象，仅仅用作参数容器，未避免查询时创建大量Key对象
     * */
    private val key = Key(0)

    /**
     * 分块总数
     * */
    val total: Int

    init {
        /*
        * 初始化索引
        * 1. 读取数据，直到读满整个buffer或者读到末尾
        * 2. 按读取顺序，使用序列号构造checksum，并且存入map
        * 3. 重复1，直到读完流
        * */
        var seq = 0
        var pos = 0
        val bufferSize = BUFFER_SIZE
        val buffer = ByteArray(bufferSize)
        while (true) {
            val len = buffer.size - pos
            val bytes = inputStream.read(buffer, pos, len)
            if (bytes == -1) {
                // 流已经读完，消费缓冲区的剩余数据
                if (pos > 0) {
                    val remain = buffer.copyOfRange(0, pos)
                    seq = putCheckSum(remain, seq)
                }
                break
            }
            pos += bytes
            if (pos == bufferSize) {
                // 读满,消费buffer
                seq = putCheckSum(buffer, seq)
                pos = 0
            }
        }
        total = seq
    }

    /**
     * 将字节数据分块反序列化成checksum，放入缓存
     * @param bytes 包含校验和的字节数组，必须为校验和的整数倍
     * @param seqStart 当前校验和的序列号开始值
     * */
    private fun putCheckSum(bytes: ByteArray, seqStart: Int): Int {
        assert(bytes.size % CHECKSUM_SIZE == 0) { "checksum stream broken" }
        var pos = 0
        val rollingHashBytes = ByteArray(ROLLING_HASH_BYTES_SIZE)
        var seq = seqStart
        while (pos < bytes.size) {
            bytes.copyInto(rollingHashBytes, startIndex = pos, endIndex = pos + ROLLING_HASH_BYTES_SIZE)
            pos += ROLLING_HASH_BYTES_SIZE
            val md5Bytes = ByteArray(MD5_BYTES_SIZE)
            bytes.copyInto(md5Bytes, startIndex = pos, endIndex = pos + MD5_BYTES_SIZE)
            pos += MD5_BYTES_SIZE
            val rollingHash = Ints.fromByteArray(rollingHashBytes)
            val checksum = Checksum(rollingHash = rollingHash, md5 = md5Bytes, seq = seq)
            put(checksum)
            seq++
        }
        return seq
    }

    /**
     * 查找是否存在rollingHash
     * */
    fun exist(rollingHash: Int): Boolean {
        key.rollingHash = rollingHash
        key.md5 = null
        return index.containsKey(key)
    }

    /**
     * 获取checksum块内容
     * @return checksum块内容
     * */
    fun get(rollingHash: Int, md5: ByteArray): Checksum? {
        key.rollingHash = rollingHash
        key.md5 = md5
        return index[key]
    }

    /**
     * 存储checksum块内容
     * @param checksum checksum块内容
     * */
    fun put(checksum: Checksum) {
        val key = Key(checksum.rollingHash, checksum.md5)
        index[key] = checksum
    }

    /**
     * 返回当前索引中的校验和大小
     * */
    fun size(): Int {
        return index.size
    }

    /**
     * 使用rollingHash作为key的哈希函数，以实现三级查询
     * level 1: 检查rollingHash的hash值是否存在
     * level 2: 检查rollingHash是否相等
     * level 3: 检查md5是否相等
     * */
    data class Key(
        var rollingHash: Int,
        var md5: ByteArray? = null
    ) {
        override fun hashCode(): Int {
            return rollingHash.hashCode()
        }

        override fun equals(other: Any?): Boolean {
            if (other is Key) {
                if (other.md5 == null || this.md5 == null) {
                    // 比较rolling hash，用于快速检索
                    return this.rollingHash == other.rollingHash
                }
                return this.md5!!.contentEquals(other.md5!!)
            }
            return false
        }
    }

    companion object {
        const val INITIAL_CAPACITY = 2 shl 16
        const val ROLLING_HASH_BYTES_SIZE = 4
        const val MD5_BYTES_SIZE = 16
        const val CHECKSUM_SIZE = ROLLING_HASH_BYTES_SIZE + MD5_BYTES_SIZE
        const val BUFFER_SIZE = 20 * CHECKSUM_SIZE
    }
}
