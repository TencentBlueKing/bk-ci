package com.tencent.devops.worker.common.task

import com.tencent.devops.common.api.exception.TaskExecuteException
import com.tencent.devops.common.pipeline.enums.BuildTaskStatus
import com.tencent.devops.process.pojo.BuildTask
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.worker.common.utils.TaskUtil
import com.tencent.devops.worker.common.api.ApiFactory
import com.tencent.devops.worker.common.api.log.LogSDKApi
import com.tencent.devops.worker.common.api.archive.ArchiveSDKApi
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class TaskDaemonCancellationTest {
    companion object {
        @JvmStatic @BeforeAll
        fun prepareLocalLogApis() {
            mockkObject(ApiFactory)
            every { ApiFactory.create(LogSDKApi::class) } returns mockk(relaxed = true)
            every { ApiFactory.create(ArchiveSDKApi::class) } returns mockk(relaxed = true)
        }

        @JvmStatic @AfterAll
        fun restoreApis() { unmockkObject(ApiFactory) }
    }
    private val variables = BuildVariables(
        buildId = "build", vmSeqId = "1", vmName = "test", projectId = "project", pipelineId = "pipeline",
        variables = emptyMap(), buildEnvs = emptyList(), containerId = "1", containerHashId = "container",
        jobId = "job", variablesWithType = emptyList()
    )
    private val buildTask = BuildTask("build", "1", BuildTaskStatus.DO, taskId = "cancel-test", stepId = "step",
        elementName = "test", executeCount = 1)

    @Test
    fun interruptedTaskIsNeverReportedAsSuccessful() {
        val task = object : ITask() {
            override fun execute(buildTask: BuildTask, buildVariables: BuildVariables, workspace: File) {
                throw InterruptedException("cancelled")
            }
        }
        val daemon = TaskDaemon(task, buildTask, variables, File("."))
        assertThrows(InterruptedException::class.java) { daemon.call() }
        assertNull(TaskExecutorCache.currentExecution.get())
    }

    @Test
    fun cancellationWakesWaiterEvenWhenTaskIgnoresInterruption() {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val taskExited = CountDownLatch(1)
        val task = object : ITask() {
            override fun execute(buildTask: BuildTask, buildVariables: BuildVariables, workspace: File) {
                entered.countDown()
                try {
                    while (release.count > 0) {
                        try { release.await() } catch (e: InterruptedException) { /* native call */ }
                    }
                } finally { taskExited.countDown() }
            }
        }
        val daemon = TaskDaemon(task, buildTask, variables, File("."))
        val pool = Executors.newSingleThreadExecutor()
        try {
            val result = pool.submit<Throwable?> {
                try { daemon.runWithTimeout(); null } catch (e: Throwable) { e }
            }
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            TaskExecutorCache.cancel(buildTask.taskId!!)
            val failure = result.get(2, TimeUnit.SECONDS)
            assertTrue(failure is TaskExecuteException)
            assertTrue(failure?.message.orEmpty().contains("cancelled"))
            assertNull(TaskExecutorCache.getExecution(buildTask.taskId!!))
            assertEquals(1L, taskExited.count)
        } finally {
            release.countDown()
            taskExited.await(3, TimeUnit.SECONDS)
            pool.shutdownNow()
        }
    }

    @Test
    fun executionCookieBelongsToAttemptAndIsInheritedByChildren() {
        var cookie: String? = null
        var inherited: String? = null
        val task = object : ITask() {
            override fun execute(buildTask: BuildTask, buildVariables: BuildVariables, workspace: File) {
                cookie = TaskUtil.getTaskEnvVariables(buildVariables, buildTask.taskId)[TaskExecutorCache.EXECUTION_ID_ENV]
                val child = Thread {
                    inherited = TaskUtil.getTaskEnvVariables(buildVariables, buildTask.taskId)[TaskExecutorCache.EXECUTION_ID_ENV]
                }
                child.start()
                child.join(2000)
                assertFalse(child.isAlive)
            }
        }
        val daemon = TaskDaemon(task, buildTask, variables, File("."))
        daemon.runWithTimeout()
        assertEquals(daemon.executionId, cookie)
        assertEquals(cookie, inherited)
        assertNull(TaskExecutorCache.getExecution(buildTask.taskId!!))
    }
}
