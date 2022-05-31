/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.common.artifact.stream

import java.io.Closeable
import java.io.File
import java.io.InputStream
import java.nio.channels.FileLock

/**
 * Returns the default buffer size when working with buffered streams.
 */
const val STREAM_BUFFER_SIZE: Int = 8 * 1024
const val EOF = -1

fun Closeable.closeQuietly() {
    try {
        this.close()
    } catch (ignored: Throwable) {
    }
}

fun FileLock.releaseQuietly() {
    try {
        if (this.isValid) {
            this.release()
        }
    } catch (ignored: Throwable) {
    }
}

fun InputStream.artifactStream(range: Range): ArtifactInputStream {
    return if (this is ArtifactInputStream) this else ArtifactInputStream(this, range)
}

fun InputStream.rateLimit(rate: Long): RateLimitInputStream {
    return if (this is RateLimitInputStream) this else RateLimitInputStream(this, rate)
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

fun File.artifactStream(range: Range): ArtifactInputStream {
    return FileArtifactInputStream(this, range)
}
