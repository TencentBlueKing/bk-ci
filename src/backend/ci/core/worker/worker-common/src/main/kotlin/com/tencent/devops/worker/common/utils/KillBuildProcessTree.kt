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
                    ErrorMsgLogUtil.flushErrorMsgToFile()
                    logger.info("start kill process tree")
                    val killedProcessIds = killProcessTree(projectId, buildId, vmSeqId)
                    logger.info("kill process tree done, ${killedProcessIds.size} process(s) killed, " +
                        "pid(s): $killedProcessIds")
                }
            })
        } catch (t: Throwable) {
            logger.warn("Fail to add shutdown hook", t)
        }
    }

    fun killProcessTree(
        projectId: String,
        buildId: String,
        vmSeqId: String,
        taskIds: Set<String>? = null,
        forceFlag: Boolean = false
    ): List<Int> {
        val currentProcessId = if (AgentEnv.getOS() == OSType.WINDOWS) {
            getCurrentPID()
        } else {
            getUnixPID()
        }
        if (currentProcessId <= 0) {
            logger.warn("get current pid failed")
            return listOf()
        }

        val processTree = try {
            BkProcessTree.get()
        } catch (e: Exception) {
            logger.error("killProcessTree get error: ", e)
            return listOf()
        }
        val processTreeIterator = processTree.iterator()
        val killedProcessIds = mutableListOf<Int>()
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
                if (flag) {
                    osProcess.addKeepAlivePids(keepAlivePids)
                    osProcess.killRecursively(forceFlag)
                    osProcess.kill(forceFlag)
                    killedProcessIds.add(osProcess.pid)
                }
            } catch (e: Exception) {
                logger.warn("kill process ${osProcess.pid} failed: ${e.message}")
            }
        }
        return killedProcessIds
    }
}
