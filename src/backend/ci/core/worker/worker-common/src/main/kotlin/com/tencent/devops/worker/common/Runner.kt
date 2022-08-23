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

package com.tencent.devops.worker.common

import com.tencent.devops.common.api.check.Preconditions
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.exception.TaskExecuteException
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorInfo
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import com.tencent.devops.common.pipeline.enums.BuildTaskStatus
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.common.service.utils.CommonUtils
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.pojo.BuildTask
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.process.utils.PIPELINE_RETRY_COUNT
import com.tencent.devops.process.utils.PIPELINE_TASK_MESSAGE_STRING_LENGTH_MAX
import com.tencent.devops.process.utils.PipelineVarUtil
import com.tencent.devops.worker.common.env.BuildEnv
import com.tencent.devops.worker.common.env.BuildType
import com.tencent.devops.worker.common.heartbeat.Heartbeat
import com.tencent.devops.worker.common.logger.LoggerService
import com.tencent.devops.worker.common.service.EngineService
import com.tencent.devops.worker.common.service.QuotaService
import com.tencent.devops.worker.common.task.TaskDaemon
import com.tencent.devops.worker.common.task.TaskFactory
import com.tencent.devops.worker.common.utils.CredentialUtils
import com.tencent.devops.worker.common.utils.KillBuildProcessTree
import com.tencent.devops.worker.common.utils.ShellUtil
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import kotlin.system.exitProcess

object Runner {

    private const val maxSleepStep = 50L
    private const val windows = 5L
    private const val millsStep = 100L
    private val logger = LoggerFactory.getLogger(Runner::class.java)

    fun run(workspaceInterface: WorkspaceInterface, systemExit: Boolean = true) {
        logger.info("Start the worker ...")
        ErrorMsgLogUtil.init()
        var workspacePathFile: File? = null
        // 启动成功, 报告process我已经启动了, #1613 如果这都失败了，则也无法向后台上报信息了。将由devopsAgent监控传递
        val buildVariables = EngineService.setStarted()
        var failed = false
        try {
            BuildEnv.setBuildId(buildVariables.buildId)

            workspacePathFile = prepareWorkspace(buildVariables, workspaceInterface)

            try {
                // 上报agent启动给quota
                QuotaService.addRunningAgent(buildVariables)
                // 开始轮询
                failed = loopPickup(workspacePathFile, buildVariables)
            } catch (ignore: Throwable) {
                failed = true
                logger.error("Other ignore error has occurred:", ignore)
                LoggerService.addErrorLine("Other ignore error has occurred: " + ignore.message)
            } finally {
                LoggerService.stop()
                LoggerService.archiveLogFiles()
                EngineService.endBuild(buildVariables)
                QuotaService.removeRunningAgent(buildVariables)
                Heartbeat.stop()
            }
        } catch (ignore: Exception) {
            failed = true
            logger.warn("Catch unknown exceptions", ignore)
            val errMsg = when (ignore) {
                is java.lang.IllegalArgumentException -> "参数错误：${ignore.message}"
                is FileNotFoundException, is IOException -> {
                    "运行Agent需要构建机临时目录的写权限，请检查Agent运行帐号相关权限: ${ignore.message}" +
                        "\n 可以检查devopsAgent进程的启动帐号和{agent_dir}/.agent.properties文件中的" +
                        "devops.slave.user配置的指定构建帐号（此选项非必须，是由用户设置),如果有可删除或者修改为正确的帐号"
                }
                else -> "未知错误: ${ignore.message}"
            }
            // #1613 worker-agent.jar 增强在启动之前的异常情况上报（本机故障）
            EngineService.submitError(
                ErrorInfo(
                    taskId = "",
                    taskName = "",
                    atomCode = "",
                    errorMsg = errMsg,
                    errorType = ErrorType.USER.num,
                    errorCode = ErrorCode.SYSTEM_WORKER_INITIALIZATION_ERROR
                )
            )
            EngineService.endBuild(buildVariables)
            throw ignore
        } finally {
            finally(workspacePathFile, failed)

            if (systemExit) {
                exitProcess(0)
            }
        }
    }

    private fun prepareWorkspace(buildVariables: BuildVariables, workspaceInterface: WorkspaceInterface): File {
        // 为进程加上ShutdownHook事件
        KillBuildProcessTree.addKillProcessTreeHook(
            projectId = buildVariables.projectId,
            buildId = buildVariables.buildId,
            vmSeqId = buildVariables.vmSeqId
        )

        // 启动日志服务
        LoggerService.start()
        val variables = buildVariables.variables
        val retryCount = variables[PIPELINE_RETRY_COUNT] ?: "0"
        val executeCount = retryCount.toInt() + 1
        LoggerService.executeCount = executeCount
        LoggerService.jobId = buildVariables.containerHashId
        LoggerService.elementId = VMUtils.genStartVMTaskId(buildVariables.containerId)
        LoggerService.buildVariables = buildVariables

        showBuildStartupLog(buildVariables.buildId, buildVariables.vmSeqId)
        showMachineLog(buildVariables.vmName)
        showSystemLog()
        showRuntimeEnvs(buildVariables.variablesWithType)

        Heartbeat.start(buildVariables.timeoutMills, executeCount) // #2043 添加Job超时监控

        val workspaceAndLogPath = workspaceInterface.getWorkspaceAndLogDir(
            variables = variables,
            pipelineId = buildVariables.pipelineId
        )
        LoggerService.pipelineLogDir = workspaceAndLogPath.second
        return workspaceAndLogPath.first
    }

    private fun loopPickup(workspacePathFile: File, buildVariables: BuildVariables): Boolean {
        var failed = false
        LoggerService.addNormalLine("Start the runner at workspace(${workspacePathFile.absolutePath})")
        logger.info("Start the runner at workspace(${workspacePathFile.absolutePath})")

        var waitCount = 0
        loop@ while (true) {
            logger.info("Start to claim the task")
            val buildTask = EngineService.claimTask()
            logger.info("Start to execute the task($buildTask)")
            when (buildTask.status) {
                BuildTaskStatus.DO -> {
                    Preconditions.checkNotNull(
                        obj = buildTask.taskId,
                        exception = RemoteServiceException("Not valid build elementId")
                    )

                    val task = TaskFactory.create(buildTask.type ?: "empty")
                    val taskDaemon = TaskDaemon(task, buildTask, buildVariables, workspacePathFile)
                    try {
                        LoggerService.elementId = buildTask.taskId!!
                        LoggerService.elementName = buildTask.elementName ?: LoggerService.elementId
                        CredentialUtils.signToken = buildTask.signToken ?: ""

                        // 开始Task执行
                        taskDaemon.runWithTimeout()

                        // 上报Task执行结果
                        logger.info("Complete the task (${buildTask.elementName})")
                        // 获取执行结果
                        val buildTaskRst = taskDaemon.getBuildResult()
                        EngineService.completeTask(buildTaskRst)
                        logger.info("Finish completing the task ($buildTask)")
                    } catch (ignore: Throwable) {
                        failed = true
                        dealException(ignore, buildTask, taskDaemon)
                    } finally {
                        LoggerService.finishTask()
                        LoggerService.elementId = ""
                        LoggerService.elementName = ""
                        waitCount = 0
                    }
                }
                BuildTaskStatus.WAIT -> {
                    var sleepStep = waitCount++ / windows
                    if (sleepStep <= 0) {
                        sleepStep = 1
                    }
                    val sleepMills = sleepStep.coerceAtMost(maxSleepStep) * millsStep
                    logger.info("WAIT $sleepMills ms")
                    Thread.sleep(sleepMills)
                }
                BuildTaskStatus.END -> break@loop
            }
        }

        return failed
    }

    private fun finally(workspacePathFile: File?, failed: Boolean) {

        if (workspacePathFile != null && checkIfNeed2CleanWorkspace(failed)) {

            val file = workspacePathFile.absoluteFile.normalize()
            logger.warn("Need to clean up the workspace(${file.absolutePath})")

            // 去除workspace目录下的软连接，再清空workspace
            try {
                ShellUtil.execute(
                    buildId = "",
                    script = "find ${file.absolutePath} -type l | xargs rm -rf;",
                    dir = file,
                    buildEnvs = emptyList(),
                    runtimeVariables = emptyMap()
                )
                if (!file.deleteRecursively()) {
                    logger.warn("Fail to clean up the workspace")
                }
            } catch (ignore: Exception) {
                logger.error("Fail to clean up the workspace.", ignore)
            }
        }
    }

    private fun dealException(exception: Throwable, buildTask: BuildTask, taskDaemon: TaskDaemon) {

        val message: String
        val errorType: String
        val errorCode: Int

        val trueException = when {
            exception is TaskExecuteException -> exception
            exception.cause is TaskExecuteException -> exception.cause as TaskExecuteException
            else -> null
        }
        if (trueException != null) {
            // Worker内插件执行出错处理
            logger.warn("[Task Error] Fail to execute the task($buildTask) with task error", exception)
            message = trueException.errorMsg
            errorType = trueException.errorType.name
            errorCode = trueException.errorCode
        } else {
            // Worker执行的错误处理
            logger.warn("[Worker Error] Fail to execute the task($buildTask)", exception)
            val defaultMessage =
                StringBuilder("Unknown system error has occurred with StackTrace:\n")
            defaultMessage.append(exception.toString())
            exception.stackTrace.forEach {
                with(it) {
                    defaultMessage.append(
                        "\n    at $className.$methodName($fileName:$lineNumber)"
                    )
                }
            }
            message = exception.message ?: defaultMessage.toString()
            errorType = ErrorType.SYSTEM.name
            errorCode = ErrorCode.SYSTEM_WORKER_LOADING_ERROR
        }

        val buildResult = taskDaemon.getBuildResult(
            isSuccess = false,
            errorMessage = CommonUtils.interceptStringInLength(
                string = message,
                length = PIPELINE_TASK_MESSAGE_STRING_LENGTH_MAX
            ),
            errorType = errorType,
            errorCode = errorCode
        )

        EngineService.completeTask(buildResult)
    }

    private fun checkIfNeed2CleanWorkspace(failed: Boolean): Boolean {
        // current only add this option for pcg docker
        if ((BuildEnv.getBuildType() != BuildType.DOCKER) || failed) {
            return false
        }

        return System.getProperty(CLEAN_WORKSPACE)?.trim()?.toBoolean() ?: false
    }

    /**
     * 发送构建初始化日志
     */
    private fun showBuildStartupLog(buildId: String, vmSeqId: String) {
        LoggerService.addNormalLine("构建机已收到请求，准备构建(Build[$buildId] Job#$vmSeqId is ready）")
    }

    /**
     * 发送构建机环境日志
     * @param vmName 当前运行VM名称
     */
    private fun showMachineLog(vmName: String) {
        LoggerService.addNormalLine("")
        LoggerService.addFoldStartLine("[Machine Environment Properties]")
        LoggerService.addNormalLine("vmName: $vmName")
        logger.info("vmName: $vmName")
        System.getProperties().toMap().forEach { (k, v) ->
            LoggerService.addNormalLine("$k: $v")
            logger.info("$k: $v")
        }
        LoggerService.addFoldEndLine("-----")
    }

    /**
     * 发送系统环境变量日志
     */
    private fun showSystemLog() {
        LoggerService.addNormalLine("")
        LoggerService.addFoldStartLine("[System Environment Properties]")
        val envs = System.getenv()
        envs.forEach { (k, v) ->
            LoggerService.addNormalLine("$k: $v")
            logger.info("$k: $v")
        }
        LoggerService.addFoldEndLine("-----")
    }

    private val contextKeys = listOf("variables.", "settings.", "envs.", "ci.", "job.", "jobs.", "steps.", "matrix.")

    /**
     * 显示用户预定义变量
     */
    private fun showRuntimeEnvs(variables: List<BuildParameters>) {
        LoggerService.addNormalLine("")
        LoggerService.addFoldStartLine("[Build Environment Properties]")
        variables.forEach { v ->
            for (it in contextKeys) {
                if (v.key.trim().startsWith(it)) {
                    return@forEach
                }
            }
            logger.info("${v.key}: ${v.value}")
            if (PipelineVarUtil.fetchReverseVarName(v.key) != null) {
                return@forEach
            }
            if (v.valueType == BuildFormPropertyType.PASSWORD) {
                LoggerService.addNormalLine("${v.key}: ******")
            } else {
                LoggerService.addNormalLine("${v.key}: ${v.value}")
            }
        }
        LoggerService.addFoldEndLine("-----")
        LoggerService.addNormalLine("")
    }
}
