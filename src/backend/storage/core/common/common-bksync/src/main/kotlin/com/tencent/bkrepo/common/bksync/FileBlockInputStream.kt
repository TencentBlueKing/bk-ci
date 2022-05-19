package com.tencent.bkrepo.common.bksync

import java.io.File
import java.io.RandomAccessFile

/**
 * 文件块输入流
 * */
class FileBlockInputStream(file: File, val name: String) : BlockInputStream {
    private val raf = RandomAccessFile(file, READ)
    override fun getBlock(seq: Int, blockSize: Int, blockData: ByteArray): Int {
        var copyLen = blockSize
        val start = seq.toLong() * blockSize
        if (start + blockSize > raf.length()) {
            copyLen = (raf.length() - start).toInt()
        }
        raf.seek(start)
        raf.read(blockData, 0, copyLen)
        return copyLen
    }

    override fun totalSize(): Long {
        return raf.length()
    }

    override fun name(): String {
        return name
    }

    override fun close() {
        raf.close()
    }

    companion object {
        private const val READ = "r"
    }
}
