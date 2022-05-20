package com.tencent.bkrepo.common.bksync

/**
 * 字节块输入流
 * */
class ByteArrayBlockInputStream(val bytes: ByteArray, val name: String) : BlockInputStream {
    override fun getBlock(seq: Int, blockSize: Int, blockData: ByteArray): Int {
        var copyLen = blockSize
        val start = (seq.toLong() * blockSize).toInt()
        if (start + blockSize > bytes.size) {
            copyLen = bytes.size - start
        }
        bytes.copyInto(blockData, startIndex = start, endIndex = start + copyLen)
        return copyLen
    }

    override fun totalSize(): Long {
        return bytes.size.toLong()
    }

    override fun name(): String {
        return name
    }

    override fun close() {
        // nothing to close
    }
}
