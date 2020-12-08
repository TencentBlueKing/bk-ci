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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.worker.common.logger

import com.tencent.devops.common.log.Ansi
import com.tencent.devops.common.log.pojo.message.LogMessage
import com.tencent.devops.common.log.pojo.enums.LogType
import com.tencent.devops.worker.common.LOG_SUBTAG_FINISH_FLAG
import com.tencent.devops.worker.common.LOG_SUBTAG_FLAG
import com.tencent.devops.worker.common.api.ApiFactory
import com.tencent.devops.worker.common.api.log.LogSDKApi
import org.slf4j.LoggerFactory
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock

object LoggerService {

    private val logResourceApi = ApiFactory.create(LogSDKApi::class)

    private val executorService = Executors.newSingleThreadExecutor()
    private val flushExecutor = Executors.newSingleThreadExecutor()
    private val queue = LinkedBlockingQueue<LogMessage>(2000)
    private val logger = LoggerFactory.getLogger(LoggerService::class.java)
    private val running = AtomicBoolean(true)
    private var future: Future<Boolean>? = null
    // 当前执行的插件id
    var elementId = ""
    var jobId = ""
    var executeCount = 1

    private val lock = ReentrantLock()

    private val logMessages = ArrayList<LogMessage>()

    private val loggerThread = Callable {
        try {
            var lastSaveTime: Long = 0
            while (running.get()) {
                val logMessage = try {
                    queue.poll(3, TimeUnit.SECONDS)
                } catch (e: InterruptedException) {
                    logger.warn("Logger service poll thread interrupted", e)
                    null
                }
                lock.lock()
                try {
                    if (logMessage != null) {
                        logMessages.add(logMessage)
                    }
                } finally {
                    lock.unlock()
                }

                val size = logMessages.size
                val now = System.currentTimeMillis()
                // 缓冲大于200条或上次保存时间超过3秒
                if (size >= 200 || (size > 0 && (now - lastSaveTime > 3 * 1000))) {
                    flush()
                    lastSaveTime = now
                }
            }
            if (logMessages.isNotEmpty()) {
                flush()
            }
        } catch (t: Throwable) {
            logger.warn("Fail to send the logger", t)
        }
        logger.info("Finish the sending thread - (${queue.size})")
        true
    }

    private class FlushThread : Callable<Int> {
        override fun call(): Int {
            logger.info("Start to flush the logger")
            lock.lock()
            val size = logMessages.size
            try {
                if (size > 0) {
                    sendMultiLog(logMessages)
                    logMessages.clear()
                }
            } finally {
                logger.info("Finish flush the log - $size")
                lock.unlock()
            }
            return size
        }
    }

    fun start() {
        logger.info("Start the log service")
        future = executorService.submit(loggerThread)
    }

    fun flush(): Int {
        logger.info("Start to flush the log service")
        val future = flushExecutor.submit(FlushThread())
        return future.get()
    }

    fun stop() {
        try {
            logger.info("Start to stop the log service")
            if (this.running.get()) {
                this.running.set(false)
                if (future != null) {
                    future!!.get()
                }
                // 把没完成的日志打完
                while (queue.size != 0) {
                    queue.drainTo(logMessages)
                    if (logMessages.isNotEmpty()) {
                        flush()
                    }
                }
            }
            logger.info("Finish stopping the log service")
        } catch (e: Exception) {
            logger.error("Fail to stop log service for build", e)
        }
    }

    fun finishTask() = finishLog(elementId, jobId, executeCount)

    fun addNormalLine(message: String) {
        var subTag: String? = null
        var realMessage = message
        if (message.startsWith(LOG_SUBTAG_FLAG)) {
            val list = message.removePrefix(LOG_SUBTAG_FLAG).split(LOG_SUBTAG_FLAG)
            subTag = list.first()
            realMessage = list.last()
            if (realMessage.startsWith(LOG_SUBTAG_FINISH_FLAG)) {
                finishLog(elementId, jobId, executeCount, subTag)
                realMessage = realMessage.removePrefix(LOG_SUBTAG_FINISH_FLAG)
            }
        }

        val logMessage = LogMessage(
            message = realMessage,
            timestamp = System.currentTimeMillis(),
            tag = elementId,
            subTag = subTag,
            jobId = jobId,
            logType = LogType.LOG,
            executeCount = executeCount
        )
        logger.info(logMessage.toString())
        try {
            this.queue.put(logMessage)
        } catch (e: InterruptedException) {
            logger.error("写入普通日志行失败：", e)
        }
    }

    fun addYellowLine(message: String) =
        addNormalLine(Ansi().fgYellow().a(message).reset().toString())

    fun addRedLine(message: String) =
        addNormalLine(Ansi().fgRed().a(message).reset().toString())

    fun addFoldStartLine(foldName: String) {
        val logMessage = LogMessage(
            message = "##[group]$foldName",
            timestamp = System.currentTimeMillis(),
            tag = elementId,
            jobId = jobId,
            logType = LogType.LOG,
            executeCount = executeCount
        )
        addLog(logMessage)
    }

    fun addFoldEndLine(foldName: String) {
        val logMessage = LogMessage(
            message = "##[endgroup]$foldName",
            timestamp = System.currentTimeMillis(),
            tag = elementId,
            jobId = jobId,
            logType = LogType.LOG,
            executeCount = executeCount
        )
        addLog(logMessage)
    }

    fun addRangeStartLine(rangeName: String) {
        val logMessage = LogMessage(
            message = "[START] $rangeName",
            timestamp = System.currentTimeMillis(),
            tag = elementId,
            jobId = jobId,
            logType = LogType.START,
            executeCount = executeCount
        )
        addLog(logMessage)
    }

    fun addRangeEndLine(rangeName: String) {
        val logMessage = LogMessage(
            message = "[END] $rangeName",
            timestamp = System.currentTimeMillis(),
            tag = elementId,
            jobId = jobId,
            logType = LogType.END,
            executeCount = executeCount
        )
        addLog(logMessage)
    }

    private fun addLog(message: LogMessage) = queue.put(message)

    private fun sendMultiLog(logMessages: List<LogMessage>) {
        try {
            logger.info("Start to send the log - ${logMessages.size}")
            val result = logResourceApi.addLogMultiLine(logMessages)
            if (result.isNotOk()) {
                logger.error("发送构建日志失败：${result.message}")
            }
        } catch (e: Exception) {
            logger.warn("Fail to send the logs(${logMessages.size})", e)
        }
    }

    private fun finishLog(tag: String?, jobId: String?, executeCount: Int?, subTag: String? = null) {
        try {
            logger.info("Start to finish the log")
            val result = logResourceApi.finishLog(tag, jobId, executeCount, subTag)
            if (result.isNotOk()) {
                logger.error("上报日志状态日志失败：${result.message}")
            }
        } catch (e: Exception) {
            logger.warn("Fail to finish the logs", e)
        }
    }
}
