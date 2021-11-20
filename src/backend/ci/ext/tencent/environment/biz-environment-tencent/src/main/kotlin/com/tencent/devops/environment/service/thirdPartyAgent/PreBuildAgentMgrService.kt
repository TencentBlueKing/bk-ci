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

package com.tencent.devops.environment.service.thirdPartyAgent

import com.tencent.devops.common.api.enums.AgentStatus
import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.util.ApiUtil
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.SecurityUtil
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.dao.thirdPartyAgent.ThirdPartyAgentDao
import com.tencent.devops.environment.permission.EnvironmentPermissionService
import com.tencent.devops.environment.pojo.enums.NodeStatus
import com.tencent.devops.environment.pojo.enums.NodeType
import com.tencent.devops.environment.pojo.thirdPartyAgent.ThirdPartyAgentStaticInfo
import com.tencent.devops.environment.service.AgentUrlService
import com.tencent.devops.environment.service.slave.SlaveGatewayService
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Suppress("LongMethod")
@Service
class PreBuildAgentMgrService @Autowired constructor(
    private val dslContext: DSLContext,
    private val thirdPartyAgentDao: ThirdPartyAgentDao,
    private val nodeDao: NodeDao,
    private val slaveGatewayService: SlaveGatewayService,
    private val agentUrlService: AgentUrlService,
    private val environmentPermissionService: EnvironmentPermissionService
) {

    private fun generateSecretKey() = ApiUtil.randomSecretKey()

    fun createPrebuildAgent(
        userId: String,
        projectId: String,
        os: OS,
        zoneName: String?,
        initIp: String?
    ): ThirdPartyAgentStaticInfo {
        val gateway = slaveGatewayService.getGateway(zoneName)?.removePrefix("http://")
        val secretKey = generateSecretKey()

        var result: ThirdPartyAgentStaticInfo? = null
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)

            // node
            logger.info("create agent in node, projectId: $projectId, initIp: $initIp")
            val nodeId = nodeDao.addNode(
                dslContext = context,
                projectId = projectId,
                ip = initIp ?: "",
                name = "",
                osName = os.name.toLowerCase(),
                status = NodeStatus.NORMAL,
                type = NodeType.THIRDPARTY,
                userId = userId
            )

            val nodeStringId = "BUILD_${HashUtil.encodeLongId(nodeId)}_$initIp"
            logger.info("nodeStringId: $nodeStringId")
            nodeDao.insertNodeStringIdAndDisplayName(
                dslContext = context,
                id = nodeId,
                nodeStringId = nodeStringId,
                displayName = nodeStringId,
                userId = userId
            )

            // agent
            val agentId = thirdPartyAgentDao.createAgent(
                dslContext = context,
                userId = userId,
                projectId = projectId,
                os = os,
                secretKey = SecurityUtil.encrypt(secretKey),
                gateway = gateway,
                ip = initIp ?: ""
            )
            val agentRecord = thirdPartyAgentDao.getAgent(context, agentId)!!
            val agentHashId = HashUtil.encodeLongId(agentId)
            result = ThirdPartyAgentStaticInfo(
                agentId = agentHashId,
                projectId = projectId,
                os = os.name,
                secretKey = secretKey,
                createdUser = userId,
                gateway = gateway,
                link = if (os == OS.WINDOWS) {
                    agentUrlService.genAgentUrl(agentRecord)
                } else {
                    agentUrlService.genAgentInstallUrl(agentRecord)
                },
                script = agentUrlService.genAgentInstallScript(agentRecord),
                ip = agentRecord.ip,
                hostName = agentRecord.hostname,
                status = agentRecord.status
            )

            thirdPartyAgentDao.updateStatus(context, agentId, nodeId, projectId, AgentStatus.IMPORT_EXCEPTION)
            environmentPermissionService.createNode(
                userId = userId,
                projectId = projectId,
                nodeId = nodeId,
                nodeName = "$nodeStringId(${agentRecord.ip})"
            )
        }
        return result!!
    }

    fun listPreBuildAgent(userId: String, projectId: String, os: OS?): List<ThirdPartyAgentStaticInfo> {
        return thirdPartyAgentDao.listPreBuildAgent(
            dslContext, userId, projectId, os ?: OS.LINUX
        ).filter { it.nodeId != null }.map {
            ThirdPartyAgentStaticInfo(
                agentId = HashUtil.encodeLongId(it.id), // 必须用it.id，不能是it.nodeId
                projectId = it.projectId,
                os = it.os,
                secretKey = SecurityUtil.decrypt(it.secretKey),
                createdUser = it.createdUser,
                gateway = it.gateway,
                link = if (os == OS.WINDOWS) {
                    agentUrlService.genAgentUrl(it)
                } else {
                    agentUrlService.genAgentInstallUrl(it)
                },
                script = agentUrlService.genAgentInstallScript(it),
                ip = it.ip,
                hostName = it.hostname,
                status = it.status
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PreBuildAgentMgrService::class.java)
    }
}
