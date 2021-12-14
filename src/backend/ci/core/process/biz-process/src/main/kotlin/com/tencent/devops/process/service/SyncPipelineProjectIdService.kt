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

package com.tencent.devops.process.service

import com.tencent.devops.process.dao.SyncPipelineProjectIdDao
import com.tencent.devops.process.dao.label.PipelineGroupDao
import com.tencent.devops.process.dao.label.PipelineLabelDao
import com.tencent.devops.process.engine.dao.PipelineBuildDao
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import com.tencent.devops.process.engine.dao.template.TemplateInstanceBaseDao
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.Executors

@Suppress("LongParameterList")
@Service
class SyncPipelineProjectIdService @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineInfoDao: PipelineInfoDao,
    private val pipelineBuildDao: PipelineBuildDao,
    private val pipelineGroupDao: PipelineGroupDao,
    private val pipelineLabelDao: PipelineLabelDao,
    private val templateInstanceBaseDao: TemplateInstanceBaseDao,
    private val syncPipelineProjectIdDao: SyncPipelineProjectIdDao
) {

    companion object {
        private val logger = LoggerFactory.getLogger(SyncPipelineProjectIdService::class.java)
        private const val DEFAULT_PAGE_SIZE = 50
    }

    fun asyncUpdateProjectId(): Boolean {
        Executors.newFixedThreadPool(1).submit {
            logger.info("begin asyncUpdatePipelineLabelProjectId!!")
            updatePipelineLabelProjectId()
            logger.info("end asyncUpdatePipelineLabelProjectId!!")
        }
        Executors.newFixedThreadPool(1).submit {
            logger.info("begin asyncUpdatePipelineLabelPipelineProjectId!!")
            updatePipelineLabelPipelineProjectId()
            logger.info("end asyncUpdatePipelineLabelPipelineProjectId!!")
        }
        Executors.newFixedThreadPool(1).submit {
            logger.info("begin asyncUpdatePipelinePauseValueProjectId!!")
            updatePipelinePauseValueProjectId()
            logger.info("end asyncUpdatePipelinePauseValueProjectId!!")
        }
        Executors.newFixedThreadPool(1).submit {
            logger.info("begin asyncUpdatePipelineWebhookQueueProjectId!!")
            updatePipelineWebhookQueueProjectId()
            logger.info("end asyncUpdatePipelineWebhookQueueProjectId!!")
        }
        Executors.newFixedThreadPool(1).submit {
            logger.info("begin asyncUpdateTemplateInstanceItemProjectId!!")
            updateTemplateInstanceItemProjectId()
            logger.info("end asyncUpdateTemplateInstanceItemProjectId!!")
        }
        return true
    }

    private fun updateTemplateInstanceItemProjectId() {
        var offset = 0
        do {
            // 查询模板实例化对象记录
            val templateInstanceItemRecords = syncPipelineProjectIdDao.listTemplateInstanceItem(
                dslContext = dslContext,
                offset = offset,
                limit = DEFAULT_PAGE_SIZE
            )
            // 更新模板实例化对象表的projectId
            templateInstanceItemRecords?.forEach { templateInstanceItem ->
                val baseId = templateInstanceItem.baseId
                val templateInstanceBaseInfo = templateInstanceBaseDao.getTemplateInstanceBase(dslContext, baseId)
                if (templateInstanceBaseInfo != null) {
                    syncPipelineProjectIdDao.updateTemplateInstanceItemProject(
                        dslContext = dslContext,
                        baseId = baseId,
                        projectId = templateInstanceBaseInfo.projectId
                    )
                }
            }
            offset += DEFAULT_PAGE_SIZE
        } while (templateInstanceItemRecords?.size == DEFAULT_PAGE_SIZE)
    }

    private fun updatePipelineWebhookQueueProjectId() {
        var offset = 0
        do {
            // 查询流水线Webhook队列记录
            val pipelineWebhookQueueRecords = syncPipelineProjectIdDao.listPipelineWebhookQueue(
                dslContext = dslContext,
                offset = offset,
                limit = DEFAULT_PAGE_SIZE
            )
            // 更新流水线Webhook队列表的projectId
            pipelineWebhookQueueRecords?.forEach { pipelineWebhookQueue ->
                val pipelineId = pipelineWebhookQueue.pipelineId
                val pipelineInfo = pipelineInfoDao.getPipelineInfo(
                    dslContext = dslContext,
                    pipelineId = pipelineId,
                    delete = null
                )
                if (pipelineInfo != null) {
                    syncPipelineProjectIdDao.updatePipelineWebhookQueueProject(
                        dslContext = dslContext,
                        pipelineId = pipelineId,
                        projectId = pipelineInfo.projectId
                    )
                }
            }
            offset += DEFAULT_PAGE_SIZE
        } while (pipelineWebhookQueueRecords?.size == DEFAULT_PAGE_SIZE)
    }

    private fun updatePipelinePauseValueProjectId() {
        var offset = 0
        do {
            // 查询流水线暂停值记录
            val pipelinePauseValueRecords = syncPipelineProjectIdDao.listPipelinePauseValue(
                dslContext = dslContext,
                offset = offset,
                limit = DEFAULT_PAGE_SIZE
            )
            // 更新流水线暂停值表的projectId
            pipelinePauseValueRecords?.forEach { pipelinePauseValue ->
                val buildId = pipelinePauseValue.buildId
                val buildIdInfo = pipelineBuildDao.getBuildInfo(dslContext, buildId)
                if (buildIdInfo != null) {
                    syncPipelineProjectIdDao.updatePipelinePauseValueProject(
                        dslContext = dslContext,
                        buildId = buildId,
                        projectId = buildIdInfo.projectId
                    )
                }
            }
            offset += DEFAULT_PAGE_SIZE
        } while (pipelinePauseValueRecords?.size == DEFAULT_PAGE_SIZE)
    }

    private fun updatePipelineLabelPipelineProjectId() {
        var offset = 0
        do {
            // 查询流水线标签关联关系记录
            val pipelineLabelPipelineRecords = syncPipelineProjectIdDao.listPipelineLabelPipeline(
                dslContext = dslContext,
                offset = offset,
                limit = DEFAULT_PAGE_SIZE
            )
            // 更新流水线标签关联关系表的projectId
            pipelineLabelPipelineRecords?.forEach { pipelineLabelPipeline ->
                val labelId = pipelineLabelPipeline.labelId
                val pipelineLabel = pipelineLabelDao.getById(dslContext, labelId) ?: return@forEach
                val groupInfo = pipelineGroupDao.get(dslContext, pipelineLabel.groupId)
                if (groupInfo != null) {
                    syncPipelineProjectIdDao.updatePipelineLabelPipelineProject(
                        dslContext = dslContext,
                        pipelineId = pipelineLabelPipeline.pipelineId,
                        projectId = groupInfo.projectId
                    )
                }
            }
            offset += DEFAULT_PAGE_SIZE
        } while (pipelineLabelPipelineRecords?.size == DEFAULT_PAGE_SIZE)
    }

    private fun updatePipelineLabelProjectId() {
        var offset = 0
        do {
            // 查询流水线标签记录
            val pipelineLabelRecords = syncPipelineProjectIdDao.listPipelineLabel(
                dslContext = dslContext,
                offset = offset,
                limit = DEFAULT_PAGE_SIZE
            )
            // 更新流水线标签表的projectId
            pipelineLabelRecords?.forEach { pipelineLabel ->
                val groupId = pipelineLabel.groupId
                val groupInfo = pipelineGroupDao.get(dslContext, groupId)
                if (groupInfo != null) {
                    syncPipelineProjectIdDao.updatePipelineLabelProject(
                        dslContext = dslContext,
                        groupId = groupId,
                        projectId = groupInfo.projectId
                    )
                }
            }
            offset += DEFAULT_PAGE_SIZE
        } while (pipelineLabelRecords?.size == DEFAULT_PAGE_SIZE)
    }
}
