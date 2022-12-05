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

package com.tencent.bkrepo.common.storage.innercos.response.handler

import com.tencent.bkrepo.common.api.util.HumanReadable
import com.tencent.bkrepo.common.storage.innercos.http.Headers
import com.tencent.bkrepo.common.storage.innercos.http.HttpResponseHandler
import okhttp3.Request
import okhttp3.Response
import org.apache.commons.logging.LogFactory
import java.time.Duration

class SlowLogHandler<T>(
    private val handler: HttpResponseHandler<T>,
    private val slowLogSpeed: Int,
    private val slowLogTime: Long,
    private val ignoreFileSize: Long = -1
) : HttpResponseHandler<T>() {
    private val beginTime = System.currentTimeMillis()
    override fun handle(response: Response): T {
        try {
            if (slowLogSpeed <= 0 && slowLogTime <= 0L) {
                return handler.handle(response)
            }
            val endTime = System.currentTimeMillis()
            val request = response.request()
            // 时间定义慢日志
            if (slowLogTime > 0 && endTime - beginTime > slowLogTime) {
                val requestTime = HumanReadable.time(Duration.ofMillis(endTime - beginTime).toNanos())
                val detail = buildMessage(request, response)
                logger.warn("[slowLog] handler ${handler::class.simpleName} elapse: $requestTime, detail: $detail")
            }
            val length = request.header(Headers.CONTENT_LENGTH)?.toLong() ?: 0
            // 速度定义慢日志
            if (slowLogSpeed > 0 && length > ignoreFileSize && getSpeed(endTime - beginTime, length) < slowLogSpeed) {
                val nanos = Duration.ofMillis(endTime - beginTime).toNanos()
                val detail = buildMessage(request, response)
                val throughput = HumanReadable.throughput(length, nanos)
                logger.warn("[slowLog] handler ${handler::class.simpleName} average:$throughput: $detail")
            }
        } catch (e: Exception) {
            logger.error("SlowLog record error", e)
        }
        return handler.handle(response)
    }

    /**
     * 计算上传速度
     * @param time 用时，单位ms
     * @param size 传输大小,单位byte
     * */
    private fun getSpeed(time: Long, size: Long): Int {
        return (size / time * 1000).toInt()
    }

    private fun buildMessage(request: Request, response: Response? = null): String {
        val requestTitle = "${request.method()} ${request.url()} ${response?.protocol()}"

        val builder = StringBuilder()
            .append("\n>>>> ")
            .appendln(requestTitle)
            .appendln(request.headers())

        if (response != null) {
            builder.append("<<<< ")
                .append(requestTitle)
                .append(response.code())
                .appendln("[${response.message()}]")
                .appendln(response.headers())
        }
        return builder.toString()
    }

    companion object {
        private val logger = LogFactory.getLog(SlowLogHandler::class.java)
    }
}
