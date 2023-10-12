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

package com.tencent.devops.dispatch.docker.client

import com.tencent.devops.common.api.util.SecurityUtil
import com.tencent.devops.dispatch.docker.client.context.BuildLessEndHandlerContext
import com.tencent.devops.dispatch.docker.dao.PipelineDockerBuildDao
import com.tencent.devops.dispatch.docker.utils.RedisUtils
import com.tencent.devops.dispatch.pojo.enums.PipelineTaskStatus
import com.tencent.devops.model.dispatch.tables.records.TDispatchPipelineDockerBuildRecord
import com.tencent.devops.process.pojo.mq.PipelineBuildLessShutdownDispatchEvent
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class BuildLessEndPrepareHandler @Autowired constructor(
    private val dslContext: DSLContext,
    private val redisUtils: RedisUtils,
    private val pipelineDockerBuildDao: PipelineDockerBuildDao,
    private val buildLessEndHandler: BuildLessEndHandler
) : Handler<BuildLessEndHandlerContext>() {
    private val logger = LoggerFactory.getLogger(BuildLessEndPrepareHandler::class.java)

    override fun handlerRequest(handlerContext: BuildLessEndHandlerContext) {
        with(handlerContext) {
            handlerContext.buildLogKey = "${event.pipelineId}|${event.buildId}|" +
                "${event.vmSeqId}|${event.executeCount}"

            logger.info("$buildLogKey Start to finish the pipeline build($event)")
            if (event.vmSeqId.isNullOrBlank()) {
                val records = pipelineDockerBuildDao.listBuilds(dslContext, event.buildId)
                records.forEach {
                    finishBuild(event.buildResult, event, it)
                }
            } else {
                val record = pipelineDockerBuildDao.getBuild(dslContext, event.buildId, event.vmSeqId!!.toInt())
                if (record != null) {
                    finishBuild(event.buildResult, event, record)
                } else {
                    logger.info("$buildLogKey no record.")
                }
            }
        }
    }

    private fun finishBuild(
        success: Boolean,
        event: PipelineBuildLessShutdownDispatchEvent,
        record: TDispatchPipelineDockerBuildRecord
    ) {
        logger.info("${record.buildId}|${record.vmSeqId} Finish the docker buildless with result($success)")
        try {
            if (record.dockerIp.isNotEmpty()) {
                buildLessEndHandler.handlerRequest(
                    BuildLessEndHandlerContext(
                        event = event,
                        containerId = record.containerId,
                        buildLessHost = record.dockerIp
                    )
                )
            }

            pipelineDockerBuildDao.updateStatus(
                dslContext,
                record.buildId,
                record.vmSeqId,
                if (success) PipelineTaskStatus.DONE else PipelineTaskStatus.FAILURE
            )
        } catch (e: Exception) {
            logger.warn("${record.buildId}|${record.vmSeqId} Finish buildless error.", e)
        } finally {
            // 无编译环境清除redisAuth
            val decryptSecretKey = SecurityUtil.decrypt(record.secretKey)
            logger.info("${record.buildId}|${record.vmSeqId} delete dockerBuildKey ${record.id}|$decryptSecretKey")
            redisUtils.deleteDockerBuild(record.id, SecurityUtil.decrypt(record.secretKey))
        }
    }
}
