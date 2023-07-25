package com.tencent.devops.dispatch.service

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.enums.AgentStatus
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.pojo.AgentResult
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentDockerInfoDispatch
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.dispatch.dao.ThirdPartyAgentBuildDao
import com.tencent.devops.dispatch.dao.ThirdPartyAgentDockerDebugDao
import com.tencent.devops.dispatch.exception.ErrorCodeEnum
import com.tencent.devops.dispatch.pojo.enums.PipelineTaskStatus
import com.tencent.devops.dispatch.pojo.thirdPartyAgent.ThirdPartyBuildDockerInfo
import com.tencent.devops.dispatch.pojo.thirdPartyAgent.ThirdPartyDockerDebugDoneInfo
import com.tencent.devops.dispatch.pojo.thirdPartyAgent.ThirdPartyDockerDebugInfo
import com.tencent.devops.dispatch.utils.ThirdPartyAgentDockerDebugDoLock
import com.tencent.devops.dispatch.utils.ThirdPartyAgentDockerDebugLock
import com.tencent.devops.dispatch.utils.ThirdPartyAgentUtils
import com.tencent.devops.environment.api.thirdPartyAgent.ServiceThirdPartyAgentResource
import com.tencent.devops.model.dispatch.tables.records.TDispatchThirdpartyAgentBuildRecord
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.ws.rs.NotFoundException

@Service
@Suppress("ComplexMethod", "NestedBlockDepth")
class ThirdPartyAgentDockerService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val thirdPartyAgentDockerDebugDao: ThirdPartyAgentDockerDebugDao,
    private val thirdPartyAgentBuildDao: ThirdPartyAgentBuildDao
) {
    fun startDockerDebug(
        projectId: String,
        agentId: String,
        secretKey: String
    ): AgentResult<ThirdPartyDockerDebugInfo?> {
        logger.debug("Start the docker debug agent($agentId) of project($projectId)")
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

            val redisLock = ThirdPartyAgentDockerDebugLock(redisOperation, projectId, agentId)
            try {
                redisLock.lock()
                val debug = thirdPartyAgentDockerDebugDao.fetchOneQueueBuild(dslContext, agentId) ?: run {
                    logger.debug("There is not docker debug by agent($agentId) in queue")
                    return AgentResult(AgentStatus.IMPORT_OK, null)
                }

                logger.info("Start the docker debug id(${debug.id}) of agent($agentId)")
                thirdPartyAgentDockerDebugDao.updateStatusById(
                    dslContext = dslContext,
                    id = debug.id,
                    status = PipelineTaskStatus.RUNNING,
                    errMsg = null
                )

                // 第三方构建机docker启动获取镜像凭据
                val dockerInfo = if (debug.dockerInfo == null) {
                    logger.warn("There is no docker info debug by agent($agentId) project($projectId)")
                    return AgentResult(AgentStatus.IMPORT_OK, null)
                } else {
                    JsonUtil.getObjectMapper().readValue(
                        debug.dockerInfo.data(),
                        object : TypeReference<ThirdPartyAgentDockerInfoDispatch>() {}
                    )
                }
                var errMsg: String? = null
                val buildDockerInfo: ThirdPartyBuildDockerInfo?

                // 只有凭据ID的参与计算
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
                        logger.error("$projectId agent docker debug get ticket ${dockerInfo.credential} error", e)
                        errMsg = e.message
                        Pair(null, null)
                    }
                    dockerInfo.credential?.user = userName
                    dockerInfo.credential?.password = password
                }
                buildDockerInfo = ThirdPartyBuildDockerInfo(dockerInfo)
                buildDockerInfo.credential?.errMsg = errMsg

                return AgentResult(
                    AgentStatus.IMPORT_OK,
                    ThirdPartyDockerDebugInfo(
                        projectId = debug.projectId,
                        buildId = debug.buildId,
                        vmSeqId = debug.vmSeqId,
                        workspace = debug.workspace,
                        pipelineId = debug.pipelineId,
                        debugUserId = debug.userId,
                        debugId = debug.id,
                        image = buildDockerInfo.image,
                        credential = buildDockerInfo.credential,
                        options = buildDockerInfo.options
                    )
                )
            } finally {
                redisLock.unlock()
            }
        } catch (ignored: Throwable) {
            logger.warn("Fail to start debug for agent($agentId)", ignored)
            throw ignored
        }
    }

    fun startDockerDebugDone(
        projectId: String,
        agentId: String,
        secretKey: String,
        debugInfo: ThirdPartyDockerDebugDoneInfo
    ) {
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

        thirdPartyAgentDockerDebugDao.updateStatus(
            dslContext = dslContext,
            id = debugInfo.debugId,
            debugUrl = debugInfo.debugUrl,
            errMsg = debugInfo.error?.errorMessage,
            status = if (!debugInfo.success) {
                PipelineTaskStatus.FAILURE
            } else {
                PipelineTaskStatus.DONE
            }
        )
    }

    fun createThirdDockerDebugUrl(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String?,
        vmSeqId: String
    ): String {
        logger.info("$userId start debug third agent docker pipeline: $pipelineId build: $buildId vmSeq:$vmSeqId")
        // 根据是否传入buildId 查找agentId
        val his: TDispatchThirdpartyAgentBuildRecord? = if (buildId.isNullOrBlank()) {
            thirdPartyAgentBuildDao.getLastDockerBuild(dslContext, projectId, pipelineId, vmSeqId)
        } else {
            thirdPartyAgentBuildDao.getDockerBuild(dslContext, buildId, vmSeqId)
        }

        if (his == null || his.dockerInfo == null) {
            throw ErrorCodeException(
                errorCode = "${ErrorCodeEnum.NO_CONTAINER_IS_READY_DEBUG.errorCode}",
                defaultMessage = "Can not found debug container.",
                params = arrayOf(pipelineId)
            )
        }

        // 先查找是否有正在运行的任务防止重复创建，如果有可以直接返回
        val debug = thirdPartyAgentDockerDebugDao.getDebug(
            dslContext = dslContext,
            buildId = his.buildId,
            vmSeqId = vmSeqId,
            userId = userId,
            last = true
        )
        if (debug != null &&
            (debug.status == PipelineTaskStatus.QUEUE.status || debug.status == PipelineTaskStatus.RUNNING.status)
        ) {
            return loopWait(debug.id)
        }

        val redisLock = ThirdPartyAgentDockerDebugDoLock(redisOperation, his.buildId, vmSeqId, userId)
        val id = try {
            redisLock.lock()
            // 锁进来再次查询防止重复入
            val record = thirdPartyAgentDockerDebugDao.getDebug(
                dslContext = dslContext,
                buildId = his.buildId,
                vmSeqId = vmSeqId,
                userId = userId,
                last = true
            )
            if (record != null &&
                (record.status == PipelineTaskStatus.QUEUE.status || record.status == PipelineTaskStatus.RUNNING.status)
            ) {
                record.id
            } else {
                // 为空则重新下发登录调试任务
                thirdPartyAgentDockerDebugDao.add(
                    dslContext = dslContext,
                    projectId = projectId,
                    agentId = his.agentId,
                    pipelineId = pipelineId,
                    buildId = his.buildId,
                    vmSeqId = vmSeqId,
                    userId = userId,
                    thirdPartyAgentWorkspace = his.workspace,
                    dockerInfo = his.dockerInfo
                )
            }
        } finally {
            redisLock.unlock()
        }

        // 轮训等待调试任务完成，倒计时 5 分钟如果任务还没结束就返回失败，防止阻塞后台线程
        return loopWait(id)
    }

    private fun loopWait(id: Long): String {
        // 轮训等待调试任务完成，倒计时 5 分钟如果任务还没结束就返回失败，防止阻塞后台线程
        var shutdownFlag = 150
        while (true) {
            shutdownFlag--
            try {
                Thread.sleep(THIRD_DOCKER_TASK_INTERVAL)
            } catch (e: InterruptedException) {
                logger.error("third agent docker wait error", e)
            }
            if (shutdownFlag <= 0) {
                logger.error("get debug container url timeout 5m")
                // 超时时将任务标记失败
                thirdPartyAgentDockerDebugDao.updateStatusById(
                    dslContext = dslContext,
                    id = id,
                    status = PipelineTaskStatus.FAILURE,
                    errMsg = "get debug container url timeout 5m"
                )
                throw ErrorCodeException(
                    errorCode = "${ErrorCodeEnum.DEBUG_CONTAINER_URL_ERROR.errorCode}",
                    defaultMessage = "Get debug container url error.",
                    params = arrayOf("timeout 5m")
                )
            }

            val record = thirdPartyAgentDockerDebugDao.getDoneDebugById(dslContext, id) ?: continue
            if (record.status == PipelineTaskStatus.FAILURE.status) {
                logger.error("get debug container url error id $id error ${record.errMsg}")
                throw ErrorCodeException(
                    errorCode = "${ErrorCodeEnum.DEBUG_CONTAINER_URL_ERROR.errorCode}",
                    defaultMessage = "Get debug container url error.",
                    params = arrayOf(record.errMsg)
                )
            }
            return record.debugUrl
        }
    }

    fun fetchDebugStatus(
        debugId: Long
    ): String? {
        val statusInt = thirdPartyAgentDockerDebugDao.getDebugById(
            dslContext = dslContext,
            id = debugId
        )?.status ?: return null
        return PipelineTaskStatus.toStatus(statusInt).name
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ThirdPartyAgentDockerService::class.java)

        // 轮询间隔时间，单位为毫秒
        private const val THIRD_DOCKER_TASK_INTERVAL: Long = 2000
    }
}
