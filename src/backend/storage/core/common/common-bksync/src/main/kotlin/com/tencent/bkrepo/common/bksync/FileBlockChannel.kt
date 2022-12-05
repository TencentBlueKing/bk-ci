package com.tencent.bkrepo.common.bksync

import java.io.File
import java.nio.channels.FileChannel
import java.nio.channels.WritableByteChannel
import java.nio.file.StandardOpenOption

/**
 * 文件块输入流
 * */
class FileBlockChannel(file: File, val name: String) : BlockChannel {
    private val srcChannel = FileChannel.open(file.toPath(), StandardOpenOption.READ)
    private val length = file.length()

    override fun transferTo(startSeq: Int, endSeq: Int, blockSize: Int, target: WritableByteChannel): Long {
        val start = startSeq.toLong() * blockSize
        var copyLen = (endSeq - startSeq + 1) * blockSize
        if (start + copyLen > length) {
            copyLen = (length - start).toInt()
        }
        return srcChannel.transferTo(start, copyLen.toLong(), target)
    }

    override fun totalSize(): Long {
        return length
    }

    override fun name(): String {
        return name
    }

    override fun close() {
        srcChannel.close()
    }

    companion object {
        private const val READ = "r"
    }
}
