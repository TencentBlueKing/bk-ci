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

package com.tencent.devops.environment.service.thirdpartyagent

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.bk.audit.annotations.ActionAuditRecord
import com.tencent.bk.audit.annotations.AuditAttribute
import com.tencent.bk.audit.annotations.AuditInstanceRecord
import com.tencent.bk.audit.context.ActionAuditContext
import com.tencent.devops.common.api.constant.coerceAtMaxLength
import com.tencent.devops.common.api.enums.AgentAction
import com.tencent.devops.common.api.enums.AgentStatus
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.pojo.AgentResult
import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.agent.NewHeartbeatInfo
import com.tencent.devops.common.api.util.ApiUtil
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.SecurityUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.audit.ActionAuditContent
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.ResourceTypeId
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.common.service.utils.ByteUtils
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.common.websocket.dispatch.WebSocketDispatcher
import com.tencent.devops.dispatch.api.ServiceAgentResource
import com.tencent.devops.environment.constant.EnvironmentMessageCode
import com.tencent.devops.environment.constant.EnvironmentMessageCode.ERROR_NODE_NO_EDIT_PERMISSSION
import com.tencent.devops.environment.constant.EnvironmentMessageCode.ERROR_NODE_NO_VIEW_PERMISSSION
import com.tencent.devops.environment.constant.EnvironmentMessageCode.ERROR_NO_PERMISSION_TO_USE_THIRD_PARTY_BUILD_ENV
import com.tencent.devops.environment.constant.EnvironmentMessageCode.ERROR_THIRD_PARTY_BUILD_ENV_NODE_NOT_EXIST
import com.tencent.devops.environment.constant.EnvironmentMessageCode.THIRD_PARTY_BUILD_ENVIRONMENT_NOT_EXIST
import com.tencent.devops.environment.dao.AgentShareProjectDao
import com.tencent.devops.environment.dao.EnvDao
import com.tencent.devops.environment.dao.EnvNodeDao
import com.tencent.devops.environment.dao.EnvShareProjectDao
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.dao.thirdpartyagent.AgentPipelineRefDao
import com.tencent.devops.environment.dao.thirdpartyagent.ThirdPartyAgentActionDao
import com.tencent.devops.environment.dao.thirdpartyagent.ThirdPartyAgentDao
import com.tencent.devops.environment.dao.thirdpartyagent.ThirdPartyAgentEnableProjectsDao
import com.tencent.devops.environment.exception.AgentPermissionUnAuthorizedException
import com.tencent.devops.environment.model.AgentHostInfo
import com.tencent.devops.environment.model.AgentProps
import com.tencent.devops.environment.permission.EnvironmentPermissionService
import com.tencent.devops.environment.pojo.EnvVar
import com.tencent.devops.environment.pojo.enums.NodeStatus
import com.tencent.devops.environment.pojo.enums.NodeType
import com.tencent.devops.environment.pojo.enums.SharedEnvType
import com.tencent.devops.environment.pojo.thirdpartyagent.AgentBuildDetail
import com.tencent.devops.environment.pojo.thirdpartyagent.AgentTask
import com.tencent.devops.environment.pojo.thirdpartyagent.EnvNodeAgent
import com.tencent.devops.environment.pojo.thirdpartyagent.HeartbeatResponse
import com.tencent.devops.environment.pojo.thirdpartyagent.ThirdPartyAgent
import com.tencent.devops.environment.pojo.thirdpartyagent.ThirdPartyAgentAction
import com.tencent.devops.environment.pojo.thirdpartyagent.ThirdPartyAgentDetail
import com.tencent.devops.environment.pojo.thirdpartyagent.ThirdPartyAgentInfo
import com.tencent.devops.environment.pojo.thirdpartyagent.ThirdPartyAgentLink
import com.tencent.devops.environment.pojo.thirdpartyagent.ThirdPartyAgentStartInfo
import com.tencent.devops.environment.pojo.thirdpartyagent.ThirdPartyAgentStatusWithInfo
import com.tencent.devops.environment.pojo.thirdpartyagent.UpdateAgentRequest
import com.tencent.devops.environment.service.AgentUrlService
import com.tencent.devops.environment.service.NodeWebsocketService
import com.tencent.devops.environment.service.slave.SlaveGatewayService
import com.tencent.devops.environment.service.thirdpartyagent.upgrade.AgentPropsScope
import com.tencent.devops.environment.utils.FileMD5CacheUtils.getAgentJarFile
import com.tencent.devops.environment.utils.FileMD5CacheUtils.getFileMD5
import com.tencent.devops.environment.utils.NodeStringIdUtils
import com.tencent.devops.environment.utils.ThirdPartyAgentHeartbeatUtils
import com.tencent.devops.model.environment.tables.records.TEnvironmentThirdpartyAgentRecord
import com.tencent.devops.repository.api.ServiceOauthResource
import com.tencent.devops.repository.api.scm.ServiceGitResource
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.core.Response
import java.time.LocalDateTime
import java.util.Date
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
@Suppress("ALL")
class ThirdPartyAgentMgrService @Autowired(required = false) constructor(
    private val dslContext: DSLContext,
    private val thirdPartyAgentDao: ThirdPartyAgentDao,
    private val thirdPartyAgentEnableProjectsDao: ThirdPartyAgentEnableProjectsDao,
    private val nodeDao: NodeDao,
    private val envNodeDao: EnvNodeDao,
    private val envDao: EnvDao,
    private val agentPipelineRefDao: AgentPipelineRefDao,
    private val slaveGatewayService: SlaveGatewayService,
    private val thirdPartyAgentHeartbeatUtils: ThirdPartyAgentHeartbeatUtils,
    private val client: Client,
    private val objectMapper: ObjectMapper,
    private val agentUrlService: AgentUrlService,
    private val environmentPermissionService: EnvironmentPermissionService,
    private val agentPropsScope: AgentPropsScope,
    private val webSocketDispatcher: WebSocketDispatcher,
    private val websocketService: NodeWebsocketService,
    private val envShareProjectDao: EnvShareProjectDao,
    private val commonConfig: CommonConfig,
    private val agentMetricService: AgentMetricService,
    private val agentShareProjectDao: AgentShareProjectDao,
    private val thirdPartyAgentActionDao: ThirdPartyAgentActionDao,
    private val thirdPartAgentService: ThirdPartAgentService
) {

    fun getAgentDetailById(
        userId: String,
        projectId: String,
        agentHashId: String,
        checkPermission: Boolean = false
    ): ThirdPartyAgentDetail? {
        val id = HashUtil.decodeIdToLong(agentHashId)
        val agentRecord = thirdPartyAgentDao.getAgent(dslContext, id = id) ?: return null
        if (checkPermission && !environmentPermissionService.checkNodePermission(
                userId = userId,
                projectId = projectId,
                nodeId = agentRecord.nodeId,
                permission = AuthPermission.VIEW
            )
        ) {
            throw PermissionForbiddenException(
                message = I18nUtil.getCodeLanMessage(ERROR_NODE_NO_VIEW_PERMISSSION)
            )
        }
        return getThirdPartyAgentDetail(agentRecord, userId, true)
    }

    @ActionAuditRecord(
        actionId = ActionId.ENV_NODE_VIEW,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.ENV_NODE
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.ENV_NODE_VIEW_CONTENT
    )
    fun getAgentDetail(userId: String, projectId: String, nodeHashId: String): ThirdPartyAgentDetail? {
        val nodeId = HashUtil.decodeIdToLong(nodeHashId)
        if (!environmentPermissionService.checkNodePermission(userId, projectId, nodeId, AuthPermission.VIEW)) {
            throw PermissionForbiddenException(
                message = I18nUtil.getCodeLanMessage(ERROR_NODE_NO_VIEW_PERMISSSION)
            )
        }
        val agentRecord = thirdPartyAgentDao.getAgentByNodeId(dslContext, nodeId = nodeId, projectId = projectId)
            ?: return null
        ActionAuditContext.current()
            .setInstanceName(agentRecord.nodeId.toString())
            .setInstanceId(agentRecord.nodeId.toString())
        return getThirdPartyAgentDetail(agentRecord, userId)
    }

    private fun getThirdPartyAgentDetail(
        agentRecord: TEnvironmentThirdpartyAgentRecord,
        userId: String,
        needHeartbeatInfo: Boolean = false
    ): ThirdPartyAgentDetail? {

        val nodeRecord = nodeDao.get(dslContext, agentRecord.projectId, nodeId = agentRecord.nodeId ?: return null)
            ?: return null

        val agentHashId = HashUtil.encodeLongId(agentRecord.id)
        val nodeHashId = HashUtil.encodeLongId(agentRecord.nodeId)
        val nodeStringId = NodeStringIdUtils.getNodeStringId(nodeRecord)
        val displayName = NodeStringIdUtils.getRefineDisplayName(nodeStringId, nodeRecord.displayName)
        val heartBeatInfo = thirdPartyAgentHeartbeatUtils.getNewHeartbeat(agentRecord.projectId, agentRecord.id)
        val lastHeartbeatTime = heartBeatInfo?.heartbeatTime
        val parallelTaskCount = (agentRecord.parallelTaskCount ?: "").toString()
        val dockerParallelTaskCount = (agentRecord.dockerParallelTaskCount ?: "").toString()
        val agentHostInfo = try {
            if (needHeartbeatInfo) {
                AgentHostInfo(nCpus = "0", memTotal = "0", diskTotal = "0")
            } else {
                agentMetricService.queryHostInfo(nodeRecord.projectId, agentHashId)
            }
        } catch (e: Throwable) {
            logger.warn("[$agentHashId]|[$nodeHashId]|[${agentRecord.projectId}]|influx query error: ", e)
            AgentHostInfo(nCpus = "0", memTotal = "0", diskTotal = "0")
        }

        val props = getAgentProperties(agentRecord)

        val tpad = ThirdPartyAgentDetail(
            agentId = HashUtil.encodeLongId(agentRecord.id),
            nodeId = nodeHashId,
            displayName = displayName,
            projectId = agentRecord.projectId,
            status = nodeRecord.nodeStatus,
            hostname = agentRecord.hostname,
            os = agentRecord.os,
            osName = agentRecord.detectOs,
            ip = agentRecord.ip,
            createdUser = nodeRecord.createdUser,
            createdTime = nodeRecord.createdTime?.let { self -> DateTimeUtil.toDateTime(self) } ?: "",
            agentVersion = agentRecord.masterVersion ?: "",
            slaveVersion = agentRecord.version ?: "",
            agentInstallPath = agentRecord.agentInstallPath ?: "",
            maxParallelTaskCount = MAX_PARALLEL_TASK_COUNT,
            parallelTaskCount = parallelTaskCount,
            dockerParallelTaskCount = dockerParallelTaskCount,
            startedUser = agentRecord.startedUser ?: "",
            agentUrl = agentUrlService.genAgentUrl(agentRecord),
            agentScript = agentUrlService.genAgentInstallScript(agentRecord),
            lastHeartbeatTime = lastHeartbeatTime?.let { self -> DateTimeUtil.formatDate(Date(self)) } ?: "",
            ncpus = agentHostInfo.nCpus,
            memTotal = agentHostInfo.memTotal,
            diskTotal = agentHostInfo.diskTotal,
            currentAgentVersion = agentPropsScope.getAgentVersion(),
            currentWorkerVersion = agentPropsScope.getWorkerVersion(),
            exitErrorMsg = if (props?.exitError != null) {
                "${trans(props.exitError.errorEnum, userId)}|${props.exitError.message}"
            } else {
                null
            }
        )

        if (needHeartbeatInfo) {
            tpad.heartbeatInfo = heartBeatInfo
        } else {
            tpad.canEdit = environmentPermissionService.checkNodePermission(
                userId = userId,
                projectId = agentRecord.projectId,
                nodeId = nodeRecord.nodeId,
                permission = AuthPermission.EDIT
            )
        }

        return tpad
    }

    private fun trans(enum: String, userId: String): String {
        return MessageUtil.getMessageByLocale(enum, I18nUtil.getLanguage(userId))
    }

    private fun getAgentProperties(agentRecord: TEnvironmentThirdpartyAgentRecord): AgentProps? {
        if (agentRecord.agentProps.isNullOrBlank()) {
            return null
        }

        return try {
            JsonUtil.to(agentRecord.agentProps, AgentProps::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun saveAgentEnv(userId: String, projectId: String, nodeHashId: String, envs: List<EnvVar>) {
        val nodeId = HashUtil.decodeIdToLong(nodeHashId)
        checkEditPermmission(userId, projectId, nodeId)

        val agentRecord = thirdPartyAgentDao.getAgentByNodeId(dslContext, nodeId, projectId)
            ?: throw ErrorCodeException(
                errorCode = EnvironmentMessageCode.ERROR_NODE_NOT_EXISTS,
                params = arrayOf(nodeHashId)
            )
        thirdPartyAgentDao.saveAgentEnvs(
            dslContext = dslContext,
            agentIds = setOf(agentRecord.id),
            envStr = objectMapper.writeValueAsString(envs)
        )
    }

    fun getAgentEnv(projectId: String, nodeHashId: String): List<EnvVar> {
        val nodeId = HashUtil.decodeIdToLong(nodeHashId)
        val agentRecord = thirdPartyAgentDao.getAgentByNodeId(dslContext, nodeId, projectId)
            ?: throw ErrorCodeException(
                errorCode = EnvironmentMessageCode.ERROR_NODE_NOT_EXISTS,
                params = arrayOf(nodeHashId)
            )

        return if (agentRecord.agentEnvs.isNullOrBlank()) {
            listOf()
        } else {
            objectMapper.readValue(agentRecord.agentEnvs)
        }
    }

    private fun checkEditPermmission(userId: String, projectId: String, nodeId: Long) {
        if (!environmentPermissionService.checkNodePermission(userId, projectId, nodeId, AuthPermission.EDIT)) {
            throw PermissionForbiddenException(
                message = I18nUtil.getCodeLanMessage(
                    ERROR_NODE_NO_EDIT_PERMISSSION,
                    language = I18nUtil.getLanguage(userId)
                )
            )
        }
    }

    fun setParallelTaskCount(
        userId: String,
        projectId: String,
        nodeHashId: String,
        parallelTaskCount: Int?,
        dockerParallelTaskCount: Int?
    ) {
        val nodeId = HashUtil.decodeIdToLong(nodeHashId)
        checkEditPermmission(userId, projectId, nodeId)

        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            val agentRecord = thirdPartyAgentDao.getAgentByNodeId(context, nodeId, projectId)
                ?: throw ErrorCodeException(
                    errorCode = EnvironmentMessageCode.ERROR_NODE_NOT_EXISTS,
                    params = arrayOf(nodeHashId)
                )
            agentRecord.parallelTaskCount = parallelTaskCount ?: agentRecord.parallelTaskCount
            agentRecord.dockerParallelTaskCount = dockerParallelTaskCount ?: agentRecord.dockerParallelTaskCount
            thirdPartyAgentDao.saveAgent(context, agentRecord)
        }
    }

    fun listAgentBuilds(
        userId: String,
        projectId: String,
        nodeHashId: String,
        status: String?,
        pipelineId: String?,
        page: Int?,
        pageSize: Int?
    ): Page<AgentBuildDetail> {
        val nodeId = HashUtil.decodeIdToLong(nodeHashId)
        if (!environmentPermissionService.checkNodePermission(
                userId = userId,
                projectId = projectId,
                nodeId = nodeId,
                permission = AuthPermission.VIEW
            )
        ) {
            throw ErrorCodeException(
                errorCode = ERROR_NODE_NO_VIEW_PERMISSSION
            )
        }
        val agentRecord = thirdPartyAgentDao.getAgentByNodeId(
            dslContext = dslContext,
            nodeId = nodeId,
            projectId = projectId
        )
            ?: throw ErrorCodeException(
                errorCode = EnvironmentMessageCode.ERROR_NODE_NOT_EXISTS,
                params = arrayOf(nodeHashId)
            )
        val agentHashId = HashUtil.encodeLongId(agentRecord.id)
        val agentBuildPage = client.get(ServiceAgentResource::class).listAgentBuild(
            agentId = agentHashId,
            status = status,
            pipelineId = pipelineId,
            page = page,
            pageSize = pageSize
        )

        val heartbeatInfo = thirdPartyAgentHeartbeatUtils.getNewHeartbeat(
            projectId = projectId,
            agentId = agentRecord.id
        )
        val agentTasks = heartbeatInfo?.taskList ?: listOf()
        val taskMap = agentTasks.associate { "${it.projectId}_${it.buildId}_${it.vmSeqId}" to AgentTask("RUNNING") }

        val agentBuildDetails = agentBuildPage.records.map {
            AgentBuildDetail(
                nodeId = nodeHashId,
                agentId = agentHashId,
                projectId = it.projectId,
                pipelineId = it.pipelineId,
                pipelineName = it.pipelineName,
                buildId = it.buildId,
                buildNumber = it.buildNum,
                vmSetId = it.vmSeqId,
                taskName = it.taskName,
                status = it.status,
                createdTime = it.createdTime,
                updatedTime = it.updatedTime,
                workspace = it.workspace,
                agentTask = taskMap["${it.projectId}_${it.buildId}_${it.vmSeqId}"]
            )
        }

        return Page(
            count = agentBuildPage.count,
            page = agentBuildPage.page,
            pageSize = agentBuildPage.pageSize,
            totalPages = agentBuildPage.totalPages,
            records = agentBuildDetails
        )
    }

    fun listLatestBuildPipelines(
        userId: String,
        projectId: String,
        page: Int?,
        pageSize: Int?
    ): Page<AgentBuildDetail> {
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: 100
        val sqlLimit =
            if (pageSizeNotNull != -1) PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull) else null
        val count = thirdPartyAgentDao.countProjectAgentId(
            dslContext = dslContext,
            projectId = projectId
        )
        val agentRecord /*map<agent hash id, node hash id>*/ = thirdPartyAgentDao.listProjectAgentId(
            dslContext = dslContext,
            projectId = projectId,
            offset = sqlLimit?.offset ?: 0,
            limit = sqlLimit?.limit ?: 100
        )
        val agentBuildPage = client.get(ServiceAgentResource::class).listLatestBuildPipelines(
            agentIds = agentRecord.keys.toList()
        )
        val agentBuildDetails = agentBuildPage.map {
            AgentBuildDetail(
                nodeId = agentRecord[it.agentId] ?: "",
                agentId = it.agentId,
                projectId = it.projectId,
                pipelineId = it.pipelineId,
                pipelineName = it.pipelineName,
                buildId = it.buildId,
                buildNumber = it.buildNum,
                vmSetId = it.vmSeqId,
                taskName = it.taskName,
                status = it.status,
                createdTime = it.createdTime,
                updatedTime = it.updatedTime,
                workspace = it.workspace,
                agentTask = null
            )
        }
        return Page(page = pageNotNull, pageSize = pageSizeNotNull, count = count, records = agentBuildDetails)
    }

    fun listAgentActions(
        user: String,
        projectId: String,
        nodeHashId: String,
        page: Int?,
        pageSize: Int?
    ): Page<ThirdPartyAgentAction> {
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: 100
        val sqlLimit =
            if (pageSizeNotNull != -1) PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull) else null
        val offset = sqlLimit?.offset ?: 0
        val limit = sqlLimit?.limit ?: 100

        val nodeId = HashUtil.decodeIdToLong(nodeHashId)
        val agentRecord = thirdPartyAgentDao.getAgentByNodeId(
            dslContext = dslContext,
            nodeId = nodeId,
            projectId = projectId
        )
            ?: throw ErrorCodeException(
                errorCode = EnvironmentMessageCode.ERROR_NODE_NOT_EXISTS,
                params = arrayOf(nodeHashId)
            )
        val agentHashId = HashUtil.encodeLongId(agentRecord.id)

        val agentActionCount = thirdPartyAgentActionDao.getAgentActionsCount(
            dslContext = dslContext,
            projectId = projectId,
            agentId = agentRecord.id
        )
        val agentActions =
            thirdPartyAgentActionDao.listAgentActions(
                dslContext = dslContext,
                projectId = projectId,
                agentId = agentRecord.id,
                offset = offset,
                limit = limit
            ).map {
                ThirdPartyAgentAction(
                    agentId = agentHashId,
                    projectId = it.projectId,
                    action = it.action,
                    actionTime = it.actionTime.timestamp()
                )
            }
        return Page(page = pageNotNull, pageSize = pageSizeNotNull, count = agentActionCount, records = agentActions)
    }

    fun generateAgent(
        userId: String,
        projectId: String,
        os: OS,
        zoneName: String?
    ): ThirdPartyAgentLink {
        val gateway = slaveGatewayService.getGateway(zoneName)
        val fileGateway = slaveGatewayService.getFileGateway(zoneName)
        logger.info("Generate agent($os) info of project($projectId) with gateway $gateway by user($userId)")
        val unimportAgent = thirdPartyAgentDao.listUnimportAgent(
            dslContext = dslContext,
            projectId = projectId,
            userId = userId,
            os = os
        )
        val agentRecord: TEnvironmentThirdpartyAgentRecord = if (unimportAgent.isEmpty()) {
            val secretKey = generateSecretKey()
            val id = thirdPartyAgentDao.add(
                dslContext = dslContext,
                userId = userId,
                projectId = projectId,
                os = os,
                secretKey = SecurityUtil.encrypt(secretKey),
                gateway = gateway,
                fileGateway = fileGateway
            )
            thirdPartyAgentDao.getAgent(dslContext, id)!!
        } else {
            val agentRecord = unimportAgent[0]
            logger.debug("The agent(${agentRecord.id}) exist")
            if (!gateway.isNullOrBlank()) {
                thirdPartyAgentDao.updateGateway(
                    dslContext = dslContext,
                    agentId = agentRecord.id,
                    gateway = gateway,
                    fileGateway = fileGateway
                )
            }
            agentRecord.setGateway(gateway!!)
        }

        val agentHashId = HashUtil.encodeLongId(agentRecord.id)

        if (os == OS.WINDOWS) {
            return ThirdPartyAgentLink(
                agentId = agentHashId,
                link = agentUrlService.genAgentUrl(agentRecord)
            )
        }
        return ThirdPartyAgentLink(
            agentId = agentHashId,
            link = agentUrlService.genAgentInstallScript(agentRecord)
        )
    }

    fun getAgentLink(
        userId: String,
        projectId: String,
        nodeId: String
    ): ThirdPartyAgentLink {
        val id = HashUtil.decodeIdToLong(nodeId)
        val agentRecord = thirdPartyAgentDao.getAgentByNodeId(dslContext, id, projectId)
            ?: throw NotFoundException("The agent is not exist")
        val url = agentUrlService.genAgentInstallUrl(agentRecord)
        val agentId = HashUtil.encodeLongId(agentRecord.id)
        return ThirdPartyAgentLink(
            agentId = agentId,
            link = "curl $url | bash"
        )
    }

    fun listAgents(
        userId: String,
        projectId: String,
        os: OS?
    ): List<ThirdPartyAgentInfo> {

        val canUseNodeIds = environmentPermissionService.listNodeByPermission(
            userId = userId,
            projectId = projectId,
            permission = AuthPermission.USE
        )

        if (canUseNodeIds.isEmpty()) {
            return emptyList()
        }

        logger.debug("Get the user can use node ids {}", canUseNodeIds)

        val agents = thirdPartyAgentDao.listImportAgent(
            dslContext = dslContext,
            projectId = projectId,
            nodeIds = canUseNodeIds,
            os = os
        )
        if (agents.isEmpty()) {
            return emptyList()
        }

        val nodeIds = agents.map { it.nodeId }
        val nodes = nodeDao.listByIds(dslContext, projectId, nodeIds)
        if (nodes.isEmpty()) {
            return emptyList()
        }
        val nodeMap = nodes.associateBy { it.nodeId }

        val agentInfo = ArrayList<ThirdPartyAgentInfo>()

        agents.forEach { agent ->
            val node = nodeMap[agent.nodeId]
            if (node == null || node.nodeStatus.isNullOrBlank()) {
                logger.warn("Fail to find the node status of agent(${agent.id})")
                return@forEach
            }

            agentInfo.add(
                ThirdPartyAgentInfo(
                    agentId = HashUtil.encodeLongId(agent.id),
                    projectId = projectId,
                    status = NodeStatus.valueOf(node.nodeStatus!!).statusName,
                    hostname = agent.hostname,
                    ip = agent.ip,
                    displayName = node.displayName,
                    detailName = "${node.displayName}/${agent.ip}/${agent.hostname}/${node.osName}"
                )
            )
        }
        return agentInfo
    }

    fun getAgentByDisplayName(projectId: String, displayName: String): AgentResult<ThirdPartyAgent?> {
        val nodes = nodeDao.getByDisplayName(
            dslContext = dslContext,
            projectId = projectId,
            displayName = displayName,
            nodeType = listOf(NodeType.THIRDPARTY.name, NodeType.DEVCLOUD.name)
        )
        if (nodes.isEmpty()) {
            return AgentResult(0, null, null, null)
        }
        if (nodes.size != 1) {
            logger.warn("[$projectId|$displayName] There are more than one node with the display name - ($nodes)")
            return AgentResult(0, null, null, null)
        }
        val node = nodes[0]
        val agentRecord = thirdPartyAgentDao.getAgentByNodeId(dslContext, node.nodeId, projectId)
        if (agentRecord == null) {
            logger.warn("[$projectId|$displayName|${node.nodeId}] Fail to get the agent")
            return AgentResult(0, null, null, null)
        }
        val status = AgentStatus.fromStatus(agentRecord.status)
        return AgentResult(
            status = status,
            data = ThirdPartyAgent(
                agentId = HashUtil.encodeLongId(agentRecord.id),
                projectId = projectId,
                nodeId = HashUtil.encodeLongId(node.nodeId),
                status = status,
                hostname = agentRecord.hostname,
                os = agentRecord.os,
                ip = agentRecord.ip,
                secretKey = SecurityUtil.decrypt(agentRecord.secretKey),
                createUser = agentRecord.createdUser,
                createTime = agentRecord.createdTime.timestamp(),
                parallelTaskCount = agentRecord.parallelTaskCount,
                dockerParallelTaskCount = agentRecord.dockerParallelTaskCount,
                masterVersion = agentRecord.masterVersion
            )
        )
    }

    /**
     * 兼容老的使用了其他项目构建机的 agent
     * 如果没有共享，不被删除，但是也不被调用
     */
    fun getAgent(
        projectId: String,
        agentId: String
    ): AgentResult<ThirdPartyAgent?> {
        val id = HashUtil.decodeIdToLong(agentId)
        // 先去寻找当前项目下是否存在这个 agent
        val agentRecord = thirdPartyAgentDao.getAgentByProject(dslContext, id, projectId)
        if (agentRecord != null) {
            return getResultByRecord(projectId, agentId, agentRecord)
        }
        // 如果没有则去全局搜索这个 agent, 没有则说明被删除了
        val agentGlobalRecord = thirdPartyAgentDao.getAgent(dslContext, id)
            ?: return AgentResult(AgentStatus.DELETE, null)
        // 如果还有则搜索是否被共享给当前项目，没有被共享则不能使用
        val isShared = agentShareProjectDao.selectSharedAgentCount(dslContext, id, projectId) > 0
        if (isShared) {
            return getResultByRecord(projectId, agentId, agentGlobalRecord)
        }
        // 没有则按 agent 的状态返回，但是不返回 agent 实例
        logger.warn("not allow $projectId use agent $agentId")
        return AgentResult(
            status = 1,
            message = "not allow use this agent",
            agentStatus = AgentStatus.fromStatus(agentGlobalRecord.status),
            data = null
        )
    }

    /**
     * 老的使用全局构建机的先走这个逻辑，未来评估慢慢下掉
     *
     */
    @Deprecated("getAgent")
    fun getAgentGlobal(
        projectId: String,
        agentId: String
    ): AgentResult<ThirdPartyAgent?> {
        val id = HashUtil.decodeIdToLong(agentId)
        val agentRecord = thirdPartyAgentDao.getAgent(dslContext = dslContext, id = id)
            ?: return AgentResult(AgentStatus.DELETE, null)
        return getResultByRecord(projectId, agentId, agentRecord)
    }

    private fun getResultByRecord(
        projectId: String,
        agentId: String,
        agentRecord: TEnvironmentThirdpartyAgentRecord
    ): AgentResult<ThirdPartyAgent?> {
        val status = AgentStatus.fromStatus(agentRecord.status)
        val nodeId = if (agentRecord.nodeId != null) {
            HashUtil.encodeLongId(agentRecord.nodeId)
        } else {
            null
        }
        return AgentResult(
            status = status,
            data = ThirdPartyAgent(
                agentId = agentId,
                projectId = projectId,
                nodeId = nodeId,
                status = status,
                hostname = agentRecord.hostname,
                os = agentRecord.os,
                ip = agentRecord.ip,
                secretKey = SecurityUtil.decrypt(agentRecord.secretKey),
                createUser = agentRecord.createdUser,
                createTime = agentRecord.createdTime.timestamp(),
                parallelTaskCount = agentRecord.parallelTaskCount,
                dockerParallelTaskCount = agentRecord.dockerParallelTaskCount,
                masterVersion = agentRecord.masterVersion
            )
        )
    }

    fun getAgentByEnvName(projectId: String, envName: String): Pair<Long?, List<EnvNodeAgent>> {
        // 共享环境由 被共享的项目ID@环境名称 组成，这里通过@分隔出的数量来区分是否是共享环境
        val envNameItems = envName.split("@")
        val thirdPartyAgentList = mutableListOf<EnvNodeAgent>()

        // 因为环境名称有可能也含有@所以只有 仅包含一个@的才是绝对的共享环境
        // 共享环境也有情况是共享环境自己使用，这种情况则直接走下面的逻辑
        var realEnvName = envName
        run sharedEnv@{
            if (envNameItems.size == 2 && envNameItems[0].isNotBlank() && envNameItems[1].isNotBlank()) {
                if (projectId == envNameItems[0]) {
                    realEnvName = envNameItems[1]
                    return@sharedEnv
                }
                return getSharedThirdPartyAgentList(
                    projectId = projectId,
                    sharedProjectId = envNameItems[0],
                    sharedEnvName = envNameItems[1],
                    sharedEnvId = null
                )
            }
        }

        val envRecord = envDao.getByEnvName(dslContext = dslContext, projectId = projectId, envName = realEnvName)
        if (envRecord == null) {
            logger.warn("[$projectId|$realEnvName] The env is not exist")
            throw CustomException(
                Response.Status.FORBIDDEN,
                I18nUtil.getCodeLanMessage(THIRD_PARTY_BUILD_ENVIRONMENT_NOT_EXIST) + "($projectId:$realEnvName)"
            )
        }
        thirdPartyAgentList.addAll(
            getAgentByEnvId(projectId = projectId, envHashId = HashUtil.encodeLongId(envRecord.envId))
        )

        return Pair(envRecord.envId, thirdPartyAgentList)
    }

    private fun getSharedThirdPartyAgentList(
        projectId: String,
        sharedProjectId: String,
        sharedEnvName: String?,
        sharedEnvId: Long?
    ): Pair<Long?, List<EnvNodeAgent>> {
        logger.info("[$projectId|$sharedProjectId|$sharedEnvName|$sharedEnvId]get shared third party agent list")
        val sharedEnvRecord = when {
            !sharedEnvName.isNullOrBlank() -> {
                envShareProjectDao.list(
                    dslContext = dslContext,
                    mainProjectId = sharedProjectId,
                    envName = sharedEnvName,
                    envId = null
                ).ifEmpty {
                    val env = envDao.getByEnvName(
                        dslContext = dslContext,
                        projectId = sharedProjectId,
                        envName = sharedEnvName
                    ) ?: throw CustomException(
                        Response.Status.FORBIDDEN,
                        I18nUtil.getCodeLanMessage(THIRD_PARTY_BUILD_ENVIRONMENT_NOT_EXIST) +
                            "($sharedProjectId:$sharedEnvId)"
                    )
                    envShareProjectDao.list(
                        dslContext = dslContext,
                        mainProjectId = sharedProjectId,
                        envName = null,
                        envId = env.envId
                    )
                }
            }

            sharedEnvId != null -> {
                envShareProjectDao.list(
                    dslContext = dslContext,
                    mainProjectId = sharedProjectId,
                    envName = null,
                    envId = sharedEnvId
                )
            }

            else -> emptyList()
        }
        // 兼容如果更改了环境名称
        sharedEnvRecord.getOrNull(0)?.let {
            val env = envDao.getOrNull(
                dslContext = dslContext,
                projectId = sharedProjectId,
                envId = it.envId
            ) ?: throw CustomException(
                Response.Status.FORBIDDEN,
                I18nUtil.getCodeLanMessage(THIRD_PARTY_BUILD_ENVIRONMENT_NOT_EXIST) + "($sharedProjectId:$sharedEnvId)"
            )
            if (env.envName != it.envName) {
                envShareProjectDao.batchUpdateEnvName(dslContext, it.envId, env.envName)
            }
        }
        if (sharedEnvRecord.isEmpty()) {
            logger.info(
                "env name not exists, envName: $sharedEnvName, envId: $sharedEnvId, projectId：$projectId, " +
                    "mainProjectId: $sharedProjectId"
            )
            throw CustomException(
                Response.Status.FORBIDDEN,
                I18nUtil.getCodeLanMessage(ERROR_NO_PERMISSION_TO_USE_THIRD_PARTY_BUILD_ENV) +
                    "($sharedProjectId:${sharedEnvName ?: sharedEnvId})"
            )
        }
        logger.info("sharedEnvRecord size: ${sharedEnvRecord.size}")
        val sharedThirdPartyAgents = mutableListOf<EnvNodeAgent>()

        run outSide@{
            // 优先进行单个项目的匹配
            sharedEnvRecord.sortedByDescending { it.type }.forEach nextRecord@{
                // 对于分享的单独项目则查看是否是同一个
                if (it.type == SharedEnvType.PROJECT.name && it.sharedProjectId != projectId) {
                    return@nextRecord
                }

                // 通过项目组获取所有项目，判断当前项目是否处于被分享的项目组中
                if (it.type == SharedEnvType.GROUP.name) {
                    val projectsInGroups = try {
                        val token = client.get(ServiceOauthResource::class).gitGet(it.creator).data?.accessToken
                            ?: throw NotFoundException("cannot found oauth access token for user(${it.creator})")
                        client.get(ServiceGitResource::class).getProjectGroupInfo(
                            id = it.sharedProjectId.removePrefix("git_"),
                            includeSubgroups = true,
                            token = token,
                            tokenType = TokenTypeEnum.OAUTH
                        ).data
                    } catch (e: Exception) {
                        logger.warn("$projectId $sharedProjectId:$sharedEnvName get share project error: ${e.message}")
                        null
                    }
                    val gitProjectId = projectId.removePrefix("git_")
                    projectsInGroups?.projects?.filter { project -> project.id == gitProjectId }?.ifEmpty {
                        projectsInGroups.subProjects?.filter { subProject -> subProject.id == gitProjectId }?.ifEmpty {
                            return@nextRecord
                        }
                    }
                }

                sharedThirdPartyAgents.addAll(getAgentByEnvId(it.mainProjectId, HashUtil.encodeLongId(it.envId)))
                // 找到了环境可用就可以退出了
                return@outSide
            }
        }
        if (sharedThirdPartyAgents.isEmpty()) {
            throw CustomException(
                Response.Status.FORBIDDEN,
                I18nUtil.getCodeLanMessage(ERROR_NO_PERMISSION_TO_USE_THIRD_PARTY_BUILD_ENV) +
                    "($sharedProjectId:$sharedEnvName)"
            )
        }
        logger.info("sharedThirdPartyAgents size: ${sharedThirdPartyAgents.size}")
        return Pair(sharedEnvRecord.getOrNull(0)?.envId, sharedThirdPartyAgents)
    }

    fun getAgentByEnvId(projectId: String, envHashId: String): List<EnvNodeAgent> {
        logger.info("[$projectId|$envHashId] Get the agents by envId")
        run {
            val sharedProjEnv = envHashId.split("@") // sharedProjId@poolName
            if (sharedProjEnv.size != 2 || sharedProjEnv[0].isBlank() || sharedProjEnv[1].isBlank()) {
                return@run
            }
            val (_, aList) = getSharedThirdPartyAgentList(
                projectId = projectId,
                sharedProjectId = sharedProjEnv[0],
                sharedEnvName = null,
                sharedEnvId = HashUtil.decodeIdToLong(sharedProjEnv[1])
            )
            return aList
        }
        val envId = HashUtil.decodeIdToLong(envHashId)
        val nodes = envNodeDao.list(dslContext = dslContext, projectId = projectId, envIds = listOf(envId))
        if (nodes.isEmpty()) {
            logger.warn("[$projectId|$envHashId] The env is not exist")
            throw CustomException(
                Response.Status.FORBIDDEN,
                I18nUtil.getCodeLanMessage(ERROR_THIRD_PARTY_BUILD_ENV_NODE_NOT_EXIST) + "($projectId:$envHashId)"
            )
        }
        val nodeIdMap = nodes.associate {
            it.nodeId to it.enableNode
        }
        val nodeDisplayNameIds = nodeIdMap.filter { !it.value }.keys
        val nodeDisplayNameMap = if (nodeDisplayNameIds.isNotEmpty()) {
            nodeDao.listByIds(
                dslContext = dslContext,
                projectId = projectId,
                nodeIds = nodeDisplayNameIds
            ).associate { it.nodeId to it.displayName }
        } else {
            null
        }
        val agents = thirdPartyAgentDao.getAgentsByNodeIds(
            dslContext = dslContext,
            nodeIds = nodeIdMap.keys,
            projectId = projectId
        )
        return agents.map {
            val nodeId = if (it.nodeId != null) {
                HashUtil.encodeLongId(it.nodeId)
            } else {
                null
            }
            EnvNodeAgent(
                ThirdPartyAgent(
                    agentId = HashUtil.encodeLongId(it.id),
                    projectId = projectId,
                    nodeId = nodeId,
                    status = AgentStatus.fromStatus(it.status),
                    hostname = it.hostname,
                    os = it.os,
                    ip = it.ip,
                    secretKey = SecurityUtil.decrypt(it.secretKey),
                    createUser = it.createdUser,
                    createTime = it.createdTime.timestamp(),
                    parallelTaskCount = it.parallelTaskCount,
                    dockerParallelTaskCount = it.dockerParallelTaskCount,
                    masterVersion = it.masterVersion
                ),
                enableNode = nodeIdMap[it.nodeId] ?: true,
                nodeDisplayName = nodeDisplayNameMap?.get(it.nodeId)
            )
        }
    }

    fun checkIfCanUpgrade(
        projectId: String,
        agentId: String,
        secretKey: String,
        tag: String
    ): AgentResult<Boolean> {
        logger.info("Checking if the agent($agentId) of project($projectId) can upgrade")
        val id = HashUtil.decodeIdToLong(agentId)
        val agentRecord = thirdPartyAgentDao.getAgent(dslContext = dslContext, id = id)
            ?: return AgentResult(AgentStatus.DELETE, false)
        val status = AgentStatus.fromStatus(agentRecord.status)
        if (status != AgentStatus.IMPORT_OK) {
            return AgentResult(status, false)
        }

        val key = SecurityUtil.decrypt(agentRecord.secretKey)

        if (key != secretKey) {
            logger.warn("The agent($id) of project($projectId)'s secret($secretKey) is not match the expect one($key)")
            return AgentResult(AgentStatus.DELETE, false)
        }

        val jarFile = getAgentJarFile()
        val md5 = getFileMD5(jarFile)
        if (md5 == tag) {
            return AgentResult(status, false)
        }
        logger.info("The agent($id) can upgrade")
        return AgentResult(status, true)
    }

    @ActionAuditRecord(
        actionId = ActionId.ENV_NODE_DELETE,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.ENV_NODE
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.ENV_NODE_DELETE_CONTENT
    )
    fun deleteAgent(
        userId: String,
        projectId: String,
        nodeHashIds: Set<String>
    ) {
        logger.info("Delete the node($nodeHashIds) of project($projectId) by user($userId)")
        val nodeIds = nodeHashIds.map { HashUtil.decodeIdToLong(it) }.toSet()
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            val records = thirdPartyAgentDao.getAgentsByNodeIds(context, nodeIds, projectId)
            if (records.isEmpty()) {
                logger.warn("The node($nodeIds) is not exist")
                throw NotFoundException("The node is not exist")
            }
            ActionAuditContext.current()
                .setInstanceId(nodeIds.joinToString(","))
                .setInstanceName(nodeIds.joinToString(","))
            val count = thirdPartyAgentDao.batchUpdateStatus(
                dslContext = context,
                ids = records.map { it.id }.toSet(),
                projectId = projectId,
                status = AgentStatus.DELETE
            )
            if (count < 1) {
                logger.warn("Can't delete the agent($count)")
            }

            nodeDao.updateNodeStatus(dslContext = context, ids = nodeIds, status = NodeStatus.DELETED)
            records.forEach { record ->
                if (record.nodeId != null) {
                    environmentPermissionService.deleteNode(projectId = projectId, nodeId = record.nodeId)
                }
            }
        }
    }

    fun getAgentStatusWithInfo(
        userId: String,
        projectId: String,
        agentId: String
    ): ThirdPartyAgentStatusWithInfo {
        val record = thirdPartyAgentDao.getAgent(
            dslContext,
            HashUtil.decodeIdToLong(agentId)
        ) ?: throw NotFoundException("The agent($agentId) is not exist")
        // #4686 优化导入流程之后构建机启动会自动导入，此web的导入界面需要继续展示让用户可见，以使之保持用户现有操作习惯，
        var fromStatus = AgentStatus.fromStatus(record.status)
        if (fromStatus == AgentStatus.IMPORT_OK) {
            fromStatus = AgentStatus.UN_IMPORT_OK
        }
        return ThirdPartyAgentStatusWithInfo(
            status = fromStatus,
            hostname = record.hostname ?: "",
            ip = record.ip ?: "",
            os = record.detectOs ?: ""
        )
    }

    /**
     * API FROM AGENT
     */
    fun agentStartup(
        projectId: String,
        agentId: String,
        secretKey: String,
        startInfo: ThirdPartyAgentStartInfo
    ): AgentStatus {
        val id = HashUtil.decodeIdToLong(agentId)
        logger.info("The agent($id) is start up by ${startInfo.hostIp}")
        val agentRecord = thirdPartyAgentDao.getAgent(dslContext, id) ?: return AgentStatus.DELETE

        if (secretKey != SecurityUtil.decrypt(agentRecord.secretKey)) {
            throw AgentPermissionUnAuthorizedException("The secret key is not match")
        }

        var status = AgentStatus.fromStatus(agentRecord.status)

        if (AgentStatus.IMPORT_OK == status && !agentRecord.startRemoteIp.isNullOrBlank()) {
            if (startInfo.hostIp != agentRecord.startRemoteIp) {
                // 目前agent侧启动并没消费DELETE，所以这逻辑只是短路后面刷新表字段的逻辑，但实际上心跳接口仍然能刷新到，暂不动
                return AgentStatus.DELETE
            }
        }

        if (AgentStatus.isUnImport(status)) {
            status = AgentStatus.UN_IMPORT_OK // 在外面Resource调用层做自动导入动作而设置的状态
            thirdPartAgentService.addAgentAction(projectId, agentId = id, action = AgentAction.ONLINE)
        } else if (AgentStatus.isImportException(status)) {
            status = AgentStatus.IMPORT_OK
            thirdPartAgentService.addAgentAction(projectId, agentId = id, action = AgentAction.ONLINE)
        }

        agentRecord.startRemoteIp = startInfo.hostIp
        agentRecord.hostname = startInfo.hostname
        agentRecord.status = status.status
        agentRecord.ip = startInfo.hostIp
        agentRecord.detectOs = startInfo.detectOS
        agentRecord.version = startInfo.version ?: ""
        agentRecord.masterVersion = startInfo.masterVersion ?: ""
        thirdPartyAgentDao.saveAgent(dslContext, agentRecord)

        if (agentRecord.nodeId == null) {
            return status
        }

        // 更新node表信息
        val nodeRecord = nodeDao.get(dslContext, projectId, agentRecord.nodeId) ?: return status
        var nodeChanged = false
        if (nodeRecord.nodeStatus == NodeStatus.ABNORMAL.name) {
            nodeChanged = true
            nodeRecord.nodeStatus = NodeStatus.NORMAL.name
        }
        if (!nodeRecord.osName.equals(startInfo.detectOS, ignoreCase = true)) {
            nodeRecord.osName = startInfo.detectOS.lowercase()
            nodeChanged = true
        }
        if (startInfo.hostIp.isNotBlank() && nodeRecord.nodeIp != startInfo.hostIp) {
            nodeChanged = true
            nodeRecord.nodeIp = startInfo.hostIp
        }
        if (startInfo.hostname.isNotBlank() && nodeRecord.nodeName != startInfo.hostname) {
            nodeChanged = true
            nodeRecord.nodeName = startInfo.hostname
        }
        if (!startInfo.masterVersion.isNullOrBlank() && nodeRecord.agentVersion != startInfo.masterVersion) {
            nodeChanged = true
            nodeRecord.agentVersion = startInfo.masterVersion
        }
        if (nodeChanged) {
            nodeDao.saveNode(dslContext, nodeRecord)
            webSocketDispatcher.dispatch(websocketService.buildDetailMessage(projectId, nodeRecord.lastModifyUser))
        }

        return status
    }

    fun agentShutdown(
        projectId: String,
        agentId: String,
        secretKey: String,
        shutdownNormal: Boolean
    ): AgentStatus {
        val id = HashUtil.decodeIdToLong(agentId)
        logger.info("The agent($id) shutdown($shutdownNormal)")
        val agentRecord = thirdPartyAgentDao.getAgent(dslContext = dslContext, id = id)
            ?: return AgentStatus.DELETE

        if (secretKey != SecurityUtil.decrypt(agentRecord.secretKey)) {
            throw AgentPermissionUnAuthorizedException("The secret key is not match")
        }

        return AgentStatus.fromStatus(agentRecord.status)
    }

    fun getAgentStatus(
        projectId: String,
        agentId: String,
        secretKey: String
    ): AgentStatus {
        val id = HashUtil.decodeIdToLong(agentId)
        val agentRecord = thirdPartyAgentDao.getAgent(dslContext = dslContext, id = id)
            ?: return AgentStatus.DELETE
        if (secretKey != SecurityUtil.decrypt(agentRecord.secretKey)) {
            throw AgentPermissionUnAuthorizedException("The secret key is not match")
        }
        return AgentStatus.fromStatus(agentRecord.status)
    }

    fun newHeartbeat(
        projectId: String,
        agentHashId: String,
        secretKey: String,
        newHeartbeatInfo: NewHeartbeatInfo
    ): HeartbeatResponse {

        var online: Boolean? = null

        val agentId = HashUtil.decodeIdToLong(hash = agentHashId)
        val heartbeatResponse = dslContext.transactionResult { configuration ->
            val context = DSL.using(configuration)
            val agentRecord = getAgentRecord(context = context, id = agentId, secretKey = secretKey)
                ?: run {
                    logger.warn("The agent($agentHashId) is not exist")
                    return@transactionResult HeartbeatResponse(
                        AgentStatus = AgentStatus.DELETE.name,
                        language = commonConfig.devopsDefaultLocaleLanguage
                    )
                }

            val nodeRecord = agentRecord.nodeId?.let {
                nodeDao.get(dslContext = context, projectId = agentRecord.projectId, nodeId = it)
            } ?: run {
                logger.warn("The agent($agentHashId)'s node(${agentRecord.nodeId}) not exist!")
                return@transactionResult HeartbeatResponse(
                    AgentStatus = AgentStatus.DELETE.name,
                    language = commonConfig.devopsDefaultLocaleLanguage
                )
            }

            val oldUserProps = getUserProps(projectId, agentId = agentId, record = agentRecord)

            var agentChanged = false
            var nodeChanged = false
            var agentStatus: AgentStatus = AgentStatus.fromStatus(status = agentRecord.status)

            if (AgentStatus.isImportException(agentStatus)) {
                online = true
                agentStatus = AgentStatus.IMPORT_OK
                agentRecord.status = agentStatus.status
                agentChanged = true
            }

            if (newHeartbeatInfo.masterVersion != agentRecord.masterVersion) {
                agentRecord.masterVersion = newHeartbeatInfo.masterVersion
                agentChanged = true
                nodeRecord.agentVersion = newHeartbeatInfo.masterVersion // 同时更新T_NODE表中的构建机agent版本字段
                nodeChanged = true
            }
            if (newHeartbeatInfo.slaveVersion != agentRecord.version) {
                agentRecord.version = newHeartbeatInfo.slaveVersion.coerceAtMaxLength(MAX_VERSION_LENGTH)
                agentChanged = true
            }
            if (newHeartbeatInfo.agentIp != agentRecord.ip) {
                agentRecord.ip = newHeartbeatInfo.agentIp
                agentChanged = true
                nodeRecord.nodeIp = newHeartbeatInfo.agentIp
                nodeChanged = true
            }
            if (newHeartbeatInfo.hostName != agentRecord.hostname) {
                agentRecord.hostname = newHeartbeatInfo.hostName
                agentChanged = true
            }
            if (newHeartbeatInfo.hostName != nodeRecord.nodeName) {
                nodeRecord.nodeName = newHeartbeatInfo.hostName
                nodeChanged = true
            }
            if (agentRecord.parallelTaskCount == null) {
                agentRecord.parallelTaskCount = newHeartbeatInfo.parallelTaskCount.coerceAtLeast(0) // 负数则为0
                agentChanged = true
            }
            if (newHeartbeatInfo.agentInstallPath != agentRecord.agentInstallPath) {
                agentRecord.agentInstallPath = newHeartbeatInfo.agentInstallPath
                agentChanged = true
            }
            if (newHeartbeatInfo.startedUser != agentRecord.startedUser) {
                agentRecord.startedUser = newHeartbeatInfo.startedUser
                agentChanged = true
            }
            if (newHeartbeatInfo.props != null) {
                val props = JsonUtil.toJson(
                    AgentProps(
                        arch = newHeartbeatInfo.props!!.arch,
                        jdkVersion = newHeartbeatInfo.props!!.jdkVersion ?: listOf(),
                        userProps = oldUserProps,
                        dockerInitFileInfo = newHeartbeatInfo.props?.dockerInitFileInfo,
                        exitError = newHeartbeatInfo.errorExitData
                    ),
                    false
                )
                if (props != agentRecord.agentProps) {
                    agentRecord.agentProps = props
                    agentChanged = true
                }
            }
            if (newHeartbeatInfo.dockerParallelTaskCount != null && agentRecord.dockerParallelTaskCount == null) {
                agentRecord.dockerParallelTaskCount = newHeartbeatInfo.dockerParallelTaskCount
                agentChanged = true
            }
            if (agentChanged) {
                thirdPartyAgentDao.saveAgent(dslContext = context, agentRecode = agentRecord)
            }

            if (nodeRecord.nodeStatus == NodeStatus.ABNORMAL.name) {
                nodeRecord.nodeStatus = NodeStatus.NORMAL.name
                nodeChanged = true
            }
            if (nodeChanged) {
                nodeDao.saveNode(dslContext = context, nodeRecord = nodeRecord)
            }

            HeartbeatResponse(
                // 避免老的没有删除 master 校验的版本进程阻塞导致心跳异常
                masterVersion = newHeartbeatInfo.masterVersion,
                slaveVersion = agentPropsScope.getWorkerVersion(),
                AgentStatus = agentStatus.name,
                ParallelTaskCount = agentRecord.parallelTaskCount,
                envs = if (agentRecord.agentEnvs.isNullOrBlank()) {
                    mapOf()
                } else {
                    val envVar: List<EnvVar> = objectMapper.readValue(agentRecord.agentEnvs)
                    envVar.associate { it.name to it.value }
                },
                gateway = agentRecord.gateway,
                fileGateway = agentRecord.fileGateway,
                props = oldUserProps,
                dockerParallelTaskCount = agentRecord.dockerParallelTaskCount ?: 0,
                language = commonConfig.devopsDefaultLocaleLanguage
            )
        }
        thirdPartyAgentHeartbeatUtils.saveNewHeartbeat(projectId, agentId, newHeartbeatInfo)
        online?.let { thirdPartAgentService.addAgentAction(projectId, agentId, action = AgentAction.ONLINE) }
        return heartbeatResponse
    }

    fun getOs(userId: String, projectId: String, agentId: String): String {
        return thirdPartyAgentDao.getAgent(dslContext, HashUtil.decodeIdToLong(agentId))?.os ?: "LINUX"
    }

    fun enableThirdPartyAgent(projectId: String, enable: Boolean) =
        thirdPartyAgentEnableProjectsDao.enable(dslContext, projectId, enable)

    fun listEnableThirdPartyAgentProjects() =
        thirdPartyAgentEnableProjectsDao.list(dslContext)
            .filter { ByteUtils.byte2Bool(it.enalbe) }
            .map {
                it.projectId
            }

    private fun getAgentRecord(
        context: DSLContext,
        id: Long,
        secretKey: String
    ): TEnvironmentThirdpartyAgentRecord? {
        val agentRecord =
            thirdPartyAgentDao.getAgent(dslContext = context, id = id) ?: return null
        if (secretKey != SecurityUtil.decrypt(agentRecord.secretKey)) {
            throw AgentPermissionUnAuthorizedException("The secret key is not match")
        }
        return agentRecord
    }

    fun updateAgentGateway(updateAgentRequest: UpdateAgentRequest) {
        with(updateAgentRequest) {
            thirdPartyAgentDao.updateGateway(
                dslContext = dslContext,
                agentId = HashUtil.decodeIdToLong(agentId),
                gateway = gateway,
                fileGateway = fileGateway
            )
        }
    }

    fun generateSecretKey() = ApiUtil.randomSecretKey()

    fun agentTaskStarted(projectId: String, pipelineId: String, buildId: String, vmSeqId: String, agentId: String) {
        val agentLongId = HashUtil.decodeIdToLong(agentId)
        val agent = thirdPartyAgentDao.getAgent(dslContext, agentLongId)
        if (agent == null) {
            logger.warn("agent no found")
            return
        }
        val now = LocalDateTime.now()
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            nodeDao.updateLastBuildTime(context, pipelineId, agent.nodeId, now)
            agentPipelineRefDao.updateLastBuildTime(context, projectId, pipelineId, vmSeqId, agentLongId, now)
        }
    }

    private fun getUserProps(
        projectId: String,
        agentId: Long,
        record: TEnvironmentThirdpartyAgentRecord
    ): Map<String, Any> {
        if (record.agentProps == null) {
            return mapOf()
        }
        // 兼容曾经在数据库中的数据
        val oldVersion = try {
            JsonUtil.to(record.agentProps, object : TypeReference<Map<String, Any>>() {})
        } catch (ignore: Exception) {
            logger.warn("projectId: $projectId|agentId: $agentId|json to map props error", ignore)
            return mapOf()
        }

        if (!oldVersion.containsKey("arch") && !oldVersion.containsKey("jdkVersion")) {
            return oldVersion
        }

        return try {
            JsonUtil.to(record.agentProps, AgentProps::class.java).userProps ?: mapOf()
        } catch (ignore: Exception) {
            logger.warn("projectId: $projectId|agentId: $agentId|json to props error", ignore)
            mapOf()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ThirdPartyAgentMgrService::class.java)
        private const val MAX_PARALLEL_TASK_COUNT = "10"
        private const val MAX_VERSION_LENGTH = 128
    }
}
