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

package com.tencent.devops.dispatch.service

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.enums.AgentStatus
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.pojo.AgentResult
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.SimpleResult
import com.tencent.devops.common.api.pojo.agent.UpgradeItem
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentDockerInfoDispatch
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.dispatch.dao.ThirdPartyAgentBuildDao
import com.tencent.devops.dispatch.pojo.enums.PipelineTaskStatus
import com.tencent.devops.dispatch.pojo.thirdPartyAgent.AgentBuildInfo
import com.tencent.devops.dispatch.pojo.thirdPartyAgent.BuildJobType
import com.tencent.devops.dispatch.pojo.thirdPartyAgent.ThirdPartyBuildDockerInfo
import com.tencent.devops.dispatch.pojo.thirdPartyAgent.ThirdPartyBuildInfo
import com.tencent.devops.dispatch.pojo.thirdPartyAgent.ThirdPartyBuildWithStatus
import com.tencent.devops.dispatch.service.dispatcher.agent.DispatchService
import com.tencent.devops.dispatch.utils.ThirdPartyAgentLock
import com.tencent.devops.dispatch.utils.ThirdPartyAgentUtils
import com.tencent.devops.dispatch.utils.redis.ThirdPartyAgentBuildRedisUtils
import com.tencent.devops.environment.api.thirdPartyAgent.ServiceThirdPartyAgentResource
import com.tencent.devops.environment.pojo.thirdPartyAgent.ThirdPartyAgent
import com.tencent.devops.environment.pojo.thirdPartyAgent.ThirdPartyAgentUpgradeByVersionInfo
import com.tencent.devops.model.dispatch.tables.records.TDispatchThirdpartyAgentBuildRecord
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.pojo.mq.PipelineAgentShutdownEvent
import com.tencent.devops.process.pojo.mq.PipelineAgentStartupEvent
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DeadlockLoserDataAccessException
import org.springframework.stereotype.Service
import javax.ws.rs.NotFoundException

@Service
@Suppress("ALL")
class ThirdPartyAgentService @Autowired constructor(
    private val dslContext: DSLContext,
    private val thirdPartyAgentBuildRedisUtils: ThirdPartyAgentBuildRedisUtils,
    private val client: Client,
    private val redisOperation: RedisOperation,
    private val thirdPartyAgentBuildDao: ThirdPartyAgentBuildDao,
    private val dispatchService: DispatchService
) {

    fun queueBuild(
        agent: ThirdPartyAgent,
        thirdPartyAgentWorkspace: String,
        event: PipelineAgentStartupEvent,
        retryCount: Int = 0,
        dockerInfo: ThirdPartyAgentDockerInfoDispatch?
    ) {
        with(event) {
            try {
                thirdPartyAgentBuildDao.add(
                    dslContext = dslContext,
                    projectId = projectId,
                    agentId = agent.agentId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    vmSeqId = vmSeqId,
                    thirdPartyAgentWorkspace = thirdPartyAgentWorkspace,
                    pipelineName = pipelineName,
                    buildNum = buildNo,
                    taskName = taskName,
                    agentIp = agent.ip,
                    nodeId = HashUtil.decodeIdToLong(agent.nodeId ?: ""),
                    dockerInfo = dockerInfo,
                    executeCount = event.executeCount,
                    containerHashId = event.containerHashId
                )
            } catch (e: DeadlockLoserDataAccessException) {
                logger.warn("Fail to add the third party agent build of ($buildId|$vmSeqId|${agent.agentId}")
                if (retryCount <= QUEUE_RETRY_COUNT) {
                    queueBuild(agent, thirdPartyAgentWorkspace, event, retryCount + 1, dockerInfo)
                } else {
                    throw OperationException("Fail to add the third party agent build")
                }
            }
        }
    }

    fun getPreBuildAgentIds(projectId: String, pipelineId: String, vmSeqId: String, size: Int): List<String> {
        return thirdPartyAgentBuildDao.getPreBuildAgentIds(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            vmSeqId = vmSeqId,
            size = size
        )
    }

    fun getRunningBuilds(agentId: String): Int {
        return thirdPartyAgentBuildDao.getRunningAndQueueBuilds(dslContext, agentId).size
    }

    fun getDockerRunningBuilds(agentId: String): Int {
        return thirdPartyAgentBuildDao.getDockerRunningAndQueueBuilds(dslContext, agentId).size
    }

    fun startBuild(
        projectId: String,
        agentId: String,
        secretKey: String,
        buildType: BuildJobType
    ): AgentResult<ThirdPartyBuildInfo?> {
        // Get the queue status build by buildId and agentId
        logger.debug("Start the third party agent($agentId) of project($projectId)")
        try {
            val agentResult = try {
                client.get(ServiceThirdPartyAgentResource::class).getAgentById(projectId, agentId)
            } catch (e: RemoteServiceException) {
                logger.warn("Fail to get the agent($agentId) of project($projectId) because of ${e.message}")
                return AgentResult(1, e.message ?: "Fail to get the agent")
            }

            if (agentResult.agentStatus == AgentStatus.DELETE) {
                return AgentResult(AgentStatus.DELETE, null)
            }

            if (agentResult.isNotOk()) {
                logger.warn("Fail to get the third party agent($agentId) because of ${agentResult.message}")
                throw NotFoundException("Fail to get the agent")
            }

            if (agentResult.data == null) {
                logger.warn("Get the null third party agent($agentId)")
                throw NotFoundException("Fail to get the agent")
            }

            if (agentResult.data!!.secretKey != secretKey) {
                logger.warn(
                    "The secretKey($secretKey) is not match the expect one(${agentResult.data!!.secretKey} " +
                        "of project($projectId) and agent($agentId)"
                )
                throw NotFoundException("Fail to get the agent")
            }

            if (agentResult.data!!.status != AgentStatus.IMPORT_OK) {
                logger.warn("The agent($agentId) is not import(${agentResult.data!!.status})")
                throw NotFoundException("Fail to get the agent")
            }

            logger.debug("Third party agent($agentId) start up")

            val redisLock = ThirdPartyAgentLock(redisOperation, projectId, agentId)
            try {
                redisLock.lock()
                val build = thirdPartyAgentBuildDao.fetchOneQueueBuild(dslContext, agentId, buildType) ?: run {
                    logger.debug("There is not build by agent($agentId) in queue")
                    return AgentResult(AgentStatus.IMPORT_OK, null)
                }

                logger.debug(
                    "Third party agent($agentId) start up agent project($projectId) build project(${build.projectId})"
                )

                logger.info("Start the build(${build.buildId}) of agent($agentId) and seq(${build.vmSeqId})")
                thirdPartyAgentBuildDao.updateStatus(dslContext, build.id, PipelineTaskStatus.RUNNING)

                try {
                    client.get(ServiceThirdPartyAgentResource::class)
                        .agentTaskStarted(
                            build.projectId,
                            build.pipelineId,
                            build.buildId,
                            build.vmSeqId,
                            build.agentId
                        )
                } catch (e: RemoteServiceException) {
                    logger.warn(
                        "notify agent task[$build.projectId|${build.buildId}|${build.vmSeqId}|$agentId]" +
                            " claim failed, cause: ${e.message} agent project($projectId)"
                    )
                }

                // 第三方构建机docker启动获取镜像凭据
                val dockerInfo = if (build.dockerInfo == null) {
                    null
                } else {
                    JsonUtil.getObjectMapper().readValue(
                        build.dockerInfo.data(),
                        object : TypeReference<ThirdPartyAgentDockerInfoDispatch>() {}
                    )
                }
                var errMsg: String? = null
                var buildDockerInfo: ThirdPartyBuildDockerInfo? = null
                // 只有凭据ID的参与计算
                if (dockerInfo != null) {
                    if ((
                        dockerInfo.credential?.user.isNullOrBlank() &&
                            dockerInfo.credential?.password.isNullOrBlank()
                        ) &&
                        !(dockerInfo.credential?.credentialId.isNullOrBlank())
                    ) {
                        val (userName, password) = try {
                            ThirdPartyAgentUtils.getTicket(
                                client = client,
                                projectId = projectId,
                                credInfo = dockerInfo.credential!!
                            )
                        } catch (e: Exception) {
                            logger.error("$projectId agent docker build get ticket ${dockerInfo.credential} error", e)
                            errMsg = e.message
                            Pair(null, null)
                        }
                        dockerInfo.credential?.user = userName
                        dockerInfo.credential?.password = password
                    }
                    buildDockerInfo = ThirdPartyBuildDockerInfo(dockerInfo)
                    buildDockerInfo.credential?.errMsg = errMsg
                }

                return AgentResult(
                    AgentStatus.IMPORT_OK,
                    ThirdPartyBuildInfo(
                        projectId = build.projectId,
                        buildId = build.buildId,
                        vmSeqId = build.vmSeqId,
                        workspace = build.workspace,
                        pipelineId = build.pipelineId,
                        dockerBuildInfo = buildDockerInfo,
                        executeCount = build.executeCount,
                        containerHashId = build.containerHashId
                    )
                )
            } finally {
                redisLock.unlock()
            }
        } catch (ignored: Throwable) {
            logger.warn("Fail to start build for agent($agentId)", ignored)
            throw ignored
        }
    }

    fun checkIfCanUpgradeByVersion(
        projectId: String,
        agentId: String,
        secretKey: String,
        version: String?,
        masterVersion: String?
    ): AgentResult<Boolean> {
        // logger.info("Start to check if the agent($agentId) of version $version of project($projectId) can upgrade")
        return try {
            val agentUpgradeResult = client.get(ServiceThirdPartyAgentResource::class)
                .upgradeByVersion(projectId, agentId, secretKey, version, masterVersion)
            return if (agentUpgradeResult.data != null && !agentUpgradeResult.data!!) {
                agentUpgradeResult
            } else {
                thirdPartyAgentBuildRedisUtils.setThirdPartyAgentUpgrading(projectId, agentId)
                AgentResult(AgentStatus.IMPORT_OK, true)
            }
        } catch (t: Throwable) {
            logger.warn("Fail to check if agent can upgrade", t)
            AgentResult(AgentStatus.IMPORT_EXCEPTION, false)
        }
    }

    fun checkIfCanUpgradeByVersionNew(
        projectId: String,
        agentId: String,
        secretKey: String,
        info: ThirdPartyAgentUpgradeByVersionInfo
    ): AgentResult<UpgradeItem> {
        // logger.info("Start to check if the agent($agentId) of version $version of project($projectId) can upgrade")
        return try {
            val agentUpgradeResult = client.get(ServiceThirdPartyAgentResource::class)
                .upgradeByVersionNew(projectId, agentId, secretKey, info)
            return if (!agentUpgradeResult.data!!.agent && !agentUpgradeResult.data!!.worker &&
                !agentUpgradeResult.data!!.jdk
            ) {
                agentUpgradeResult
            } else {
                thirdPartyAgentBuildRedisUtils.setThirdPartyAgentUpgrading(projectId, agentId)
                agentUpgradeResult
            }
        } catch (t: Throwable) {
            logger.warn("Fail to check if agent can upgrade", t)
            AgentResult(
                AgentStatus.IMPORT_EXCEPTION,
                UpgradeItem(
                    agent = false,
                    worker = false,
                    jdk = false,
                    dockerInitFile = false
                )
            )
        }
    }

    fun finishUpgrade(projectId: String, agentId: String, secretKey: String, success: Boolean): AgentResult<Boolean> {
        logger.info("The agent($agentId) of project($projectId) finish upgrading with result $success")
        try {
            val agentResult = try {
                client.get(ServiceThirdPartyAgentResource::class).getAgentById(projectId, agentId)
            } catch (e: RemoteServiceException) {
                logger.warn("Fail to get the agent($agentId) of project($projectId) because of ${e.message}")
                return AgentResult(1, e.message ?: "Fail to get the agent")
            }

            if (agentResult.agentStatus == AgentStatus.DELETE) {
                return AgentResult(AgentStatus.DELETE, false)
            }

            if (agentResult.data == null) {
                logger.warn("Get the null third party agent($agentId)")
                throw NotFoundException("Fail to get the agent")
            }

            if (agentResult.data!!.secretKey != secretKey) {
                logger.warn("The secretKey($secretKey) is not match of project($projectId) and agent($agentId)")
                throw NotFoundException("Fail to get the agent")
            }
            thirdPartyAgentBuildRedisUtils.thirdPartyAgentUpgradingDone(projectId, agentId)
            return AgentResult(agentResult.agentStatus!!, true)
        } catch (ignored: Throwable) {
            logger.warn("Fail to finish upgrading", ignored)
            return AgentResult(AgentStatus.IMPORT_EXCEPTION, false)
        }
    }

    fun finishBuild(event: PipelineAgentShutdownEvent) {
        val buildId = event.buildId
        val vmSeqId = event.vmSeqId
        val success = event.buildResult
        if (vmSeqId.isNullOrBlank()) {
            val records = thirdPartyAgentBuildDao.list(dslContext, buildId)
            if (records.isEmpty()) {
                return
            }
            records.forEach {
                finishBuild(it, success)
                if (it.dockerInfo != null) {
                    // 第三方构建机可能是docker构建机时需要在这里删除docker类型的redisKey
                    dispatchService.shutdown(event)
                }
            }
        } else {
            val record = thirdPartyAgentBuildDao.get(dslContext, buildId, vmSeqId) ?: return
            finishBuild(record, success)
            if (record.dockerInfo != null) {
                // 第三方构建机可能是docker构建机时需要在这里删除docker类型的redisKey
                dispatchService.shutdown(event)
            }
        }
    }

    fun listAgentBuilds(agentId: String, page: Int?, pageSize: Int?): Page<AgentBuildInfo> {
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: PageUtil.MAX_PAGE_SIZE
        val sqlLimit = PageUtil.convertPageSizeToSQLMAXLimit(pageNotNull, pageSizeNotNull)
        val offset = sqlLimit.offset
        val limit = sqlLimit.limit

        val agentBuildCount = thirdPartyAgentBuildDao.countAgentBuilds(dslContext, agentId)
        val agentBuilds = thirdPartyAgentBuildDao.listAgentBuilds(dslContext, agentId, offset, limit).map {
            AgentBuildInfo(
                projectId = it.projectId,
                agentId = it.agentId,
                pipelineId = it.pipelineId,
                pipelineName = it.pipelineName,
                buildId = it.buildId,
                buildNum = it.buildNum,
                vmSeqId = it.vmSeqId,
                taskName = it.taskName,
                status = PipelineTaskStatus.toStatus(it.status).name,
                createdTime = it.createdTime.timestamp(),
                updatedTime = it.updatedTime.timestamp(),
                workspace = it.workspace
            )
        }
        return Page(pageNotNull, pageSizeNotNull, agentBuildCount, agentBuilds)
    }

    private fun finishBuild(record: TDispatchThirdpartyAgentBuildRecord, success: Boolean) {
        logger.info(
            "Finish the third party agent(${record.agentId}) build(${record.buildId}) " +
                "of seq(${record.vmSeqId}) and status(${record.status})"
        )
        val agentResult = client.get(ServiceThirdPartyAgentResource::class)
            .getAgentById(record.projectId, record.agentId)
        if (agentResult.isNotOk()) {
            logger.warn("Fail to get the third party agent(${record.agentId}) because of ${agentResult.message}")
            throw RemoteServiceException("Fail to get the third party agent")
        }

        if (agentResult.data == null) {
            logger.warn("Get the null third party agent(${record.agentId})")
            throw RemoteServiceException("Fail to get the third party agent")
        }

        thirdPartyAgentBuildRedisUtils.deleteThirdPartyBuild(
            secretKey = agentResult.data!!.secretKey,
            agentId = record.agentId,
            buildId = record.buildId,
            vmSeqId = record.vmSeqId
        )
        thirdPartyAgentBuildDao.updateStatus(
            dslContext, record.id,
            if (success) PipelineTaskStatus.DONE else PipelineTaskStatus.FAILURE
        )
    }

    fun workerBuildFinish(projectId: String, agentId: String, secretKey: String, buildInfo: ThirdPartyBuildWithStatus) {
        val agentResult = client.get(ServiceThirdPartyAgentResource::class).getAgentById(projectId, agentId)
        if (agentResult.isNotOk()) {
            logger.warn("Fail to get the third party agent($agentId) because of ${agentResult.message}")
            throw NotFoundException("Fail to get the agent")
        }
        if (agentResult.data == null) {
            logger.warn("Get the null third party agent($agentId)")
            throw NotFoundException("Fail to get the agent")
        }

        if (agentResult.data!!.secretKey != secretKey) {
            throw NotFoundException("Fail to get the agent")
        }

        // 有些并发情况可能会导致在finish时AgentBuild状态没有被置为Done在这里改一下
        val buildRecord = thirdPartyAgentBuildDao.get(dslContext, buildInfo.buildId, buildInfo.vmSeqId)
        if (buildRecord != null && (
            buildRecord.status != PipelineTaskStatus.DONE.status ||
                buildRecord.status != PipelineTaskStatus.FAILURE.status
            )
        ) {
            thirdPartyAgentBuildDao.updateStatus(
                dslContext = dslContext,
                id = buildRecord.id,
                status = if (!buildInfo.success) {
                    PipelineTaskStatus.FAILURE
                } else {
                    PipelineTaskStatus.DONE
                }
            )
        }

        client.get(ServiceBuildResource::class).workerBuildFinish(
            projectId = buildInfo.projectId,
            pipelineId = if (buildInfo.pipelineId.isNullOrBlank()) "dummyPipelineId" else buildInfo.pipelineId!!,
            buildId = buildInfo.buildId,
            vmSeqId = buildInfo.vmSeqId,
            nodeHashId = agentResult.data!!.nodeId,
            simpleResult = SimpleResult(
                success = buildInfo.success,
                message = buildInfo.message,
                error = buildInfo.error
            )
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ThirdPartyAgentService::class.java)

        private const val QUEUE_RETRY_COUNT = 3

        private const val THIRD_DOCKER_TASK_INTERVAL: Long = 2000 // 轮询间隔时间，单位为毫秒
    }
}
