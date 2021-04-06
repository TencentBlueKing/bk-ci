package com.tencent.bkrepo.common.artifact.stream

import com.google.common.util.concurrent.RateLimiter
import java.io.InputStream

/**
 * 支持限速的输入流
 * @param delegate 实际操作的输入流
 * @param rate 限制每秒读取的字节数
 */
@Suppress("UnstableApiUsage")
class RateLimitInputStream(
    delegate: InputStream,
    rate: Long
) : DelegateInputStream(delegate) {

    private val rateLimiter = rate.takeIf { it > 0 }?.let { RateLimiter.create(rate.toDouble()) }

    override fun read(): Int {
        rateLimiter?.acquire()
        return super.read()
    }

    override fun read(byteArray: ByteArray): Int {
        rateLimiter?.acquire(byteArray.size)
        return super.read(byteArray)
    }

    override fun read(byteArray: ByteArray, off: Int, len: Int): Int {
        rateLimiter?.acquire(len)
        return super.read(byteArray, off, len)
    }
}
