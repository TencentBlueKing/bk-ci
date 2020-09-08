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

import com.tencent.devops.common.api.pojo.Zone
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.DockerVersion
import com.tencent.devops.common.pipeline.enums.EnvControlTaskType
import com.tencent.devops.common.pipeline.enums.VMBaseOS
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.type.docker.DockerDispatchType
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PIPELINE_MODEL_NOT_EXISTS
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PIPELINE_NODEL_CONTAINER_NOT_EXISTS
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS
import com.tencent.devops.process.engine.atom.AtomResponse
import com.tencent.devops.process.engine.atom.AtomUtils
import com.tencent.devops.process.engine.atom.IAtomTask
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.engine.exception.BuildTaskException
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.engine.pojo.event.PipelineContainerAgentHeartBeatEvent
import com.tencent.devops.process.engine.service.PipelineBuildDetailService
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.process.engine.atom.defaultFailAtomResponse
import com.tencent.devops.process.pojo.mq.PipelineBuildLessShutdownDispatchEvent
import com.tencent.devops.process.pojo.mq.PipelineBuildLessStartupDispatchEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

/**
 * 无构建环境docker启动
 * @version 1.0
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class DispatchBuildLessDockerStartupTaskAtom @Autowired constructor(
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val client: Client,
    private val pipelineBuildDetailService: PipelineBuildDetailService,
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val buildLogPrinter: BuildLogPrinter
) : IAtomTask<NormalContainer> {
    override fun getParamElement(task: PipelineBuildTask): NormalContainer {
        return JsonUtil.mapTo(task.taskParams, NormalContainer::class.java)
    }

    private val logger = LoggerFactory.getLogger(DispatchBuildLessDockerStartupTaskAtom::class.java)

    override fun execute(
        task: PipelineBuildTask,
        param: NormalContainer,
        runVariables: Map<String, String>
    ): AtomResponse {
        var atomResponse: AtomResponse
        try {
            atomResponse = startUpDocker(task, param)
        } catch (e: BuildTaskException) {
            buildLogPrinter.addRedLine(
                buildId = task.buildId,
                message = "Build container init failed: ${e.message}",
                tag = task.taskId,
                jobId = task.containerHashId,
                executeCount = task.executeCount ?: 1
            )
            logger.warn("Build container init failed", e)
            atomResponse = AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = e.errorType,
                errorCode = e.errorCode,
                errorMsg = e.message
            )
        } catch (t: Throwable) {
            buildLogPrinter.addRedLine(
                buildId = task.buildId,
                message = "Build container init failed: ${t.message}",
                tag = task.taskId,
                jobId = task.containerHashId,
                executeCount = task.executeCount ?: 1
            )
            logger.warn("Build container init failed", t)
            atomResponse = AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = ErrorType.SYSTEM,
                errorCode = ErrorCode.SYSTEM_WORKER_INITIALIZATION_ERROR,
                errorMsg = t.message
            )
        }
        return atomResponse
    }

    // TODO Exception中的错误码对应提示信息修改提取
    fun startUpDocker(task: PipelineBuildTask, param: NormalContainer): AtomResponse {
        val projectId = task.projectId
        val pipelineId = task.pipelineId
        val buildId = task.buildId
        val taskId = task.taskId

        // 构建环境容器序号ID
        val vmSeqId = task.containerId

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
                errorMsg = "流水线的模型中指定构建Job不存在",
                pipelineId = pipelineId,
                buildId = buildId,
                taskId = taskId
            )
        pipelineBuildDetailService.updateStartVMStatus(
            buildId = buildId,
            containerId = task.containerId,
            buildStatus = BuildStatus.RUNNING
        )
        // 读取原子市场中的原子信息，写入待构建处理
        val atoms = AtomUtils.parseContainerMarketAtom(
            container = container,
            task = task,
            client = client,
            buildLogPrinter = buildLogPrinter
        )

        val source = "dockerStartupTaskAtom"
        val dispatchType =
            DockerDispatchType(DockerVersion.TLINUX2_2.value)
        val osType = VMBaseOS.LINUX
        pipelineEventDispatcher.dispatch(
            PipelineBuildLessStartupDispatchEvent(
                source = source,
                projectId = projectId,
                pipelineId = pipelineId,
                userId = task.starter,
                buildId = buildId,
                vmSeqId = vmSeqId,
                containerId = task.containerId,
                containerHashId = task.containerHashId,
                os = osType.name,
                startTime = System.currentTimeMillis(),
                channelCode = pipelineInfo.channelCode.name,
                dispatchType = dispatchType,
                zone = getBuildZone(container),
                atoms = atoms,
                executeCount = task.executeCount
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
        logger.info("[$buildId]|STARTUP_DOCKER|($vmSeqId)|Dispatch startup")
        return AtomResponse(BuildStatus.CALL_WAITING)
    }

    override fun tryFinish(task: PipelineBuildTask, param: NormalContainer, runVariables: Map<String, String>, force: Boolean): AtomResponse {
        return if (force) {
            if (BuildStatus.isFinish(task.status)) {
                AtomResponse(
                    buildStatus = task.status,
                    errorType = task.errorType,
                    errorCode = task.errorCode,
                    errorMsg = task.errorMsg
                )
            } else { // 强制终止的设置为失败
                logger.warn("[${task.buildId}]|[FORCE_STOP_BUILD_LESS_IN_START_TASK]")
                pipelineEventDispatcher.dispatch(
                    PipelineBuildLessShutdownDispatchEvent(
                        source = "force_stop_startBuildLess",
                        projectId = task.projectId,
                        pipelineId = task.pipelineId,
                        userId = task.starter,
                        buildId = task.buildId,
                        vmSeqId = task.containerId,
                        buildResult = true
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

    private fun getBuildZone(container: Container): Zone? {
        try {
            if (container !is VMBuildContainer) {
                return null
            }

            if (container.enableExternal == true) {
                return Zone.EXTERNAL
            }
        } catch (t: Throwable) {
            logger.warn("Fail to get the build zone of container $container", t)
        }
        return null
    }

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

            // 防止
            val taskParams = container.genTaskParams()
            taskParams["elements"] = emptyList<Element>() // elements可能过多导致存储问题
            val taskAtom = AtomUtils.parseAtomBeanName(DispatchBuildLessDockerStartupTaskAtom::class.java)
            return PipelineBuildTask(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                stageId = stageId,
                containerId = container.id!!,
                containerHashId = container.containerId ?: "",
                containerType = container.getClassType(),
                taskSeq = taskSeq,
                taskId = VMUtils.genStartVMTaskId(container.id!!),
                taskName = "Prepare_Job#${container.id!!}(N)",
                taskType = EnvControlTaskType.NORMAL.name,
                taskAtom = taskAtom,
                status = BuildStatus.QUEUE,
                taskParams = taskParams,
                executeCount = 1,
                starter = userId,
                approver = null,
                subBuildId = null,
                additionalOptions = null,
                atomCode = taskAtom
            )
        }
    }
}
