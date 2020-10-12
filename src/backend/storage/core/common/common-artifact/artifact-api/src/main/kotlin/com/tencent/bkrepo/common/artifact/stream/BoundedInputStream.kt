/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.  
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *
 */

package com.tencent.bkrepo.common.artifact.stream

import java.io.File
import java.io.InputStream
import kotlin.math.min

class BoundedInputStream(
    private val source: InputStream,
    limit: Long
) : InputStream() {

    private var pos: Long = 0
    private val length: Long = limit
    private var isPropagateClose: Boolean = true

    init {
        require(length >= 0) { "Limit value must greater than 0." }
    }

    override fun read(): Int {
        if (pos >= length) {
            return EOF
        }
        val result = source.read()
        pos++
        return result
    }

    override fun read(b: ByteArray): Int {
        return this.read(b, 0, b.size)
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        if (pos >= length) {
            return EOF
        }
        val maxRead = min(len.toLong(), length - pos).toInt()
        val bytesRead = source.read(b, off, maxRead)
        if (bytesRead == EOF) {
            return EOF
        }
        pos += bytesRead
        return bytesRead
    }

    override fun skip(n: Long): Long {
        val toSkip = min(n, (length - pos))
        val skippedBytes = source.skip(toSkip)
        pos += skippedBytes
        return skippedBytes
    }

    override fun available(): Int {
        return (length - pos).toInt()
    }

    override fun close() {
        if (isPropagateClose) {
            source.close()
        }
    }

    @Synchronized
    override fun reset() {
        source.reset()
    }

    @Synchronized
    override fun mark(readlimit: Int) {
        source.mark(readlimit)
    }

    override fun markSupported(): Boolean {
        return source.markSupported()
    }

    companion object {
        private const val EOF = -1
    }
}

fun InputStream.bound(range: Range): InputStream {
    return if (range.isPartialContent()) {
        BoundedInputStream(this, range.length)
    } else this
}

fun File.bound(range: Range): InputStream {
    return if (range.isPartialContent()) {
        this.inputStream().apply { skip(range.start) }.bound(range)
    } else {
        this.inputStream()
    }
}
