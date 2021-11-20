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

package com.tencent.devops.process.engine.service

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.model.process.tables.records.TPipelineBuildContainerRecord
import com.tencent.devops.process.engine.dao.PipelineBuildContainerDao
import com.tencent.devops.process.engine.dao.PipelineBuildDao
import com.tencent.devops.process.engine.dao.PipelineBuildStageDao
import com.tencent.devops.process.engine.dao.PipelineBuildSummaryDao
import com.tencent.devops.process.engine.pojo.PipelineBuildContainer
import com.tencent.devops.process.engine.service.detail.StageBuildDetailService
import com.tencent.devops.process.service.BuildVariableService
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 流水线Container相关的服务
 * @version 1.0
 */
@Service
@Suppress("TooManyFunctions", "LongParameterList")
class PipelineContainerService @Autowired constructor(
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val dslContext: DSLContext,
    private val pipelineTaskService: PipelineTaskService,
    private val pipelineBuildContainerDao: PipelineBuildContainerDao,
    private val buildVariableService: BuildVariableService,
    private val stageBuildDetailService: StageBuildDetailService,
    private val client: Client
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PipelineContainerService::class.java)
    }

    fun getContainer(buildId: String, stageId: String?, containerSeqId: String): PipelineBuildContainer? {
        // #4518 防止出错暂时保留两个字段的兼容查询
        val result = try {
            pipelineBuildContainerDao.getBySeqId(dslContext, buildId, stageId, containerSeqId.toInt())
        } catch (ignore: Throwable) {
            pipelineBuildContainerDao.getByContainerId(dslContext, buildId, stageId, containerSeqId)
        }
        if (result != null) {
            return pipelineBuildContainerDao.convert(result)
        }
        return null
    }

    fun listContainers(buildId: String, stageId: String? = null): List<PipelineBuildContainer> {
        val list = pipelineBuildContainerDao.listByBuildId(dslContext, buildId, stageId)
        val result = mutableListOf<PipelineBuildContainer>()
        if (list.isNotEmpty()) {
            list.forEach {
                result.add(pipelineBuildContainerDao.convert(it)!!)
            }
        }
        return result
    }

    fun listByBuildId(buildId: String, stageId: String? = null): Collection<TPipelineBuildContainerRecord> {
        return pipelineBuildContainerDao.listByBuildId(dslContext, buildId, stageId)
    }

    fun batchSave(transactionContext: DSLContext?, taskList: Collection<PipelineBuildContainer>) {
        return pipelineBuildContainerDao.batchSave(transactionContext ?: dslContext, taskList)
    }

    fun batchUpdate(transactionContext: DSLContext?, taskList: List<TPipelineBuildContainerRecord>) {
        return pipelineBuildContainerDao.batchUpdate(transactionContext ?: dslContext, taskList)
    }

    fun updateContainerStatus(
        buildId: String,
        stageId: String,
        containerSeqId: String,
        startTime: LocalDateTime? = null,
        endTime: LocalDateTime? = null,
        buildStatus: BuildStatus
    ) {
        logger.info("[$buildId]|updateContainerStatus|status=$buildStatus|containerSeqId=$containerSeqId|stageId=$stageId")
        pipelineBuildContainerDao.updateStatus(
            dslContext = dslContext,
            buildId = buildId,
            stageId = stageId,
            containerSeqId = containerSeqId.toInt(),
            buildStatus = buildStatus,
            startTime = startTime,
            endTime = endTime
        )
    }

    fun deletePipelineBuildContainers(transactionContext: DSLContext?, projectId: String, pipelineId: String): Int {
        return pipelineBuildContainerDao.deletePipelineBuildContainers(
            dslContext = transactionContext ?: dslContext,
            projectId = projectId,
            pipelineId = pipelineId
        )
    }
}
