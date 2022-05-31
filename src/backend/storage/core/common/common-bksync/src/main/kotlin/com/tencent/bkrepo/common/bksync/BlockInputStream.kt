package com.tencent.bkrepo.common.bksync

/**
 * 块输入流
 * */
interface BlockInputStream : AutoCloseable {
    /**
     * 获取块内容
     * */
    fun getBlock(seq: Int, blockSize: Int, blockData: ByteArray): Int

    /**
     * 流大小
     * */
    fun totalSize(): Long

    /**
     * 流名称
     * */
    fun name(): String
}
