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

package com.tencent.devops.worker.common.logger

import com.tencent.devops.common.log.pojo.message.LogMessage

/**
 * 上报切批：条数 + 预估 JSON 字节 + 刷新间隔，对齐 Filebeat / Fluent Bit 的
 * max_count / max_bytes / flush_timeout，避免 1000 条长日志打满 30s 读超时。
 */
object LoggerUploadBatch {

    const val MAX_COUNT = 500
    const val MIN_COUNT = 200
    const val MAX_BYTES = 256 * 1024
    const val FLUSH_INTERVAL_MS = 1_000L
    const val QUEUE_OFFER_TIMEOUT_MS = 100L
    const val QUEUE_MAX_COUNT = 8192
    const val QUEUE_MAX_BYTES = 16L * 1024 * 1024
    const val PENDING_MAX_COUNT = 2000
    const val PENDING_MAX_BYTES = 4L * 1024 * 1024
    const val LOCAL_LOG_BUFFER_BYTES = 16 * 1024
    const val JSON_FIELD_OVERHEAD = 256
    const val READ_TIMEOUT_SECONDS = 30L
    const val MAX_PENDING_IN_MEMORY = 5000
    const val RETRY_BACKOFF_INITIAL_MS = 500L
    const val RETRY_BACKOFF_MAX_MS = 5_000L
    const val FINISH_FLUSH_MAX_DURATION_MS = 120_000L
    const val STOP_FLUSH_MAX_DURATION_MS = 60_000L
    const val FINISH_FLUSH_STALL_ROUNDS = 5

    fun retryBackoffMs(consecutiveFailures: Int): Long {
        if (consecutiveFailures <= 0) {
            return 0L
        }
        val shift = (consecutiveFailures - 1).coerceAtMost(4)
        return (RETRY_BACKOFF_INITIAL_MS shl shift).coerceAtMost(RETRY_BACKOFF_MAX_MS)
    }

    fun utf8Length(value: String): Int {
        var bytes = 0
        var index = 0
        val length = value.length
        while (index < length) {
            val char = value[index]
            when {
                char.code < 0x80 -> bytes += 1
                char.code < 0x800 -> bytes += 2
                Character.isHighSurrogate(char) -> {
                    bytes += 4
                    index++
                }
                else -> bytes += 3
            }
            index++
        }
        return bytes
    }

    fun estimateBytes(message: String): Int = utf8Length(message) + JSON_FIELD_OVERHEAD

    fun nextChunkEnd(
        messages: List<LogMessage>,
        start: Int,
        maxCount: Int = MAX_COUNT,
        maxBytes: Int = MAX_BYTES
    ): Int {
        if (start >= messages.size) {
            return start
        }
        val countLimit = maxCount.coerceAtLeast(1)
        var end = start
        var bytes = 0
        while (end < messages.size && (end - start) < countLimit) {
            val next = estimateBytes(messages[end].message)
            if (end > start && bytes + next > maxBytes) {
                break
            }
            bytes += next
            end++
        }
        return end.coerceAtMost(messages.size)
    }

    private const val MAX_ARCHIVE_REASON_LEN = 240
    private val HEADERS_BLOCK_REGEX = Regex("""headers=\[[^]]*]""", RegexOption.IGNORE_CASE)
    private val URL_QUERY_REGEX = Regex("""(https?://[^\s,})?]+)\?[^\s,})]+""", RegexOption.IGNORE_CASE)
    private val SECRET_KV_REGEX = Regex(
        """(token|secret|password|apikey|api_key)\s*[:=]\s*[^\s,;&)}]+""",
        RegexOption.IGNORE_CASE
    )

    /**
     * 对写入用户构建日志的归档失败原因脱敏，剥离请求头中的密钥与 URL 中的临时上传 token。
     */
    fun sanitizeArchiveFailureReason(reason: String): String {
        val masked = reason
            .replace(HEADERS_BLOCK_REGEX, "headers=[<redacted>]")
            .replace(URL_QUERY_REGEX, "$1?<redacted>")
            .replace(SECRET_KV_REGEX, "$1=<redacted>")
            .replace(Regex("""\s+"""), " ")
            .trim()
        return if (masked.length <= MAX_ARCHIVE_REASON_LEN) {
            masked
        } else {
            masked.substring(0, MAX_ARCHIVE_REASON_LEN) + "..."
        }
    }
}
