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

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.dao.PipelineSettingVersionDao
import com.tencent.devops.process.engine.control.lock.PipelineVersionLock
import com.tencent.devops.process.engine.dao.PipelineBuildDao
import com.tencent.devops.process.engine.dao.PipelineResVersionDao
import com.tencent.devops.process.engine.pojo.PipelineInfo
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Service

@Service
class PipelineRepositoryVersionService(
    private val dslContext: DSLContext,
    private val pipelineResVersionDao: PipelineResVersionDao,
    private val pipelineSettingVersionDao: PipelineSettingVersionDao,
    private val pipelineBuildDao: PipelineBuildDao,
    private val redisOperation: RedisOperation
) {

    fun addVerRef(projectId: String, pipelineId: String, resourceVersion: Int) {
        PipelineVersionLock(redisOperation, pipelineId, resourceVersion).use { versionLock ->
            versionLock.lock()
            // 查询流水线版本记录
            val pipelineVersionInfo = pipelineResVersionDao.getPipelineVersionSimple(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                version = resourceVersion
            )
            val referFlag = pipelineVersionInfo?.referFlag ?: true
            val referCount = pipelineVersionInfo?.referCount?.let { self -> self + 1 }
            // 兼容老数据缺少关联构建记录的情况，全量统计关联数据数量
                ?: pipelineBuildDao.countBuildNumByVersion(
                    dslContext = dslContext,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    version = resourceVersion
                )

            // 更新流水线版本关联构建记录信息
            pipelineResVersionDao.updatePipelineVersionReferInfo(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                version = resourceVersion,
                referCount = referCount,
                referFlag = referFlag
            )
        }
    }

    fun deletePipelineVer(projectId: String, pipelineId: String, version: Int) {
        // 判断该流水线版本是否还有关联的构建记录，没有记录才能删除
        val pipelineVersionLock = PipelineVersionLock(redisOperation, pipelineId, version)
        try {
            pipelineVersionLock.lock()
            val count = pipelineBuildDao.countBuildNumByVersion(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                version = version
            )
            if (count > 0) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_PIPELINE_CAN_NOT_DELETE_WHEN_HAVE_BUILD_RECORD
                )
            }
            dslContext.transaction { t ->
                val transactionContext = DSL.using(t)
                pipelineResVersionDao.deleteByVer(transactionContext, projectId, pipelineId, version)
                pipelineSettingVersionDao.deleteByVer(transactionContext, projectId, pipelineId, version)
            }
        } finally {
            pipelineVersionLock.unlock()
        }
    }

    fun listPipelineVersion(
        pipelineInfo: PipelineInfo?,
        projectId: String,
        pipelineId: String,
        offset: Int,
        limit: Int
    ): Pair<Int, List<PipelineInfo>> {
        if (pipelineInfo == null) {
            return Pair(0, emptyList())
        }

        val count = pipelineResVersionDao.count(dslContext, projectId, pipelineId)
        val result = pipelineResVersionDao.listPipelineVersion(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            offset = offset,
            limit = limit
        )
        val list = mutableListOf<PipelineInfo>()

        result.forEach {
            list.add(
                pipelineInfo.copy(
                    createTime = it.createTime,
                    creator = it.creator,
                    version = it.version,
                    versionName = it.versionName
                )
            )
        }
        return count to list
    }
}
