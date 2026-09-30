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
import com.tencent.devops.worker.common.LOG_WARN_FLAG
import com.tencent.devops.worker.common.api.ApiFactory
import com.tencent.devops.worker.common.api.archive.ArchiveSDKApi
import com.tencent.devops.worker.common.api.log.LogSDKApi
import com.tencent.devops.worker.common.env.AgentEnv
import com.tencent.devops.worker.common.service.SensitiveValueService
import com.tencent.devops.worker.common.utils.ArchiveUtils
import com.tencent.devops.worker.common.utils.FileUtils
import com.tencent.devops.worker.common.utils.WorkspaceUtils
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.net.SocketTimeoutException
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import org.slf4j.LoggerFactory

@Suppress("MagicNumber", "TooManyFunctions", "ComplexMethod", "LongMethod")
object LoggerService {

    private const val BATCH_RECOVER_COOLDOWN_MS = 30_000L
    private const val FINISH_FLUSH_ROUNDS = 3
    private const val ARCHIVE_FAIL_NOTICE =
        "日志归档到制品库失败，完整日志未能保存。下载只能获取已上报到日志服务的部分内容。"

    private val logResourceApi = ApiFactory.create(LogSDKApi::class)
    private val archiveApi = ApiFactory.create(ArchiveSDKApi::class)
    private val logger = LoggerFactory.getLogger(LoggerService::class.java)
    private var future: Future<Boolean>? = null
    private val running = AtomicBoolean(true)
    private var currentTaskLineNo = 0
    private val localLogTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss:SSS")
        .withZone(ZoneId.systemDefault())
    private val circuitBreakerRegistry = CircuitBreakerRegistry.of(
        CircuitBreakerConfig.custom()
            .enableAutomaticTransitionFromOpenToHalfOpen()
            .writableStackTraceEnabled(false)
            // 当熔断后等待 60s 放开熔断，避免短暂超时后长时间丢日志
            .waitDurationInOpenState(Duration.ofSeconds(60))
            // 熔断放开后，运行通过的请求数，如果达到熔断条件，继续熔断
            .permittedNumberOfCallsInHalfOpenState(100)
            // 当错误率达到 10% 开启熔断
            .failureRateThreshold(10.0F)
            // 慢请求超过 10% 开启熔断；阈值对齐读超时，避免成功但偏慢的上报被算成 slow
            .slowCallRateThreshold(10.0F)
            .slowCallDurationThreshold(Duration.ofSeconds(LoggerUploadBatch.READ_TIMEOUT_SECONDS))
            // 滑动窗口大小为 100，默认值
            .slidingWindowSize(100)
            .build()
    )

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
     * 单次上报条数上限，默认 [LoggerUploadBatch.MAX_COUNT]。超时后降到
     * [LoggerUploadBatch.MIN_COUNT]，冷却后再加倍恢复。发送时还会按预估字节切批。
     */
    private val uploadBatchSize = AtomicInteger(LoggerUploadBatch.MAX_COUNT)
    private val lastUploadTimeoutAt = AtomicLong(0)
    private val localLogWriters = ConcurrentHashMap<String, BufferedWriter>()

    /**
     * 每个插件的日志存储属性映射
     */
    private val elementId2LogProperty = mutableMapOf<String, TaskBuildLogProperty>()

    /**
     * 当前执行插件的各类构建信息
     *
     * 注意：这些字段是Job级别的兜底值，仅在没有[threadLocalContext]时使用。
     * 真正的"当前插件"应优先从[threadLocalContext]中读取，避免主循环切换插件后，
     * 上一个插件残留的pump线程把日志打到下一个插件名下。
     */
    var elementId = ""
    var stepId = ""
    var elementName = ""
    var containerHashId = ""
    var jobId = ""
    var executeCount = 1
    var buildVariables: BuildVariables? = null
    var pipelineLogDir: File? = null
    var loggingLineLimit: Int = LOG_TASK_LINE_LIMIT

    /**
     * 单个插件执行期间的不变上下文，仅供日志归属使用。
     *
     * 由[com.tencent.devops.worker.common.task.TaskDaemon.call]在插件执行线程入口
     * 设置、出口清理，通过[InheritableThreadLocal]使插件派生的子线程（如commons-exec
     * 的PumpStreamHandler）自动继承，从而保证残余/异步日志始终以正确的elementId上报。
     */
    data class TaskExecutionContext(
        val elementId: String,
        val stepId: String,
        val elementName: String,
        val containerHashId: String,
        val jobId: String,
        val executeCount: Int
    )

    private val threadLocalContext = InheritableThreadLocal<TaskExecutionContext?>()

    /**
     * 由插件执行线程入口调用，绑定当前线程及其后续派生子线程的日志归属。
     */
    fun setTaskContext(ctx: TaskExecutionContext) {
        threadLocalContext.set(ctx)
    }

    /**
     * 由插件执行线程出口调用，清理本线程绑定的上下文。
     */
    fun clearTaskContext() {
        threadLocalContext.remove()
    }

    /**
     * 获取当前生效的日志上下文：优先取线程绑定值，无则回退到Job级单例字段。
     */
    private fun effectiveContext(): TaskExecutionContext {
        return threadLocalContext.get() ?: TaskExecutionContext(
            elementId = elementId,
            stepId = stepId,
            elementName = elementName,
            containerHashId = containerHashId,
            jobId = jobId,
            executeCount = executeCount
        )
    }

    private val lock = ReentrantLock()

    private val logMessages = ArrayList<LogMessage>()

    private val loggerThread = Callable {
        try {
            var lastSaveTime: Long = 0
            while (running.get()) {
                val logMessage = try {
                    uploadQueue.poll(1, TimeUnit.SECONDS)
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
                if (!isUploadDisabled() && size >= LoggerUploadBatch.MAX_PENDING_IN_MEMORY) {
                    logger.warn(
                        "Pending logs $size exceed memory cap ${LoggerUploadBatch.MAX_PENDING_IN_MEMORY}, " +
                            "switch to LOCAL"
                    )
                    disableLogUpload()
                }
                val now = System.currentTimeMillis()
                // 达到当前上报条数或距上次保存超过刷新间隔
                if (shouldFlushPendingLogs(size = size, now = now, lastSaveTime = lastSaveTime)) {
                    val sent = flush()
                    lastSaveTime = now
                    currentTaskLineNo += sent
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
            val snapshot = lock.withLock {
                if (logMessages.isEmpty()) {
                    emptyList()
                } else {
                    ArrayList(logMessages)
                }
            }
            if (snapshot.isEmpty()) {
                logger.info("Finish flush the log - size=0 sent=0")
                return 0
            }
            val sent = sendMultiLog(snapshot)
            if (sent > 0) {
                lock.withLock {
                    logMessages.subList(0, sent.coerceAtMost(logMessages.size)).clear()
                }
            }
            if (sent < snapshot.size) {
                logger.warn("Keep ${logMessages.size} unsent logs after flush (sent=$sent/${snapshot.size})")
            }
            flushAllLocalLogWriters()
            logger.info("Finish flush the log - size=${snapshot.size} sent=$sent")
            return sent
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
                flushUntilIdleOrLocal(tag = null, disableAllOnGiveUp = true)
            }
            logger.info("Finish stopping the log service")
        } catch (ignored: Exception) {
            logger.error("Fail to stop log service for build", ignored)
        } finally {
            closeAllLocalLogWriters()
        }
    }

    fun finishTask() {
        val ctx = effectiveContext()
        finishLog(ctx.elementId, ctx.containerHashId, ctx.executeCount)
    }

    fun addNormalLine(message: String) {
        val ctx = effectiveContext()
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
                finishLog(ctx.elementId, ctx.containerHashId, ctx.executeCount, subTag)
                realMessage = realMessage.removePrefix(LOG_SUBTAG_FINISH_FLAG)
            }
            realMessage = prefix + realMessage
        }
        // 脚本 stdout/stderr 在 CommandLineExecutor 已合并，不再按 fd 标 ERROR。
        // 分级只认行前缀（与 java-plugin-sdk SimpleLogger 的 ##[error] 等约定一致）。
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
            tag = ctx.elementId,
            subTag = subTag,
            containerHashId = ctx.containerHashId,
            logType = logType,
            executeCount = ctx.executeCount,
            jobId = ctx.jobId,
            stepId = ctx.stepId
        )
        logger.info(logMessage.toString())
        // #3772 如果已经进入Job执行任务，则可以做日志本地落盘
        if (ctx.elementId.isNotBlank() && pipelineLogDir != null) {
            saveLocalLog(ctx.elementId, ctx.executeCount, logMessage)
        }

        if (currentTaskLineNo <= loggingLineLimit) {
            var offset = 0
            // 上报前做长度等内容限制
            while (offset < logMessage.message.length) {
                val chunk = logMessage.message.substring(
                    offset, minOf(offset + LOG_MESSAGE_LENGTH_LIMIT, logMessage.message.length)
                )
                enqueueLog(logMessage.copy(message = chunk))
                offset += LOG_MESSAGE_LENGTH_LIMIT
            }
        } else if (elementId2LogProperty[ctx.elementId]?.logStorageMode != LogStorageMode.LOCAL) {
            logger.warn(
                "The number of Task[${ctx.elementId}] log lines exceeds the limit, " +
                    "the log file will be archived."
            )
            enqueueLog(
                logMessage.copy(
                    message = "Printed logs cannot exceed $loggingLineLimit lines. " +
                        "Please download logs to view.",
                    logType = LogType.WARN
                )
            )
            elementId2LogProperty[ctx.elementId]?.logStorageMode = LogStorageMode.LOCAL
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
        val ctx = effectiveContext()
        val logMessage = LogMessage(
            message = "##[group]$foldName",
            timestamp = System.currentTimeMillis(),
            tag = ctx.elementId,
            containerHashId = ctx.containerHashId,
            logType = LogType.LOG,
            executeCount = ctx.executeCount,
            jobId = ctx.jobId,
            stepId = ctx.stepId
        )
        addLog(logMessage)
    }

    fun addFoldEndLine(foldName: String) {
        val ctx = effectiveContext()
        val logMessage = LogMessage(
            message = "##[endgroup]$foldName",
            timestamp = System.currentTimeMillis(),
            tag = ctx.elementId,
            containerHashId = ctx.containerHashId,
            logType = LogType.LOG,
            executeCount = ctx.executeCount,
            jobId = ctx.jobId,
            stepId = ctx.stepId
        )
        addLog(logMessage)
    }

    fun archiveLogFiles() {
        logger.info("Start to archive log files with LogMode[${AgentEnv.getLogMode()}]")
        closeAllLocalLogWriters()
        try {
            val expireSeconds = buildVariables!!.timeoutMills / 1000
            val token = archiveApi.getRepoToken(
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
                    markArchiveFailed(property, "local log file does not exist")
                    return@forEach
                }

                val zipLog = ZipUtil.zipDir(property.logFile, property.logFile.absolutePath + ".zip")
                // 如果日志文件过大，则取消归档
                if (zipLog.length() > LOG_FILE_LENGTH_LIMIT) {
                    logger.warn(
                        "Cancel archiving task[$elementId] build log " +
                            "file(${property.logFile.absolutePath}), length(${property.logFile.length()})"
                    )
                    markArchiveFailed(
                        property,
                        "zip size ${zipLog.length()} exceeds $LOG_FILE_LENGTH_LIMIT"
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
                    markArchiveFailed(property, ignore.message ?: ignore.javaClass.simpleName)
                }
                archivedCount++
            }
            logger.info("Finished archiving log $archivedCount files")
            syncStorageModeToLogService()
            logger.info("Finished update mode to log service.")
        } catch (ignored: Throwable) {
            logger.warn("Fail to archive log files", ignored)
            elementId2LogProperty.values
                .filter { it.logStorageMode == LogStorageMode.LOCAL }
                .forEach { markArchiveFailed(it, ignored.message ?: "archive aborted") }
            try {
                syncStorageModeToLogService()
            } catch (ignore: Exception) {
                logger.warn("Fail to sync archive-failed mode after archive abort", ignore)
            }
        } finally {
            logger.info("Remove temp log files in [$pipelineLogDir].")
            FileUtils.deleteRecursivelyOnExit(pipelineLogDir!!)
        }
    }

    private fun addLog(message: LogMessage) = enqueueLog(message)

    private fun isUploadDisabled(): Boolean = LogStorageMode.LOCAL == AgentEnv.getLogMode()

    private fun shouldFlushPendingLogs(size: Int, now: Long, lastSaveTime: Long): Boolean {
        if (size <= 0) {
            return false
        }
        if (size >= uploadBatchSize.get()) {
            return true
        }
        if (now - lastSaveTime > LoggerUploadBatch.FLUSH_INTERVAL_MS) {
            return true
        }
        return isUploadDisabled()
    }

    private fun shouldSkipUpload(tag: String): Boolean {
        if (isUploadDisabled()) {
            return true
        }
        val mode = elementId2LogProperty[tag]?.logStorageMode
        return tag.isNotBlank() && (mode == LogStorageMode.LOCAL || mode == LogStorageMode.ARCHIVE_FAILED)
    }

    private fun enqueueLog(message: LogMessage) {
        if (shouldSkipUpload(message.tag)) {
            return
        }
        try {
            if (uploadQueue.offer(
                    message,
                    LoggerUploadBatch.QUEUE_OFFER_TIMEOUT_MS,
                    TimeUnit.MILLISECONDS
                )
            ) {
                return
            }
        } catch (ignored: InterruptedException) {
            logger.error("Writing to upload queue interrupted", ignored)
            Thread.currentThread().interrupt()
            return
        }
        logger.warn(
            "Log upload queue is full (size=${uploadQueue.size}), " +
                "keep local file only and switch Task[${message.tag}] to LOCAL"
        )
        if (message.tag.isNotBlank()) {
            elementId2LogProperty[message.tag]?.logStorageMode = LogStorageMode.LOCAL
        }
    }

    private fun sendMultiLog(messages: List<LogMessage>): Int {
        logger.info("Start to save the log - ${messages.size}")

        // 本地模式不上报，视为已处理，避免缓冲区无限堆积
        if (isUploadDisabled()) {
            return messages.size
        }

        val batchSize = uploadBatchSize.get().coerceAtLeast(LoggerUploadBatch.MIN_COUNT)
        var index = 0
        while (index < messages.size) {
            val end = LoggerUploadBatch.nextChunkEnd(messages, index, batchSize)
            if (!sendLogChunk(messages.subList(index, end))) {
                break
            }
            index = end
        }
        return index
    }

    private fun sendLogChunk(chunk: List<LogMessage>): Boolean {
        try {
            // 通过上报的结果感知是否需要调整模式。
            // projectId 由 AbstractBuildResourceApi 自动带上 X-DEVOPS-PROJECT-ID（AgentEnv.getProjectId()），
            // pipelineId 由同一处可选带上 X-DEVOPS-PIPELINE-ID；旧 log 服务忽略未知 header，新旧可任意顺序发布。
            val result = doWithCircuitBreaker {
                logResourceApi.addLogMultiLine(buildVariables?.buildId ?: "", chunk)
            }
            when {
                // 当log服务返回拒绝请求或者并发量超限制时，自动切换模式为本地保存并归档
                result.status == 503 || result.status == 509 -> {
                    logger.warn("Log service storage is unable：${result.message}")
                    disableLogUpload()
                    return false
                }

                result.isNotOk() -> {
                    logger.error("Fail to send the multi logs：${result.message}")
                    return false
                }
            }
            recoverUploadBatch()
            return true
        } catch (ignored: CallNotPermittedException) {
            logger.warn("Log upload circuit is open, switch to LOCAL to stop memory growth")
            disableLogUpload()
            return false
        } catch (ignored: Exception) {
            logger.warn("Fail to send the logs(${chunk.size})", ignored)
            if (isTimeout(ignored)) {
                shrinkUploadBatch(chunk.size)
            }
            return false
        }
    }

    private fun shrinkUploadBatch(failedSize: Int) {
        lastUploadTimeoutAt.set(System.currentTimeMillis())
        val current = uploadBatchSize.get()
        if (current > LoggerUploadBatch.MIN_COUNT &&
            uploadBatchSize.compareAndSet(current, LoggerUploadBatch.MIN_COUNT)
        ) {
            logger.warn(
                "Log upload timed out (size=$failedSize), " +
                    "shrink batch $current -> ${LoggerUploadBatch.MIN_COUNT}"
            )
        }
    }

    private fun recoverUploadBatch() {
        val elapsed = System.currentTimeMillis() - lastUploadTimeoutAt.get()
        if (lastUploadTimeoutAt.get() > 0 && elapsed < BATCH_RECOVER_COOLDOWN_MS) {
            return
        }
        val current = uploadBatchSize.get()
        if (current < LoggerUploadBatch.MAX_COUNT) {
            val next = minOf(LoggerUploadBatch.MAX_COUNT, current * 2)
            if (uploadBatchSize.compareAndSet(current, next)) {
                logger.info("Log upload batch recovered to $next")
            }
        }
    }

    private fun isTimeout(error: Throwable): Boolean {
        var current: Throwable? = error
        while (current != null) {
            if (current is SocketTimeoutException) {
                return true
            }
            current = current.cause
        }
        return false
    }

    private fun saveLocalLog(taskId: String, taskExecuteCount: Int, logMessage: LogMessage) {
        try {
            // 必要的本地保存
            var logProperty = elementId2LogProperty[taskId]
            if (null == logProperty) {
                logProperty = WorkspaceUtils.getBuildLogProperty(
                    pipelineLogDir = pipelineLogDir!!,
                    pipelineId = buildVariables?.pipelineId!!,
                    buildId = buildVariables?.buildId!!,
                    elementId = taskId,
                    executeCount = taskExecuteCount,
                    logStorageMode = AgentEnv.getLogMode()
                )
                logger.info("Create new build log file(${logProperty.logFile.absolutePath})")
                elementId2LogProperty[taskId] = logProperty
            }
            val dateTime = localLogTimeFormatter.format(Instant.ofEpochMilli(logMessage.timestamp))
            val writer = writerFor(taskId, taskExecuteCount, logProperty.logFile)
            synchronized(writer) {
                writer.write("$dateTime : ${logMessage.message}\n")
            }
        } catch (ignored: Exception) {
            logger.warn("Fail to save the logs($logMessage)", ignored)
        }
    }

    private fun writerKey(taskId: String, taskExecuteCount: Int) = "$taskId:$taskExecuteCount"

    private fun writerFor(taskId: String, taskExecuteCount: Int, logFile: File): BufferedWriter {
        return localLogWriters.computeIfAbsent(writerKey(taskId, taskExecuteCount)) {
            BufferedWriter(
                OutputStreamWriter(FileOutputStream(logFile, true), StandardCharsets.UTF_8),
                LoggerUploadBatch.LOCAL_LOG_BUFFER_BYTES
            )
        }
    }

    private fun flushAllLocalLogWriters() {
        localLogWriters.values.forEach { writer ->
            try {
                synchronized(writer) {
                    writer.flush()
                }
            } catch (ignored: Exception) {
                logger.warn("Fail to flush local log writer", ignored)
            }
        }
    }

    private fun flushLocalLogWriter(taskId: String?) {
        if (taskId.isNullOrBlank()) {
            return
        }
        val prefix = "$taskId:"
        localLogWriters.forEach { (key, writer) ->
            if (!key.startsWith(prefix)) {
                return@forEach
            }
            try {
                synchronized(writer) {
                    writer.flush()
                }
            } catch (ignored: Exception) {
                logger.warn("Fail to flush local log writer $key", ignored)
            }
        }
    }

    private fun closeAllLocalLogWriters() {
        val keys = localLogWriters.keys.toList()
        keys.forEach { key ->
            val writer = localLogWriters.remove(key) ?: return@forEach
            try {
                synchronized(writer) {
                    writer.flush()
                    writer.close()
                }
            } catch (ignored: Exception) {
                logger.warn("Fail to close local log writer $key", ignored)
            }
        }
    }

    private fun discardPendingLogs() {
        lock.withLock {
            uploadQueue.clear()
            logMessages.clear()
        }
    }

    private fun flushUntilIdleOrLocal(tag: String?, disableAllOnGiveUp: Boolean) {
        if (isUploadDisabled()) {
            discardPendingLogs()
            return
        }
        repeat(FINISH_FLUSH_ROUNDS) { round ->
            lock.withLock {
                uploadQueue.drainTo(logMessages)
            }
            if (uploadQueue.isEmpty() && logMessages.isEmpty()) {
                return
            }
            if (isUploadDisabled()) {
                discardPendingLogs()
                return
            }
            val pending = logMessages.size
            val sent = if (pending > 0) flush() else 0
            if (uploadQueue.isEmpty() && logMessages.isEmpty()) {
                return
            }
            if (sent <= 0 && logMessages.isNotEmpty()) {
                logger.warn("Finish flush round ${round + 1} sent 0, still ${logMessages.size} pending")
                Thread.sleep(500)
            } else if (sent < pending) {
                logger.warn("Finish flush round ${round + 1} sent=$sent/$pending")
            }
        }
        if (uploadQueue.isEmpty() && logMessages.isEmpty()) {
            return
        }
        logger.warn(
            "Unsent logs remain after finish flush (queue=${uploadQueue.size}, " +
                "buffer=${logMessages.size}), switch to LOCAL"
        )
        if (!tag.isNullOrBlank()) {
            elementId2LogProperty[tag]?.logStorageMode = LogStorageMode.LOCAL
        }
        if (disableAllOnGiveUp) {
            disableLogUpload()
            discardPendingLogs()
        }
    }

    private fun markArchiveFailed(property: TaskBuildLogProperty, reason: String) {
        property.logStorageMode = LogStorageMode.ARCHIVE_FAILED
        appendArchiveFailureNotice(property, reason)
        reportArchiveFailureLine(property.elementId, reason)
    }

    private fun appendArchiveFailureNotice(property: TaskBuildLogProperty, reason: String) {
        try {
            val dateTime = localLogTimeFormatter.format(Instant.now())
            property.logFile.appendText(
                "$dateTime : $LOG_WARN_FLAG$ARCHIVE_FAIL_NOTICE ($reason)\n"
            )
        } catch (ignored: Exception) {
            logger.warn("Fail to append archive-failed notice to ${property.logFile}", ignored)
        }
    }

    private fun reportArchiveFailureLine(elementId: String, reason: String) {
        try {
            val ctx = effectiveContext()
            logResourceApi.addLogMultiLine(
                buildVariables?.buildId ?: "",
                listOf(
                    LogMessage(
                        message = "$LOG_WARN_FLAG$ARCHIVE_FAIL_NOTICE ($reason)",
                        timestamp = System.currentTimeMillis(),
                        tag = elementId,
                        containerHashId = ctx.containerHashId,
                        logType = LogType.WARN,
                        executeCount = ctx.executeCount,
                        jobId = ctx.jobId,
                        stepId = ctx.stepId
                    )
                )
            )
        } catch (ignored: Exception) {
            logger.warn("Fail to report archive-failed notice for Task[$elementId]", ignored)
        }
    }

    /**
     * 状态回写不走熔断：内容上报熔断后仍要把 LOCAL / ARCHIVED / ARCHIVE_FAILED 同步到 log。
     * ARCHIVE_FAILED 用 finishLog 的 query 传递，避免旧 log 反序列化 JSON 枚举失败。
     */
    private fun syncStorageModeToLogService() {
        val succeeded = elementId2LogProperty.values.filter {
            it.logStorageMode != LogStorageMode.ARCHIVE_FAILED
        }
        if (succeeded.isNotEmpty()) {
            logResourceApi.updateStorageMode(succeeded.toList(), executeCount)
        }
        elementId2LogProperty.forEach { (elementId, property) ->
            if (property.logStorageMode != LogStorageMode.ARCHIVE_FAILED) {
                return@forEach
            }
            try {
                logResourceApi.finishLog(
                    tag = elementId,
                    jobId = containerHashId.ifBlank { null },
                    executeCount = executeCount,
                    subTag = null,
                    logMode = LogStorageMode.ARCHIVE_FAILED
                )
            } catch (ignored: Exception) {
                logger.warn("Fail to finish archive-failed status for Task[$elementId]", ignored)
            }
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
            flushUntilIdleOrLocal(tag, disableAllOnGiveUp = false)
            flushLocalLogWriter(tag)
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

    private fun <T> doWithCircuitBreaker(
        action: () -> T
    ): T {
        return circuitBreakerRegistry.let {
            val breaker = it.circuitBreaker(this.javaClass.name)
            breaker.executeCallable {
                action()
            }
        }
    }
}
