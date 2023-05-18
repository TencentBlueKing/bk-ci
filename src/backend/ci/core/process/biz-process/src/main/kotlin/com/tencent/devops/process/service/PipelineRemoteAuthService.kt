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

import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.consul.ConsulConstants
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.BkTag
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.model.process.tables.records.TPipelineRemoteAuthRecord
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.constant.ProcessMessageCode.BK_REMOTE_CALL_SOURCE_IP
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_GENERATE_REMOTE_TRIGGER_TOKEN_FAILED
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_NO_MATCHING_PIPELINE
import com.tencent.devops.process.dao.PipelineRemoteAuthDao
import com.tencent.devops.process.engine.control.lock.PipelineRemoteAuthLock
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.process.pojo.PipelineRemoteToken
import com.tencent.devops.process.service.builds.PipelineBuildFacadeService
import com.tencent.devops.process.utils.PIPELINE_START_REMOTE_USER_ID
import com.tencent.devops.process.utils.PIPELINE_START_TASK_ID
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
@Suppress("ALL")
class PipelineRemoteAuthService @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineRemoteAuthDao: PipelineRemoteAuthDao,
    private val pipelineBuildFacadeService: PipelineBuildFacadeService,
    private val pipelineReportService: PipelineRepositoryService,
    private val redisOperation: RedisOperation,
    private val client: Client,
    private val bkTag: BkTag,
    private val buildLogPrinter: BuildLogPrinter,
    private val buildVariableService: BuildVariableService
) {

    fun generateAuth(pipelineId: String, projectId: String, userId: String): PipelineRemoteToken {
        val redisLock = PipelineRemoteAuthLock(redisOperation, pipelineId)
        try {
            redisLock.lock()
            val record = pipelineRemoteAuthDao.getByPipelineId(dslContext, projectId, pipelineId)
            return if (record == null) {
                val auth = UUIDUtil.generate()
                pipelineRemoteAuthDao.addAuth(dslContext, pipelineId, auth, projectId, userId)
                PipelineRemoteToken(auth)
            } else {
                PipelineRemoteToken(record.pipelineAuth)
            }
        } catch (ignored: Throwable) {
            logger.warn("Fail to generate the remote pipeline token of pipeline $pipelineId - $projectId", ignored)
            throw OperationException(
                MessageUtil.getMessageByLocale(ERROR_GENERATE_REMOTE_TRIGGER_TOKEN_FAILED, I18nUtil.getLanguage(userId))
            )
        } finally {
            redisLock.unlock()
        }
    }

    private fun getPipeline(auth: String): TPipelineRemoteAuthRecord? {
        return pipelineRemoteAuthDao.getByAuth(dslContext, auth)
    }

    fun startPipeline(
        auth: String,
        values: Map<String, String>,
        sourceIp: String? = null,
        startUser: String? = null
    ): BuildId {
        val pipeline = getPipeline(auth)
        if (pipeline == null) {
            logger.warn("The pipeline of auth $auth is not exist")
            throw OperationException(
                I18nUtil.getCodeLanMessage(ERROR_NO_MATCHING_PIPELINE)
            )
        }
        var userId = pipelineReportService.getPipelineInfo(pipeline.projectId, pipeline.pipelineId)?.lastModifyUser

        if (userId.isNullOrBlank()) {
            logger.info("Fail to get the userId of the pipeline, use ${pipeline.createUser}")
            userId = pipeline.createUser
        }
        val vals = values.toMutableMap()
        if (!startUser.isNullOrBlank()) {
            vals[PIPELINE_START_REMOTE_USER_ID] = startUser
        }

        logger.info("Start the pipeline remotely of $userId ${pipeline.pipelineId} of project ${pipeline.projectId}")
        // #5779 为兼容多集群的场景。流水线的启动需要路由到项目对应的集群。此处携带X-DEVOPS-PROJECT-ID头重新请求网关,由网关路由到项目对应的集群
        /* #7095 因Bktag设置了router_tag 默认为本集群，导致网关不会根据X-DEVOPS-PROJECT-ID路由。故直接根据项目获取router
                 不使用client.get直接调用，因client内不支持同服务间的feign调用。故只能通过网关代理下 */
        val projectConsulTag = redisOperation.hget(ConsulConstants.PROJECT_TAG_REDIS_KEY, pipeline.projectId)
        return bkTag.invokeByTag(projectConsulTag) {
            logger.info("start call service api ${pipeline.projectId} ${pipeline.pipelineId}, $projectConsulTag ${bkTag.getFinalTag()}")
            val buildId = client.getGateway(ServiceBuildResource::class).manualStartupNew(
                userId = userId!!,
                projectId = pipeline.projectId,
                pipelineId = pipeline.pipelineId,
                values = vals.toMap(),
                channelCode = ChannelCode.BS,
                startType = StartType.REMOTE,
                buildNo = null
            ).data!!
            // 在远程触发器job中打印sourcIp
            val taskId = buildVariableService.getVariable(
                projectId = pipeline.projectId,
                pipelineId = pipeline.pipelineId,
                buildId = buildId.id,
                varName = PIPELINE_START_TASK_ID
            )
            if (taskId != null) {
                buildLogPrinter.addLine(
                    buildId = buildId.id,
                    message = MessageUtil.getMessageByLocale(
                        BK_REMOTE_CALL_SOURCE_IP,
                        I18nUtil.getLanguage(userId),
                        arrayOf("$sourceIp")
                    ),
                    tag = taskId,
                    executeCount = 1
                )
            }
            BuildId(
                id = buildId.id,
                executeCount = 1,
                pipelineId = pipeline.pipelineId,
                projectId = pipeline.projectId
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineRemoteAuthService::class.java)
    }
}
