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

package com.tencent.devops.process.engine.atom.vm

import com.tencent.devops.common.api.check.Preconditions
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.pojo.Zone
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.type.DispatchType
import com.tencent.devops.common.pipeline.type.agent.AgentType
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentEnvDispatchType
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentIDDispatchType
import com.tencent.devops.common.pipeline.type.docker.DockerDispatchType
import com.tencent.devops.common.pipeline.type.exsi.ESXiDispatchType
import com.tencent.devops.dispatch.api.ServiceDispatchJobResource
import com.tencent.devops.dispatch.pojo.AgentStartMonitor
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PIPELINE_NODEL_CONTAINER_NOT_EXISTS
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS
import com.tencent.devops.process.engine.atom.AtomResponse
import com.tencent.devops.process.engine.atom.AtomUtils
import com.tencent.devops.process.engine.atom.IAtomTask
import com.tencent.devops.process.engine.atom.defaultFailAtomResponse
import com.tencent.devops.process.engine.atom.parser.DispatchTypeParser
import com.tencent.devops.process.engine.exception.BuildTaskException
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.engine.service.detail.ContainerBuildDetailService
import com.tencent.devops.process.pojo.mq.PipelineAgentShutdownEvent
import com.tencent.devops.process.pojo.mq.PipelineAgentStartupEvent
import com.tencent.devops.process.service.BuildVariableService
import com.tencent.devops.process.service.PipelineContextService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

/**
 *
 * @version 1.0
 */
@Suppress("UNUSED", "LongParameterList")
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class DispatchVMStartupTaskAtom @Autowired constructor(
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val client: Client,
    private val containerBuildDetailService: ContainerBuildDetailService,
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val buildVariableService: BuildVariableService,
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val buildLogPrinter: BuildLogPrinter,
    private val dispatchTypeParser: DispatchTypeParser,
    private val pipelineContextService: PipelineContextService
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
        val buildEnv = param.customBuildEnv?.map { mit ->
            mit.key to EnvUtils.parseEnv(mit.value, context)
        }?.toMap()
        val fixParam = param.copy(customBuildEnv = buildEnv)

        try {
            atomResponse = execute(task, fixParam)
            buildLogPrinter.stopLog(
                buildId = task.buildId,
                tag = task.taskId,
                jobId = task.containerHashId,
                executeCount = task.executeCount ?: 1
            )
        } catch (e: BuildTaskException) {
            buildLogPrinter.addRedLine(
                buildId = task.buildId,
                message = "Fail to execute the task atom: ${e.message}",
                tag = task.taskId,
                jobId = task.containerHashId,
                executeCount = task.executeCount ?: 1
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
                jobId = task.containerHashId,
                executeCount = task.executeCount ?: 1
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

    fun execute(task: PipelineBuildTask, param: VMBuildContainer): AtomResponse {
        val projectId = task.projectId
        val pipelineId = task.pipelineId
        val buildId = task.buildId
        val taskId = task.taskId

        // 构建环境容器序号ID
        val vmSeqId = task.containerId

        // 预指定VM名称列表（逗号分割）
        val vmNames = param.vmNames.joinToString(",")

        val pipelineInfo = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId)
        Preconditions.checkNotNull(
            obj = pipelineInfo,
            exception = BuildTaskException(
                errorType = ErrorType.SYSTEM,
                errorCode = ERROR_PIPELINE_NOT_EXISTS.toInt(),
                errorMsg = "流水线不存在",
                pipelineId = pipelineId,
                buildId = buildId,
                taskId = taskId
            )
        )

        val container = containerBuildDetailService.getBuildModel(projectId, buildId)?.getContainer(vmSeqId)
        Preconditions.checkNotNull(
            obj = container,
            exception = BuildTaskException(
                errorType = ErrorType.SYSTEM,
                errorCode = ERROR_PIPELINE_NODEL_CONTAINER_NOT_EXISTS.toInt(),
                errorMsg = "流水线的模型中指定构建容器${vmNames}不存在",
                pipelineId = pipelineId,
                buildId = buildId,
                taskId = taskId
            )
        )

        // 这个任务是在构建子流程启动的，所以必须使用根流程进程ID
        // 注意区分buildId和vmSeqId，BuildId是一次构建整体的ID，
        // vmSeqId是该构建环境下的ID,旧流水引擎数据无法转换为String，仍然是序号的方式
        containerBuildDetailService.containerPreparing(projectId, buildId, vmSeqId)

        dispatch(task, pipelineInfo!!, param, vmNames, container!!)
        logger.info("[$buildId]|STARTUP_VM|VM=${param.baseOS}-$vmNames($vmSeqId)|Dispatch startup")
        return AtomResponse(BuildStatus.CALL_WAITING)
    }

    private fun dispatch(
        task: PipelineBuildTask,
        pipelineInfo: PipelineInfo,
        param: VMBuildContainer,
        vmNames: String,
        container: Container
    ) {

        // 读取插件市场中的插件信息，写入待构建处理
        val atoms = AtomUtils.parseContainerMarketAtom(
            container = container,
            task = task,
            client = client,
            buildLogPrinter = buildLogPrinter
        )

        val dispatchType = getDispatchType(task, param)

        pipelineEventDispatcher.dispatch(
            PipelineAgentStartupEvent(
                source = "vmStartupTaskAtom",
                projectId = task.projectId,
                pipelineId = task.pipelineId,
                pipelineName = pipelineInfo.pipelineName,
                userId = task.starter,
                buildId = task.buildId,
                buildNo = pipelineRuntimeService.getBuildInfo(task.projectId, task.buildId)!!.buildNum,
                vmSeqId = task.containerId,
                taskName = param.name,
                os = param.baseOS.name,
                vmNames = vmNames,
                startTime = System.currentTimeMillis(),
                channelCode = pipelineInfo.channelCode.name,
                dispatchType = dispatchType,
                zone = getBuildZone(container),
                atoms = atoms,
                executeCount = task.executeCount,
                routeKeySuffix = dispatchType.routeKeySuffix?.routeKeySuffix,
                stageId = task.stageId,
                containerId = task.containerId,
                containerHashId = task.containerHashId,
                queueTimeoutMinutes = param.jobControlOption?.prepareTimeout,
                containerType = task.containerType,
                customBuildEnv = param.customBuildEnv
            )
        )
    }

    private fun getDispatchType(task: PipelineBuildTask, param: VMBuildContainer): DispatchType {

        val dispatchType: DispatchType
        /**
         * 新版的构建环境直接传入指定的构建机方式
         */
        if (param.dispatchType != null) {
            dispatchType = param.dispatchType!!
        } else {
            // 第三方构建机ID
            val agentId = param.thirdPartyAgentId ?: ""
            // 构建环境ID
            val envId = param.thirdPartyAgentEnvId ?: ""
            val workspace = param.thirdPartyWorkspace ?: ""
            dispatchType = if (agentId.isNotBlank()) {
                ThirdPartyAgentIDDispatchType(displayName = agentId, workspace = workspace, agentType = AgentType.ID)
            } else if (envId.isNotBlank()) {
                ThirdPartyAgentEnvDispatchType(envName = envId, workspace = workspace, agentType = AgentType.ID)
            } // docker建机指定版本(旧)
            else if (!param.dockerBuildVersion.isNullOrBlank()) {
                DockerDispatchType(param.dockerBuildVersion!!)
            } else {
                ESXiDispatchType()
            }
        }

        // 处理dispatchType中的BKSTORE镜像信息
        dispatchTypeParser.parse(
            userId = task.starter, projectId = task.projectId,
            pipelineId = task.pipelineId, buildId = task.buildId, dispatchType = dispatchType
        )

        dispatchType.replaceVariable(buildVariableService.getAllVariable(task.projectId, task.buildId))
        return dispatchType
    }

    private fun getBuildZone(container: Container): Zone? {
        return when {
            container !is VMBuildContainer -> null
            container.enableExternal == true -> Zone.EXTERNAL
            else -> null
        }
    }

    override fun tryFinish(
        task: PipelineBuildTask,
        param: VMBuildContainer,
        runVariables: Map<String, String>,
        force: Boolean
    ): AtomResponse {
        monitorPrint(task)
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
                        routeKeySuffix = param.dispatchType?.routeKeySuffix?.routeKeySuffix,
                        executeCount = task.executeCount
                    )
                )
                defaultFailAtomResponse
            }
        } else {
            AtomResponse(
                buildStatus = task.status,
                errorType = task.errorType,
                errorCode = task.errorCode,
                errorMsg = task.errorMsg
            )
        }
    }

    private fun monitorPrint(task: PipelineBuildTask) {
        // #5806 超过10秒，开始查询调度情况，并Log出来
        val timePasses = System.currentTimeMillis() - (task.startTime?.timestampmilli() ?: 0L)
        val modSeconds = TimeUnit.MILLISECONDS.toSeconds(timePasses) % 60

        var firstTime = false
        if (TimeUnit.MILLISECONDS.toMinutes(timePasses) == 0L) { // 首次循环，都是在1分钟内。
            if (modSeconds < 10) {
                firstTime = true
            }
        }
        /*
            此处说明： 在每20秒的前9秒内会执行一下以下逻辑。每5秒一次的本方法调用，在取9秒是防5秒在不断累计延迟可能会产生的最大限度不精准
         */
        if (firstTime || (modSeconds % 20 < 9)) {

            val agentMonitor = AgentStartMonitor(
                projectId = task.projectId,
                pipelineId = task.pipelineId,
                buildId = task.buildId,
                vmSeqId = task.containerId,
                containerHashId = task.containerHashId,
                userId = task.starter,
                firstTime = true,
                executeCount = task.executeCount
            )
            client.get(ServiceDispatchJobResource::class).monitor(agentStartMonitor = agentMonitor)
        }
    }
}
