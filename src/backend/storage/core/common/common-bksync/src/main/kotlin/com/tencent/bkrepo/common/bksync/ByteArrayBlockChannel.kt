package com.tencent.bkrepo.common.bksync

import java.nio.ByteBuffer
import java.nio.channels.WritableByteChannel

/**
 * 字节块输入流
 * */
class ByteArrayBlockChannel(val bytes: ByteArray, val name: String) : BlockChannel {
    private val buf = ByteBuffer.wrap(bytes)

    override fun transferTo(startSeq: Int, endSeq: Int, blockSize: Int, target: WritableByteChannel): Long {
        val start = (startSeq.toLong() * blockSize).toInt()
        var copyLen = (endSeq - startSeq + 1) * blockSize
        if (start + copyLen > bytes.size) {
            copyLen = bytes.size - start
        }
        buf.position(start)
        buf.limit(start + copyLen)
        return target.write(buf).toLong()
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
