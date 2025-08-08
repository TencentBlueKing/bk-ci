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

package com.tencent.devops.worker.common

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.check.Preconditions
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorInfo
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.pipeline.EnvReplacementParser
import com.tencent.devops.common.pipeline.NameAndValue
import com.tencent.devops.common.pipeline.dialect.PipelineDialectUtil
import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import com.tencent.devops.common.pipeline.enums.BuildTaskStatus
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.pojo.BuildJobResult
import com.tencent.devops.process.pojo.BuildTask
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.process.utils.PIPELINE_DIALECT
import com.tencent.devops.process.utils.PIPELINE_RETRY_COUNT
import com.tencent.devops.process.utils.PipelineVarUtil
import com.tencent.devops.worker.common.constants.WorkerMessageCode.BK_PREPARE_TO_BUILD
import com.tencent.devops.worker.common.constants.WorkerMessageCode.PARAMETER_ERROR
import com.tencent.devops.worker.common.constants.WorkerMessageCode.RUN_AGENT_WITHOUT_PERMISSION
import com.tencent.devops.worker.common.constants.WorkerMessageCode.UNKNOWN_ERROR
import com.tencent.devops.worker.common.env.AgentEnv
import com.tencent.devops.worker.common.env.BuildEnv
import com.tencent.devops.worker.common.env.BuildType
import com.tencent.devops.worker.common.exception.TaskExecuteExceptionDecorator
import com.tencent.devops.worker.common.expression.SpecialFunctions
import com.tencent.devops.worker.common.heartbeat.Heartbeat
import com.tencent.devops.worker.common.logger.LoggerService
import com.tencent.devops.worker.common.service.EngineService
import com.tencent.devops.worker.common.service.QuotaService
import com.tencent.devops.worker.common.task.TaskDaemon
import com.tencent.devops.worker.common.task.TaskFactory
import com.tencent.devops.worker.common.utils.CredentialUtils
import com.tencent.devops.worker.common.utils.KillBuildProcessTree
import com.tencent.devops.worker.common.utils.ShellUtil
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import kotlin.system.exitProcess
import org.slf4j.LoggerFactory

object Runner {

    private const val maxSleepStep = 80L
    private const val windows = 5L
    private const val millsStep = 100L
    private val logger = LoggerFactory.getLogger(Runner::class.java)

    fun run(workspaceInterface: WorkspaceInterface, systemExit: Boolean = true) {
        logger.info("Start the worker ...")
        ErrorMsgLogUtil.init()
        var workspacePathFile: File? = null
        val buildVariables = getBuildVariables()
        var failed = false
        var errMsg: String? = null
        try {
            BuildEnv.setBuildId(buildVariables.buildId)

            // 准备工作空间并返回 + 启动日志服务 + 启动心跳 + 打印构建信息
            workspacePathFile = prepareWorker(buildVariables, workspaceInterface)

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
                // 仅当有插件运行时会产生待归档日志
                LoggerService.archiveLogFiles()
                // 兜底try中的增加配额
                QuotaService.removeRunningAgent(buildVariables)
            }
        } catch (ignore: Exception) {
            failed = true
            logger.warn("Catch unknown exceptions", ignore)
            errMsg = when (ignore) {
                is java.lang.IllegalArgumentException ->
                    MessageUtil.getMessageByLocale(
                        messageCode = PARAMETER_ERROR,
                        language = AgentEnv.getLocaleLanguage()
                    ) + "：${ignore.message}"

                is FileNotFoundException, is IOException -> {
                    MessageUtil.getMessageByLocale(
                        messageCode = RUN_AGENT_WITHOUT_PERMISSION,
                        language = AgentEnv.getLocaleLanguage(),
                        params = arrayOf("${ignore.message}")
                    )
                }

                else -> MessageUtil.getMessageByLocale(
                    messageCode = UNKNOWN_ERROR,
                    language = AgentEnv.getLocaleLanguage()
                ) + " ${ignore.message}"
            }
            // #1613 worker-agent.jar 增强在启动之前的异常情况上报（本机故障）
            EngineService.submitError(
                ErrorInfo(
                    stageId = "",
                    containerId = buildVariables.containerId,
                    taskId = "",
                    taskName = "",
                    atomCode = "",
                    errorMsg = errMsg,
                    errorType = ErrorType.USER.num,
                    errorCode = ErrorCode.SYSTEM_WORKER_INITIALIZATION_ERROR
                )
            )
            throw ignore
        } finally {
            // 对应prepareWorker的兜底动作
            finishWorker(buildVariables, errMsg)
            finally(workspacePathFile, failed)

            if (systemExit) {
                exitProcess(0)
            }
        }
    }

    private fun getBuildVariables(): BuildVariables {
        try {
            // 启动成功, 报告process我已经启动了
            return EngineService.setStarted()
        } catch (ignored: Exception) {
            logger.warn("Set started catch unknown exceptions", ignored)
            handleStartException(ignored)
            throw ignored
        }
    }

    private fun handleStartException(ignored: Exception) {
        var endBuildFlag = true
        if (ignored is RemoteServiceException) {
            val errorCode = ignored.errorCode
            if (errorCode == 2101182 || errorCode == 2101255) {
                // 当构建已结束或者已经启动构建机时则不需要调结束构建接口
                endBuildFlag = false
            }
        }
        if (endBuildFlag) {
            // 启动失败，尝试结束构建
            try {
                EngineService.endBuild(emptyMap(), "", BuildJobResult(ignored.message))
            } catch (ignored: Exception) {
                logger.warn("End build catch unknown exceptions", ignored)
            }
        }
    }

    private fun prepareWorker(buildVariables: BuildVariables, workspaceInterface: WorkspaceInterface): File {
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
        LoggerService.containerHashId = buildVariables.containerHashId
        LoggerService.jobId = buildVariables.jobId ?: ""
        LoggerService.elementId = VMUtils.genStartVMTaskId(buildVariables.containerId)
        LoggerService.stepId = VMUtils.genStartVMTaskId(buildVariables.containerId)
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

    private fun finishWorker(buildVariables: BuildVariables, errMsg: String? = null) {
        LoggerService.stop()
        EngineService.endBuild(variables = buildVariables.variables, result = BuildJobResult(errMsg))
        Heartbeat.stop()
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
                    Preconditions.checkNotNull(buildTask.taskId) {
                        RemoteServiceException("Not valid build elementId")
                    }
                    // 处理task和job级别的上下文
                    combineVariables(buildTask, buildVariables)
                    val task = TaskFactory.create(buildTask.type ?: "empty")
                    val taskDaemon = TaskDaemon(task, buildTask, buildVariables, workspacePathFile)
                    try {
                        LoggerService.elementId = buildTask.taskId!!
                        LoggerService.stepId = buildTask.stepId ?: ""
                        LoggerService.elementName = buildTask.elementName ?: LoggerService.elementId
                        LoggerService.loggingLineLimit = buildVariables.loggingLineLimit?.coerceIn(1, 100)
                            ?.times(10000) ?: LOG_TASK_LINE_LIMIT
                        CredentialUtils.signToken = buildTask.signToken ?: ""

                        // 开始Task执行
                        taskDaemon.runWithTimeout()

                        // 上报Task执行结果
                        logger.info("Complete the task (${buildTask.elementName})")
                        // 获取执行结果
                        val buildTaskRst = taskDaemon.getBuildResult()
                        val finishKillFlag = task.getFinishKillFlag()
                        val projectId = buildVariables.projectId
                        handleTaskProcess(finishKillFlag, projectId, buildTask)
                        EngineService.completeTask(buildTaskRst)
                        logger.info("Finish completing the task ($buildTask)")
                    } catch (ignore: Throwable) {
                        failed = true
                        dealException(ignore, buildTask, taskDaemon)
                    } finally {
                        LoggerService.finishTask()
                        LoggerService.elementId = ""
                        LoggerService.elementName = ""
                        LoggerService.stepId = ""
                        LoggerService.loggingLineLimit = LOG_TASK_LINE_LIMIT
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

    private fun handleTaskProcess(finishKillFlag: Boolean?, projectId: String, buildTask: BuildTask) {
        if (finishKillFlag == true) {
            // 杀掉task对应的进程（配置DEVOPS_DONT_KILL_PROCESS_TREE标识的插件除外）
            KillBuildProcessTree.killProcessTree(
                projectId = projectId,
                buildId = buildTask.buildId,
                vmSeqId = buildTask.vmSeqId,
                taskIds = setOf(buildTask.taskId!!),
                forceFlag = true
            )
        }
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
        logger.warn("[Task Error] Fail to execute the task($buildTask) with task error", exception)
        val trueException = TaskExecuteExceptionDecorator.decorate(exception)

        val buildResult = taskDaemon.getBuildResult(
            isSuccess = false,
            errorMessage = trueException.errorMsg,
            errorType = trueException.errorType.name,
            errorCode = trueException.errorCode
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
        LoggerService.addNormalLine(
            MessageUtil.getMessageByLocale(
                messageCode = BK_PREPARE_TO_BUILD,
                params = arrayOf(buildId, vmSeqId),
                language = AgentEnv.getLocaleLanguage()
            )
        )
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

    /**
     *  为插件级变量填充Job前序产生的流水线变量，以及插件级的ENV
     */
    private fun combineVariables(
        buildTask: BuildTask,
        jobBuildVariables: BuildVariables
    ) {
        // 如果之前的插件不是在构建机执行, 会缺少环境变量
        val taskBuildVariable = buildTask.buildVariable?.toMutableMap() ?: mutableMapOf()
        val variablesWithType = jobBuildVariables.variablesWithType
            .associateBy { it.key }
        // job 变量能取到真实readonly，保证task 变量readOnly属性不会改变
        val taskBuildParameters = taskBuildVariable.map { (key, value) ->
            BuildParameters(key = key, value = value, readOnly = variablesWithType[key]?.readOnly)
        }.associateBy { it.key }
        jobBuildVariables.variables = jobBuildVariables.variables.plus(taskBuildVariable)
        // 以key去重, 并以buildTask中的为准
        jobBuildVariables.variablesWithType = variablesWithType
            .plus(taskBuildParameters)
            .values.toList()

        // 填充插件级的ENV参数
        val customEnvStr = buildTask.params?.get(Element::customEnv.name)
        val dialect = PipelineDialectUtil.getPipelineDialect(jobBuildVariables.variables[PIPELINE_DIALECT])
        if (customEnvStr != null) {
            val customEnv = try {
                JsonUtil.toOrNull(customEnvStr, object : TypeReference<List<NameAndValue>>() {})
            } catch (ignore: Throwable) {
                logger.warn("Parse customEnv with error: ", ignore)
                null
            }
            if (customEnv.isNullOrEmpty()) return
            val jobVariables = jobBuildVariables.variables.toMutableMap()
            customEnv.forEach {
                if (!it.key.isNullOrBlank()) {
                    // 解决BUG:93319235,将Task的env变量key加env.前缀塞入variables，塞入之前需要对value做替换
                    val value = EnvReplacementParser.parse(
                        value = it.value ?: "",
                        contextMap = jobVariables,
                        onlyExpression = dialect.supportUseExpression(),
                        functions = SpecialFunctions.functions,
                        output = SpecialFunctions.output
                    )
                    jobVariables["envs.${it.key}"] = value
                    taskBuildVariable[it.key!!] = value
                }
            }
            buildTask.buildVariable = taskBuildVariable
            jobBuildVariables.variables = jobVariables
        }
    }
}
