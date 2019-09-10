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

package com.tencent.devops.agent.utils

import com.tencent.devops.common.api.enums.OSType
import com.tencent.devops.worker.common.env.AgentEnv
import com.tencent.process.ProcessTree
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.management.ManagementFactory

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

    fun killProcessTree(projectId: String, buildId: String, vmSeqId: String): List<Int> {
        val currentProcessId = if (AgentEnv.getOS() == OSType.WINDOWS) {
            getCurrentPID()
        } else {
            getUnixPID()
        }
        if (currentProcessId <= 0) {
            logger.warn("get current pid failed")
            return listOf()
        }

        val processTree = ProcessTree.get()
        val processTreeIterator = processTree.iterator()
        val killedProcessIds = mutableListOf<Int>()
        while (processTreeIterator.hasNext()) {
            val osProcess = processTreeIterator.next()
            val envVars = osProcess.environmentVariables
            if (envVars.isEmpty()) {
                continue
            }

            val dontKillProcessTree = envVars["DEVOPS_DONT_KILL_PROCESS_TREE"]
            if ("true".equals(dontKillProcessTree, ignoreCase = true)) {
                logger.info("DEVOPS_DONT_KILL_PROCESS_TREE is true, skip")
                continue
            }

            if (osProcess.pid == currentProcessId) {
                continue
            }
            try {
                val envProjectId = envVars["PROJECT_ID"]
                val envBuildId = envVars["BUILD_ID"]
                val envVmSeqId = envVars["VM_SEQ_ID"]
                if (projectId.equals(envProjectId, ignoreCase = true) &&
                    buildId.equals(envBuildId, ignoreCase = true) &&
                    vmSeqId.equals(envVmSeqId, ignoreCase = true)
                ) {
                    osProcess.killRecursively()
                    osProcess.kill()
                    killedProcessIds.add(osProcess.pid)
                }
            } catch (e: Exception) {
                logger.warn("kill process ${osProcess.pid} failed: ${e.message}")
            }
        }
        return killedProcessIds
    }
}