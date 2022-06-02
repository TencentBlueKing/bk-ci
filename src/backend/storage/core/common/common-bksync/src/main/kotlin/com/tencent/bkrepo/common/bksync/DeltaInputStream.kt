package com.tencent.bkrepo.common.bksync

import com.google.common.primitives.Ints
import com.tencent.bkrepo.common.api.util.StreamUtils
import java.io.InputStream

/**
 * delta数据输入流
 * 使用Buffer读取，并负责解析流中数据
 * */
class DeltaInputStream(val inputStream: InputStream, refSize: Int = DEFAULT_BLOCK_REF_BYTE_SIZE) {
    /**
     * 增量数据块，必须是ref或者begin标志
     * */
    val data = ByteArray(refSize)

    /**
     * 缓冲区大小
     * */
    val bufferSize = refSize * BUFFER_COUNT

    private val bufferInputStream = inputStream.buffered(bufferSize)

    /**
     * 是否是块引用
     * */
    fun isBlockReference(): Boolean {
        return Ints.fromByteArray(data) >= 0
    }

    /**
     * 块引用序号
     * */
    fun getBlockReference(): Int {
        return Ints.fromByteArray(data)
    }

    /**
     * 是否是数据引用
     * */
    fun isDataSequence(): Boolean {
        return Ints.fromByteArray(data) == -1
    }

    /**
     * 获取流中数据的长度
     * */
    fun getDataSequenceLength(): Int {
        return Ints.fromByteArray(data)
    }

    /**
     * 读取数据到指定数组
     * @param bytes 读取到的目标数组
     * */
    fun read(bytes: ByteArray): Int {
        return bufferInputStream.read(bytes)
    }

    /**
     * 移动到下一块数据
     * 如果数据读取不完整，则抛出异常
     * */
    fun moveToNext(): Int {
        val bytes = StreamUtils.readFully(bufferInputStream, data)
        if (bytes == 0) {
            return 0
        }
        if (bytes < DEFAULT_BLOCK_REF_BYTE_SIZE) {
            throw IllegalStateException(
                "delta stream is broken,need[$DEFAULT_BLOCK_REF_BYTE_SIZE] but only read[$bytes]"
            )
        }
        return bytes
    }

    companion object {
        const val DEFAULT_BLOCK_REF_BYTE_SIZE = 4
        const val BUFFER_COUNT = 2000
    }
}
