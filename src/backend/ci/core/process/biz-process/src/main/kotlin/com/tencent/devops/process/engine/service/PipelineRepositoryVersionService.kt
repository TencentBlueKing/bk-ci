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

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.pojo.pipeline.PipelineModelAnalysisEvent
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.process.dao.PipelineSettingDao
import com.tencent.devops.process.dao.PipelineSettingVersionDao
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import com.tencent.devops.process.engine.dao.PipelineInfoVersionDao
import com.tencent.devops.process.engine.dao.PipelineModelTaskVersionDao
import com.tencent.devops.process.engine.dao.PipelineResVersionDao
import com.tencent.devops.process.engine.dao.template.TemplatePipelineDao
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.engine.pojo.event.PipelineDeleteEvent
import com.tencent.devops.process.pojo.setting.PipelineRunLockType
import com.tencent.devops.process.pojo.setting.PipelineSetting
import com.tencent.devops.process.pojo.setting.Subscription
import org.joda.time.LocalDateTime
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import javax.ws.rs.NotFoundException

/**
 *
 *
 * @version 1.0
 */
@Service
class PipelineRepositoryVersionService constructor(
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val objectMapper: ObjectMapper,
    private val dslContext: DSLContext,
    private val pipelineInfoDao: PipelineInfoDao,
    private val pipelineInfoVersionDao: PipelineInfoVersionDao,
    private val pipelineResVersionDao: PipelineResVersionDao,
    private val pipelineModelTaskVersionDao: PipelineModelTaskVersionDao,
    private val pipelineSettingDao: PipelineSettingDao,
    private val pipelineSettingVersionDao: PipelineSettingVersionDao,
    private val templatePipelineDao: TemplatePipelineDao
) {

    fun getPipelineInfo(projectId: String?, pipelineId: String, channelCode: ChannelCode? = null): PipelineInfo? {
        val template = templatePipelineDao.get(dslContext, pipelineId)
        val templateId = template?.templateId
        return pipelineInfoDao.convert(pipelineInfoDao.getPipelineInfo(dslContext, projectId, pipelineId, channelCode), templateId)
    }

    fun getModel(pipelineId: String, version: Int? = null): Model? {
        val modelString = pipelineResVersionDao.getVersionModelString(dslContext, pipelineId, version)
        return try {
            objectMapper.readValue(modelString, Model::class.java)
        } catch (e: Exception) {
            logger.error("get process($pipelineId) model fail", e)
            null
        }
    }

    fun deletePipeline(
        projectId: String,
        pipelineId: String,
        userId: String,
        version: Int,
        channelCode: ChannelCode?,
        delete: Boolean
    ) {

        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)

            val record =
                (pipelineInfoVersionDao.getPipelineInfo(transactionContext, projectId, pipelineId, version, channelCode, delete)
                    ?: throw NotFoundException("要删除的流水线版本不存在"))
            if (delete) {
                pipelineInfoVersionDao.delete(transactionContext, projectId, pipelineId)
            } else {
                // 删除前改名，防止名称占用
                val deleteTime = LocalDateTime.now().toString("yyyyMMddHHmm")
                var deleteName = "${record.pipelineName}[$deleteTime]"
                if (deleteName.length > 255) { // 超过截断，且用且珍惜
                    deleteName = deleteName.substring(0, 255)
                }

                pipelineInfoVersionDao.softDelete(
                    transactionContext,
                    projectId,
                    pipelineId,
                    deleteName,
                    userId,
                    version,
                    channelCode
                )
                // 同时要对Setting中的name做设置
                pipelineSettingVersionDao.updateSetting(
                    transactionContext,
                    pipelineId,
                    version,
                    deleteName,
                    "DELETE BY $userId in $deleteTime"
                )
            }

            pipelineModelTaskVersionDao.deletePipelineTasks(transactionContext, projectId, pipelineId, version)

            pipelineEventDispatcher.dispatch(
                PipelineDeleteEvent(
                    source = "delete_pipeline",
                    projectId = projectId,
                    pipelineId = pipelineId,
                    userId = userId,
                    clearUpModel = delete
                ),
                PipelineModelAnalysisEvent(
                    source = "delete_pipeline",
                    projectId = projectId,
                    pipelineId = pipelineId,
                    userId = userId,
                    model = "",
                    channelCode = record.channel
                )
            )
        }
    }

    fun getSetting(pipelineId: String): PipelineSetting? {
        val t = pipelineSettingDao.getSetting(dslContext, pipelineId)
        return if (t != null) {
            PipelineSetting(
                t.projectId,
                t.pipelineId,
                t.name,
                t.desc,
                PipelineRunLockType.valueOf(t.runLockType),
                Subscription(), Subscription(),
                emptyList(),
                t.waitQueueTimeSecond / 60,
                t.maxQueueSize
            )
        } else null
    }

    fun listPipelineVersion(projectId: String, pipelineId: String): List<PipelineInfo> {
        val result = pipelineInfoVersionDao.listPipelineVersion(dslContext, projectId, pipelineId)
        val list = mutableListOf<PipelineInfo>()

        result?.forEach {
            if (it != null) {
                val template = templatePipelineDao.get(dslContext, pipelineId)
                val templateId = template?.templateId
                list.add(pipelineInfoVersionDao.convert(it, templateId)!!)
            }
        }
        return list
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineRepositoryVersionService::class.java)
    }
}
