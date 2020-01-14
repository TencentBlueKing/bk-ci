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

import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildFinishBroadCastEvent
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.process.dao.PipelineFailureBuildDao
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.sql.Timestamp

@Service
class PipelineFailureBuildService @Autowired constructor(
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val dslContext: DSLContext,
    private val pipelineFailureBuildDao: PipelineFailureBuildDao
) {

    fun onPipelineFinish(event: PipelineBuildFinishBroadCastEvent) {
        val buildStatus = try {
            BuildStatus.valueOf(event.status)
        } catch (t: Throwable) {
            logger.warn("Fail to convert the build status(${event.status})", t)
            return
        }
        if (BuildStatus.isFailure(buildStatus)) {
            val buildInfo = pipelineRuntimeService.getBuildInfo(event.buildId)
            if (buildInfo == null) {
                logger.warn("[${event.pipelineId}] build (${event.buildId}) is not exist")
                return
            }
            val startTime = buildInfo.startTime!!
            val endTime = buildInfo.endTime ?: System.currentTimeMillis()
            val count = pipelineFailureBuildDao.insert(
                dslContext = dslContext,
                projectId = event.projectId,
                pipelineId = event.pipelineId,
                buildId = event.buildId,
                startTime = Timestamp(startTime).toLocalDateTime(),
                endTime = Timestamp(endTime).toLocalDateTime()
            )
            logger.info("[${event.projectId}|${event.pipelineId}|${event.buildId}] Insert $count records")
        } else {
            val count = pipelineFailureBuildDao.delete(
                dslContext = dslContext,
                pipelineId = event.pipelineId)
            logger.info("[${event.projectId}|${event.pipelineId}|${event.buildId}] Delete $count records")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineFailureBuildService::class.java)
    }
}
