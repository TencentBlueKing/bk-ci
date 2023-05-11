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

package com.tencent.devops.process.engine.atom.task

import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.element.SubPipelineCallElement
import com.tencent.devops.common.pipeline.pojo.element.atom.SubPipelineType
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.api.builds.BuildSubPipelineResource
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_BUILD_TASK_SUBPIPELINEID_NOT_EXISTS
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_BUILD_TASK_SUBPIPELINEID_NULL
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_NO_BUILD_RECORD_FOR_CORRESPONDING_SUB_PIPELINE
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_NO_CORRESPONDING_SUB_PIPELINE
import com.tencent.devops.process.engine.atom.AtomResponse
import com.tencent.devops.process.engine.atom.IAtomTask
import com.tencent.devops.process.engine.atom.defaultFailAtomResponse
import com.tencent.devops.process.engine.exception.BuildTaskException
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.utils.PIPELINE_START_CHANNEL
import org.springframework.stereotype.Component

@Suppress("UNUSED")
@Component
class SubPipelineCallAtom constructor(
    private val client: Client,
    private val buildLogPrinter: BuildLogPrinter,
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val pipelineRepositoryService: PipelineRepositoryService
) : IAtomTask<SubPipelineCallElement> {

    override fun getParamElement(task: PipelineBuildTask): SubPipelineCallElement {
        return JsonUtil.mapTo(task.taskParams, SubPipelineCallElement::class.java)
    }

    override fun tryFinish(
        task: PipelineBuildTask,
        param: SubPipelineCallElement,
        runVariables: Map<String, String>,
        force: Boolean
    ): AtomResponse {
        return if (task.subBuildId.isNullOrBlank()) {
            AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = ErrorType.USER,
                errorCode = ErrorCode.USER_RESOURCE_NOT_FOUND,
                errorMsg = MessageUtil.getMessageByLocale(
                    ERROR_NO_CORRESPONDING_SUB_PIPELINE,
                    I18nUtil.getDefaultLocaleLanguage()
                )
            )
        } else {
            val subBuildId = task.subBuildId!!
            val subBuildInfo = pipelineRuntimeService.getBuildInfo(task.subProjectId!!, subBuildId)
            return if (subBuildInfo == null) {
                buildLogPrinter.addRedLine(
                    buildId = task.buildId,
                    message = "Can not found sub pipeline build record(${task.subBuildId})",
                    tag = task.taskId, jobId = task.containerHashId, executeCount = task.executeCount ?: 1
                )
                AtomResponse(
                    buildStatus = BuildStatus.FAILED,
                    errorType = ErrorType.USER,
                    errorCode = ErrorCode.USER_RESOURCE_NOT_FOUND,
                    errorMsg = MessageUtil.getMessageByLocale(
                        ERROR_NO_BUILD_RECORD_FOR_CORRESPONDING_SUB_PIPELINE,
                        I18nUtil.getDefaultLocaleLanguage()
                    )
                )
            } else { // 此处逻辑与 研发商店上架的BuildSubPipelineResourceImpl.getSubPipelineStatus 不同，
                // 原因是后者在插件实现上检测这种情况并判断，本处为内置的子流水线插件，需在此增加判断处理
                var status: BuildStatus = when {
                    subBuildInfo.isSuccess() && subBuildInfo.isStageSuccess() -> BuildStatus.SUCCEED
                    subBuildInfo.isFinish() -> subBuildInfo.status
                    subBuildInfo.isReadyToRun() -> BuildStatus.RUNNING // QUEUE状态
                    subBuildInfo.isStageSuccess() -> BuildStatus.RUNNING // stage 特性， 未结束，只是卡在Stage审核中
                    else -> subBuildInfo.status
                }

                buildLogPrinter.addYellowLine(
                    buildId = task.buildId,
                    message = "sub pipeline status: ${status.name}",
                    tag = task.taskId, jobId = task.containerHashId, executeCount = task.executeCount ?: 1
                )

                if (force && !status.isFinish()) { // 补充强制终止对子流水线插件的处理
                    pipelineRuntimeService.cancelBuild(
                        projectId = subBuildInfo.projectId,
                        pipelineId = subBuildInfo.pipelineId,
                        buildId = subBuildId,
                        userId = subBuildInfo.startUser,
                        executeCount = subBuildInfo.executeCount ?: 1,
                        buildStatus = BuildStatus.CANCELED
                    )
                    status = BuildStatus.CANCELED
                }

                AtomResponse(
                    buildStatus = status,
                    errorType = ErrorType.getErrorType(subBuildInfo.errorInfoList?.last()?.errorType),
                    errorCode = subBuildInfo.errorInfoList?.last()?.errorCode,
                    errorMsg = subBuildInfo.errorInfoList?.last()?.errorMsg
                )
            }
        }
    }

    override fun execute(
        task: PipelineBuildTask,
        param: SubPipelineCallElement,
        runVariables: Map<String, String>
    ): AtomResponse {
        val subPipelineId =
            if (param.subPipelineType == SubPipelineType.NAME && !param.subPipelineName.isNullOrBlank()) {
                val subPipelineRealName = parseVariable(param.subPipelineName, runVariables)
                pipelineRepositoryService.listPipelineIdByName(
                    projectId = task.projectId,
                    pipelineNames = setOf(subPipelineRealName),
                    filterDelete = true
                )[subPipelineRealName]
            } else {
                parseVariable(param.subPipelineId, runVariables)
            }

        if (subPipelineId.isNullOrBlank()) {
            throw BuildTaskException(
                errorType = ErrorType.USER,
                errorCode = ERROR_BUILD_TASK_SUBPIPELINEID_NULL.toInt(),
                errorMsg = MessageUtil.getMessageByLocale(
                    ERROR_BUILD_TASK_SUBPIPELINEID_NULL,
                    I18nUtil.getDefaultLocaleLanguage()
                ),
                pipelineId = task.pipelineId, buildId = task.buildId, taskId = task.taskId
            )
        }

        val pipelineInfo = pipelineRepositoryService.getPipelineInfo(task.projectId, subPipelineId)
            ?: throw BuildTaskException(
                errorType = ErrorType.USER,
                errorCode = ERROR_BUILD_TASK_SUBPIPELINEID_NOT_EXISTS.toInt(),
                errorMsg = MessageUtil.getMessageByLocale(
                    ERROR_BUILD_TASK_SUBPIPELINEID_NOT_EXISTS,
                    I18nUtil.getDefaultLocaleLanguage()
                ),
                pipelineId = task.pipelineId, buildId = task.buildId, taskId = task.taskId
            )

        val startParams = mutableMapOf<String, String>()
        param.parameters?.forEach {
            startParams[it.key] = parseVariable(it.value, runVariables)
        }

        val result = client.get(BuildSubPipelineResource::class).callPipelineStartup(
            projectId = task.projectId,
            parentPipelineId = task.pipelineId,
            callPipelineId = subPipelineId,
            buildId = task.buildId,
            channelCode = ChannelCode.valueOf(runVariables.getValue(PIPELINE_START_CHANNEL)),
            atomCode = task.taskId,
            taskId = task.taskId,
            runMode = if (param.asynchronous) {
                "asyn"
            } else {
                "syn"
            },
            values = startParams
        )

        if (result.isNotOk()) {
            buildLogPrinter.addErrorLine(
                buildId = task.buildId,
                message = result.message ?: result.status.toString(),
                tag = task.taskId, jobId = task.containerHashId, executeCount = task.executeCount ?: 1
            )
            return defaultFailAtomResponse
        }

        val subBuildId = result.data?.id

        buildLogPrinter.addLine(
            buildId = task.buildId,
            message = "<a target='_blank' href='/console/pipeline/${task.projectId}/" +
                "$subPipelineId/detail/$subBuildId'>Click Link[${pipelineInfo.pipelineName}]</a>",
            tag = task.taskId, jobId = task.containerHashId, executeCount = task.executeCount ?: 1
        )
        return AtomResponse(if (param.asynchronous) BuildStatus.SUCCEED else BuildStatus.CALL_WAITING)
    }
}
