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

package com.tencent.devops.worker.common.utils

import com.tencent.devops.common.api.enums.OSType
import com.tencent.devops.process.utils.PIPELINE_ELEMENT_ID
import com.tencent.devops.worker.common.ErrorMsgLogUtil
import com.tencent.devops.worker.common.env.AgentEnv
import com.tencent.devops.worker.common.task.TaskExecutorCache
import com.tencent.process.BkProcessTree
import com.tencent.process.EnvVars
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.management.ManagementFactory

@Suppress("ALL")
object KillBuildProcessTree {
    private val logger = LoggerFactory.getLogger(KillBuildProcessTree::class.java)

    private fun getCurrentPID(): Int {
        val runtime = ManagementFactory.getRuntimeMXBean()
        val name = runtime.name

        val index = name.indexOf("@")
        return if (index != -1) {
            Integer.parseInt(name.substring(0, index))
        } else -1
    }

    private fun getUnixPID(): Int {
        var reader: BufferedReader? = null
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", "echo \$PPID"))
            reader = BufferedReader(InputStreamReader(process.inputStream))
            reader.readLine().toIntOrNull() ?: -1
        } catch (e: Exception) {
            logger.error("get Unix PID err: ", e)
            -1
        } finally {
            reader?.close()
        }
    }

    fun addKillProcessTreeHook(projectId: String, buildId: String, vmSeqId: String) {
        try {
            Runtime.getRuntime().addShutdownHook(object : Thread() {
                override fun run() {
                    // 任务清理超时后不能在 shutdown hook 再无限等待同一类原生操作；此处另有独立等待期限。
                    TaskProcessCleanup.run {
                        ErrorMsgLogUtil.flushErrorMsgToFile()
                        logger.info("start kill process tree")
                        val killedProcessIds = killProcessTree(projectId, buildId, vmSeqId)
                        logger.info("kill process tree done, ${killedProcessIds.size} process(s) killed, " +
                            "pid(s): $killedProcessIds")
                    }
                }
            })
        } catch (t: Throwable) {
            logger.warn("Fail to add shutdown hook", t)
        }
    }

    /**
     * 按环境标识定位进程；仅凭父子关系无法覆盖已脱离原父进程的后台任务。
     *
     * executionId 非空时额外限定执行批次，并将枚举/清理异常上抛，供 Runner 决定停止领取任务；
     * 为空时保留整个构建退出清理的兼容行为。forceFlag 不覆盖显式的进程保留标记。
     * 环境不可读的进程仍会跳过，返回的 PID 表示已发起清理，不是所有后代都已退出的证明。
     */
    fun killProcessTree(
        projectId: String,
        buildId: String,
        vmSeqId: String,
        taskIds: Set<String>? = null,
        forceFlag: Boolean = false,
        executionId: String? = null
    ): List<Int> {
        val currentProcessId = if (AgentEnv.getOS() == OSType.WINDOWS) {
            getCurrentPID()
        } else {
            getUnixPID()
        }
        if (currentProcessId <= 0) {
            if (executionId != null) throw java.io.IOException("Cannot identify worker process for cleanup")
            logger.warn("get current pid failed")
            return listOf()
        }

        val processTree = try {
            BkProcessTree.get()
        } catch (e: Exception) {
            if (executionId != null) throw java.io.IOException("Cannot enumerate task process tree", e)
            logger.error("killProcessTree get error: ", e)
            return listOf()
        }
        val processTreeIterator = processTree.iterator()
        val killedProcessIds = mutableListOf<Int>()
        val failures = mutableListOf<Exception>()
        val keepAlivePids = mutableSetOf(currentProcessId)
        while (processTreeIterator.hasNext()) {
            val osProcess = processTreeIterator.next()
            var envVars: EnvVars?
            try {
                envVars = osProcess.environmentVariables
            } catch (ignore: Throwable) {
                logger.warn("read [${osProcess.pid}] environmentVariables fail, skip", ignore)
                continue
            }
            if (envVars.isEmpty()) {
                continue
            }

            val dontKillProcessTree = envVars["DEVOPS_DONT_KILL_PROCESS_TREE"]
            if ("true".equals(dontKillProcessTree, ignoreCase = true)) {
                logger.info("DEVOPS_DONT_KILL_PROCESS_TREE is true, skip")
                /*
                 Q: 这里为什么只排除本进程，而不顺便加parent？
                 A: 1、因这类进程基本上parent为1，或者为当前worker，这2类不需要保护，worker自身已经排除了。
                    2、如果parent是业务自身产生的进程，则应由业务自己控制退出，不要主动去keep业务进程，这会导致本该兜底杀掉的没有被杀掉。
                   （注：如果发生了上述情况，那是业务没控制好进程，本应该要退出的进程，出现了残留，因此是要被兜底杀掉，不能因
                        子进程的DEVOPS_DONT_KILL_PROCESS_TREE 而被级联keep
                        ）
                 */
                keepAlivePids.add(osProcess.pid)
                continue
            }

            if (keepAlivePids.contains(osProcess.pid)) {
                osProcess.parent?.let { parent ->
                    keepAlivePids.add(parent.pid) // 防止父进程被干掉，级联到自身
                }
                continue
            }
            try {
                val envProjectId = envVars["PROJECT_ID"]
                val envBuildId = envVars["BUILD_ID"]
                val envVmSeqId = envVars["VM_SEQ_ID"]
                var flag = projectId.equals(envProjectId, ignoreCase = true) &&
                    buildId.equals(envBuildId, ignoreCase = true) &&
                    vmSeqId.equals(envVmSeqId, ignoreCase = true)
                if (!taskIds.isNullOrEmpty()) {
                    val envTaskId = envVars[PIPELINE_ELEMENT_ID]
                    flag = flag && taskIds.contains(envTaskId)
                }
                // taskId 在重试间不变；必须额外匹配执行 ID，防止旧清理线程误杀新一次执行的进程。
                if (executionId != null) {
                    flag = flag && envVars[TaskExecutorCache.EXECUTION_ID_ENV] == executionId
                }
                if (flag) {
                    osProcess.addKeepAlivePids(keepAlivePids)
                    osProcess.killRecursively(forceFlag)
                    osProcess.kill(forceFlag)
                    killedProcessIds.add(osProcess.pid)
                }
            } catch (e: Exception) {
                failures.add(e)
                logger.warn("kill process ${osProcess.pid} failed: ${e.message}")
            }
        }
        // 任务级清理不能只记日志后假装成功，否则 Runner 会在残留进程状态不明时继续运行下一任务。
        if (executionId != null && failures.isNotEmpty()) {
            throw java.io.IOException("Task process cleanup failed", failures.first()).apply {
                failures.drop(1).forEach { addSuppressed(it) }
            }
        }
        return killedProcessIds
    }
}
