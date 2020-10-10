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

import java.io.InputStream

class ArtifactInputStream(
    private val delegate: InputStream,
    val range: Range
) : InputStream() {

    private val listenerList = mutableListOf<StreamReadListener>()

    override fun read(): Int {
        return delegate.read().apply {
            if (this >= 0) {
                listenerList.forEach { it.data(this) }
            } else {
                notifyClose()
            }
        }
    }

    override fun read(b: ByteArray): Int {
        return delegate.read(b).apply {
            if (this >= 0) {
                listenerList.forEach { it.data(b, this) }
            } else {
                notifyClose()
            }
        }
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        return delegate.read(b, off, len).apply {
            if (this >= 0) {
                listenerList.forEach { it.data(b, this) }
            } else {
                notifyClose()
            }
        }
    }

    override fun close() {
        listenerList.forEach { it.close() }
        delegate.close()
    }

    override fun skip(n: Long) = delegate.skip(n)
    override fun available() = delegate.available()
    override fun reset() = delegate.reset()
    override fun mark(readlimit: Int) = delegate.mark(readlimit)
    override fun markSupported(): Boolean = delegate.markSupported()

    fun addListener(listener: StreamReadListener) {
        require(!range.isPartialContent()) { "ArtifactInputStream is partial content, may be result in data inconsistent" }
        listenerList.add(listener)
    }

    private fun notifyClose() {
        listenerList.forEach { it.close() }
    }
}

fun InputStream.toArtifactStream(range: Range): ArtifactInputStream {
    return if (this is ArtifactInputStream) this else ArtifactInputStream(this, range)
}

interface StreamReadListener {
    fun data(i: Int)
    fun data(buffer: ByteArray, length: Int)
    fun close()
}
