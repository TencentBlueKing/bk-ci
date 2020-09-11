package com.tencent.devops.common.archive.util

import java.io.Closeable
import java.nio.channels.FileLock

/**
 * Returns the default buffer size when working with buffered streams.
 */
const val STREAM_BUFFER_SIZE: Int = 8 * 1024

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
