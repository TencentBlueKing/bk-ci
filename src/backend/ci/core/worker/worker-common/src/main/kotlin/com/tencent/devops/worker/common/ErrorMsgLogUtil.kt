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

package com.tencent.devops.worker.common

import org.slf4j.LoggerFactory
import java.io.File

/**
 * 用于极端情况下无法上报给服务端信息时，写到一个特定日志文件中
 */
object ErrorMsgLogUtil {
    private const val CAPACITY = 2048

    private val logger = LoggerFactory.getLogger(ErrorMsgLogUtil::class.java)

    private val message = StringBuilder(CAPACITY) // 不存在并发场景，也可忍受并发不保护

    // 旧版的Agent没有AGENT_ERROR_MSG_FILE参数，所以不默认创建了，否则无法被清理。
    private fun getErrorFile(): File? = System.getProperty(AGENT_ERROR_MSG_FILE)?.let { filePath -> File(filePath) }

    fun init() {
        getErrorFile()?.let { file ->
            logger.info("initErrorMsgFile: ${file.absoluteFile}")
            file.writeText("")
        }
    }

    fun resetErrorMsg() {
        if (message.isNotBlank()) {
            logger.info("resetErrorMsg| $message")
        }
        message.clear()
    }

    fun appendErrorMsg(log: String) {
        if (message.length >= CAPACITY) {
            logger.info("appendErrorMsg_exceed| $log")
            return
        }
        message.append(log)
        message.append("\n")
        if (message.length > CAPACITY) {
            message.delete(CAPACITY, message.length)
        }
        logger.info("appendErrorMsg| $log")
    }

    /**
     * (覆盖)回写构建过程中的错误信息到文件中
     */
    fun flushErrorMsgToFile() {
        logger.info("flushErrorMsgToFile| $message")
        getErrorFile()?.writeText(message.toString())
        resetErrorMsg()
    }
}
