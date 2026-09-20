package com.tencent.devops.worker.common.logger

import com.tencent.devops.common.log.pojo.enums.LogStorageMode
import com.tencent.devops.common.log.pojo.message.LogMessage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LoggerUploadBatchTest {

    @Test
    fun utf8LengthCountsAsciiChineseAndEmoji() {
        assertEquals(5, LoggerUploadBatch.utf8Length("hello"))
        assertEquals(6, LoggerUploadBatch.utf8Length("你好"))
        assertEquals(4, LoggerUploadBatch.utf8Length("😀"))
    }

    @Test
    fun nextChunkEndStopsByCount() {
        val messages = messages(6, "x")
        assertEquals(2, LoggerUploadBatch.nextChunkEnd(messages, 0, maxCount = 2, maxBytes = 1024))
        assertEquals(4, LoggerUploadBatch.nextChunkEnd(messages, 2, maxCount = 2, maxBytes = 1024))
        assertEquals(6, LoggerUploadBatch.nextChunkEnd(messages, 4, maxCount = 3, maxBytes = 1024))
    }

    @Test
    fun nextChunkEndStopsByBytes() {
        val payload = "a".repeat(100)
        val perLine = LoggerUploadBatch.estimateBytes(payload)
        val messages = messages(10, payload)
        val end = LoggerUploadBatch.nextChunkEnd(messages, 0, maxCount = 500, maxBytes = perLine * 3 + 10)
        assertEquals(3, end)
    }

    @Test
    fun nextChunkEndKeepsOversizedFirstLine() {
        val huge = "b".repeat(LoggerUploadBatch.MAX_BYTES)
        val messages = listOf(log(huge), log("tail"))
        assertEquals(1, LoggerUploadBatch.nextChunkEnd(messages, 0, maxCount = 500))
    }

    @Test
    fun parseRecognizesArchiveFailed() {
        assertEquals(LogStorageMode.ARCHIVE_FAILED, LogStorageMode.parse("ARCHIVE_FAILED"))
        assertEquals(LogStorageMode.UPLOAD, LogStorageMode.parse("UNKNOWN"))
    }

    @Test
    fun nextChunkEndAtOrPastEnd() {
        val messages = messages(2, "ok")
        assertEquals(2, LoggerUploadBatch.nextChunkEnd(messages, 2))
        assertEquals(0, LoggerUploadBatch.nextChunkEnd(emptyList(), 0))
    }

    private fun messages(count: Int, message: String): List<LogMessage> {
        return List(count) { log(message) }
    }

    private fun log(message: String) = LogMessage(message = message, timestamp = 1L)
}
