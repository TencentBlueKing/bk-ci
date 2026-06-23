package com.tencent.devops.environment.service

import com.tencent.devops.common.api.enums.AgentStatus
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.util.AESUtil
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.environment.config.async.AsyncExecute
import com.tencent.devops.environment.constant.EnvironmentMessageCode
import com.tencent.devops.environment.dao.AgentDao
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.dao.thirdpartyagent.ThirdPartyAgentDao
import com.tencent.devops.environment.model.AgentProps
import com.tencent.devops.environment.model.AgentPropsSource
import com.tencent.devops.environment.permission.EnvironmentPermissionService
import com.tencent.devops.environment.pojo.AsyncInstallImateData
import com.tencent.devops.environment.pojo.NodeAgentDetail
import com.tencent.devops.environment.pojo.enums.AgentType
import com.tencent.devops.environment.pojo.enums.NodeStatus
import com.tencent.devops.environment.pojo.imate.ImateListItem
import com.tencent.devops.environment.pojo.imate.ImateOriginEngine
import com.tencent.devops.environment.pojo.imate.ImportImageNodeData
import com.tencent.devops.environment.service.thirdpartyagent.BatchInstallAgentService
import com.tencent.devops.environment.service.thirdpartyagent.ImportService
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.support.api.service.ServiceIMateResource
import jakarta.ws.rs.core.Response
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.Base64
import java.util.concurrent.TimeUnit

@Service
class TencentNodeService @Autowired constructor(
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val client: Client,
    private val thirdPartyAgentDao: ThirdPartyAgentDao,
    private val agentDao: AgentDao,
    private val nodeService: NodeService,
    private val environmentPermissionService: EnvironmentPermissionService,
    private val batchInstallAgentService: BatchInstallAgentService,
    private val nodeDao: NodeDao,
    private val importService: ImportService,
    private val streamBridge: StreamBridge
) {
    @Value("\${environment.batch-install.aes-key}")
    private val batchInstallAesKey = ""

    fun updateCreateNodeDisplay(
        userId: String,
        projectId: String,
        workspaceName: String,
        displayName: String
    ): Boolean {
        val record =
            agentDao.getAgentByWorkspaceIdGlobal(dslContext, workspaceName, projectId) ?: return false
        nodeService.updateDisplayName(userId, projectId, HashUtil.encodeLongId(record.nodeId), displayName)
        return true
    }

    fun getUserImateList(
        userId: String,
        projectId: String
    ): List<ImateListItem> {
        checkProjectScope(userId, projectId)
        // 只能导入自己创建的团队的
        val imateList =
            client.get(ServiceIMateResource::class).queryUserRobots(userId).data?.filter { it.username == userId }
                ?.filter { ImateOriginEngine.teamType(it.clientType) }
                ?: return emptyList()
        val installedAgents =
            agentDao.fetchAgentsByWorkspaceIdGlobal(dslContext, imateList.map { it.clientUuid }.toList(), null)
                // 已经导入的或者其他项目有的也不能导入
                .filter { (it.status == AgentStatus.IMPORT_OK.status || projectId != it.projectId) }
                .associateBy { it.createWorkspaceName }
        return imateList.map {
            ImateListItem(
                name = it.botName,
                deviceId = it.clientUuid,
                ip = it.envId ?: it.deviceName,
                // 暂时只有linux，未来有了再加，他们的接口没字段
                os = OS.LINUX,
                engine = ImateOriginEngine.toEngine(it.clientType),
                status = it.status,
                createUser = it.username,
                createTime = it.createdAt,
                installedProjectId = installedAgents[it.clientUuid]?.projectId
            )
        }
    }

    fun batchImportImateNodes(
        userId: String,
        projectId: String,
        data: ImportImageNodeData
    ): Boolean {
        checkProjectScope(userId, projectId)
        data.agentList.forEach { imate ->
            // 校验是否有权限
            if (!environmentPermissionService.checkNodePermission(userId, projectId, AuthPermission.CREATE)) {
                throw PermissionForbiddenException(
                    message = I18nUtil.getCodeLanMessage(
                        EnvironmentMessageCode.ERROR_NODE_NO_CREATE_PERMISSSION,
                        language = I18nUtil.getLanguage(userId)
                    )
                )
            }
            if (!(client.get(ServiceIMateResource::class).queryUserRobots(userId).data?.filter { it.username == userId }
                    ?.any { it.clientUuid == imate.deviceId } ?: false)) {
                throw PermissionForbiddenException(
                    message = I18nUtil.getCodeLanMessage(
                        EnvironmentMessageCode.ERROR_NODE_NO_CREATE_PERMISSSION,
                        language = I18nUtil.getLanguage(userId)
                    )
                )
            }
            // 生成节点,如果有说明没导入成功，再次导入
            val record = agentDao.getAgentByWorkspaceIdGlobal(dslContext, imate.deviceId, null)
            var agentId = record?.id
            if (record == null) {
                agentId = batchInstallAgentService.genNewAgent(
                    projectId = projectId,
                    userId = userId,
                    os = data.os,
                    zoneName = data.zoneName,
                    agentType = AgentType.CREATE,
                    createWorkspaceName = imate.deviceId,
                    agentProps = AgentProps.emptyBySource(AgentPropsSource.DEVCLOUD)
                )
                importService.preImport(projectId, agentId, userId, imate.name)
            } else {
                // 防止在不同项目导入
                if (projectId != record.projectId) {
                    throw CustomException(
                        status = Response.Status.BAD_REQUEST,
                        message = "imported in ${record.projectId}"
                    )
                }
                importService.preImport(projectId, record.id, userId, imate.name)
            }
            // 生成临时1小时TOKEN用来导入鉴权
            val tokenData = "${projectId};${imate.deviceId};$userId;${Instant.now().plusSeconds(3600).toEpochMilli()}"
            val token = Base64.getUrlEncoder()
                .encodeToString(AESUtil.encrypt(batchInstallAesKey, tokenData.toByteArray(charset("UTF-8"))))
            val taskId = client.get(ServiceIMateResource::class).installLandunPlugin(
                username = userId,
                clientUuid = imate.deviceId,
                token = token
            )
            // 生成异步任务监听超时，默认10分钟
            if (agentId != null) {
                AsyncExecute.dispatch(
                    streamBridge = streamBridge,
                    data = AsyncInstallImateData(
                        projectId = projectId,
                        agentId = agentId
                    ),
                    delayMills = (TimeUnit.MINUTES.toMillis(
                        redisOperation.get(INSTALL_IMATE_TIMEOUT_KEY, false)?.toLongOrNull() ?: 10
                    )).toInt(),
                )
            }
            logger.info("batchImportImateNodes install plugin ${projectId}|${imate.deviceId}|$userId|$taskId")
        }
        return true
    }

    fun importImateCallBack(data: AsyncInstallImateData) {
        try {
            val record = thirdPartyAgentDao.getAgentByProject(dslContext, data.agentId, data.projectId) ?: return
            if (record.status != AgentStatus.UN_IMPORT.status) {
                return
            }
            // 为导入成功则设为失败，只改node，方便用户看到，agent不改，方便可能的重新导入
            nodeDao.updateNodeStatus(dslContext, setOf(record.nodeId), NodeStatus.ABNORMAL)
        } catch (e: Throwable) {
            logger.error("importImateCallBack error", e)
        }
    }

    fun getNodeAgentDetail(userId: String, projectId: String, agentHashId: String): NodeAgentDetail? {
        val agent = thirdPartyAgentDao.getAgentByProject(dslContext, HashUtil.decodeIdToLong(agentHashId), projectId)
            ?: return null
        val node = nodeDao.get(dslContext, projectId, agent.nodeId) ?: return null
        return NodeAgentDetail(
            displayName = node.displayName,
            ip = agent.ip,
            workspaceName = agent.createWorkspaceName
        )
    }

    // 因为现在没有团队imate同步到我们的方式，所以每天轮询
    @Scheduled(cron = "0 20 1 * * ?")
    fun checkImateProps() {
        val redisLock = RedisLock(redisOperation, CHECK_IMATE_PROPS_KEY, 3600L)
        try {
            if (!redisLock.tryLock()) {
                return
            }
            doCheckImateProps()
        } catch (ex: Throwable) {
            logger.error("checkImateProps error", ex)
        } finally {
            redisLock.unlock()
        }
    }

    fun doCheckImateProps() {
        val recordsMap = mutableMapOf<String, MutableMap<String, CheckImateAgentData>>()
        agentDao.fetchImateAgents(dslContext).forEach {
            recordsMap.putIfAbsent(
                it.createdUser,
                mutableMapOf(
                    it.createWorkspaceName to CheckImateAgentData(
                        projectId = it.projectId,
                        agentId = it.id,
                        nodeId = it.nodeId,
                        status = it.status
                    )
                )
            )?.set(
                it.createWorkspaceName, CheckImateAgentData(
                    projectId = it.projectId,
                    agentId = it.id,
                    nodeId = it.nodeId,
                    status = it.status
                )
            )
        }

        val needUpdateDeleteStatusAgents = mutableMapOf<String, MutableSet<Long>>()
        val needDeleteAgents = mutableMapOf<String, MutableSet<Long>>()
        val needCheckRenameAgents = mutableMapOf<String, MutableMap<Long, String>>()
        recordsMap.forEach { (userId, deviceMap) ->
            val imateMap =
                client.get(ServiceIMateResource::class).queryUserRobots(userId).data?.filter { it.username == userId }
                    ?.associate { it.clientUuid to it.botName }
                    ?: return@forEach
            deviceMap.forEach deviceMap@{ (deviceId, agentData) ->
                val (projectId, agentId, nodeId, status) = agentData
                // 找到的标记下筛查要不要改名
                val botName = imateMap[deviceId]
                if (botName != null) {
                    needCheckRenameAgents.putIfAbsent(projectId, mutableMapOf(nodeId to botName))?.set(nodeId, botName)
                    return@deviceMap
                }
                // 如果没找到这个用户的 imate 说明可能被删除了，先标记下删除，如果第二次就删掉
                if (status == AgentStatus.IMPORT_EXCEPTION.status) {
                    needUpdateDeleteStatusAgents.putIfAbsent(projectId, mutableSetOf(agentId))?.add(agentId)
                }
                if (status == AgentStatus.DELETE.status) {
                    needDeleteAgents.putIfAbsent(projectId, mutableSetOf(agentId))?.add(agentId)
                }
                // 正常来说不该有这种情况，需要提醒
                if (status == AgentStatus.IMPORT_OK.status) {
                    logger.error("doCheckImateProps $userId|$deviceId not find imate but agent running")
                }
            }
        }
        // 检查要不要改名
        needCheckRenameAgents.forEach { (projectId, nodeIdAndBotName) ->
            nodeDao.listByIds(dslContext, projectId, nodeIdAndBotName.keys).forEach { node ->
                val botName = nodeIdAndBotName[node.nodeId]
                if (node.displayName == botName) {
                    nodeIdAndBotName.remove(node.nodeId)
                }
            }
        }
        needUpdateDeleteStatusAgents.forEach { (projectId, agentList) ->
            thirdPartyAgentDao.batchUpdateStatus(dslContext, projectId, agentList.toSet(), AgentStatus.DELETE)
            logger.info("doCheckImateProps update delete status $projectId|$agentList")
        }
        needDeleteAgents.forEach { (projectId, agentList) ->
            agentDao.batchDeleteAgents(dslContext, projectId, agentList)
            logger.info("doCheckImateProps delete  $projectId|$agentList")
        }
        needCheckRenameAgents.forEach { (projectId, nodeIdAndBotName) ->
            agentDao.batchUpdateNodeDisplayName(dslContext, projectId, nodeIdAndBotName)
            logger.info("doCheckImateProps update displayName $projectId|$nodeIdAndBotName")
        }
    }


    // 校验不是个人项目
    private fun checkProjectScope(userId: String, projectId: String) {
        if (client.get(ServiceProjectResource::class).get(projectId).data?.projectScope != 0) {
            throw PermissionForbiddenException(
                message = I18nUtil.getCodeLanMessage(
                    EnvironmentMessageCode.ERROR_ENV_NO_VIEW_PERMISSSION,
                    language = I18nUtil.getLanguage(userId)
                )
            )
        }
    }


    companion object {
        private const val INSTALL_IMATE_TIMEOUT_KEY = "environment:install_imate_timeout_key"
        private const val CHECK_IMATE_PROPS_KEY = "environment:check_imate_props_key"
        private val logger = LoggerFactory.getLogger(TencentNodeService::class.java)
    }
}

data class CheckImateAgentData(
    val projectId: String,
    val agentId: Long,
    val nodeId: Long,
    val status: Int
)