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

package com.tencent.devops.plugin.quality.task

import com.tencent.devops.common.api.exception.TaskExecuteException
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.client.Client
import com.tencent.devops.log.utils.LogUtils
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.utils.PIPELINE_BUILD_NUM
import com.tencent.devops.quality.api.v2.ServiceQualityRuleResource
import com.tencent.devops.quality.api.v2.pojo.request.BuildCheckParams
import com.tencent.devops.quality.pojo.RuleCheckResult
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import java.time.LocalDateTime

object QualityUtils {

    private val logger = LoggerFactory.getLogger(QualityUtils::class.java)

    fun getAuditUserList(
        client: Client,
        projectId: String,
        pipelineId: String,
        buildId: String,
        taskId: String
    ): Set<String> {
        return try {
            client.get(ServiceQualityRuleResource::class).getAuditUserList(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                taskId = taskId
            ).data ?: setOf()
        } catch (e: Exception) {
            logger.error("quality get audit user list fail: ${e.message}", e)
            return setOf()
        }
    }

    private fun check(client: Client, buildCheckParams: BuildCheckParams): RuleCheckResult {
        return try {
            client.getWithoutRetry(ServiceQualityRuleResource::class).check(buildCheckParams).data!!
        } catch (e: Exception) {
            logger.error("quality get audit user list fail: ${e.message}", e)
            return RuleCheckResult(
                success = true,
                failEnd = true,
                auditTimeoutSeconds = 15 * 6000,
                resultList = listOf()
            )
        }
    }

    fun getCheckResult(
        task: PipelineBuildTask,
        interceptTaskName: String?,
        interceptTask: String?,
        runVariables: Map<String, String>,
        client: Client,
        rabbitTemplate: RabbitTemplate,
        position: String
    ): RuleCheckResult {

        val pipelineId = task.pipelineId
        val projectId = task.projectId
        val buildId = task.buildId
        val templateId = task.templateId
        val buildNo = runVariables[PIPELINE_BUILD_NUM].toString()
        val elementId = task.taskId

        if (interceptTask == null) {
            logger.error("Fail to find quality gate intercept element")
            throw TaskExecuteException(
                errorCode = ErrorCode.USER_RESOURCE_NOT_FOUND,
                errorType = ErrorType.USER,
                errorMsg = "Fail to find quality gate intercept element"
            )
        }

        return try {
            val buildCheckParams = BuildCheckParams(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                buildNo = buildNo,
                interceptTaskName = interceptTaskName ?: "",
                startTime = LocalDateTime.now().timestamp(),
                taskId = interceptTask,
                position = position,
                templateId = templateId,
                runtimeVariable = runVariables
            )
            if (QUALITY_CODECC_LAZY_ATOM.contains(interceptTask)) {
                run loop@{
                    QUALITY_LAZY_TIME_GAP.forEachIndexed { index, gap ->
                        val hisMetadata =
                            client.get(ServiceQualityRuleResource::class).getHisMetadata(buildId).data ?: listOf()
                        val hasMetadata = hisMetadata.any { it.elementType in QUALITY_CODECC_LAZY_ATOM }
                        if (hasMetadata) return@loop
                        LogUtils.addLine(
                            rabbitTemplate = rabbitTemplate,
                            buildId = buildId,
                            message = "第 $index 次轮询等待红线结果",
                            tag = elementId,
                            jobId = task.containerHashId,
                            executeCount = task.executeCount ?: 1
                        )
                        Thread.sleep(gap * 1000L)
                    }
                }
                check(client, buildCheckParams)
            } else {
                LogUtils.addLine(
                    rabbitTemplate = rabbitTemplate,
                    buildId = buildId,
                    message = "检测红线结果",
                    tag = elementId,
                    jobId = task.containerHashId,
                    executeCount = task.executeCount ?: 1
                )
                check(client, buildCheckParams)
            }
        } catch (t: Throwable) {
            logger.error("Quality Gate check in fail", t)
            throw TaskExecuteException(
                errorCode = ErrorCode.USER_TASK_OPERATE_FAIL,
                errorType = ErrorType.USER,
                errorMsg = "质量红线(准入)检测失败"
            )
        }
    }
}