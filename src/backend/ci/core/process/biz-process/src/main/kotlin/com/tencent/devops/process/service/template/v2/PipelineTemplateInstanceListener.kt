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

package com.tencent.devops.process.service.template.v2

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.event.dispatcher.SampleEventDispatcher
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.engine.dao.template.TemplateInstanceBaseDao
import com.tencent.devops.process.engine.dao.template.TemplateInstanceItemDao
import com.tencent.devops.process.engine.pojo.event.PipelineTemplateInstanceEvent
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.pojo.pipeline.DeployPipelineResult
import com.tencent.devops.process.pojo.pipeline.version.PipelineTemplateInstanceReq
import com.tencent.devops.process.pojo.template.TemplateInstanceStatus
import com.tencent.devops.process.pojo.template.TemplatePipelineStatus
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateInstanceBase
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateInstanceItem
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateInstanceItemCondition
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateInstanceItemUpdate
import com.tencent.devops.process.service.pipeline.version.PipelineVersionManager
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 *  MQ实现的流水线模板实例事件
 *
 * @version 1.0
 */
@Service
class PipelineTemplateInstanceListener @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val dslContext: DSLContext,
    private val templateInstanceItemDao: TemplateInstanceItemDao,
    private val templateInstanceBaseDao: TemplateInstanceBaseDao,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val pipelineVersionManager: PipelineVersionManager,
    private val pipelineTemplateRelatedService: PipelineTemplateRelatedService,
    private val pipelineTemplateInstanceService: PipelineTemplateInstanceService,
    private val sampleEventDispatcher: SampleEventDispatcher,
) {
    fun handle(event: PipelineTemplateInstanceEvent) {
        logger.info("consume pipeline template instance event {}", event)
        handleTemplateInstanceEvent(event)
    }

    private fun handleTemplateInstanceEvent(event: PipelineTemplateInstanceEvent) {
        val baseId = event.baseId
        val projectId = event.projectId
        val type = event.templateInstanceType
        PipelineTemplateInstanceLock(redisOperation, event.templateId).use { lock ->
            logger.info("start to handle template event {}|,{}", type, event)
            if (!lock.tryLock()) {
                logger.warn("handle template instance event running ${event.projectId}|${event.baseId}")
                event.retry()
                return
            }
            val instanceBase = templateInstanceBaseDao.getTemplateInstanceBase(
                dslContext = dslContext,
                projectId = event.projectId,
                baseId = event.baseId
            ) ?: run {
                logger.info(
                    "handle template instance event failed, baseId not found|${event.projectId}|${event.baseId}"
                )
                return
            }

            val itemCount = templateInstanceItemDao.getTemplateInstanceItemCountByBaseId(
                dslContext = dslContext,
                projectId = projectId,
                baseId = baseId,
                excludeStatusList = listOf(
                    TemplateInstanceStatus.SUCCESS.name,
                    TemplateInstanceStatus.WAIT_MERGE.name
                )
            )

            checkTemplateInstanceEvent(
                instanceBase = instanceBase,
                projectId = projectId,
                baseId = baseId,
                templateInstanceItemCount = itemCount
            )

            instanceBase.handleTemplateInstanceBase(
                projectId = projectId,
                itemCount = itemCount
            )
        }
    }

    private fun PipelineTemplateInstanceEvent.retry() {
        logger.info("template instance|$projectId|$templateId|RETRY_TO_TEMPLATE_INSTANCE_LOCK")
        this.delayMills = DEFAULT_DELAY
        sampleEventDispatcher.dispatch(this)
    }

    private fun PipelineTemplateInstanceBase.handleTemplateInstanceBase(projectId: String, itemCount: Long) {
        // 开始执行任务
        templateInstanceBaseDao.updateTemplateInstanceBase(
            dslContext = dslContext,
            projectId = projectId,
            baseId = baseId,
            status = TemplateInstanceStatus.INSTANCING.name,
            userId = "system"
        )
        val successPipelines = mutableListOf<String>()
        val failurePipelines = mutableListOf<String>()
        val totalPages = PageUtil.calTotalPage(PageUtil.MAX_PAGE_SIZE, itemCount)
        var pullRequestId: Long? = null
        var pullRequestUrl: String? = null
        for (page in 1..totalPages) {
            val templateInstanceItems = templateInstanceItemDao.listTemplateInstanceItemByBaseIds(
                dslContext = dslContext,
                projectId = projectId,
                baseIds = listOf(baseId),
                page = page,
                pageSize = PageUtil.MAX_PAGE_SIZE
            )
            templateInstanceItems.forEach { item ->
                val deployPipelineResult = item.handleTemplateInstanceItem(
                    instanceBase = this,
                    successPipelines = successPipelines,
                    failurePipelines = failurePipelines
                )
                pullRequestId = deployPipelineResult?.pullRequestId
                pullRequestUrl = deployPipelineResult?.targetUrl
            }
        }
        val baseStatus = when {
            successPipelines.size == itemCount.toInt() -> TemplateInstanceStatus.SUCCESS
            failurePipelines.size == itemCount.toInt() -> TemplateInstanceStatus.FAILED
            else -> TemplateInstanceStatus.PARTIAL_SUCCESS
        }
        templateInstanceBaseDao.updateTemplateInstanceBase(
            dslContext = dslContext,
            projectId = projectId,
            baseId = baseId,
            status = baseStatus.name,
            successItemNum = successPipelines.size,
            failItemNum = failurePipelines.size,
            pullRequestId = pullRequestId,
            pullRequestUrl = pullRequestUrl,
            userId = "system"
        )
        if (successPipelines.isNotEmpty()) {
            pipelineRepositoryService.updateInstancePipelineCount(
                projectId = projectId,
                templateId = templateId
            )
        }
    }

    private fun PipelineTemplateInstanceItem.handleTemplateInstanceItem(
        instanceBase: PipelineTemplateInstanceBase,
        successPipelines: MutableList<String>,
        failurePipelines: MutableList<String>
    ): DeployPipelineResult? {
        logger.info("${instanceBase.type} template instance item|$id|$projectId|$pipelineId")
        if (status == TemplateInstanceStatus.SUCCESS || status == TemplateInstanceStatus.WAIT_MERGE) {
            logger.info("${instanceBase.type} template instance item $status|$id|$projectId|$pipelineId")
            return null
        }
        templateInstanceItemDao.updateStatus(
            dslContext = dslContext,
            projectId = projectId,
            baseId = baseId,
            pipelineIds = listOf(pipelineId),
            status = TemplateInstanceStatus.INSTANCING
        )
        val instanceCreateReq = PipelineTemplateInstanceReq(
            projectId = projectId,
            templateId = instanceBase.templateId,
            templateVersion = instanceBase.templateVersion,
            templateRefType = instanceBase.templateRefType,
            templateRef = instanceBase.templateRef,
            pipelineName = pipelineName,
            buildNo = buildNo,
            params = params,
            triggerConfigs = triggerConfigs,
            resetBuildNo = resetBuildNo,
            overrideTemplateField = overrideTemplateField,
            useTemplateSetting = instanceBase.useTemplateSetting,
            enablePac = instanceBase.pac,
            repoHashId = instanceBase.repoHashId,
            filePath = filePath,
            targetAction = instanceBase.targetAction,
            targetBranch = instanceBase.targetBranch,
            description = instanceBase.description
        )

        return try {
            val deployPipelineResult = pipelineVersionManager.deployPipeline(
                userId = creator,
                projectId = projectId,
                pipelineId = pipelineId,
                request = instanceCreateReq
            )
            handleTemplateInstanceEventSuccess(
                deployPipelineResult = deployPipelineResult
            )
            successPipelines.add(pipelineId)
            deployPipelineResult
        } catch (ignored: Throwable) {
            logger.warn(
                "Failed to instance template|$projectId|$baseId|$pipelineId",
                ignored
            )
            handleTemplateInstanceEventError(
                projectId = projectId,
                userId = creator,
                instanceItem = this,
                exception = ignored
            )
            failurePipelines.add(pipelineId)
            null
        }
    }

    fun checkTemplateInstanceEvent(
        projectId: String,
        baseId: String,
        templateInstanceItemCount: Long,
        instanceBase: PipelineTemplateInstanceBase
    ) {
        if (instanceBase.status == TemplateInstanceStatus.SUCCESS) {
            logger.warn(
                "The template instance task has been completed." +
                    "${instanceBase.projectId}|${instanceBase.baseId}|${instanceBase.type}"
            )
        }
        if (templateInstanceItemCount < 1) {
            templateInstanceBaseDao.updateTemplateInstanceBase(
                dslContext = dslContext,
                projectId = projectId,
                baseId = baseId,
                status = TemplateInstanceStatus.SUCCESS.name,
                userId = "system"
            )
            logger.warn("The template instance task has been completed.$projectId|$baseId")
        }
    }

    private fun PipelineTemplateInstanceItem.handleTemplateInstanceEventSuccess(
        deployPipelineResult: DeployPipelineResult
    ) {
        logger.info(
            "success to template instance item|$baseId|$projectId|$pipelineId|" +
                "${deployPipelineResult.version}|${deployPipelineResult.versionName}"
        )
        val record = PipelineTemplateInstanceItemUpdate(
            status = TemplateInstanceStatus.SUCCESS,
            pipelineVersion = deployPipelineResult.version,
            pipelineVersionName = deployPipelineResult.versionName
        )
        val condition = PipelineTemplateInstanceItemCondition(
            projectId = projectId,
            baseId = baseId,
            pipelineId = pipelineId
        )
        val status = if (deployPipelineResult.targetUrl != null) {
            TemplatePipelineStatus.UPDATING
        } else {
            TemplatePipelineStatus.UPDATED
        }
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            templateInstanceItemDao.update(
                dslContext = transactionContext,
                record = record,
                condition = condition
            )
            pipelineTemplateRelatedService.updateStatus(
                transactionContext = transactionContext,
                projectId = projectId,
                pipelineIds = listOf(pipelineId),
                status = status,
                pullRequestUrl = deployPipelineResult.targetUrl,
                pullRequestId = deployPipelineResult.pullRequestId
            )
        }
    }

    private fun handleTemplateInstanceEventError(
        projectId: String,
        userId: String,
        instanceItem: PipelineTemplateInstanceItem,
        exception: Throwable
    ) {
        val failedReason = pipelineTemplateInstanceService.translateInstanceException(
            userId = userId,
            projectId = projectId,
            pipelineId = instanceItem.pipelineId,
            exception = exception
        )
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            templateInstanceItemDao.updateErrorMessage(
                dslContext = dslContext,
                projectId = projectId,
                baseId = instanceItem.baseId,
                pipelineId = instanceItem.pipelineId,
                errorMessage = JsonUtil.toJson(failedReason, false)
            )
            pipelineTemplateRelatedService.updateStatus(
                transactionContext = transactionContext,
                projectId = projectId,
                pipelineIds = listOf(instanceItem.pipelineId),
                status = TemplatePipelineStatus.FAILED,
                instanceErrorInfo = JsonUtil.toJson(failedReason, false)
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineTemplateInstanceListener::class.java)
        private const val DEFAULT_DELAY = 1000
    }
}
