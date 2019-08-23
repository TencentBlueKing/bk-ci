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

package com.tencent.devops.worker.common

import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.log.Ansi
import com.tencent.devops.common.pipeline.enums.BuildTaskStatus
import com.tencent.devops.process.utils.PIPELINE_RETRY_COUNT
import com.tencent.devops.worker.common.heartbeat.Heartbeat
import com.tencent.devops.worker.common.logger.LoggerService
import com.tencent.devops.worker.common.service.ProcessService
import com.tencent.devops.worker.common.task.TaskDaemon
import com.tencent.devops.worker.common.task.TaskFactory
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

object Runner {
    private val logger = LoggerFactory.getLogger(Runner::class.java)

    fun run(workspaceInterface: WorkspaceInterface, systemExit: Boolean = true) {
        try {
            logger.info("Start the worker ...")
            // 启动成功了，报告process我已经启动了
            val buildVariables = ProcessService.setStarted()
            // 启动日志服务
            LoggerService.start()
            val retryCount = buildVariables.variables[PIPELINE_RETRY_COUNT] ?: "0"
            LoggerService.executeCount = retryCount.toInt() + 1

            Heartbeat.start()
            // 开始轮询
            try {
                showBuildStartupLog(buildVariables.buildId, buildVariables.vmSeqId)
                showMachineLog(buildVariables.vmName)
                showSystemLog()
                showRuntimeEnvs(buildVariables.variables)
                val workspacePathFile =
                    workspaceInterface.getWorkspace(buildVariables.variables, buildVariables.pipelineId)

                LoggerService.addNormalLine("Start the runner at workspace(${workspacePathFile.absolutePath})")
                logger.info("Start the runner at workspace(${workspacePathFile.absolutePath})")

                loop@ while (true) {
                    logger.info("Start to claim the task")
                    val buildTask = ProcessService.claimTask()
                    logger.info("Start to execute the task($buildTask)")
                    when (buildTask.status) {
                        BuildTaskStatus.DO -> {
                            val taskType = buildTask.type ?: "empty"
                            val taskId = buildTask.taskId ?: throw RemoteServiceException("Not valid build taskId")
                            if (buildTask.elementId == null) {
                                throw RemoteServiceException("Not valid build elementId")
                            }
                            val task = TaskFactory.create(taskType)
                            val taskDaemon = TaskDaemon(task, buildTask, buildVariables, workspacePathFile)
                            try {
                                LoggerService.elementId = buildTask.elementId!!
                                LoggerService.addNormalLine("")
                                LoggerService.addFoldStartLine("${buildTask.elementName}-[${buildTask.elementId}]")
                                LoggerService.addNormalLine(Ansi().bold().a("Start Element").reset().toString())
                                taskDaemon.run()
                                val env = taskDaemon.getAllEnv()
                                logger.info("Complete the task ($buildTask)")
                                ProcessService.completeTask(taskId, buildTask.elementId!!, env, buildTask.type)
                                logger.info("Finish completing the task ($buildTask)")
                            } catch (ie: Throwable) {
                                logger.warn("Fail to execute the task($buildTask)", ie)
                                val env = taskDaemon.getAllEnv()
                                val message = ie.message ?: "Unknown error has occurred"
                                LoggerService.addNormalLine(Ansi().fgRed().a(message).reset().toString())
                                LoggerService.addFoldEndLine("${buildTask.elementName}-[${buildTask.elementId}]")
                                ProcessService.completeTask(taskId, buildTask.elementId!!, env, buildTask.type, message)
                            } finally {
                                LoggerService.addFoldEndLine("${buildTask.elementName}-[${buildTask.elementId}]")
                                LoggerService.elementId = ""
                            }
                        }
                        BuildTaskStatus.WAIT -> {
                            Thread.sleep(5000)
                        }
                        BuildTaskStatus.END -> {
                            break@loop
                        }
                    }
                }
            } catch (ignored: Throwable) {
                logger.error("Other unknown error has occurred:", ignored)
                LoggerService.addNormalLine(Ansi().fgRed().a("Other unknown error has occurred: " + ignored.message).reset().toString())
            } finally {
                LoggerService.stop()
                Heartbeat.stop()
                ProcessService.endBuild()
            }
        } catch (e: Exception) {
            logger.warn("Catch unknown exceptions", e)
            throw e
        } finally {
            if (systemExit) {
                exitProcess(0)
            }
        }
    }

    /**
     * 发送构建初始化日志
     */
    private fun showBuildStartupLog(buildId: String, vmSeqId: String) {
        LoggerService.addNormalLine("The build $buildId environment #$vmSeqId is ready")
    }

    /**
     * 发送构建机环境日志
     * @param vmName 当前运行VM名称
     */
    private fun showMachineLog(vmName: String) {
        LoggerService.addNormalLine("")
        LoggerService.addFoldStartLine("env_machine")
        LoggerService.addNormalLine(Ansi().bold().a("Get build machine properties").reset().toString())
        LoggerService.addNormalLine(Ansi().bold().a("machine.current: ").reset().a(vmName).toString())
        System.getProperties().forEach { k, v ->
            LoggerService.addNormalLine(Ansi().bold().a("$k: ").reset().a(v.toString()).toString())
        }
        LoggerService.addFoldEndLine("env_machine")
    }

    /**
     * 发送系统环境变量日志
     */
    private fun showSystemLog() {
        LoggerService.addNormalLine("")
        LoggerService.addFoldStartLine("env_system")
        LoggerService.addNormalLine(Ansi().bold().a("Get build system properties").reset().toString())
        val envs = System.getenv()
        envs.forEach { (k, v) ->
            LoggerService.addNormalLine(Ansi().bold().a("$k: ").reset().a(v).toString())
        }
        LoggerService.addFoldEndLine("env_system")
    }

    /**
     * 显示用户预定义变量
     */
    private fun showRuntimeEnvs(variables: Map<String, String>) {
        LoggerService.addNormalLine("")
        LoggerService.addFoldStartLine("env_user")
        LoggerService.addNormalLine(Ansi().bold().a("Resolve the construction process parameter variable table").reset().toString())
        variables.forEach { (k, v) ->
            LoggerService.addNormalLine(Ansi().bold().a("$k: ").reset().a(v).toString())
        }
        LoggerService.addFoldEndLine("env_user")
    }
}