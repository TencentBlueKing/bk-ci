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
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.DockerVersion
import com.tencent.devops.common.pipeline.enums.VMBaseOS
import com.tencent.devops.common.pipeline.type.docker.DockerDispatchType
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PIPELINE_NODEL_CONTAINER_NOT_EXISTS
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS
import com.tencent.devops.process.engine.atom.AtomResponse
import com.tencent.devops.process.engine.atom.AtomUtils
import com.tencent.devops.process.engine.atom.IAtomTask
import com.tencent.devops.process.engine.atom.defaultFailAtomResponse
import com.tencent.devops.process.engine.exception.BuildTaskException
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.service.detail.ContainerBuildDetailService
import com.tencent.devops.process.engine.service.record.ContainerBuildRecordService
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
@Suppress("UNUSED", "LongParameterList")
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class DispatchBuildLessDockerStartupTaskAtom @Autowired constructor(
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val client: Client,
    private val containerBuildDetailService: ContainerBuildDetailService,
    private val containerBuildRecordService: ContainerBuildRecordService,
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
            buildLogPrinter.stopLog(
                buildId = task.buildId,
                tag = task.taskId,
                jobId = task.containerHashId,
                executeCount = task.executeCount ?: 1
            )
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
        } catch (ignored: Throwable) {
            buildLogPrinter.addRedLine(
                buildId = task.buildId,
                message = "Build container init failed: ${ignored.message}",
                tag = task.taskId,
                jobId = task.containerHashId,
                executeCount = task.executeCount ?: 1
            )
            logger.warn("Build container init failed", ignored)
            atomResponse = AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = ErrorType.SYSTEM,
                errorCode = ErrorCode.SYSTEM_WORKER_INITIALIZATION_ERROR,
                errorMsg = ignored.message
            )
        }
        return atomResponse
    }

    fun startUpDocker(task: PipelineBuildTask, param: NormalContainer): AtomResponse {
        val projectId = task.projectId
        val pipelineId = task.pipelineId
        val buildId = task.buildId
        val taskId = task.taskId

        // 构建环境容器序号ID
        val vmSeqId = task.containerId

        val pipelineInfo = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId)
        Preconditions.checkNotNull(pipelineInfo, BuildTaskException(
            errorType = ErrorType.SYSTEM,
            errorCode = ERROR_PIPELINE_NOT_EXISTS.toInt(),
            errorMsg =
            I18nUtil.getCodeLanMessage(messageCode = ERROR_PIPELINE_NOT_EXISTS, params = arrayOf(pipelineId)),
            pipelineId = pipelineId,
            buildId = buildId,
            taskId = taskId
        ))

        val container = containerBuildDetailService.getBuildModel(projectId, buildId)?.getContainer(vmSeqId)
        Preconditions.checkNotNull(container, BuildTaskException(
            errorType = ErrorType.SYSTEM,
            errorCode = ERROR_PIPELINE_NODEL_CONTAINER_NOT_EXISTS.toInt(),
            errorMsg = I18nUtil.getCodeLanMessage(
                messageCode = ERROR_PIPELINE_NOT_EXISTS,
                params = arrayOf(vmSeqId)
            ),
            pipelineId = pipelineId,
            buildId = buildId,
            taskId = taskId
        ))

        containerBuildRecordService.containerPreparing(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            containerId = vmSeqId,
            executeCount = param.executeCount ?: 1
        )
        dispatch(container = container!!, task = task, pipelineInfo = pipelineInfo!!, param)

        logger.info("[$buildId]|STARTUP_DOCKER|($vmSeqId)|Dispatch startup")
        return AtomResponse(BuildStatus.CALL_WAITING)
    }

    private fun dispatch(
        container: Container,
        task: PipelineBuildTask,
        pipelineInfo: PipelineInfo,
        param: NormalContainer
    ) {
        // 读取原子市场中的原子信息，写入待构建处理
        val atoms = AtomUtils.parseContainerMarketAtom(
            container = container,
            task = task,
            client = client,
            buildLogPrinter = buildLogPrinter
        )

        pipelineEventDispatcher.dispatch(
            PipelineBuildLessStartupDispatchEvent(
                source = "dockerStartupTaskAtom",
                projectId = task.projectId,
                pipelineId = task.pipelineId,
                userId = task.starter,
                buildId = task.buildId,
                vmSeqId = task.containerId,
                containerId = task.containerId,
                containerHashId = task.containerHashId,
                os = VMBaseOS.LINUX.name,
                startTime = System.currentTimeMillis(),
                channelCode = pipelineInfo.channelCode.name,
                dispatchType = DockerDispatchType(DockerVersion.CUSTOMIZE.value),
                zone = getBuildZone(container),
                atoms = atoms,
                executeCount = task.executeCount,
                customBuildEnv = param.matrixContext
            )
        )
    }

    override fun tryFinish(
        task: PipelineBuildTask,
        param: NormalContainer,
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
                logger.warn("[${task.buildId}]|[FORCE_STOP_BUILD_LESS_IN_START_TASK]")
                pipelineEventDispatcher.dispatch(
                    PipelineBuildLessShutdownDispatchEvent(
                        source = "force_stop_startBuildLess",
                        projectId = task.projectId,
                        pipelineId = task.pipelineId,
                        userId = task.starter,
                        buildId = task.buildId,
                        vmSeqId = task.containerId,
                        buildResult = true,
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

    private fun getBuildZone(container: Container): Zone? {
        if (container is VMBuildContainer) {
            if (container.enableExternal == true) {
                return Zone.EXTERNAL
            }
        }
        return null
    }
}
