/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.worker.common.utils

import com.tencent.devops.worker.common.LOG_MESSAGE_LENGTH_LIMIT
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.lang.reflect.Field
import java.nio.charset.Charset
import org.apache.commons.exec.LogOutputStream

/**
 * 带单行字节上限的 [LogOutputStream]：
 * 1. 缓存父类私有 `buffer` 字段，避免每行一次反射开销；
 * 2. 同时拦截单字节与字节数组写入，长时间不输出换行符时按 [maxLineBytes] 分段刷出，防 OOM；
 * 3. 在 UTF-8 延续字节处延后至多 4 字节切分，避免把多字节字符从中间截断。
 */
internal abstract class BoundedLogOutputStream(
    private val charsetName: String = Charset.defaultCharset().name(),
    private val maxLineBytes: Int = LOG_MESSAGE_LENGTH_LIMIT
) : LogOutputStream() {

    private var bufferedBytes = 0

    @Throws(IOException::class)
    override fun write(cc: Int) {
        val isLineBreak = isLineBreak(cc.toByte())
        if (!isLineBreak && maxLineBytes > 0 && shouldFlushBefore(cc and 0xFF)) {
            processBuffer()
        }
        super.write(cc)
        if (!isLineBreak) {
            bufferedBytes++
        }
    }

    @Throws(IOException::class)
    override fun write(b: ByteArray, off: Int, len: Int) {
        if (maxLineBytes <= 0 || len <= 0) {
            super.write(b, off, len)
            return
        }
        val end = off + len
        var pos = off
        while (pos < end) {
            pos = writeStep(b, pos, end)
        }
    }

    private fun writeStep(b: ByteArray, start: Int, end: Int): Int {
        if (isLineBreak(b[start])) {
            super.write(b, start, 1)
            return start + 1
        }
        if (shouldFlushBefore(b[start].toInt() and 0xFF)) {
            processBuffer()
        }
        val remaining = maxLineBytes - bufferedBytes
        if (remaining <= 0) {
            return writeUtf8Slack(b, start, end)
        }
        val limit = (start + remaining).coerceAtMost(end)
        var pos = start
        while (pos < limit && !isLineBreak(b[pos])) {
            pos++
        }
        writeChunk(b, start, pos - start)
        return pos
    }

    /**
     * 正处于 UTF-8 延续字节宽限区间内，继续吸收延续字节直至字符边界或宽限用尽，随后刷出。
     */
    private fun writeUtf8Slack(b: ByteArray, start: Int, end: Int): Int {
        val maxSlackEnd = (start + (maxLineBytes + UTF8_CHAR_SLACK - bufferedBytes)).coerceAtMost(end)
        var pos = start
        while (pos < maxSlackEnd && isContinuableUtf8Byte(b[pos])) {
            pos++
        }
        writeChunk(b, start, pos - start)
        if (pos < end && !isLineBreak(b[pos])) {
            processBuffer()
        }
        return pos
    }

    private fun writeChunk(b: ByteArray, start: Int, chunk: Int) {
        if (chunk > 0) {
            super.write(b, start, chunk)
            bufferedBytes += chunk
        }
    }

    private fun isLineBreak(c: Byte): Boolean = c == LF || c == CR

    private fun isContinuableUtf8Byte(c: Byte): Boolean = !isLineBreak(c) && isUtf8Continuation(c.toInt() and 0xFF)

    override fun processBuffer() {
        val buffer = BUFFER_FIELD.get(this) as ByteArrayOutputStream
        processLine(buffer.toString(charsetName))
        buffer.reset()
        bufferedBytes = 0
    }

    private fun shouldFlushBefore(nextByte: Int): Boolean {
        if (bufferedBytes < maxLineBytes) {
            return false
        }
        return !isUtf8Continuation(nextByte) || bufferedBytes >= maxLineBytes + UTF8_CHAR_SLACK
    }

    private fun isUtf8Continuation(unsignedByte: Int): Boolean = (unsignedByte and 0xC0) == 0x80

    companion object {
        private const val LF: Byte = 0x0a
        private const val CR: Byte = 0x0d
        private const val UTF8_CHAR_SLACK = 4

        private val BUFFER_FIELD: Field = LogOutputStream::class.java.getDeclaredField("buffer").apply {
            isAccessible = true
        }
    }
}
