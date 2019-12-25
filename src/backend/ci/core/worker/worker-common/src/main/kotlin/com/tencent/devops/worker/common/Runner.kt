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
import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import com.tencent.devops.common.pipeline.enums.BuildTaskStatus
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.common.pipeline.utils.ParameterUtils
import com.tencent.devops.process.pojo.AtomErrorCode
import com.tencent.devops.process.pojo.ErrorType
import com.tencent.devops.process.utils.PIPELINE_RETRY_COUNT
import com.tencent.devops.worker.common.env.BuildEnv
import com.tencent.devops.worker.common.env.BuildType
import com.tencent.devops.worker.common.exception.TaskExecuteException
import com.tencent.devops.worker.common.heartbeat.Heartbeat
import com.tencent.devops.worker.common.logger.LoggerService
import com.tencent.devops.worker.common.service.ProcessService
import com.tencent.devops.worker.common.task.TaskDaemon
import com.tencent.devops.worker.common.task.TaskFactory
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.system.exitProcess

object Runner {
    private val logger = LoggerFactory.getLogger(Runner::class.java)

    fun run(workspaceInterface: WorkspaceInterface, systemExit: Boolean = true) {
        var workspacePathFile: File? = null
        try {
            logger.info("Start the worker ...")
            // 启动成功了，报告process我已经启动了
            val buildVariables = ProcessService.setStarted()

            // 启动日志服务
            LoggerService.start()
            val variables = buildVariables.variablesWithType
            val retryCount = ParameterUtils.getListValueByKey(variables, PIPELINE_RETRY_COUNT) ?: "0"
            LoggerService.executeCount = retryCount.toInt() + 1
            LoggerService.jobId = buildVariables.containerId

            Heartbeat.start()
            // 开始轮询
            try {
                showBuildStartupLog(buildVariables.buildId, buildVariables.vmSeqId)
                showMachineLog(buildVariables.vmName)
                showSystemLog()
                showRuntimeEnvs(buildVariables.variablesWithType)
                val variablesMap = buildVariables.variablesWithType.map { it.key to it.value.toString() }.toMap()
                workspacePathFile = workspaceInterface.getWorkspace(variablesMap, buildVariables.pipelineId)

                LoggerService.addNormalLine("Start the runner at workspace(${workspacePathFile.absolutePath})")
                logger.info("Start the runner at workspace(${workspacePathFile.absolutePath})")

                loop@ while (true) {
                    logger.info("Start to claim the task")
                    val buildTask = ProcessService.claimTask()
                    val taskName = buildTask.elementName ?: "Task"
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

                                // 开始Task执行
                                LoggerService.addFoldStartLine(taskName)
                                taskDaemon.run()

                                // 获取执行结果
                                val env = taskDaemon.getAllEnv()

                                // 上报Task执行结果
                                logger.info("Complete the task ($buildTask)")
                                ProcessService.completeTask(
                                    taskId = taskId,
                                    elementId = buildTask.elementId!!,
                                    elementName = buildTask.elementName ?: "",
                                    containerId = buildVariables.containerId,
                                    isSuccess = true,
                                    buildResult = env,
                                    type = buildTask.type
                                )
                                logger.info("Finish completing the task ($buildTask)")
                            } catch (e: Throwable) {
                                var message: String
                                var errorType: String
                                var errorCode: Int

                                val trueException = when {
                                    e is TaskExecuteException -> e
                                    e.cause is TaskExecuteException -> e.cause as TaskExecuteException
                                    else -> null
                                }
                                if (trueException != null) {
                                    // Worker内插件执行出错处理
                                    logger.warn("[Task Error] Fail to execute the task($buildTask) with task error", e)
                                    message = trueException.errorMsg
                                    errorType = trueException.errorType.name
                                    errorCode = trueException.errorCode
                                } else {
                                    // Worker执行的错误处理
                                    logger.warn("[Worker Error] Fail to execute the task($buildTask) with system error", e)
                                    val defaultMessage = StringBuilder("Unknown system error has occurred with StackTrace:\n")
                                    defaultMessage.append(e.toString())
                                    e.stackTrace.forEach {
                                        with(it) {
                                            defaultMessage.append("\n    at $className.$methodName($fileName:$lineNumber)")
                                        }
                                    }
                                    message = e.message ?: defaultMessage.toString()
                                    errorType = ErrorType.SYSTEM.name
                                    errorCode = AtomErrorCode.SYSTEM_WORKER_LOADING_ERROR
                                }

                                val env = taskDaemon.getAllEnv()
                                LoggerService.addNormalLine(Ansi().fgRed().a(message).reset().toString())

                                ProcessService.completeTask(
                                    taskId = taskId,
                                    elementId = buildTask.elementId!!,
                                    elementName = buildTask.elementName ?: "",
                                    containerId = buildVariables.containerId,
                                    isSuccess = false,
                                    buildResult = env,
                                    type = buildTask.type,
                                    message = message,
                                    errorType = errorType,
                                    errorCode = errorCode
                                )
                            } finally {
                                LoggerService.addFoldEndLine(taskName)
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
            } catch (e: Exception) {
                logger.error("Other unknown error has occurred:", e)
                LoggerService.addNormalLine(Ansi().fgRed().a("Other unknown error has occurred: " + e.message).reset().toString())
            } finally {
                LoggerService.stop()
                Heartbeat.stop()
                ProcessService.endBuild()
            }
        } catch (e: Exception) {
            logger.warn("Catch unknown exceptions", e)
            throw e
        } finally {
            if (workspacePathFile != null && checkIfNeed2CleanWorkspace()) {
                val file = workspacePathFile.absoluteFile.normalize()
                logger.warn("Need to clean up the workspace(${file.absolutePath})")
                if (!file.deleteRecursively()) {
                    logger.warn("Fail to clean up the workspace")
                }
            }

            if (systemExit) {
                exitProcess(0)
            }
        }
    }

    private fun checkIfNeed2CleanWorkspace(): Boolean {
        // current only add this option for pcg docker
        if (BuildEnv.getBuildType() != BuildType.DOCKER) {
            return false
        }
        if (System.getProperty(CLEAN_WORKSPACE)?.trim() == true.toString()) {
            return true
        }
        return false
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
        LoggerService.addFoldStartLine("[Machine Environment Properties]")
        System.getProperties().forEach { k, v ->
            LoggerService.addYellowLine("$k: $v")
            logger.info("$k: $v")
        }
        LoggerService.addFoldEndLine("env_machine")
    }

    /**
     * 发送系统环境变量日志
     */
    private fun showSystemLog() {
        LoggerService.addNormalLine("")
        LoggerService.addFoldStartLine("[System Environment Properties]")
        val envs = System.getenv()
        envs.forEach { (k, v) ->
            LoggerService.addYellowLine("$k: $v")
            logger.info("$k: $v")
        }
        LoggerService.addFoldEndLine("-----")
    }

    /**
     * 显示用户预定义变量
     */
    private fun showRuntimeEnvs(variables: List<BuildParameters>) {
        LoggerService.addNormalLine("")
        LoggerService.addFoldStartLine("[Build Environment Properties]")
        variables.forEach { v ->
            if (v.valueType == BuildFormPropertyType.PASSWORD) {
                LoggerService.addYellowLine(Ansi().a("${v.key}: ").reset().a("******").toString())
            } else {
                LoggerService.addYellowLine(Ansi().a("${v.key}: ").reset().a(v.value.toString()).toString())
            }
            logger.info("${v.key}: ${v.value}")
        }
        LoggerService.addFoldEndLine("env_user")
    }
}