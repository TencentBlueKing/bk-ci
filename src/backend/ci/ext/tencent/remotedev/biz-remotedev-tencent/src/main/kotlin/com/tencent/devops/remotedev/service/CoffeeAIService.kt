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

package com.tencent.devops.remotedev.service

import com.tencent.devops.common.client.Client
import com.tencent.devops.project.api.service.service.ServiceTxUserResource
import com.tencent.devops.remotedev.dao.WindowsResourceZoneDao
import com.tencent.devops.remotedev.dao.WorkspaceJoinDao
import com.tencent.devops.remotedev.dispatch.kubernetes.dao.DispatchWorkspaceDao
import com.tencent.devops.remotedev.dispatch.kubernetes.startcloud.client.WorkspaceStartCloudClient
import com.tencent.devops.remotedev.dispatch.kubernetes.startcloud.pojo.CoffeeAIToken
import com.tencent.devops.remotedev.dispatch.kubernetes.startcloud.pojo.WorkspaceRegistration
import com.tencent.devops.remotedev.pojo.WorkspaceAiInfo
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.crypto.SecretKey
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class CoffeeAIService @Autowired constructor(
    private val client: Client,
    private val workspaceStartCloudClient: WorkspaceStartCloudClient,
    private val workspaceJoinDao: WorkspaceJoinDao,
    private val dispatchWorkspaceDao: DispatchWorkspaceDao,
    private val windowsResourceZoneDao: WindowsResourceZoneDao,
    private val dslContext: DSLContext
) {
    companion object {
        private val logger = LoggerFactory.getLogger(CoffeeAIService::class.java)
        private const val TOKEN_EXPIRATION_MINUTES = 60L
    }

    @Value("\${ai.jwt.secret:bk-cds-ai-agent-secret-key-change-in-production}")
    private lateinit var jwtSecret: String

    private val signingKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(jwtSecret.toByteArray())
    }

    fun generateUserToken(userId: String): String {
        val now = Date()
        val expirationDate = Date(now.time + TimeUnit.MINUTES.toMillis(TOKEN_EXPIRATION_MINUTES))

        val token = Jwts.builder()
            .subject(userId)
            .claim("userId", userId)
            .claim("type", "USER")
            .issuedAt(now)
            .expiration(expirationDate)
            .signWith(signingKey)
            .compact()

        workspaceStartCloudClient.setCoffeeAIToken(userId, CoffeeAIToken(userId, token, TOKEN_EXPIRATION_MINUTES))
        logger.info("生成用户WebSocket令牌：userId={}", userId)
        val workspaces = getAiWorkspaceList(userId)

        val organization = kotlin.runCatching {
            client.get(ServiceTxUserResource::class).get(userId).data?.let {
                "${it.bgName}/${it.businessLineName}/${it.deptName}/${it.centerName}/${it.groupName}"
            }
        }.onFailure { logger.warn("get user $userId info error|${it.message}") }
            .getOrElse { null }
        workspaceStartCloudClient.setCoffeeAIWorkspace(userId, workspaces.map {
            WorkspaceRegistration(
                workspaceName = it.workspaceName,
                envId = it.envId,
                hostIp = it.ip,
                owner = userId,
                description = it.displayName,
                projectId = it.projectId,
                zoneConfigType = it.zoneConfigType,
                organization = organization ?: ""
            )
        })
        return token
    }

    fun getAiWorkspaceList(userId: String): List<WorkspaceAiInfo> {
        logger.info("查询用户开启AI功能的云桌面列表：userId={}", userId)
        val records = workspaceJoinDao.fetchWindowsWorkspacesSimple(
            dslContext = dslContext,
            owner = userId,
            status = WorkspaceStatus.RUNNING,
            coffeeAi = true
        )
        if (records.isEmpty()) return emptyList()

        val workspaceNames = records.map { it.workspaceName }
        val envIdMap = dispatchWorkspaceDao.getEnvIdsByWorkspaceNames(workspaceNames, dslContext)

        // 批量查询 zoneConfigType
        val zoneTypeMap = records.mapNotNull { it.zoneId }
            .distinct()
            .associateWith { zoneId ->
                windowsResourceZoneDao.fetchAny(dslContext, zoneId)?.type?.name
            }
        logger.info("return records workspace names: ${records.map { it.workspaceName }}")
        return records.map { record ->
            WorkspaceAiInfo(
                workspaceName = record.workspaceName,
                displayName = record.displayName.takeIf { it != "NO_CHECK" } ?: record.workspaceName,
                ip = record.ip ?: "",
                envId = envIdMap[record.workspaceName]?.takeIf { it.isNotBlank() } ?: "",
                projectId = record.projectId.takeIf { it != "NO_CHECK" } ?: "",
                zoneConfigType = record.zoneId?.let { zoneTypeMap[it] } ?: ""
            )
        }
    }
}
