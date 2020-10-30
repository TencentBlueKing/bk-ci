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

package com.tencent.devops.process.engine.service

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.TaskExecuteException
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.ManualReviewAction
import com.tencent.devops.common.pipeline.pojo.element.quality.QualityGateInElement
import com.tencent.devops.common.pipeline.pojo.element.quality.QualityGateOutElement
import com.tencent.devops.common.service.utils.LogUtils
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.dao.template.TemplatePipelineDao
import com.tencent.devops.process.engine.utils.QualityUtils
import com.tencent.devops.quality.api.v2.ServiceQualityRuleResource
import com.tencent.devops.quality.api.v2.pojo.ControlPointPosition
import com.tencent.devops.quality.api.v2.pojo.request.BuildCheckParams
import com.tencent.devops.quality.api.v2.pojo.response.QualityRuleMatchTask
import com.tencent.devops.quality.pojo.RuleCheckResult
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import javax.ws.rs.core.Response

@Service
class PipelineBuildQualityService(
    private val client: Client,
    private val templatePipelineDao: TemplatePipelineDao,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val buildDetailService: PipelineBuildDetailService,
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val dslContext: DSLContext
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PipelineBuildQualityService::class.java)
    }

    fun buildManualQualityGateReview(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        elementId: String,
        action: ManualReviewAction,
        channelCode: ChannelCode,
        checkPermission: Boolean = true
    ) {
        val pipelineInfo = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId)
            ?: throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS,
                defaultMessage = "流水线不存在"
            )

        if (pipelineInfo.channelCode != channelCode) {
            throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS,
                defaultMessage = "流水线不存在"
            )
        }

        val modelDetail = buildDetailService.get(buildId)
            ?: throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_NO_BUILD_EXISTS_BY_ID,
                defaultMessage = "流水线构建不存在",
                params = arrayOf(buildId)
            )

        var find = false
        var taskType = ""
        modelDetail.model.stages.forEachIndexed { index, s ->
            if (index == 0) {
                return@forEachIndexed
            }
            s.containers.forEach {
                it.elements.forEach { element ->
                    logger.info("${element.id}, ${element.name}")
                    if ((element is QualityGateInElement || element is QualityGateOutElement) && element.id == elementId) {
                        find = true
                        if (element is QualityGateInElement) {
                            taskType = element.interceptTask!!
                        }
                        if (element is QualityGateOutElement) {
                            taskType = element.interceptTask!!
                        }
                        return@forEachIndexed
                    }
                }
            }
        }

        if (!find) {
            throw ErrorCodeException(
                statusCode = Response.Status.FORBIDDEN.statusCode,
                errorCode = ProcessMessageCode.ERROR_QUALITY_TASK_NOT_FOUND,
                defaultMessage = "质量红线拦截的任务[$elementId]不存在",
                params = arrayOf(elementId)
            )
        }

        // 校验审核权限
        val auditUserSet = getAuditUserList(projectId, pipelineId, buildId, taskType)
        if (!auditUserSet.contains(userId)) {
            throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_QUALITY_REVIEWER_NOT_MATCH,
                defaultMessage = "用户($userId)不在审核人员名单中",
                params = arrayOf(userId)
            )
        }

        logger.info("[$buildId]|buildManualReview|taskId=$elementId|userId=$userId|action=$action")
        pipelineRuntimeService.manualDealBuildTask(buildId, elementId, userId, action)
    }

    fun addQualityGateReviewUsers(projectId: String, pipelineId: String, buildId: String, model: Model) {
        model.stages.forEach { stage ->
            stage.containers.forEach { container ->
                container.elements.forEach { element ->
                    if (element is QualityGateInElement && element.status == BuildStatus.REVIEWING.name) {
                        element.reviewUsers = getAuditUserList(
                            projectId,
                            pipelineId,
                            buildId,
                            element.interceptTask ?: ""
                        )
                    }
                    if (element is QualityGateOutElement && element.status == BuildStatus.REVIEWING.name) {
                        element.reviewUsers = getAuditUserList(
                            projectId,
                            pipelineId,
                            buildId,
                            element.interceptTask ?: ""
                        )
                    }
                }
            }
        }
    }

    fun fillingRuleInOutElement(
        projectId: String,
        pipelineId: String,
        startParams: Map<String, Any>,
        model: Model
    ): Model {
        val templateId = if (model.instanceFromTemplate == true) {
            templatePipelineDao.get(dslContext, pipelineId)?.templateId
        } else {
            null
        }
        val ruleMatchList = getMatchRuleList(projectId, pipelineId, templateId)
        logger.info("Rule match list for pipeline- $pipelineId, template- $templateId($ruleMatchList)")

        val cleaningModel = model.removeElements(setOf(QualityGateInElement.classType, QualityGateOutElement.classType))
        val fillingModel = if (ruleMatchList.isEmpty()) {
            cleaningModel
        } else {
            val convertList = ruleMatchList.map {
                val gatewayIds = it.ruleList.filter { !it.gatewayId.isNullOrBlank() }.map { it.gatewayId!! }
                mapOf("position" to it.controlStage.name,
                    "taskId" to it.taskId,
                    "gatewayIds" to gatewayIds)
            }
            QualityUtils.fillInOutElement(cleaningModel, startParams, convertList)
        }
        logger.info("FillingModel($fillingModel)")

        return fillingModel
    }

    fun getMatchRuleList(projectId: String, pipelineId: String, templateId: String?): List<QualityRuleMatchTask> {
        val startTime = System.currentTimeMillis()
        return try {
            client.get(ServiceQualityRuleResource::class).matchRuleList(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    templateId = templateId,
                    startTime = LocalDateTime.now().timestamp()
            ).data ?: listOf()
        } catch (e: Exception) {
            logger.error("quality get match rule list fail: ${e.message}", e)
            return listOf()
        } finally {
            LogUtils.costTime("call rule", startTime)
        }
    }

    fun getAuditUserList(projectId: String, pipelineId: String, buildId: String, taskId: String): Set<String> {
        return try {
            client.get(ServiceQualityRuleResource::class).getAuditUserList(
                projectId,
                pipelineId,
                buildId,
                taskId
            ).data ?: setOf()
        } catch (e: Exception) {
            logger.error("quality get audit user list fail: ${e.message}", e)
            setOf()
        }
    }

    fun check(buildCheckParams: BuildCheckParams, position: String): RuleCheckResult {
        return try {
            client.get(ServiceQualityRuleResource::class).check(buildCheckParams).data!!
        } catch (e: Exception) {
            logger.error("Quality Gate check in fail", e)
            val atomDesc = if (position == ControlPointPosition.BEFORE_POSITION) "准入" else "准出"
            throw TaskExecuteException(
                errorCode = ErrorCode.USER_TASK_OPERATE_FAIL,
                errorType = ErrorType.USER,
                errorMsg = "质量红线($atomDesc)检测失败"
            )
        }
    }

    fun hasCodeccHisMetadata(buildId: String): Boolean {
        val hisMetadata = client.get(ServiceQualityRuleResource::class).getHisMetadata(buildId).data ?: listOf()
        return hisMetadata.any { it.elementType in QualityUtils.QUALITY_CODECC_LAZY_ATOM }
    }
}