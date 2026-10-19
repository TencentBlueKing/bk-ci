package com.tencent.devops.remotedev.utils

import com.tencent.devops.remotedev.pojo.WorkspaceRecordWithWindows
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.Executor
import org.slf4j.MDC

object WorkspaceListUtil {

    /**
     * 列表需要向 Start Cloud 查询登录态的 IP：已关机的实例没有登录用户，状态修正也不依赖它们的机器状态。
     */
    fun loginStatusHostIps(workspaces: Collection<WorkspaceRecordWithWindows>): Set<String> {
        return workspaces.asSequence()
            .filterNot { it.status.checkSleeping() }
            .mapNotNull { it.hostIp?.takeIf { ip -> ip.isNotBlank() } }
            .toSet()
    }

    /**
     * 并行执行并按输入顺序返回结果；任务异常原样抛出，执行线程的 MDC 在任务结束后复原。
     */
    fun <T, R> parallelMap(items: List<T>, executor: Executor, block: (T) -> R): List<R> {
        val callerMdc = MDC.getCopyOfContextMap()
        val futures = items.map { item ->
            CompletableFuture.supplyAsync({ withMdc(callerMdc) { block(item) } }, executor)
        }
        return futures.map { future ->
            try {
                future.join()
            } catch (e: CompletionException) {
                throw e.cause ?: e
            }
        }
    }

    private fun <R> withMdc(mdc: Map<String, String>?, block: () -> R): R {
        val previous = MDC.getCopyOfContextMap()
        setMdc(mdc)
        try {
            return block()
        } finally {
            setMdc(previous)
        }
    }

    private fun setMdc(mdc: Map<String, String>?) {
        if (mdc == null) MDC.clear() else MDC.setContextMap(mdc)
    }
}
