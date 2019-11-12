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

package com.tencent.devops.process.service

import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.model.process.tables.records.TPipelineRemoteAuthRecord
import com.tencent.devops.process.dao.PipelineRemoteAuthDao
import com.tencent.devops.process.engine.service.PipelineBuildService
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.process.pojo.PipelineRemoteToken
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineRemoteAuthService @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineRemoteAuthDao: PipelineRemoteAuthDao,
    private val pipelineBuildService: PipelineBuildService,
    private val pipelineReportService: PipelineRepositoryService,
    private val redisOperation: RedisOperation
) {

    fun generateAuth(pipelineId: String, projectId: String, userId: String): PipelineRemoteToken {
        val redisLock = RedisLock(redisOperation, "process_pipeline_remote_token_lock_key_$pipelineId", 10)
        try {
            redisLock.lock()
            val record = pipelineRemoteAuthDao.getByPipelineId(dslContext, pipelineId)
            return if (record == null) {
                val auth = UUIDUtil.generate()
                pipelineRemoteAuthDao.addAuth(dslContext, pipelineId, auth, projectId, userId)
                PipelineRemoteToken(auth)
            } else {
                PipelineRemoteToken(record.pipelineAuth)
            }
        } catch (ignored: Throwable) {
            logger.warn("Fail to generate the remote pipeline token of pipeline $pipelineId - $projectId", ignored)
            throw OperationException("生成远程触发token失败")
        } finally {
            redisLock.unlock()
        }
    }

    private fun getPipeline(auth: String): TPipelineRemoteAuthRecord? {
        return pipelineRemoteAuthDao.getByAuth(dslContext, auth)
    }

    fun startPipeline(auth: String, values: Map<String, String>): BuildId {
        val pipeline = getPipeline(auth)
        if (pipeline == null) {
            logger.warn("The pipeline of auth $auth is not exist")
            throw OperationException("没有找到对应的流水线")
        }
        var userId = pipelineReportService.getPipelineInfo(pipeline.pipelineId)?.lastModifyUser

        if (userId.isNullOrBlank()) {
            logger.info("Fail to get the userId of the pipeline, use ${pipeline.createUser}")
            userId = pipeline.createUser
        }

        logger.info("Start the pipeline remotely of $userId ${pipeline.pipelineId} of project ${pipeline.projectId}")
        return BuildId(
            pipelineBuildService.buildManualStartup(
                userId!!, StartType.REMOTE, pipeline.projectId, pipeline.pipelineId, values,
                ChannelCode.BS, true, false, "m-$auth"
            )
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineRemoteAuthService::class.java)
    }
}
