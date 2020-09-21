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

package com.tencent.devops.process.engine.atom.vm

import com.tencent.devops.common.api.enums.AgentStatus
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.Zone
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.EnvControlTaskType
import com.tencent.devops.common.pipeline.enums.VMBaseOS
import com.tencent.devops.common.pipeline.type.DispatchType
import com.tencent.devops.common.pipeline.type.agent.AgentType
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentEnvDispatchType
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentIDDispatchType
import com.tencent.devops.common.pipeline.type.docker.DockerDispatchType
import com.tencent.devops.common.pipeline.type.exsi.ESXiDispatchType
import com.tencent.devops.common.pipeline.type.tstack.TStackDispatchType
import com.tencent.devops.environment.api.thirdPartyAgent.ServiceThirdPartyAgentResource
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PIPELINE_AGENT_STATUS_EXCEPTION
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PIPELINE_MODEL_NOT_EXISTS
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PIPELINE_NODEL_CONTAINER_NOT_EXISTS
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS
import com.tencent.devops.process.engine.atom.AtomResponse
import com.tencent.devops.process.engine.atom.AtomUtils
import com.tencent.devops.process.engine.atom.IAtomTask
import com.tencent.devops.process.engine.atom.parser.DispatchTypeParser
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.engine.exception.BuildTaskException
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.engine.pojo.event.PipelineContainerAgentHeartBeatEvent
import com.tencent.devops.process.engine.service.PipelineBuildDetailService
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.pipeline.type.BuildType
import com.tencent.devops.process.engine.atom.defaultFailAtomResponse
import com.tencent.devops.process.pojo.mq.PipelineAgentShutdownEvent
import com.tencent.devops.process.pojo.mq.PipelineAgentStartupEvent
import com.tencent.devops.process.service.BuildVariableService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import javax.xml.bind.Element

/**
 *
 * @version 1.0
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class DispatchVMStartupTaskAtom @Autowired constructor(
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val client: Client,
    private val pipelineBuildDetailService: PipelineBuildDetailService,
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val buildVariableService: BuildVariableService,
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val buildLogPrinter: BuildLogPrinter,
    private val dispatchTypeParser: DispatchTypeParser
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
        try {
            atomResponse = execute(task, param)
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
        } catch (t: Throwable) {
            buildLogPrinter.addRedLine(
                buildId = task.buildId,
                message = "Fail to execute the task atom: ${t.message}",
                tag = task.taskId,
                jobId = task.containerHashId,
                executeCount = task.executeCount ?: 1
            )
            logger.warn("Fail to execute the task atom", t)
            atomResponse = AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = ErrorType.SYSTEM,
                errorCode = ErrorCode.SYSTEM_WORKER_INITIALIZATION_ERROR,
                errorMsg = t.message
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
            ?: throw BuildTaskException(
                errorType = ErrorType.SYSTEM,
                errorCode = ERROR_PIPELINE_NOT_EXISTS.toInt(),
                errorMsg = "流水线不存在",
                pipelineId = pipelineId,
                buildId = buildId,
                taskId = taskId
            )

        val model = pipelineBuildDetailService.getBuildModel(buildId)
            ?: throw BuildTaskException(
                errorType = ErrorType.SYSTEM,
                errorCode = ERROR_PIPELINE_MODEL_NOT_EXISTS.toInt(),
                errorMsg = "流水线模型不存在",
                pipelineId = pipelineId,
                buildId = buildId,
                taskId = taskId
            )

        val container = model.getContainer(vmSeqId)
            ?: throw BuildTaskException(
                errorType = ErrorType.SYSTEM,
                errorCode = ERROR_PIPELINE_NODEL_CONTAINER_NOT_EXISTS.toInt(),
                errorMsg = "流水线的模型中指定构建容器${vmNames}不存在",
                pipelineId = pipelineId,
                buildId = buildId,
                taskId = taskId
            )

        // 这个任务是在构建子流程启动的，所以必须使用根流程进程ID
        // 注意区分buildId和vmSeqId，BuildId是一次构建整体的ID，
        // TODO vmSeqId是该构建环境下的ID,旧流水引擎数据无法转换为String，仍然是序号的方式
        pipelineBuildDetailService.containerPreparing(buildId, vmSeqId.toInt())

        // 读取插件市场中的插件信息，写入待构建处理
        val atoms = AtomUtils.parseContainerMarketAtom(
            container = container,
            task = task,
            client = client,
            buildLogPrinter = buildLogPrinter
        )

        val source = "vmStartupTaskAtom"
        val dispatchType = getDispatchType(projectId, pipelineId, buildId, param, param.baseOS)

        // 处理dispatchType中的BKSTORE镜像信息
        dispatchTypeParser.parse(
            userId = task.starter,
            projectId = task.projectId,
            pipelineId = task.pipelineId,
            buildId = task.buildId,
            dispatchType = dispatchType
        )

        dispatchType.replaceVariable(buildVariableService.getAllVariable(buildId))

        pipelineEventDispatcher.dispatch(
            PipelineAgentStartupEvent(
                source = source,
                projectId = projectId,
                pipelineId = pipelineId,
                pipelineName = pipelineInfo.pipelineName,
                userId = task.starter,
                buildId = buildId,
                buildNo = pipelineRuntimeService.getBuildInfo(buildId)!!.buildNum,
                vmSeqId = vmSeqId,
                taskName = param.name,
                os = param.baseOS.name,
                vmNames = vmNames,
                startTime = System.currentTimeMillis(),
                channelCode = pipelineInfo.channelCode.name,
                dispatchType = dispatchType,
                zone = getBuildZone(projectId, container),
                atoms = atoms,
                executeCount = task.executeCount,
                routeKeySuffix = dispatchType.routeKeySuffix?.routeKeySuffix,
                stageId = task.stageId,
                containerId = task.containerId,
                containerHashId = task.containerHashId,
                containerType = task.containerType
            ),
            PipelineContainerAgentHeartBeatEvent(
                source = source,
                projectId = projectId,
                pipelineId = pipelineId,
                userId = task.starter,
                buildId = buildId,
                containerId = task.containerId
            )
        )
        logger.info("[$buildId]|STARTUP_VM|VM=${param.baseOS}-$vmNames($vmSeqId)|Dispatch startup")
        return AtomResponse(BuildStatus.CALL_WAITING)
    }

    private fun getDispatchType(
        projectId: String,
        pipelineId: String,
        buildId: String,
        param: VMBuildContainer,
        os: VMBaseOS
    ): DispatchType {

        /**
         * 新版的构建环境直接传入指定的构建机方式
         */
        if (param.dispatchType != null) {
            return param.dispatchType!!
        }

        // 第三方构建机ID
        val agentId = param.thirdPartyAgentId ?: ""
        // 构建环境ID
        val thirdPartyEnvId = param.thirdPartyAgentEnvId ?: ""
        // 第三方构建机指定工作空间
        val thirdPartyWorkspace = param.thirdPartyWorkspace ?: ""

        if (!agentId.isBlank() || !thirdPartyEnvId.isBlank()) {
            checkThirdPartyAgentStatus(projectId, pipelineId, buildId, agentId, thirdPartyEnvId)
            return if (!agentId.isBlank()) {
                ThirdPartyAgentIDDispatchType(
                    agentId,
                    thirdPartyWorkspace,
                    AgentType.ID
                )
            } else {
                ThirdPartyAgentEnvDispatchType(
                    thirdPartyEnvId,
                    thirdPartyWorkspace,
                    AgentType.ID
                )
            }
        }
        // docker建机指定版本
        val dockerBuildVersion = param.dockerBuildVersion

        if (!dockerBuildVersion.isNullOrBlank()) {
            return DockerDispatchType(dockerBuildVersion!!)
        }

        // tstack节点ID
        val tstackAgentId = param.tstackAgentId

        if (os == VMBaseOS.WINDOWS) {
            return TStackDispatchType(tstackAgentId ?: "")
        }
        return ESXiDispatchType()
    }

    private fun getBuildZone(projectId: String, container: Container): Zone? {
        try {
            if (container !is VMBuildContainer) {
                return null
            }

            if (container.enableExternal == true) {
                return Zone.EXTERNAL
            }
        } catch (ignored: Throwable) {
            logger.warn("Fail to get the build zone of container $container", ignored)
        }
        return null
    }

    // 检查构建机的状态，如果是有问题就直接抛出异常
    private fun checkThirdPartyAgentStatus(
        projectId: String,
        pipelineId: String,
        buildId: String,
        agentId: String?,
        agentEnvId: String?
    ) {
        try {
            if (!agentId.isNullOrBlank()) {
                checkByAgentId(projectId, agentId, pipelineId, buildId)
            } else if (!agentEnvId.isNullOrBlank()) {
                checkByAgentEnvId(projectId, agentEnvId, buildId, pipelineId)
            }
        } catch (t: BuildTaskException) {
            throw t
        } catch (ignored: Throwable) {
            logger.warn("Fail to check the agent($agentId) status of project $projectId", ignored)
        }
    }

    private fun checkByAgentEnvId(
        projectId: String,
        agentEnvId: String?,
        buildId: String,
        pipelineId: String
    ) {
        val agentResult =
            client.get(ServiceThirdPartyAgentResource::class).getAgentsByEnvId(projectId, agentEnvId!!)
        if (agentResult.isNotOk()) {
            logger.warn("[$buildId]|Fail to get the agent env($agentEnvId) status: $agentResult")
            return
        }
        val agents = agentResult.data ?: throw agentException(pipelineId, buildId)

        agents.forEach {
            logger.info("Get the agent $it of env $agentEnvId of project $projectId")
            if (it.status == AgentStatus.IMPORT_OK) {
                return
            }
        }

        logger.warn("All agents($agents) of env $agentEnvId are exception of project $projectId")
        throw agentException(pipelineId, buildId)
    }

    private fun checkByAgentId(
        projectId: String,
        agentId: String?,
        pipelineId: String,
        buildId: String
    ) {
        val agentResult = client.get(ServiceThirdPartyAgentResource::class).getAgentById(projectId, agentId!!)
        if (agentResult.isAgentDelete()) {
            logger.warn("The agent($agentId) of project $projectId is already delete")
            throw agentException(pipelineId, buildId)
        }
        val agent = agentResult.data ?: throw agentException(pipelineId, buildId)
        if (agent.status != AgentStatus.IMPORT_OK) {
            logger.warn("The agent status(${agent.status}) is not OK")
            throw agentException(pipelineId, buildId)
        }
    }

    override fun tryFinish(task: PipelineBuildTask, param: VMBuildContainer, runVariables: Map<String, String>, force: Boolean): AtomResponse {
        return if (force) {
            if (BuildStatus.isFinish(task.status)) {
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
                        buildResult = true,
                        routeKeySuffix = param.dispatchType?.routeKeySuffix?.routeKeySuffix
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

    private fun agentException(pipelineId: String, buildId: String) =
        BuildTaskException(
            errorType = ErrorType.SYSTEM,
            errorCode = ERROR_PIPELINE_AGENT_STATUS_EXCEPTION.toInt(),
            errorMsg = "第三方构建机的Agent状态异常",
            pipelineId = pipelineId,
            buildId = buildId,
            taskId = null
        )

    companion object {
        /**
         * 生成构建任务
         */
        fun makePipelineBuildTask(
            projectId: String,
            pipelineId: String,
            buildId: String,
            stageId: String,
            container: Container,
            containerSeq: Int,
            taskSeq: Int,
            userId: String
        ): PipelineBuildTask {

            val taskParams = container.genTaskParams()
            taskParams["elements"] = emptyList<Element>() // elements可能过多导致存储问题
            val taskAtom = AtomUtils.parseAtomBeanName(DispatchVMStartupTaskAtom::class.java)
            val buildType = (container as VMBuildContainer).dispatchType?.buildType()?.name ?: BuildType.DOCKER.name
            val baseOS = (container as VMBuildContainer).baseOS.name

            return PipelineBuildTask(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                stageId = stageId,
                containerId = container.id!!,
                containerHashId = container.containerId,
                containerType = container.getClassType(),
                taskSeq = taskSeq,
                taskId = VMUtils.genStartVMTaskId(container.id!!),
                taskName = "Prepare_Job#${container.id!!}",
                taskType = EnvControlTaskType.VM.name,
                taskAtom = taskAtom,
                status = BuildStatus.QUEUE,
                taskParams = taskParams,
                executeCount = 1,
                starter = userId,
                approver = null,
                subBuildId = null,
                additionalOptions = null,
                atomCode = "$taskAtom-$buildType-$baseOS"
            )
        }
    }
}
