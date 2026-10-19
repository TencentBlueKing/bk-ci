package com.tencent.devops.dispatch.utils

import com.tencent.devops.common.log.pojo.enums.LogType
import com.tencent.devops.common.log.pojo.message.LogMessage
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.log.meta.Ansi
import com.tencent.devops.process.engine.common.VMUtils
import org.slf4j.LoggerFactory
import java.util.ArrayDeque

/**
 * 用于同步环境选机和灰度队列消费：锁内记录日志，最外层 finally（所有调度锁释放后）批量发送。
 * 不跨线程传递；嵌套调用复用当前缓冲，finally 必须 remove，避免 MQ 消费线程复用时串日志。
 */
object DispatchLogBuffer {
    // 日志由服务内部生成、长度可预估，只限制整轮条数，不截断单条内容。
    // 按当前总条数分级准入，为非 DEBUG 日志预留 500 条，其中最后 200 条仅供警告和错误使用。
    internal const val MAX_BUFFERED_MESSAGES = 2000
    internal const val DEBUG_BUFFER_LIMIT = 1500
    internal const val NORMAL_BUFFER_LIMIT = 1800
    private const val FLUSH_BATCH_SIZE = 200

    private class Buffer(val buildId: String?) {
        val messages: MutableMap<String, ArrayDeque<LogMessage>> = mutableMapOf()
        var size = 0
        var droppedDebug = 0L
        var droppedNormal = 0L
        var droppedImportant = 0L

        fun tryReserve(logType: LogType, yellow: Boolean, red: Boolean): Boolean {
            // 现有警告和错误还通过颜色标记，不能只依赖 LogType。
            val important = yellow || red || logType == LogType.WARN || logType == LogType.ERROR
            val limit = when {
                important -> MAX_BUFFERED_MESSAGES
                logType == LogType.DEBUG -> DEBUG_BUFFER_LIMIT
                else -> NORMAL_BUFFER_LIMIT
            }
            if (size < limit) {
                size++
                return true
            }
            when {
                important -> droppedImportant++
                logType == LogType.DEBUG -> droppedDebug++
                else -> droppedNormal++
            }
            return false
        }
    }
    private val current = ThreadLocal<Buffer>()
    private val logger = LoggerFactory.getLogger(DispatchLogBuffer::class.java)

    /** buildId 为 null 时缓冲整轮队列的日志，所有构建共享限额，保留各自已接收日志的顺序。 */
    fun <T> withBuffer(buildId: String?, printer: BuildLogPrinter, action: () -> T): T {
        if (current.get() != null) return action()
        val buffer = Buffer(buildId)
        current.set(buffer)
        try {
            return action()
        } finally {
            current.remove()
            flush(buffer, printer)
        }
    }

    /** 返回 true 表示已由缓冲接管，包含超限省略；返回 false 才允许调用方直接发送。 */
    fun append(
        buildId: String,
        vmSeqId: String,
        containerHashId: String?,
        executeCount: Int?,
        jobId: String?,
        message: String,
        logType: LogType = LogType.LOG,
        yellow: Boolean = false,
        red: Boolean = false
    ): Boolean {
        val buffer = current.get()?.takeIf { it.buildId == null || it.buildId == buildId } ?: return false
        // 必须返回 true，避免调用方回退到锁内 RPC；先判断限额，省略时不再格式化或创建构建条目。
        if (!buffer.tryReserve(logType, yellow, red)) return true
        val text = when {
            yellow -> Ansi().bold().fgYellow().a(message).reset().toString()
            red -> Ansi().bold().fgRed().a(message).reset().toString()
            logType == LogType.DEBUG -> "##[debug]${message.replace("\n", "\n##[debug]")}"
            else -> message
        }
        val tag = VMUtils.genStartVMTaskId(vmSeqId)
        buffer.messages.getOrPut(buildId) { ArrayDeque() }.addLast(
            LogMessage(
                message = text,
                timestamp = System.currentTimeMillis(),
                tag = tag,
                containerHashId = containerHashId ?: "",
                logType = logType,
                executeCount = executeCount ?: 1,
                jobId = jobId ?: "",
                stepId = if (yellow) "" else tag
            )
        )
        return true
    }

    private fun flush(buffer: Buffer, printer: BuildLogPrinter) {
        // 只保存整轮计数，不为被省略的构建建立额外 Map；汇总也在锁外输出。
        if (buffer.droppedDebug > 0 || buffer.droppedNormal > 0 || buffer.droppedImportant > 0) {
            logger.warn(
                "dispatch log buffer overflow|buildId=${buffer.buildId ?: "queue"}|" +
                    "limit=$MAX_BUFFERED_MESSAGES|droppedDebug=${buffer.droppedDebug}|" +
                    "droppedNormal=${buffer.droppedNormal}|droppedImportant=${buffer.droppedImportant}"
            )
        }
        // 失败/抛重试也要输出；日志发送失败不能覆盖调度结果，也不能影响其他构建的日志。
        val builds = buffer.messages.iterator()
        while (builds.hasNext()) {
            val (messageBuildId, messages) = builds.next()
            while (messages.isNotEmpty()) {
                val batchSize = minOf(FLUSH_BATCH_SIZE, messages.size)
                val batch = ArrayList<LogMessage>(batchSize)
                repeat(batchSize) { batch.add(messages.removeFirst()) }
                buffer.size -= batchSize
                try {
                    printer.addLines(messageBuildId, batch)
                } catch (e: Exception) {
                    logger.warn("flush dispatch logs failed|$messageBuildId", e)
                }
            }
            // 已发送的日志和构建条目及时解除引用，不等待整轮 RPC 全部完成。
            builds.remove()
        }
    }
}
