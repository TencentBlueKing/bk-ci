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

import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.engine.control.lock.PipelineNextQueueLock
import com.tencent.devops.process.engine.dao.PipelineBuildDao
import com.tencent.devops.process.engine.pojo.BuildInfo
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineRuntimeExtService @Autowired constructor(
    val redisOperation: RedisOperation,
    val pipelineBuildDao: PipelineBuildDao,
    val dslContext: DSLContext
) {

    fun popNextQueueBuildInfo(
        projectId: String,
        pipelineId: String,
        buildStatus: BuildStatus = BuildStatus.QUEUE_CACHE
    ): BuildInfo? {

        val redisLock = PipelineNextQueueLock(redisOperation, pipelineId)
        try {
            redisLock.lock()
            val buildInfo = pipelineBuildDao.convert(
                pipelineBuildDao.getOneQueueBuild(dslContext, projectId = projectId, pipelineId = pipelineId)
            )
            if (buildInfo != null) {
                pipelineBuildDao.updateStatus(
                    dslContext = dslContext,
                    projectId = projectId,
                    buildId = buildInfo.buildId,
                    oldBuildStatus = buildInfo.status,
                    newBuildStatus = buildStatus
                )
            }
            return buildInfo
        } finally {
            redisLock.unlock()
        }
    }

    fun queueCanPend2Start(projectId: String, pipelineId: String, buildId: String): Boolean {
        val redisLock = PipelineNextQueueLock(redisOperation, pipelineId)
        try {
            redisLock.lock()
            val buildRecord = pipelineBuildDao.getOneQueueBuild(
                dslContext,
                projectId = projectId,
                pipelineId = pipelineId
            )
            if (buildRecord != null) {
                if (buildId == buildRecord.buildId) {
                    return pipelineBuildDao.updateStatus(
                        dslContext = dslContext,
                        projectId = projectId,
                        buildId = buildRecord.buildId,
                        oldBuildStatus = BuildStatus.QUEUE,
                        newBuildStatus = BuildStatus.QUEUE_CACHE
                    )
                }
            }
            return false
        } finally {
            redisLock.unlock()
        }
    }

    /**
     *  获取同一个并发组内首个排队的BuildInfo
     */
    fun popNextConcurrencyGroupQueueCanPend2Start(
        projectId: String,
        concurrencyGroup: String,
        pipelineId: String? = null,
        buildId: String? = null,
        buildStatus: BuildStatus = BuildStatus.QUEUE_CACHE
    ): BuildInfo? {
        val buildInfo = pipelineBuildDao.convert(
            pipelineBuildDao.getOneConcurrencyQueueBuild(
                dslContext = dslContext,
                projectId = projectId,
                concurrencyGroup = concurrencyGroup,
                pipelineId = pipelineId
            )
        )
        val updateBuildId = buildId ?: buildInfo?.buildId
        if (buildInfo != null && updateBuildId == buildInfo.buildId) {
            pipelineBuildDao.updateStatus(
                dslContext = dslContext,
                projectId = projectId,
                buildId = buildInfo.buildId,
                oldBuildStatus = buildInfo.status,
                newBuildStatus = buildStatus
            )
            return buildInfo
        }
        return null
    }

    fun existQueue(projectId: String, pipelineId: String, buildId: String, buildStatus: BuildStatus): Boolean {
        val redisLock = PipelineNextQueueLock(redisOperation, pipelineId, buildId)
        try {
            redisLock.lock()
            return pipelineBuildDao.updateStatus(
                dslContext = dslContext,
                projectId = projectId,
                buildId = buildId,
                oldBuildStatus = buildStatus,
                newBuildStatus = BuildStatus.UNEXEC
            )
        } finally {
            redisLock.unlock()
        }
    }
}
