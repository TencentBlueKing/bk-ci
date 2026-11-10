package com.tencent.devops.dispatch.utils

import org.slf4j.LoggerFactory

/** 仅隔离明确入队成功后的解锁异常，不改变抢锁、校验或占位失败的处理。 */
internal object DispatchLockCleanup {
    private val logger = LoggerFactory.getLogger(DispatchLockCleanup::class.java)

    fun <T> run(
        description: String,
        unlock: () -> Unit,
        isQueued: (T) -> Boolean,
        action: () -> T
    ): T {
        var queued = false
        val cleanup = AutoCloseable {
            try {
                unlock()
            } catch (e: Exception) {
                if (!queued) throw e
                // 已成功写库及 Redis，不能因清理解锁失败重新调度；继续锁外收尾，残留锁按原租约到期。
                logger.warn("dispatch queued but unlock failed|$description|continue post steps", e)
            }
        }
        // 沿用 use 的异常语义：action 失败时保留原异常，解锁异常只作为 suppressed，不能误报成功。
        return cleanup.use { action().also { queued = isQueued(it) } }
    }
}
