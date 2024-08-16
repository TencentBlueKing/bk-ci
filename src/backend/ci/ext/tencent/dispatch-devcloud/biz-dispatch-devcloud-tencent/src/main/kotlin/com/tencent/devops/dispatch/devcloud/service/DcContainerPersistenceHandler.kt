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

package com.tencent.devops.dispatch.devcloud.service

import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.dispatch.sdk.BuildFailureException
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.dispatch.devcloud.client.DispatchDevCloudClient
import com.tencent.devops.dispatch.devcloud.common.ErrorCodeEnum
import com.tencent.devops.dispatch.devcloud.dao.DcPersistenceBuildDao
import com.tencent.devops.dispatch.devcloud.dao.DcPersistenceContainerDao
import com.tencent.devops.dispatch.devcloud.pojo.ContainerBuildStatus
import com.tencent.devops.dispatch.devcloud.pojo.DestroyContainerReq
import com.tencent.devops.dispatch.devcloud.pojo.persistence.PersistenceBuildStatus
import com.tencent.devops.dispatch.devcloud.pojo.persistence.PersistenceContainerStatus
import com.tencent.devops.dispatch.devcloud.service.context.DcStartupHandlerContext
import com.tencent.devops.dispatch.devcloud.utils.PersistenceContainerLock
import com.tencent.devops.model.dispatch.devcloud.tables.records.TDevcloudPersistenceContainerRecord
import org.apache.commons.lang3.RandomStringUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DeadlockLoserDataAccessException
import org.springframework.stereotype.Service

@Service
class DcContainerPersistenceHandler @Autowired constructor(
    private val dslContext: DSLContext,
    private val commonConfig: CommonConfig,
    private val buildLogPrinter: BuildLogPrinter,
    private val redisOperation: RedisOperation,
    private val dcPersistenceContainerDao: DcPersistenceContainerDao,
    private val dcPersistenceBuildDao: DcPersistenceBuildDao,
    private val dispatchDevCloudClient: DispatchDevCloudClient,
    private val persistenceBuildService: PersistenceBuildService
) : StartupContainerHandler(commonConfig, buildLogPrinter, dispatchDevCloudClient) {

    companion object {
        private val logger = LoggerFactory.getLogger(DcContainerPersistenceHandler::class.java)
        private const val QUEUE_RETRY_COUNT = 3
    }

    override fun handlerRequest(handlerContext: DcStartupHandlerContext) {
        with(handlerContext) {
            // 非持久化容器，直接跳过此handler
            if (!persistence) {
                return
            }

            // 容器配置变更
            if (containerChanged) {
                // 清理历史持久化容器
                persistenceBuildService.destroyPersistenceContainer(
                    userId = userId,
                    destroyContainerReq = DestroyContainerReq(
                        projectId = projectId,
                        pipelineId = pipelineId,
                        vmSeqId = vmSeqId,
                        poolNo = poolNo
                    )
                )

                // 重置持久化容器配置
                addPersistenceContainer(handlerContext)
            }

            // 获取当前job关联持久化容器配置
            val persistenceContainers = dcPersistenceContainerDao.getPersistenceContainer(
                dslContext = dslContext,
                pipelineId = pipelineId,
                vmSeqId = vmSeqId,
                poolNo = poolNo
            )
            if (persistenceContainers.isEmpty() || persistenceContainers[0].persistenceAgentId.isBlank()) {
                logger.error("$buildLogKey PersistenceContainer is null or PersistenceAgentId is null.")
                throw BuildFailureException(
                    ErrorCodeEnum.START_VM_ERROR.errorType,
                    ErrorCodeEnum.START_VM_ERROR.errorCode,
                    ErrorCodeEnum.START_VM_ERROR.getErrorMessage(),
                    "PersistenceContainer is null or PersistenceAgentId is null"
                )
            } else {
                setPersistenceAgentId(persistenceContainers[0].persistenceAgentId, this)
            }

            // 根据persistenceAgentId加分布式锁
            val lock = PersistenceContainerLock(redisOperation, persistenceAgentId)
            try {
                if (lock.tryLock()) {
                    queueBuild(this)
                } else {
                    logger.warn("Container agent: $persistenceAgentId is busy, can not get redislock.")
                }
            } finally {
                lock.unlock()
            }
        }
    }

    fun addPersistenceContainer(handlerContext: DcStartupHandlerContext) {
        with(handlerContext) {
            val persistenceAgentId = RandomStringUtils.randomAlphabetic(8) + "-${System.currentTimeMillis()}"
            setPersistenceAgentId(persistenceAgentId, this)

            // 存储持久化容器信息
            dcPersistenceContainerDao.createOrUpdate(
                dslContext = dslContext,
                userId = userId,
                pipelineId = pipelineId,
                vmSeqId = vmSeqId,
                poolNo = poolNo,
                projectId = projectId,
                containerName = containerName ?: "",
                persistenceAgentId = persistenceAgentId,
                status = PersistenceContainerStatus.RUNNING.status,
                buildStatus = ContainerBuildStatus.IDLE.status
            )
        }
    }

    fun setPersistenceAgentId(persistenceAgentId: String, handlerContext: DcStartupHandlerContext) {
        handlerContext.persistenceAgentId = persistenceAgentId
    }

    private fun queueBuild(
        handlerContext: DcStartupHandlerContext,
        retryCount: Int = 0
    ) {
        with(handlerContext) {
            try {
                dcPersistenceBuildDao.pushQueueBuild(
                    dslContext = dslContext,
                    userId = userId,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    vmSeqId = vmSeqId,
                    containerHashId = containerHashId!!,
                    containerName = containerName ?: "",
                    persistenceAgentId = persistenceAgentId,
                    agentId = agentId,
                    secretKey = secretKey,
                    executeCount = executeCount ?: 1,
                    status = PersistenceBuildStatus.QUEUE.status
                )
            } catch (e: DeadlockLoserDataAccessException) {
                logger.warn("Fail to queue devcloud build of $buildLogKey $persistenceAgentId")
                if (retryCount <= QUEUE_RETRY_COUNT) {
                    queueBuild(this, retryCount + 1)
                } else {
                    throw OperationException("Fail to queue devcloud build")
                }
            }
        }
    }

    fun updatePersistenceContainerStatus(persistenceAgentId: String, status: PersistenceContainerStatus) {
        dcPersistenceContainerDao.updateContainerStatus(dslContext, persistenceAgentId, status.status)
    }

    fun updatePersistenceBuildStatus(persistenceAgentId: String, status: ContainerBuildStatus) {
        dcPersistenceContainerDao.updateBuildStatus(dslContext, persistenceAgentId, status.status)
    }

    fun getPersistenceContainer(
        pipelineId: String,
        vmSeqId: String,
        poolNo: Int
    ): TDevcloudPersistenceContainerRecord? {
        return dcPersistenceContainerDao.getPersistenceContainer(dslContext, pipelineId, vmSeqId, poolNo)[0]
    }
}
