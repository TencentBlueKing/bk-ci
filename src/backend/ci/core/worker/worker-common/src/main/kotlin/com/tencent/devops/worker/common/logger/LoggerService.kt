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

package com.tencent.devops.worker.common.logger

import com.tencent.bkrepo.repository.pojo.token.TokenType
import com.tencent.devops.common.log.pojo.TaskBuildLogProperty
import com.tencent.devops.common.log.pojo.enums.LogStorageMode
import com.tencent.devops.common.log.pojo.enums.LogType
import com.tencent.devops.common.log.pojo.message.LogMessage
import com.tencent.devops.common.service.utils.ZipUtil
import com.tencent.devops.common.util.HttpRetryUtils
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.process.utils.PIPELINE_START_USER_ID
import com.tencent.devops.worker.common.LOG_DEBUG_FLAG
import com.tencent.devops.worker.common.LOG_ERROR_FLAG
import com.tencent.devops.worker.common.LOG_FILE_LENGTH_LIMIT
import com.tencent.devops.worker.common.LOG_MESSAGE_LENGTH_LIMIT
import com.tencent.devops.worker.common.LOG_SUBTAG_FINISH_FLAG
import com.tencent.devops.worker.common.LOG_SUBTAG_FLAG
import com.tencent.devops.worker.common.LOG_TASK_LINE_LIMIT
import com.tencent.devops.worker.common.LOG_UPLOAD_BUFFER_SIZE
import com.tencent.devops.worker.common.LOG_WARN_FLAG
import com.tencent.devops.worker.common.api.ApiFactory
import com.tencent.devops.worker.common.api.log.LogSDKApi
import com.tencent.devops.worker.common.env.AgentEnv
import com.tencent.devops.worker.common.service.RepoServiceFactory
import com.tencent.devops.worker.common.service.SensitiveValueService
import com.tencent.devops.worker.common.utils.ArchiveUtils
import com.tencent.devops.worker.common.utils.FileUtils
import com.tencent.devops.worker.common.utils.WorkspaceUtils
import org.slf4j.LoggerFactory
import java.io.File
import java.sql.Date
import java.text.SimpleDateFormat
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock

@Suppress("MagicNumber", "TooManyFunctions", "ComplexMethod", "LongMethod")
object LoggerService {

    private val logResourceApi = ApiFactory.create(LogSDKApi::class)
    private val logger = LoggerFactory.getLogger(LoggerService::class.java)
    private var future: Future<Boolean>? = null
    private val running = AtomicBoolean(true)
    private var currentTaskLineNo = 0
    private val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS")

    /**
     * 构建日志处理的异步线程池
     */
    private val executorService = Executors.newSingleThreadExecutor()

    /**
     * 缓冲区处理的异步线程池
     */
    private val flushExecutor = Executors.newSingleThreadExecutor()

    /**
     * 日志上报缓冲队列
     */
    private val uploadQueue = LinkedBlockingQueue<LogMessage>(2000)

    /**
     * 每个插件的日志存储属性映射
     */
    private val elementId2LogProperty = mutableMapOf<String, TaskBuildLogProperty>()

    /**
     * 当前执行插件的各类构建信息
     */
    var elementId = ""
    var elementName = ""
    var jobId = ""
    var executeCount = 1
    var buildVariables: BuildVariables? = null
    var pipelineLogDir: File? = null

    private val lock = ReentrantLock()

    private val logMessages = ArrayList<LogMessage>()

    private val loggerThread = Callable {
        try {
            var lastSaveTime: Long = 0
            while (running.get()) {
                val logMessage = try {
                    uploadQueue.poll(3, TimeUnit.SECONDS)
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
                if (size >= LOG_UPLOAD_BUFFER_SIZE || (size > 0 && (now - lastSaveTime > 3 * 1000))) {
                    flush()
                    lastSaveTime = now
                    currentTaskLineNo += size
                }
            }
            if (logMessages.isNotEmpty()) {
                flush()
            }
        } catch (ignored: Throwable) {
            logger.warn("Fail to send the logger", ignored)
        }
        logger.info("Finish the sending thread - (${uploadQueue.size})")
        true
    }

    private class FlushThread : Callable<Int> {
        override fun call(): Int {
            logger.info("Start to flush the logger")
            lock.lock()
            val size = logMessages.size
            try {
                if (size > 0) {
                    sendMultiLog()
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
        if (future == null) {
            logger.info("Start the log service")
            future = executorService.submit(loggerThread)
            addStopHook(loggerService = this)
        }
    }

    /**
     *  防止进程关闭时忘记停止，导致被hold住
     */
    private fun addStopHook(loggerService: LoggerService) {
        try {
            Runtime.getRuntime().addShutdownHook(object : Thread() {
                override fun run() = loggerService.stop()
            })
        } catch (ignore: Throwable) {
            logger.warn("Fail to add shutdown hook", ignore)
        }
    }

    fun flush(): Int {
        logger.info("Start to flush the log service")
        val future = flushExecutor.submit(FlushThread())
        return future.get()
    }

    @Suppress("NestedBlockDepth")
    fun stop() {
        try {
            logger.info("Start to stop the log service")
            if (this.running.get()) {
                this.running.set(false)
                if (future != null) {
                    future!!.get()
                }
                // 把没完成的日志打完
                while (uploadQueue.size != 0) {
                    uploadQueue.drainTo(logMessages)
                    if (logMessages.isNotEmpty()) {
                        flush()
                    }
                }
            }
            logger.info("Finish stopping the log service")
        } catch (ignored: Exception) {
            logger.error("Fail to stop log service for build", ignored)
        }
    }

    fun finishTask() = finishLog(elementId, jobId, executeCount)

    fun addNormalLine(message: String) {
        var subTag: String? = null
        var realMessage = message

        // #2342 处理插件内日志的前缀标签，进行日志分级
        if (message.contains(LOG_SUBTAG_FLAG)) {
            val prefix = message.substringBefore(LOG_SUBTAG_FLAG)
            val list = message.substringAfter(LOG_SUBTAG_FLAG).split(LOG_SUBTAG_FLAG)
            if (list.isNotEmpty()) {
                subTag = list.first()
                realMessage = list.last()
            }
            if (realMessage.startsWith(LOG_SUBTAG_FINISH_FLAG)) {
                finishLog(elementId, jobId, executeCount, subTag)
                realMessage = realMessage.removePrefix(LOG_SUBTAG_FINISH_FLAG)
            }
            realMessage = prefix + realMessage
        }
        val logType = when {
            realMessage.startsWith(LOG_DEBUG_FLAG) -> LogType.DEBUG
            realMessage.startsWith(LOG_ERROR_FLAG) -> LogType.ERROR
            realMessage.startsWith(LOG_WARN_FLAG) -> LogType.WARN
            else -> LogType.LOG
        }

        // #4273 敏感信息过滤，遍历所有敏感信息是否存在日志中
        realMessage = SensitiveValueService.fixSensitiveContent(realMessage)

        val logMessage = LogMessage(
            message = realMessage,
            timestamp = System.currentTimeMillis(),
            tag = elementId,
            subTag = subTag,
            jobId = jobId,
            logType = logType,
            executeCount = executeCount
        )
        logger.info(logMessage.toString())

        // #3772 如果已经进入Job执行任务，则可以做日志本地落盘
        if (elementId.isNotBlank() && pipelineLogDir != null) {
            saveLocalLog(logMessage)
        }

        try {
            if (currentTaskLineNo <= LOG_TASK_LINE_LIMIT) {
                var offset = 0
                // 上报前做长度等内容限制
                while (offset < logMessage.message.length) {
                    val chunk = logMessage.message.substring(
                        offset, minOf(offset + LOG_MESSAGE_LENGTH_LIMIT, logMessage.message.length)
                    )
                    this.uploadQueue.put(logMessage.copy(message = chunk))
                    offset += LOG_MESSAGE_LENGTH_LIMIT
                }
            } else if (elementId2LogProperty[elementId]?.logStorageMode != LogStorageMode.LOCAL) {
                logger.warn(
                    "The number of Task[$elementId] log lines exceeds the limit, " +
                        "the log file will be archived."
                )
                this.uploadQueue.put(
                    logMessage.copy(
                        message = "Printed logs cannot exceed 1 million lines. " +
                            "Please download logs to view."
                    )
                )
                elementId2LogProperty[elementId]?.logStorageMode = LogStorageMode.LOCAL
            }
        } catch (ignored: InterruptedException) {
            logger.error("Writing to a $logType log line failed：", ignored)
        }
    }

    fun addWarnLine(message: String) {
        // 修复换行后无法通过前缀渲染颜色的问题
        val msg = "$LOG_WARN_FLAG$message"
        addNormalLine(msg.replace("\n", "\n$LOG_WARN_FLAG"))
    }

    fun addErrorLine(message: String) {
        // 修复换行后无法通过前缀渲染颜色的问题
        val msg = "$LOG_ERROR_FLAG$message"
        addNormalLine(msg.replace("\n", "\n$LOG_ERROR_FLAG"))
    }

    fun addDebugLine(message: String) {
        // 修复换行后无法通过前缀渲染颜色的问题
        val msg = "$LOG_DEBUG_FLAG$message"
        addNormalLine(msg.replace("\n", "\n$LOG_DEBUG_FLAG"))
    }

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

    fun archiveLogFiles() {
        logger.info("Start to archive log files with LogMode[${AgentEnv.getLogMode()}]")
        try {
            val expireSeconds = buildVariables!!.timeoutMills / 1000
            val token = RepoServiceFactory.getInstance().getRepoToken(
                userId = buildVariables!!.variables[PIPELINE_START_USER_ID] ?: "",
                projectId = buildVariables!!.projectId,
                repoName = "log",
                path = "/",
                type = TokenType.UPLOAD,
                expireSeconds = expireSeconds
            )
            var archivedCount = 0
            // 将所有日志存储状态为LOCAL的插件进行文件归档
            elementId2LogProperty.forEach { (elementId, property) ->
                // 如果不是LOCAL状态直接跳过
                if (property.logStorageMode != LogStorageMode.LOCAL) return@forEach

                if (!property.logFile.exists()) {
                    logger.warn(
                        "Cancel archiving task[$elementId] build log " +
                            "file(${property.logFile.absolutePath}) which not exists"
                    )
                    return@forEach
                }

                val zipLog = ZipUtil.zipDir(property.logFile, property.logFile.absolutePath + ".zip")
                // 如果日志文件过大，则取消归档
                if (zipLog.length() > LOG_FILE_LENGTH_LIMIT) {
                    logger.warn(
                        "Cancel archiving task[$elementId] build log " +
                            "file(${property.logFile.absolutePath}), length(${property.logFile.length()})"
                    )
                    return@forEach
                }
                // 开始归档符合归档条件的日志文件
                logger.info("Archive task[$elementId] build log file(${property.logFile.absolutePath})")
                try {
                    HttpRetryUtils.retry(
                        retryTime = 5,
                        retryPeriodMills = 1000
                    ) {
                        ArchiveUtils.archiveLogFile(
                            file = zipLog,
                            destFullPath = property.childZipPath!!,
                            buildVariables = buildVariables!!,
                            token = token
                        )
                    }
                    property.logStorageMode = LogStorageMode.ARCHIVED
                } catch (ignore: Exception) {
                    logger.error("archiveLogFile| retry fail with message: ", ignore)
                }
                archivedCount++
            }
            logger.info("Finished archiving log $archivedCount files")

            // 同步所有存储状态到log服务端
            logResourceApi.updateStorageMode(elementId2LogProperty.values.toList(), executeCount)
            logger.info("Finished update mode to log service.")
        } catch (ignored: Throwable) {
            logger.warn("Fail to archive log files", ignored)
        } finally {
            logger.info("Remove temp log files in [$pipelineLogDir].")
            FileUtils.deleteRecursivelyOnExit(pipelineLogDir!!)
        }
    }

    private fun addLog(message: LogMessage) = uploadQueue.put(message)

    private fun sendMultiLog() {
        try {
            logger.info("Start to save the log - ${logMessages.size}")

            // 如果agent启动时日志模式为本地保存，则不做上报
            if (LogStorageMode.LOCAL == AgentEnv.getLogMode()) {
                return
            }

            // 通过上报的结果感知是否需要调整模式
            val result = logResourceApi.addLogMultiLine(buildVariables?.buildId ?: "", logMessages)
            when {
                // 当log服务返回拒绝请求或者并发量超限制时，自动切换模式为本地保存并归档
                result.status == 503 || result.status == 509 -> {
                    logger.warn("Log service storage is unable：${result.message}")
                    disableLogUpload()
                }
                result.isNotOk() -> {
                    logger.error("Fail to send the multi logs：${result.message}")
                }
            }
        } catch (ignored: Exception) {
            logger.warn("Fail to send the logs(${logMessages.size})", ignored)
        }
    }

    private fun saveLocalLog(logMessage: LogMessage) {
        try {
            // 必要的本地保存
            var logProperty = elementId2LogProperty[elementId]
            if (null == logProperty) {
                logProperty = WorkspaceUtils.getBuildLogProperty(
                    pipelineLogDir = pipelineLogDir!!,
                    pipelineId = buildVariables?.pipelineId!!,
                    buildId = buildVariables?.buildId!!,
                    elementId = elementId,
                    executeCount = executeCount,
                    logStorageMode = AgentEnv.getLogMode()
                )
                logger.info("Create new build log file(${logProperty.logFile.absolutePath})")
                elementId2LogProperty[elementId] = logProperty
            }
            val dateTime = sdf.format(Date(logMessage.timestamp))
            logProperty.logFile.appendText("$dateTime : ${logMessage.message}\n")
        } catch (ignored: Exception) {
            logger.warn("Fail to save the logs($logMessage)", ignored)
        }
    }

    private fun finishLog(
        tag: String?,
        jobId: String?,
        executeCount: Int?,
        subTag: String? = null
    ) {
        try {
            currentTaskLineNo = 0
            logger.info("Start to finish the log, property: ${elementId2LogProperty[tag]}")
            val result = logResourceApi.finishLog(
                tag = tag,
                jobId = jobId,
                executeCount = executeCount,
                subTag = subTag,
                logMode = elementId2LogProperty[tag]?.logStorageMode
            )
            if (result.isNotOk()) {
                logger.error("Fail to send the log status ：${result.message}")
            }
        } catch (ignored: Exception) {
            logger.warn("Fail to finish the logs", ignored)
        }
    }

    private fun disableLogUpload() {
        // 将已有任务的日志模式都设为本地保存
        elementId2LogProperty.forEach { (elementId, property) ->
            logger.warn("Set Task[$elementId] logMode to ${LogStorageMode.LOCAL.name}")
            property.logStorageMode = LogStorageMode.LOCAL
        }
        // 将全局日志模式设为本地保存
        logger.warn("Set AgentEnv logMode to ${LogStorageMode.LOCAL.name}")
        AgentEnv.setLogMode(LogStorageMode.LOCAL)
    }
}
