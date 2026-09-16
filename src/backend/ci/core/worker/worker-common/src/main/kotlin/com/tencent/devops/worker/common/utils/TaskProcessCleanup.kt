package com.tencent.devops.worker.common.utils

import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.concurrent.FutureTask
import java.util.concurrent.Semaphore
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit

/**
 * 为 Runner 和 JVM shutdown hook 提供有期限的进程树清理等待。
 * 枚举进程、读取环境或原生 kill 都可能阻塞；返回只代表等待结束，不代表后台动作已经停止。
 * 调用方必须处理返回的异常，不能在清理状态不确定时继续执行下一个任务。
 */
internal object TaskProcessCleanup {
    // 限制实际运行中的清理线程；容量耗尽立即报错，避免取消/退出时继续堆积线程。
    private val slots = Semaphore(2)

    private fun submit(action: () -> Unit): Future<*> {
        if (!slots.tryAcquire()) throw RejectedExecutionException("Process cleanup capacity exhausted")
        val future = FutureTask(action, Unit)
        // 每次创建线程以继承当前日志上下文；Future 取消不保证原生调用退出，槽位只能在 run 结束后释放。
        val thread = Thread({
            try { future.run() } finally { slots.release() }
        }, "task-process-cleanup").apply { isDaemon = true }
        try { thread.start() } catch (e: Throwable) { slots.release(); throw e }
        return future
    }
    /**
     * null 表示动作正常返回；否则返回原始异常、等待超时、中断或容量不足，供调用方决定任务结果。
     * 进入前已中断时，仍给予清理一次有期限的执行机会，退出时恢复中断状态。
     */
    fun run(timeoutMillis: Long = 30_000, action: () -> Unit): Throwable? {
        var interrupted = Thread.interrupted()
        var future: Future<*>? = null
        return try {
            future = submit(action)
            future.get(timeoutMillis, TimeUnit.MILLISECONDS)
            null
        } catch (e: InterruptedException) {
            interrupted = true
            e
        } catch (e: ExecutionException) {
            e.cause ?: e
        } catch (e: Exception) {
            e
        } finally {
            // 这里只请求中断，不 join 原生调用；守护线程确保失控清理不会阻止 JVM 退出。
            if (future != null && !future.isDone) future.cancel(true)
            if (interrupted) Thread.currentThread().interrupt()
        }
    }
}
