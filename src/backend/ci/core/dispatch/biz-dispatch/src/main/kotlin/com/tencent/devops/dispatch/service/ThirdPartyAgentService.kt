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
import com.tencent.devops.auth.api.service.ServiceResourceMemberResource
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
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.common.event.dispatcher.SampleEventDispatcher
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.event.enums.PipelineBuildStatusBroadCastEventType
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildStatusBroadCastEvent
import com.tencent.devops.common.notify.enums.NotifyType
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentDockerInfoDispatch
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.dispatch.dao.ThirdPartyAgentBuildDao
import com.tencent.devops.dispatch.pojo.ThirdPartyAgentDispatchData
import com.tencent.devops.dispatch.pojo.enums.PipelineTaskStatus
import com.tencent.devops.dispatch.pojo.thirdpartyagent.AgentBuildInfo
import com.tencent.devops.dispatch.pojo.thirdpartyagent.BuildJobType
import com.tencent.devops.dispatch.pojo.thirdpartyagent.ThirdPartyAskInfo
import com.tencent.devops.dispatch.pojo.thirdpartyagent.ThirdPartyAskResp
import com.tencent.devops.dispatch.pojo.thirdpartyagent.ThirdPartyBuildDockerInfo
import com.tencent.devops.dispatch.pojo.thirdpartyagent.ThirdPartyBuildInfo
import com.tencent.devops.dispatch.pojo.thirdpartyagent.ThirdPartyBuildWithStatus
import com.tencent.devops.dispatch.utils.TPACommonUtil
import com.tencent.devops.dispatch.utils.ThirdPartyAgentLock
import com.tencent.devops.dispatch.utils.ThirdPartyAgentUtils
import com.tencent.devops.dispatch.utils.redis.ThirdPartyAgentBuildRedisUtils
import com.tencent.devops.environment.api.thirdpartyagent.ServiceThirdPartyAgentResource
import com.tencent.devops.environment.pojo.thirdpartyagent.ThirdPartyAgent
import com.tencent.devops.environment.pojo.thirdpartyagent.ThirdPartyAgentUpgradeByVersionInfo
import com.tencent.devops.model.dispatch.tables.records.TDispatchThirdpartyAgentBuildRecord
import com.tencent.devops.notify.api.service.ServiceNotifyMessageTemplateResource
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.process.api.service.ServiceBuildResource
import java.time.LocalDateTime
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import jakarta.ws.rs.NotFoundException
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DeadlockLoserDataAccessException
import org.springframework.stereotype.Service

@Service
@Suppress("ALL")
class ThirdPartyAgentService @Autowired constructor(
    private val dslContext: DSLContext,
    private val thirdPartyAgentBuildRedisUtils: ThirdPartyAgentBuildRedisUtils,
    private val client: Client,
    private val redisOperation: RedisOperation,
    private val thirdPartyAgentBuildDao: ThirdPartyAgentBuildDao,
    private val thirdPartyAgentDockerService: ThirdPartyAgentDockerService,
    private val tokenService: ClientTokenService,
    private val commonUtil: TPACommonUtil,
    private val pipelineEventDispatcher: SampleEventDispatcher
) {
    @Value("\${thirdagent.workerErrorTemplate:#{null}}")
    val workerErrorRtxTemplate: String? = null

    fun queueBuild(
        agent: ThirdPartyAgent,
        dispatchData: ThirdPartyAgentDispatchData,
        retryCount: Int = 0,
        envId: Long?
    ) {
        with(dispatchData) {
            try {
                thirdPartyAgentBuildDao.add(
                    dslContext = dslContext,
                    projectId = projectId,
                    agentId = agent.agentId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    vmSeqId = vmSeqId,
                    thirdPartyAgentWorkspace = dispatchType.workspace ?: "",
                    pipelineName = pipelineName,
                    buildNum = buildNo,
                    taskName = taskName,
                    agentIp = agent.ip,
                    nodeId = HashUtil.decodeIdToLong(agent.nodeId ?: ""),
                    dockerInfo = dispatchType.dockerInfo?.let {
                        ThirdPartyAgentDockerInfoDispatch(
                            agentId = id,
                            secretKey = secretKey,
                            info = it
                        )
                    },
                    executeCount = executeCount,
                    containerHashId = containerHashId,
                    envId = envId,
                    ignoreEnvAgentIds = ignoreEnvAgentIds,
                    jobId = jobId
                )
            } catch (e: DeadlockLoserDataAccessException) {
                logger.warn("Fail to add the third party agent build of ($buildId|$vmSeqId|${agent.agentId}")
                if (retryCount <= QUEUE_RETRY_COUNT) {
                    queueBuild(
                        agent = agent,
                        dispatchData = dispatchData,
                        retryCount = retryCount + 1,
                        envId = envId
                    )
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
        return thirdPartyAgentBuildDao.getRunningAndQueueBuilds(dslContext, agentId, false).size
    }

    fun getDockerRunningBuilds(agentId: String): Int {
        return thirdPartyAgentBuildDao.getRunningAndQueueBuilds(dslContext, agentId, true).size
    }

    fun checkRunningAndSize(agentId: String, buildId: String, docker: Boolean): Pair<Boolean, Int> {
        val records = thirdPartyAgentBuildDao.getRunningAndQueueBuilds(dslContext, agentId, docker)
        val hasRun = records.any { it.first == buildId && it.second == PipelineTaskStatus.RUNNING.status }
        return Pair(hasRun, records.size)
    }

    fun startBuild(
        projectId: String,
        agentId: String,
        secretKey: String,
        buildType: BuildJobType
    ): AgentResult<ThirdPartyBuildInfo?> {
        // Get the queue status build by buildId and agentId
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

            val redisLock = ThirdPartyAgentLock(redisOperation, projectId, agentId)
            try {
                redisLock.lock()
                val build = thirdPartyAgentBuildDao.fetchOneQueueBuild(dslContext, agentId, buildType) ?: run {
                    logger.debug("There is not build by agent($agentId) in queue")
                    return AgentResult(AgentStatus.IMPORT_OK, null)
                }

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
                pipelineEventDispatcher.dispatch(
                    // 第三方构建机启动
                    PipelineBuildStatusBroadCastEvent(
                        source = "third-party-agent-start-$agentId", projectId = build.projectId,
                        pipelineId = build.pipelineId, userId = "",
                        buildId = build.buildId, taskId = null, actionType = ActionType.START,
                        containerHashId = build.containerHashId, jobId = build.jobId, stageId = null,
                        stepId = null, atomCode = null, executeCount = build.executeCount,
                        buildStatus = BuildStatus.RUNNING.name,
                        type = PipelineBuildStatusBroadCastEventType.BUILD_AGENT_START,
                        labels = mapOf(
                            "agentId" to build.agentId,
                            "envHashId" to (build.envId?.let { HashUtil.encodeLongId(it) } ?: ""),
                            "nodeHashId" to (build.nodeId?.let { HashUtil.encodeLongId(it) } ?: ""),
                            "agentIp" to build.agentIp
                        )
                    )
                )

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

    fun finishBuild(buildId: String, vmSeqId: String?, buildResult: Boolean, executeCount: Int?) {
        val now = LocalDateTime.now().timestampmilli()
        if (vmSeqId.isNullOrBlank()) {
            val records = thirdPartyAgentBuildDao.list(dslContext, buildId, executeCount)
            if (records.isEmpty()) {
                return
            }
            records.forEach { record ->
                // 取消时兜底结束时间
                commonUtil.updateQueueTime(
                    projectId = record.projectId,
                    pipelineId = record.pipelineId,
                    buildId = record.buildId,
                    vmSeqId = record.vmSeqId,
                    executeCount = record.executeCount,
                    createTime = null,
                    endTime = now
                )
                finishBuild(record, buildResult)
            }
        } else {
            val record = thirdPartyAgentBuildDao.getWithExecuteCount(
                dslContext = dslContext,
                buildId = buildId,
                vmSeqId = vmSeqId,
                executeCount = executeCount
            ) ?: return
            // 取消时兜底结束时间
            commonUtil.updateQueueTime(
                projectId = record.projectId,
                pipelineId = record.pipelineId,
                buildId = record.buildId,
                vmSeqId = record.vmSeqId,
                executeCount = record.executeCount,
                createTime = null,
                endTime = now
            )
            finishBuild(record, buildResult)
        }
    }

    fun listAgentBuilds(
        agentId: String,
        status: String?,
        pipelineId: String?,
        page: Int?,
        pageSize: Int?
    ): Page<AgentBuildInfo> {
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: PageUtil.MAX_PAGE_SIZE
        val sqlLimit = PageUtil.convertPageSizeToSQLMAXLimit(pageNotNull, pageSizeNotNull)
        val offset = sqlLimit.offset
        val limit = sqlLimit.limit

        val agentBuildCount = thirdPartyAgentBuildDao.countAgentBuilds(
            dslContext = dslContext,
            agentId = agentId,
            status = status,
            pipelineId = pipelineId
        )
        val agentBuilds = thirdPartyAgentBuildDao.listAgentBuilds(
            dslContext = dslContext,
            agentId = agentId,
            status = status,
            pipelineId = pipelineId,
            offset = offset,
            limit = limit
        ).map {
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

    fun listLatestBuildPipelines(agentIds: List<String>): List<AgentBuildInfo> {
        return thirdPartyAgentBuildDao.listLatestBuildPipelines(
            dslContext = dslContext,
            agentIds = agentIds
        )
    }

    private fun finishBuild(record: TDispatchThirdpartyAgentBuildRecord, success: Boolean) {
        logger.info(
            "Finish the third party agent(${record.agentId}) build(${record.buildId}) " +
                    "of seq(${record.vmSeqId}) and status(${record.status})"
        )
        val agentResult = client.get(ServiceThirdPartyAgentResource::class)
            .getAgentByIdGlobal(record.projectId, record.agentId)
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

        // #9910 环境构建时遇到启动错误时调度到一个新的Agent
        val ignoreAgentIds = if (buildRecord?.envId != null &&
            !buildInfo.success &&
            buildInfo.error != null &&
            buildInfo.error?.errorCode == 2128040
        ) {
            val ignoreIds = mutableSetOf(agentResult.data!!.agentId)
            if (buildRecord.ignoreEnvAgentIds != null) {
                ignoreIds.addAll(JsonUtil.to(buildRecord.ignoreEnvAgentIds.data()))
            }
            ignoreIds
        } else {
            null
        }

        val (starter, sendNotify) = client.get(ServiceBuildResource::class).workerBuildFinish(
            projectId = buildInfo.projectId,
            pipelineId = if (buildInfo.pipelineId.isNullOrBlank()) "dummyPipelineId" else buildInfo.pipelineId!!,
            buildId = buildInfo.buildId,
            vmSeqId = buildInfo.vmSeqId,
            nodeHashId = agentResult.data!!.nodeId,
            executeCount = buildInfo.executeCount,
            simpleResult = SimpleResult(
                success = buildInfo.success,
                message = buildInfo.message,
                error = buildInfo.error,
                // #9910 环境构建时遇到启动错误时调度到一个新的Agent
                ignoreAgentIds = ignoreAgentIds
            )
        ).data ?: return

        // #9910 构建机worker失败时发送通知
        if (workerErrorRtxTemplate.isNullOrBlank() ||
            buildRecord == null ||
            buildInfo.success ||
            buildInfo.error == null ||
            buildInfo.error?.errorCode != 2128040 ||
            !sendNotify
        ) {
            return
        }
        // 构建需要使用构建的项目id跳转，防止是共享agent，agent链接使用上报的项目Id即可
        val buildUrl = "${HomeHostUtil.innerServerHost()}/console/pipeline/${buildRecord.projectId}/" +
                "${buildRecord.pipelineId}/detail/${buildRecord.buildId}/executeDetail"
        val agentUrl = "${HomeHostUtil.innerServerHost()}/console/environment/$projectId/" +
                "nodeDetail/${agentResult.data!!.nodeId}"
        client.get(ServiceNotifyMessageTemplateResource::class).sendNotifyMessageByTemplate(
            SendNotifyMessageTemplateRequest(
                templateCode = workerErrorRtxTemplate!!,
                notifyType = mutableSetOf(NotifyType.WEWORK.name),
                titleParams = mapOf(
                    "agentId" to agentResult.data!!.agentId,
                    "projectCode" to buildRecord.projectId
                ),
                bodyParams = mapOf(
                    "userId" to (starter ?: ""),
                    "buildUrl" to buildUrl,
                    "agentUrl" to agentUrl,
                    "agentOwner" to agentResult.data!!.createUser
                ),
                receivers = if (!starter.isNullOrBlank()) {
                    mutableSetOf(starter, agentResult.data!!.createUser)
                } else {
                    mutableSetOf(agentResult.data!!.createUser)
                }
            )
        )
    }

    private val askExecutor = ThreadPoolExecutor(
        100,
        100,
        0L,
        TimeUnit.MILLISECONDS,
        LinkedBlockingQueue(3000)
    )

    fun ask(
        projectId: String,
        agentId: String,
        secretKey: String,
        info: ThirdPartyAskInfo
    ): AgentResult<ThirdPartyAskResp> {
        val heartBeatF = CompletableFuture.supplyAsync({
            client.get(ServiceThirdPartyAgentResource::class).newHeartbeat(
                projectId = projectId,
                agentId = agentId,
                secretKey = secretKey,
                heartbeatInfo = info.heartbeat
            )
        }, askExecutor)

        val upgradeF = if (info.askEnable.upgrade && info.upgrade != null) {
            CompletableFuture.supplyAsync({
                checkIfCanUpgradeByVersionNew(
                    projectId = projectId,
                    agentId = agentId,
                    secretKey = secretKey,
                    info = info.upgrade!!
                )
            }, askExecutor)
        } else {
            null
        }

        val buildType = BuildJobType.toEnum(info.askEnable.build)
        val buildF = if (buildType != BuildJobType.NONE) {
            CompletableFuture.supplyAsync({
                startBuild(projectId = projectId, agentId = agentId, secretKey = secretKey, buildType = buildType)
            }, askExecutor)
        } else {
            null
        }

        val pipelineF = if (info.askEnable.pipeline) {
            CompletableFuture.supplyAsync({
                client.get(ServiceThirdPartyAgentResource::class).getPipelines(
                    projectId = projectId,
                    agentId = agentId,
                    secretKey = secretKey
                )
            }, askExecutor)
        } else {
            null
        }

        val dockerDebugF = if (info.askEnable.dockerDebug) {
            CompletableFuture.supplyAsync({
                thirdPartyAgentDockerService.startDockerDebug(
                    projectId = projectId, agentId = agentId, secretKey = secretKey
                )
            }, askExecutor)
        } else {
            null
        }

        val heartR = try {
            heartBeatF.get()?.data
        } catch (e: Exception) {
            askExceptionDeal(agentId, e)
            null
        }
        val buildR = try {
            buildF?.get()?.data
        } catch (e: Exception) {
            askExceptionDeal(agentId, e)
            null
        }
        val upgradeR = try {
            upgradeF?.get()?.data
        } catch (e: Exception) {
            askExceptionDeal(agentId, e)
            null
        }
        val pipelineR = try {
            pipelineF?.get()?.data
        } catch (e: Exception) {
            askExceptionDeal(agentId, e)
            null
        }
        val dockerDebugR = try {
            dockerDebugF?.get()?.data
        } catch (e: Exception) {
            askExceptionDeal(agentId, e)
            null
        }

        if (heartR == null && buildR == null && upgradeR == null && pipelineR == null && dockerDebugR == null) {
            return AgentResult(1, "data is null")
        }

        return AgentResult(
            status = 0,
            message = null,
            agentStatus = if (heartR != null) {
                AgentStatus.fromString(heartR.agentStatus)
            } else {
                null
            },
            data = ThirdPartyAskResp(
                heartbeat = heartR,
                build = buildR,
                upgrade = upgradeR,
                pipeline = pipelineR,
                debug = dockerDebugR
            )
        )
    }

    fun askExceptionDeal(agentId: String, e: Exception) {
        return when (e) {
            is CancellationException -> {
                logger.warn("$agentId ask cancelled", e)
            }

            is ExecutionException -> {
                logger.warn("$agentId ask exec error", e.cause)
            }

            is InterruptedException -> {
                logger.warn("$agentId ask interrupted", e)
            }

            else -> {
                logger.warn("$agentId ask unknow error", e)
            }
        }
    }

    fun countProjectJobRunningAndQueueAll(
        pipelineId: String,
        envId: Long,
        jobId: String,
        projectId: String
    ): Long {
        return thirdPartyAgentBuildDao.countProjectJobRunningAndQueueAll(
            dslContext = dslContext,
            pipelineId = pipelineId,
            envId = envId,
            jobId = jobId,
            projectId = projectId
        )
    }

    fun countAgentsJobRunningAndQueueAll(
        projectId: String,
        pipelineId: String,
        envId: Long,
        jobId: String,
        agentIds: Set<String>
    ): Map<String, Int> {
        return thirdPartyAgentBuildDao.countAgentsJobRunningAndQueueAll(
            dslContext = dslContext,
            pipelineId = pipelineId,
            envId = envId,
            jobId = jobId,
            agentIds = agentIds,
            projectId = projectId
        )
    }

    fun agentRepeatedInstallAlarm(
        projectId: String,
        agentId: String,
        newIp: String
    ) {
        val agent = try {
            client.get(ServiceThirdPartyAgentResource::class).getAgentById(projectId, agentId).data ?: return
        } catch (e: RemoteServiceException) {
            logger.warn("Fail to get the agent($agentId) of project($projectId) because of ${e.message}")
            return
        }

        if (agent.ip == newIp || agent.ip.isIgnoreLocalIp() || newIp.isIgnoreLocalIp()) {
            return
        }

        val redisKey = "$AGENT_REPEATED_INSTALL_ALARM:$agentId"
        if (redisOperation.get(redisKey) == null) {
            redisOperation.set(redisKey, "", 60 * 60 * 24)
        } else {
            return
        }

        val users = mutableSetOf(agent.createUser)
        val nodeHashId = agent.nodeId ?: return
        val authUsers = kotlin.runCatching {
            client.get(ServiceResourceMemberResource::class).getResourceGroupMembers(
                token = tokenService.getSystemToken(),
                projectCode = projectId,
                resourceType = AuthResourceType.ENVIRONMENT_ENV_NODE.value,
                resourceCode = nodeHashId
            ).data
        }.onFailure {
            logger.warn("agentStartup|getResourceGroupMembers|$projectId|$nodeHashId")
        }.getOrNull()
        users.addAll(authUsers ?: emptySet())
        kotlin.runCatching {
            client.get(ServiceNotifyMessageTemplateResource::class).sendNotifyMessageByTemplate(
                SendNotifyMessageTemplateRequest(
                    templateCode = "THIRDPART_AGENT_REPEAT_INSTALL",
                    receivers = users,
                    titleParams = mapOf(
                        "projectId" to projectId,
                        "agentId" to agentId
                    ),
                    bodyParams = mapOf(
                        "oldIp" to agent.ip,
                        "newIp" to newIp,
                        "url" to "${HomeHostUtil.innerServerHost()}/console/environment/$projectId/" +
                                "nodeDetail/$nodeHashId"
                    )
                )
            )
        }.onFailure {
            logger.warn("agentStartup|sendNotifyMessageByTemplate|$projectId|$agentId")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ThirdPartyAgentService::class.java)

        private const val QUEUE_RETRY_COUNT = 3

        private const val AGENT_REPEATED_INSTALL_ALARM = "environment:thirdparty:goagent:repeatedinstall"

        private fun String.isIgnoreLocalIp() = this.trim() == "127.0.0.1" || this.trim().startsWith("192.168.")
    }
}
