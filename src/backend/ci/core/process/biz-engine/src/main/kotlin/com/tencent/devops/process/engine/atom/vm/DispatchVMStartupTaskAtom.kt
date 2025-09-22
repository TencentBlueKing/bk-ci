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

package com.tencent.devops.process.engine.atom.vm

import com.tencent.devops.common.api.constant.CommonMessageCode.BK_ENV_NOT_YET_SUPPORTED
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.SampleEventDispatcher
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.EnvReplacementParser
import com.tencent.devops.common.pipeline.NameAndValue
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.dialect.PipelineDialectUtil
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentEnvDispatchType
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentIDDispatchType
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.dispatch.api.ServiceDispatchJobResource
import com.tencent.devops.dispatch.pojo.AgentStartMonitor
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PIPELINE_NODEL_CONTAINER_NOT_EXISTS
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS
import com.tencent.devops.process.engine.atom.AtomResponse
import com.tencent.devops.process.engine.atom.AtomUtils
import com.tencent.devops.process.engine.atom.IAtomTask
import com.tencent.devops.process.engine.atom.defaultFailAtomResponse
import com.tencent.devops.process.engine.atom.parser.DispatchTypeBuilder
import com.tencent.devops.process.engine.common.BS_ATOM_STATUS_REFRESH_DELAY_MILLS
import com.tencent.devops.process.engine.exception.BuildTaskException
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.engine.service.PipelineTaskService
import com.tencent.devops.process.engine.service.record.ContainerBuildRecordService
import com.tencent.devops.process.pojo.mq.PipelineAgentShutdownEvent
import com.tencent.devops.process.pojo.mq.PipelineAgentStartupEvent
import com.tencent.devops.process.service.PipelineContextService
import com.tencent.devops.process.utils.BK_CI_AUTHORIZER
import com.tencent.devops.process.utils.PIPELINE_DIALECT
import com.tencent.devops.process.yaml.transfer.VariableDefault
import com.tencent.devops.store.api.container.ServiceContainerAppResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

/**
 *
 * @version 1.0
 */
@Suppress("LongParameterList", "LongMethod", "MagicNumber")
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class DispatchVMStartupTaskAtom @Autowired constructor(
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val client: Client,
    private val containerBuildRecordService: ContainerBuildRecordService,
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val pipelineEventDispatcher: SampleEventDispatcher,
    private val buildLogPrinter: BuildLogPrinter,
    private val dispatchTypeBuilder: DispatchTypeBuilder,
    private val pipelineContextService: PipelineContextService,
    private val pipelineTaskService: PipelineTaskService
) : IAtomTask<VMBuildContainer> {
    override fun getParamElement(task: PipelineBuildTask): VMBuildContainer {
        return JsonUtil.mapTo(task.taskParams, VMBuildContainer::class.java)
    }

    private val logger = LoggerFactory.getLogger(DispatchVMStartupTaskAtom::class.java)

    override fun execute(
        task: PipelineBuildTask,
        param: VMBuildContainer,
        runVariables: Map<String, String>
    ): AtomResponse {
        var atomResponse: AtomResponse
        // 解决BUG:93319235,env变量提前替换
        val context = pipelineContextService.getAllBuildContext(runVariables)
        val buildEnv = param.customEnv?.map { mit ->
            NameAndValue(mit.key, EnvUtils.parseEnv(mit.value, context))
        }
        val fixParam = param.copy(customEnv = buildEnv)
        val executeCount = task.executeCount ?: 1
        try {
            atomResponse = if (!checkBeforeStart(task, param, context)) {
                AtomResponse(
                    BuildStatus.FAILED,
                    errorType = ErrorType.USER,
                    errorCode = ErrorCode.USER_INPUT_INVAILD,
                    errorMsg = "check job start fail"
                )
            } else {
                execute(task, fixParam, null, runVariables[BK_CI_AUTHORIZER])
            }
            buildLogPrinter.stopLog(
                buildId = task.buildId,
                tag = task.taskId,
                containerHashId = task.containerHashId,
                executeCount = executeCount,
                jobId = param.jobId,
                stepId = task.stepId
            )
        } catch (e: BuildTaskException) {
            buildLogPrinter.addRedLine(
                buildId = task.buildId,
                message = "Fail to execute the task atom: ${e.message}",
                tag = task.taskId,
                containerHashId = task.containerHashId,
                executeCount = executeCount,
                jobId = null,
                stepId = task.stepId
            )
            logger.warn("Fail to execute the task atom", e)
            atomResponse = AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = e.errorType,
                errorCode = e.errorCode,
                errorMsg = e.message
            )
        } catch (ignored: Throwable) {
            buildLogPrinter.addRedLine(
                buildId = task.buildId,
                message = "Fail to execute the task atom: ${ignored.message}",
                tag = task.taskId,
                containerHashId = task.containerHashId,
                executeCount = executeCount,
                jobId = null,
                stepId = task.stepId
            )
            logger.warn("Fail to execute the task atom", ignored)
            atomResponse = AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = ErrorType.SYSTEM,
                errorCode = ErrorCode.SYSTEM_WORKER_INITIALIZATION_ERROR,
                errorMsg = ignored.message
            )
        }
        return atomResponse
    }

    fun execute(
        task: PipelineBuildTask,
        param: VMBuildContainer,
        ignoreEnvAgentIds: Set<String>?,
        pipelineAuthorizer: String? = ""
    ): AtomResponse {
        val projectId = task.projectId
        val pipelineId = task.pipelineId
        val buildId = task.buildId
        val taskId = task.taskId

        // 构建环境容器序号ID
        val vmSeqId = task.containerId

        // 预指定VM名称列表（逗号分割）
        val vmNames = param.vmNames.joinToString(",")

        val pipelineInfo = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId) ?: throw BuildTaskException(
            errorType = ErrorType.SYSTEM,
            errorCode = ERROR_PIPELINE_NOT_EXISTS.toInt(),
            errorMsg = MessageUtil.getMessageByLocale(
                ERROR_PIPELINE_NOT_EXISTS, I18nUtil.getDefaultLocaleLanguage()
            ),
            pipelineId = pipelineId,
            buildId = buildId,
            taskId = taskId
        )
        val executeCount = task.executeCount ?: 1
        val buildRecordContainer = containerBuildRecordService.getRecord(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            containerId = vmSeqId,
            executeCount = executeCount
        ) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_NO_BUILD_EXISTS_BY_ID, params = arrayOf(buildId)
        )
        val container = containerBuildRecordService.getRecordModel(
            projectId = projectId,
            pipelineId = pipelineId,
            version = buildRecordContainer.resourceVersion,
            buildId = buildId,
            executeCount = executeCount
        )?.getContainer(vmSeqId) ?: throw BuildTaskException(
            errorType = ErrorType.SYSTEM,
            errorCode = ERROR_PIPELINE_NODEL_CONTAINER_NOT_EXISTS.toInt(),
            errorMsg = MessageUtil.getMessageByLocale(
                ERROR_PIPELINE_NODEL_CONTAINER_NOT_EXISTS,
                I18nUtil.getDefaultLocaleLanguage(),
                arrayOf(vmNames)
            ),
            pipelineId = pipelineId,
            buildId = buildId,
            taskId = taskId
        )

        // 这个任务是在构建子流程启动的，所以必须使用根流程进程ID
        // 注意区分buildId和vmSeqId，BuildId是一次构建整体的ID，
        // vmSeqId是该构建环境下的ID,旧流水引擎数据无法转换为String，仍然是序号的方式
        containerBuildRecordService.containerPreparing(
            projectId, pipelineId, buildId, vmSeqId, task.executeCount ?: 1
        )
        dispatch(
            task = task,
            pipelineInfo = pipelineInfo,
            param = param,
            vmNames = vmNames,
            container = container,
            ignoreEnvAgentIds = ignoreEnvAgentIds,
            pipelineAuthorizer = pipelineAuthorizer
        )
        logger.info("[$buildId]|STARTUP_VM|VM=${param.baseOS}-$vmNames($vmSeqId)|Dispatch startup")
        return AtomResponse(BuildStatus.RUNNING)
    }

    private fun dispatch(
        task: PipelineBuildTask,
        pipelineInfo: PipelineInfo,
        param: VMBuildContainer,
        vmNames: String,
        container: Container,
        ignoreEnvAgentIds: Set<String>?,
        pipelineAuthorizer: String? = ""
    ) {

        // 读取插件市场中的插件信息，写入待构建处理
        val atoms = AtomUtils.parseContainerMarketAtom(
            container = container,
            task = task,
            client = client,
            buildLogPrinter = buildLogPrinter
        )

        val dispatchType = dispatchTypeBuilder.getDispatchType(task, param)
        val customBuildEnv = mutableMapOf<String, String>()
        param.customEnv?.forEach {
            if (!it.key.isNullOrBlank()) customBuildEnv[it.key!!] = it.value ?: ""
        }
        pipelineEventDispatcher.dispatch(
            PipelineAgentStartupEvent(
                source = "vmStartupTaskAtom",
                projectId = task.projectId,
                pipelineId = task.pipelineId,
                pipelineName = pipelineInfo.pipelineName,
                userId = pipelineAuthorizer?.takeIf { it.isNotEmpty() } ?: task.starter,
                buildId = task.buildId,
                buildNo = pipelineRuntimeService.getBuildInfo(task.projectId, task.buildId)!!.buildNum,
                vmSeqId = task.containerId,
                taskName = param.name,
                os = param.baseOS.name,
                vmNames = vmNames,
                channelCode = pipelineInfo.channelCode.name,
                dispatchType = dispatchType,
                atoms = atoms,
                executeCount = task.executeCount,
                routeKeySuffix = dispatchType.routeKeySuffix?.routeKeySuffix,
                containerId = task.containerId,
                containerHashId = task.containerHashId,
                queueTimeoutMinutes = param.jobControlOption?.prepareTimeout,
                customBuildEnv = customBuildEnv,
                jobId = container.jobId,
                ignoreEnvAgentIds = ignoreEnvAgentIds,
                singleNodeConcurrency = param.jobControlOption?.singleNodeConcurrency,
                allNodeConcurrency = param.jobControlOption?.allNodeConcurrency,
                jobTimeoutMinutes = param.jobControlOption?.timeoutVar?.toIntOrNull() ?: param.jobControlOption?.timeout
                ?: VariableDefault.DEFAULT_JOB_MAX_RUNNING_MINUTES
            )
        )
    }

    /**
     * job启动做一些特别参数的检查，检查失败时直接不启动容器
     */
    private fun checkBeforeStart(
        task: PipelineBuildTask,
        param: VMBuildContainer,
        variables: Map<String, String>
    ): Boolean {
        param.buildEnv?.let { buildEnv ->
            val asCode by lazy {
                val dialect = PipelineDialectUtil.getPipelineDialect(variables[PIPELINE_DIALECT])
                val contextPair = if (dialect.supportUseExpression()) {
                    EnvReplacementParser.getCustomExecutionContextByMap(variables)
                } else null
                Pair(dialect, contextPair)
            }
            buildEnv.forEach { env ->
                if (!env.value.startsWith("$")) {
                    return@forEach
                }
                val version = EnvReplacementParser.parse(
                    value = env.value,
                    contextMap = variables,
                    onlyExpression = asCode.first.supportUseExpression(),
                    contextPair = asCode.second
                )
                val res = client.get(ServiceContainerAppResource::class).getBuildEnv(
                    name = env.key,
                    version = version,
                    os = param.baseOS.name.lowercase()
                ).data
                if (res == null) {
                    buildLogPrinter.addRedLine(
                        buildId = task.buildId,
                        message = MessageUtil.getMessageByLocale(
                            BK_ENV_NOT_YET_SUPPORTED,
                            I18nUtil.getDefaultLocaleLanguage(),
                            arrayOf(env.key, version)
                        ),
                        tag = task.taskId,
                        containerHashId = task.containerHashId,
                        executeCount = task.executeCount ?: 1,
                        jobId = null,
                        stepId = task.stepId
                    )
                    return false
                }
            }
        }
        return true
    }

    override fun tryFinish(
        task: PipelineBuildTask,
        param: VMBuildContainer,
        runVariables: Map<String, String>,
        force: Boolean
    ): AtomResponse {
        return if (force) {
            if (task.status.isFinish()) {
                AtomResponse(
                    buildStatus = task.status,
                    errorType = task.errorType,
                    errorCode = task.errorCode,
                    errorMsg = task.errorMsg
                )
            } else { // 强制终止的设置为失败
                logger.warn("[${task.buildId}]|[FORCE_STOP_IN_START_TASK]")
                pipelineEventDispatcher.dispatch(
                    PipelineAgentShutdownEvent(
                        source = "force_stop_startVM",
                        projectId = task.projectId,
                        pipelineId = task.pipelineId,
                        userId = task.starter,
                        buildId = task.buildId,
                        vmSeqId = task.containerId,
                        buildResult = false, // #5046 强制终止为失败
                        dispatchType = dispatchTypeBuilder.getDispatchType(task, param),
                        routeKeySuffix = dispatchTypeBuilder
                            .getDispatchType(task, param)
                            .routeKeySuffix?.routeKeySuffix,
                        executeCount = task.executeCount,
                        jobId = task.jobId,
                        containerHashId = task.containerHashId
                    )
                )
                defaultFailAtomResponse
            }
        } else {
            // 第三方构建机支持打印监控
            if (param.dispatchType is ThirdPartyAgentEnvDispatchType ||
                param.dispatchType is ThirdPartyAgentIDDispatchType
            ) {
                // #9910 环境构建时遇到启动错误时调度到一个新的Agent
                // 通过获取task param一个固定的参数，重新发送启动请求
                val retryThirdAgentEnv = task.taskParams["RETRY_THIRD_AGENT_ENV"]?.toString()
                if (!retryThirdAgentEnv.isNullOrBlank()) {
                    task.taskParams.remove("RETRY_THIRD_AGENT_ENV")
                    pipelineTaskService.updateTaskParam(
                        transactionContext = null,
                        projectId = task.projectId,
                        buildId = task.buildId,
                        taskId = task.taskId,
                        taskParam = JsonUtil.toJson(task.taskParams)
                    )
                    return execute(
                        task = task,
                        param = param,
                        ignoreEnvAgentIds = retryThirdAgentEnv.split(",").filter { it.isNotBlank() }.toSet()
                    )
                }

                try {
                    thirdPartyAgentRollBackMonitor(task)
                    // 修改轮训时间,3m30s方便命中dispath的3分钟频率
                    task.taskParams[BS_ATOM_STATUS_REFRESH_DELAY_MILLS] = (3.5 * 60 * 1000).toInt()
                } catch (ignore: Exception) {
                    // 忽略掉因调用打印接口出错而导致调度失败的问题
                }
            }

            AtomResponse(
                buildStatus = task.status,
                errorType = task.errorType,
                errorCode = task.errorCode,
                errorMsg = task.errorMsg
            )
        }
    }

    private fun thirdPartyAgentRollBackMonitor(task: PipelineBuildTask) {
        val agentMonitor = AgentStartMonitor(
            projectId = task.projectId,
            pipelineId = task.pipelineId,
            buildId = task.buildId,
            vmSeqId = task.containerId,
            containerHashId = task.containerHashId,
            userId = task.starter,
            executeCount = task.executeCount,
            stepId = task.stepId
        )
        client.get(ServiceDispatchJobResource::class).monitor(agentStartMonitor = agentMonitor)
    }
}
