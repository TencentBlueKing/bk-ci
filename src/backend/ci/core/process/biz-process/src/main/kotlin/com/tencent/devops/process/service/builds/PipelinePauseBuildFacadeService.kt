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

package com.tencent.devops.process.service.builds

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.utils.ParameterUtils
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.constant.ProcessMessageCode.BK_BUILD_FINISHED_AND_DENY_PAUSE
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.engine.pojo.PipelinePauseValue
import com.tencent.devops.process.engine.pojo.event.PipelineTaskPauseEvent
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.engine.service.PipelineTaskService
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.service.PipelineTaskPauseService
import javax.ws.rs.core.Response
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 *
 * @version 1.0
 */
@Suppress("ALL")
@Service
class PipelinePauseBuildFacadeService(
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val pipelineTaskService: PipelineTaskService,
    private val pipelinePermissionService: PipelinePermissionService,
    private val buildLogPrinter: BuildLogPrinter,
    private val pipelineTaskPauseService: PipelineTaskPauseService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PipelinePauseBuildFacadeService::class.java)
    }

    fun executePauseAtom(
        userId: String,
        pipelineId: String,
        buildId: String,
        projectId: String,
        taskId: String,
        stageId: String,
        containerId: String,
        isContinue: Boolean,
        element: Element?,
        checkPermission: Boolean? = true
    ): Boolean {
        logger.info("executePauseAtom| $userId| $pipelineId|$buildId| $stageId| $containerId| $taskId| $isContinue")
        if (checkPermission!!) {
            val language = I18nUtil.getLanguage(userId)
            pipelinePermissionService.validPipelinePermission(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                permission = AuthPermission.EXECUTE,
                message = MessageUtil.getMessageByLocale(
                    CommonMessageCode.USER_NOT_PERMISSIONS_OPERATE_PIPELINE,
                    language,
                    arrayOf(userId, projectId, AuthPermission.EXECUTE.getI18n(I18nUtil.getLanguage(userId)), pipelineId)
                )
            )
        }

        val buildInfo = pipelineRuntimeService.getBuildInfo(projectId, buildId)
            ?: throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_NO_BUILD_EXISTS_BY_ID,
                params = arrayOf(buildId)
            )

        if (buildInfo.pipelineId != pipelineId) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPLEINE_INPUT
            )
        }

        if (buildInfo.isFinish()) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.OPERATE_PIPELINE_FAIL,
                defaultMessage = "Fail to execute pause atom",
                params = arrayOf(
                    I18nUtil.getCodeLanMessage(
                        messageCode = BK_BUILD_FINISHED_AND_DENY_PAUSE,
                        language = I18nUtil.getDefaultLocaleLanguage()
                    )
                )
            )
        }

        val taskRecord = pipelineTaskService.getBuildTask(projectId, buildId, taskId)

        if (taskRecord?.status != BuildStatus.PAUSE) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PARUS_PIEPLINE_IS_RUNNINT
            )
        }

        var actionType = ActionType.REFRESH
        if (!isContinue) {
            actionType = ActionType.END // END才会对应成取消状态
        }

        if (element != null) {
            findAndSaveDiff(
                element = element,
                projectId = projectId,
                buildId = buildId,
                taskId = taskId,
                taskRecord = taskRecord
            )
        }

        pipelineEventDispatcher.dispatch(
            PipelineTaskPauseEvent(
                source = "PauseTaskExecute",
                pipelineId = pipelineId,
                buildId = buildId,
                projectId = projectId,
                stageId = stageId,
                containerId = containerId,
                taskId = taskId,
                actionType = actionType,
                userId = userId
            )
        )
        return true
    }

    private fun findDiffValue(
        newElement: Element?,
        buildId: String,
        taskId: String,
        oldTask: PipelineBuildTask
    ): Boolean {
        var isDiff = false
        if (newElement == null) {
            return isDiff
        }
        val newInputData = ParameterUtils.getElementInput(newElement)

        if (newInputData.isNullOrEmpty()) {
            logger.info("newInputData is empty $buildId $taskId $newElement")
            return isDiff
        }

        // issues_6210 若原input为空,新input不为空。则直接返回有变化
        val oldInputData = ParameterUtils.getParamInputs(oldTask.taskParams) ?: emptyMap()

        if (newInputData.toString() != oldInputData.toString()) {
            logger.info("pause continue value diff,new| $newInputData, old|$oldInputData")
            isDiff = true
        }

        if (newInputData.keys != oldInputData.keys) {
            logger.info("pause continue keys diff,new| ${newInputData.keys}, old|${oldInputData.keys}")
            isDiff = true
        }

        newInputData.keys.forEach {
            val oldData = oldInputData[it] ?: ""
            val newData = newInputData[it]
            if (oldData != newData) {
                isDiff = true
                logger.info("[$buildId]|input update, add Log, key $it, newData $newData, oldData $oldData")
                buildLogPrinter.addYellowLine(
                    buildId = buildId,
                    message = "plugin: ${oldTask.taskName}, params $it updated:",
                    tag = taskId,
                    jobId = VMUtils.genStartVMTaskId(oldTask.containerId),
                    executeCount = oldTask.executeCount ?: 1
                )
                buildLogPrinter.addYellowLine(
                    buildId = buildId,
                    message = "before: $oldData",
                    tag = taskId,
                    jobId = VMUtils.genStartVMTaskId(oldTask.containerId),
                    executeCount = oldTask.executeCount ?: 1
                )
                buildLogPrinter.addYellowLine(
                    buildId = buildId,
                    message = "after: $newData",
                    tag = taskId,
                    jobId = VMUtils.genStartVMTaskId(oldTask.containerId),
                    executeCount = oldTask.executeCount ?: 1
                )
            }
        }
        return isDiff
    }

    private fun findAndSaveDiff(
        element: Element,
        projectId: String,
        buildId: String,
        taskId: String,
        taskRecord: PipelineBuildTask
    ) {
        val newElementStr = ParameterUtils.element2Str(element)
        if (newElementStr.isNullOrBlank()) {
            logger.warn("executePauseAtom element is too long")
            throw ErrorCodeException(
                statusCode = Response.Status.INTERNAL_SERVER_ERROR.statusCode,
                errorCode = ProcessMessageCode.ERROR_ELEMENT_TOO_LONG,
                params = arrayOf(buildId)
            )
        }
        val isDiff = findDiffValue(
            buildId = buildId,
            taskId = taskId,
            newElement = element,
            oldTask = taskRecord
        )

        if (isDiff) {
            pipelineTaskPauseService.savePauseValue(PipelinePauseValue(
                projectId = projectId,
                buildId = buildId,
                taskId = taskId,
                newValue = newElementStr,
                defaultValue = JsonUtil.toJson(taskRecord.taskParams, formatted = false)
            ))
        }
    }
}
