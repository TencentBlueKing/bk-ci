package com.tencent.bkrepo.common.bksync

import java.nio.channels.WritableByteChannel

/**
 * 块输入channel
 * */
interface BlockChannel : AutoCloseable {

    /**
     * 获取连续块,当startSeq和endSeq一样时，即复制单块内容
     * @param startSeq 开始块序号,包含startSeq
     * @param endSeq 结束块序号，包含endSeq
     * @param blockSize 块大小
     * */
    fun transferTo(startSeq: Int, endSeq: Int, blockSize: Int, target: WritableByteChannel): Long

    /**
     * 流大小
     * */
    fun totalSize(): Long

    /**
     * 流名称
     * */
    fun name(): String
}
