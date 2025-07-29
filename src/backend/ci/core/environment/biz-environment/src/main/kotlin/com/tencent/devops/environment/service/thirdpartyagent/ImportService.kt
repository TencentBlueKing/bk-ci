/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

import com.tencent.bk.audit.annotations.ActionAuditRecord
import com.tencent.bk.audit.annotations.AuditAttribute
import com.tencent.bk.audit.annotations.AuditInstanceRecord
import com.tencent.bk.audit.context.ActionAuditContext
import com.tencent.devops.common.api.check.Preconditions
import com.tencent.devops.common.api.enums.AgentStatus
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.util.ApiUtil
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.SecurityUtil
import com.tencent.devops.common.audit.ActionAuditContent
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.ResourceTypeId
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.redis.concurrent.SimpleRateLimiter
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.environment.TpaLock
import com.tencent.devops.environment.constant.EnvironmentMessageCode
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.dao.thirdpartyagent.ThirdPartyAgentDao
import com.tencent.devops.environment.permission.EnvironmentPermissionService
import com.tencent.devops.environment.pojo.enums.NodeStatus
import com.tencent.devops.environment.pojo.enums.NodeType
import com.tencent.devops.environment.service.NodeTagService
import com.tencent.devops.environment.service.slave.SlaveGatewayService
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import jakarta.ws.rs.NotFoundException

@Service
@Suppress("LongParameterList", "ThrowsCount")
class ImportService @Autowired constructor(
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val thirdPartyAgentDao: ThirdPartyAgentDao,
    private val nodeDao: NodeDao,
    private val slaveGatewayService: SlaveGatewayService,
    private val environmentPermissionService: EnvironmentPermissionService,
    private val simpleRateLimiter: SimpleRateLimiter,
    private val nodeTagService: NodeTagService
) {

    companion object {
        const val BU_SIZE = 100
        private val LOG = LoggerFactory.getLogger(ImportService::class.java)
        private val badStatus = setOf(AgentStatus.IMPORT_EXCEPTION.status, AgentStatus.UN_IMPORT.status)
    }

    /**
     * 利用上一次安装包[agentHashId]的ID来生成新的agent并返回hashId
     */
    fun generateAgentByOtherAgentId(agentHashId: String): String {
        val lockKey = "lock:tpa:batch:rate:$agentHashId"
        val acquire = simpleRateLimiter.acquire(BU_SIZE, lockKey = lockKey)
        if (!acquire) {
            throw OperationException("Frequency limit: $BU_SIZE")
        }
        try {
            val agentLongId = HashUtil.decodeIdToLong(agentHashId)
            val parentAgent = thirdPartyAgentDao.getAgent(dslContext = dslContext, id = agentLongId)
                ?: throw NotFoundException("The install key($agentHashId) is not exist")
            val userId = parentAgent.createdUser
            val projectId = parentAgent.projectId

            if (!environmentPermissionService.checkNodePermission(userId, projectId, AuthPermission.CREATE)) {
                throw PermissionForbiddenException(
                    message = I18nUtil.getCodeLanMessage(
                        EnvironmentMessageCode.ERROR_NODE_NO_CREATE_PERMISSSION,
                        language = I18nUtil.getLanguage(userId)
                    )
                )
            }

            val os = OS.valueOf(parentAgent.os)
            val zoneName = null
            val gateway = slaveGatewayService.getGateway(zoneName)
            val fileGateway = slaveGatewayService.getFileGateway(zoneName)
            LOG.info("Generate agent($os) info of project($projectId) with gateway $gateway by user($userId)")
            val secretKey = ApiUtil.randomSecretKey()
            val id = thirdPartyAgentDao.add(
                dslContext = dslContext,
                userId = userId,
                projectId = projectId,
                os = os,
                secretKey = SecurityUtil.encrypt(secretKey),
                gateway = gateway,
                fileGateway = fileGateway
            )

            return HashUtil.encodeLongId(id)
        } finally {
            if (acquire) {
                simpleRateLimiter.release(lockKey)
            }
        }
    }

    @ActionAuditRecord(
        actionId = ActionId.ENV_NODE_CREATE,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.ENV_NODE
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.ENV_NODE_CREATE_CONTENT
    )
    fun importAgent(userId: String, projectId: String, agentId: String, masterVersion: String?) {

        val id = HashUtil.decodeIdToLong(agentId)

        TpaLock(redisOperation = redisOperation, key = "ia:$id").use { lock ->
            if (!lock.tryLock()) { // 并发场景，不向用户展示信息
                LOG.info("$agentId duplicate import, skip")
                return
            }
            val nodeId = import(id, projectId, agentId, userId, masterVersion)
            if (nodeId != null) {
                // audit
                ActionAuditContext.current()
                    .setInstanceName(nodeId.toString())
                    .setInstanceId(nodeId.toString())
            }
        }
    }

    private fun import(id: Long, projectId: String, agentId: String, userId: String, masterVersion: String?): Long? {
        val agentRecord = thirdPartyAgentDao.getAgentByProject(dslContext, id, projectId)
            ?: throw NotFoundException("The agent($agentId) is not exist")

        if (agentRecord.status == AgentStatus.IMPORT_OK.status) { // 忽略重复导入
            return null
        }

        Preconditions.checkTrue(
            condition = !badStatus.contains(agentRecord.status),
            exception = ErrorCodeException(errorCode = EnvironmentMessageCode.ERROR_NODE_AGENT_STATUS_EXCEPTION)
        )

        Preconditions.checkTrue(
            condition = environmentPermissionService.checkNodePermission(userId, projectId, AuthPermission.CREATE),
            exception = PermissionForbiddenException(
                message = I18nUtil.getCodeLanMessage(
                    EnvironmentMessageCode.ERROR_NODE_NO_CREATE_PERMISSSION,
                    language = I18nUtil.getLanguage(userId)
                )
            )
        )
        var nodeId = 0L
        LOG.info("Trying to import the agent($agentId) of project($projectId) by user($userId)")
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)

            nodeId = nodeDao.addNode(
                dslContext = context,
                projectId = projectId,
                ip = agentRecord.ip,
                name = agentRecord.hostname,
                osName = agentRecord.os.lowercase(),
                status = NodeStatus.NORMAL,
                type = NodeType.THIRDPARTY,
                userId = userId,
                agentVersion = masterVersion
            )

            val nodeStringId = "BUILD_${HashUtil.encodeLongId(nodeId)}_${agentRecord.ip}"
            nodeDao.insertNodeStringIdAndDisplayName(
                dslContext = context,
                id = nodeId,
                nodeStringId = nodeStringId,
                displayName = nodeStringId,
                userId = userId
            )
            val count = thirdPartyAgentDao.updateStatus(context, id, nodeId, projectId, AgentStatus.IMPORT_OK)
            if (count != 1) {
                LOG.warn("Fail to update the agent($id) to OK status")
                throw ErrorCodeException(
                    errorCode = EnvironmentMessageCode.ERROR_NODE_NOT_EXISTS,
                    params = arrayOf(id.toString())
                )
            }
            environmentPermissionService.createNode(
                userId = userId,
                projectId = projectId,
                nodeId = nodeId,
                nodeName = "$nodeStringId(${agentRecord.ip})"
            )
        }

        // 导入后添加标签
        nodeTagService.editInternalTags(projectId, id)
        return nodeId
    }
}
