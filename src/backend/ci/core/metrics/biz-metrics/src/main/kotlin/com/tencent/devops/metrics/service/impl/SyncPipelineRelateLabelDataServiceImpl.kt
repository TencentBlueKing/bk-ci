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

package com.tencent.devops.metrics.service.impl

import com.tencent.devops.common.api.enums.SystemModuleEnum
import com.tencent.devops.common.api.pojo.PipelineLabelRelateInfo
import com.tencent.devops.common.db.utils.SnowFlakeUtils
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.metrics.dao.ProjectInfoDao
import com.tencent.devops.metrics.service.SyncPipelineRelateLabelDataService
import com.tencent.devops.model.metrics.tables.records.TProjectPipelineLabelInfoRecord
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class SyncPipelineRelateLabelDataServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val projectInfoDao: ProjectInfoDao
): SyncPipelineRelateLabelDataService {
    companion object {
        private val logger = LoggerFactory.getLogger(SyncPipelineRelateLabelDataServiceImpl::class.java)
        private fun metricsDataReportKey(keyWord: String) = "SyncPipelineRelateLabel:${keyWord}"
    }
    override fun syncCreatePipelineRelateLabelData(
        projectId: String,
        pipelineId: String,
        pipelineLabelRelateInfos: List<PipelineLabelRelateInfo>
    ): Boolean {
        val lock = RedisLock(redisOperation, metricsDataReportKey("$projectId::$pipelineId"), 10)
        try {
            lock.lock()
            val pipelineLabelInfoRecords = pipelineLabelRelateInfos.map {
                val createTime = it.createTime!!
                val pipelineLabelInfoRecord = TProjectPipelineLabelInfoRecord(
                    SnowFlakeUtils.getId(SystemModuleEnum.METRICS.code),
                    it.projectId,
                    it.pipelineId,
                    it.labelId,
                    it.name,
                    it.createUser,
                    it.createUser,
                    createTime,
                    createTime
                )
                pipelineLabelInfoRecord
            }
            projectInfoDao.batchCreatePipelineLabelData(
                dslContext,
                pipelineLabelInfoRecords
            )
        } finally {
            lock.unlock()
        }
        return true
    }

    override fun syncDeletePipelineRelateLabelData(
        projectId: String,
        pipelineId: String,
        pipelineLabelRelateInfos: List<PipelineLabelRelateInfo>
    ): Boolean {
        val lock = RedisLock(redisOperation, metricsDataReportKey("$projectId::$pipelineId"), 10)
        try {
            lock.lock()
            projectInfoDao.batchDeletePipelineLabelData(
                dslContext,
                pipelineLabelRelateInfos
            )
        } finally {
            lock.unlock()
        }
        return true
    }

    override fun syncUpdatePipelineRelateLabelData(
        projectId: String,
        pipelineId: String,
        userId: String,
        statisticsTime: LocalDateTime,
        pipelineLabelRelateInfos: List<PipelineLabelRelateInfo>
    ): Boolean {
        val lock = RedisLock(redisOperation, metricsDataReportKey("$projectId::$pipelineId"), 10)
        try {
            lock.lock()
            projectInfoDao.batchUpdatePipelineLabelData(
                dslContext,
                userId,
                statisticsTime,
                pipelineLabelRelateInfos
            )
        } finally {
            lock.unlock()
        }
        return true
    }
}