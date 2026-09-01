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

import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.process.api.service.ServiceVarResource
import com.tencent.devops.process.utils.BK_CI_AUTHORIZER
import com.tencent.devops.process.utils.PIPELINE_START_USER_ID
import com.tencent.devops.process.utils.PIPELINE_UPDATE_USER
import com.tencent.devops.project.api.service.service.ServiceTxProjectResource
import com.tencent.devops.project.api.service.service.ServiceTxUserResource
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.dao.WindowsResourceZoneDao
import com.tencent.devops.remotedev.dao.WorkspaceDao
import com.tencent.devops.remotedev.dao.WorkspaceJoinDao
import com.tencent.devops.remotedev.dispatch.kubernetes.dao.DispatchWorkspaceDao
import com.tencent.devops.remotedev.dispatch.kubernetes.startcloud.client.WorkspaceStartCloudClient
import com.tencent.devops.remotedev.dispatch.kubernetes.utils.WorkspaceDispatchException
import com.tencent.devops.remotedev.pojo.CoffeeAIToken
import com.tencent.devops.remotedev.pojo.ImateAvatar
import com.tencent.devops.remotedev.pojo.ImateTokenSession
import com.tencent.devops.remotedev.pojo.ImateWorkspaceInspect
import com.tencent.devops.remotedev.pojo.WindowsResourceZoneConfigType
import com.tencent.devops.remotedev.pojo.WorkspaceAiInfo
import com.tencent.devops.remotedev.pojo.WorkspaceRecordWithWindows
import com.tencent.devops.remotedev.pojo.WorkspaceRegistration
import com.tencent.devops.remotedev.pojo.WorkspaceSearch
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import com.tencent.devops.remotedev.pojo.common.QueryType
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
    private val workspaceDao: WorkspaceDao,
    private val dslContext: DSLContext,
    private val workspaceRecordService: WorkspaceRecordService,
    private val workspaceService: WorkspaceService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(CoffeeAIService::class.java)
        const val TOKEN_EXPIRATION_MINUTES = 60L
        const val TOKEN_MAX_EXPIRATION_MINUTES = 240L
    }

    @Value("\${ai.jwt.secret:bk-cds-ai-agent-secret-key-change-in-production}")
    private lateinit var jwtSecret: String

    private val signingKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(jwtSecret.toByteArray())
    }

    fun generateUserToken(
        userId: String,
        expirationMinutes: Long = TOKEN_EXPIRATION_MINUTES
    ): String {
        val minutes = normalizeExpirationMinutes(expirationMinutes)
        val token = signUserJwt(userId, minutes)
        publishCoffeeLoginSession(userId, token, minutes)
        logger.info("生成用户WebSocket令牌：userId={} expirationMinutes={}", userId, minutes)
        return token
    }

    /**
     * 只签发 JWT。构建态 / 按分身换票不要走这里再去 setCoffeeAIToken：
     * Coffee 按 userId 覆盖最新 token，并行插件步骤会互相踩踏。
     */
    private fun signUserJwt(userId: String, minutes: Long): String {
        val now = Date()
        val expirationDate = Date(now.time + TimeUnit.MINUTES.toMillis(minutes))
        return Jwts.builder()
            .subject(userId)
            .claim("userId", userId)
            .claim("type", "USER")
            .issuedAt(now)
            .expiration(expirationDate)
            .signWith(signingKey)
            .compact()
    }

    /**
     * 桌面交互登录：登记 Coffee token 并重注册该用户全部 AI 云桌面。
     * 仅 UserCoffeeAIResource 登录路径使用，不要给流水线换票复用。
     */
    private fun publishCoffeeLoginSession(userId: String, token: String, minutes: Long) {
        workspaceStartCloudClient.setCoffeeAIToken(userId, CoffeeAIToken(userId, token, minutes))
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
                organization = organization ?: "",
                os = it.os
            )
        })
    }

    /**
     * 构建态换 CDS token：username 必须是本构建的触发人、流水线最后保存人或权限代持人，
     * 且 workspaceName 必须属于该用户已开启 AI 且 RUNNING 的分身。
     */
    fun generateBuildUserToken(
        projectId: String,
        buildId: String,
        username: String,
        workspaceName: String?,
        expirationMinutes: Long? = null,
        digitalAvatarId: String? = null
    ): String {
        val inspect = inspectBuildWorkspace(
            projectId = projectId,
            buildId = buildId,
            username = username,
            workspaceName = workspaceName,
            digitalAvatarId = digitalAvatarId
        )
        assertWorkspaceUsable(inspect, username, inspect.workspaceName)
        val minutes = normalizeExpirationMinutes(expirationMinutes)
        val token = signUserJwt(username, minutes)
        logger.info(
            "构建态签发用户JWT（不覆盖Coffee登录token、不重注册云桌面）：userId={} expirationMinutes={}",
            username,
            minutes
        )
        return token
    }

    fun generateBuildUserSession(
        projectId: String,
        buildId: String,
        username: String,
        workspaceName: String?,
        expirationMinutes: Long? = null,
        digitalAvatarId: String? = null
    ): ImateTokenSession {
        val inspect = inspectBuildWorkspace(
            projectId = projectId,
            buildId = buildId,
            username = username,
            workspaceName = workspaceName,
            digitalAvatarId = digitalAvatarId
        )
        assertWorkspaceUsable(inspect, username, inspect.workspaceName)
        val minutes = normalizeExpirationMinutes(expirationMinutes)
        val token = signUserJwt(username, minutes)
        logger.info(
            "构建态签发用户会话JWT（不覆盖Coffee登录token、不重注册云桌面）：userId={} expirationMinutes={}",
            username,
            minutes
        )
        return ImateTokenSession(
            token = token,
            expirationMinutes = minutes,
            expiresAt = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(minutes),
            username = username,
            workspaceName = inspect.workspaceName,
            displayName = inspect.displayName,
            owner = inspect.owner,
            digitalAvatarId = inspect.digitalAvatarId,
            agentId = inspect.agentId,
            agentName = inspect.agentName
        )
    }

    fun inspectUserWorkspace(
        userId: String,
        workspaceName: String?,
        digitalAvatarId: String? = null
    ): ImateWorkspaceInspect {
        val inspect = inspectResolvedWorkspace(userId, workspaceName, digitalAvatarId)
        logger.info(
            "用户态imate分身归属检查：digitalAvatarId={} workspaceName={} displayName={} exists={} owner={} " +
                "identityUser={} status={} imateEnabled={} belongsToUser={} usable={} reason={}",
            inspect.digitalAvatarId,
            inspect.workspaceName,
            inspect.displayName,
            inspect.exists,
            inspect.owner,
            userId,
            inspect.status,
            inspect.imateEnabled,
            inspect.belongsToUser,
            inspect.usable,
            inspect.reason
        )
        return inspect
    }

    fun generateUserWorkspaceToken(
        userId: String,
        workspaceName: String?,
        expirationMinutes: Long? = null,
        digitalAvatarId: String? = null
    ): String {
        val inspect = inspectUserWorkspace(userId, workspaceName, digitalAvatarId)
        assertWorkspaceUsable(inspect, userId, inspect.workspaceName)
        val minutes = normalizeExpirationMinutes(expirationMinutes)
        val token = signUserJwt(userId, minutes)
        logger.info(
            "用户态按分身签发JWT（不覆盖Coffee登录token）：userId={} expirationMinutes={}",
            userId,
            minutes
        )
        return token
    }

    fun listBuildWorkspaces(
        projectId: String,
        buildId: String,
        username: String
    ): List<WorkspaceAiInfo> {
        assertAllowedBuildIdentity(projectId, buildId, username)
        return getAiWorkspaceList(username)
    }

    fun listImateAvatars(userId: String): List<ImateAvatar> {
        logger.info("查询用户可见CDS数字分身：userId={}", userId)
        return workspaceStartCloudClient.listCoffeeDigitalAvatars(userId).map { avatar ->
            avatar.copy(displayName = CoffeeAIAvatarSupport.displayNameOf(avatar))
        }
    }

    fun listBuildAvatars(
        projectId: String,
        buildId: String,
        username: String
    ): List<ImateAvatar> {
        assertAllowedBuildIdentity(projectId, buildId, username)
        return listImateAvatars(username)
    }

    /**
     * 查 remotedev 工作区归属，不访问 CDS。
     * 所属者取分享表 OWNER，没有行时回退 createUserId。
     */
    fun inspectBuildWorkspace(
        projectId: String,
        buildId: String,
        username: String,
        workspaceName: String?,
        digitalAvatarId: String? = null
    ): ImateWorkspaceInspect {
        assertAllowedBuildIdentity(projectId, buildId, username)
        val inspect = inspectResolvedWorkspace(username, workspaceName, digitalAvatarId)
        logger.info(
            "imate分身归属检查：digitalAvatarId={} workspaceName={} displayName={} exists={} owner={} " +
                "createUserId={} identityUser={} status={} imateEnabled={} belongsToUser={} usable={} " +
                "reason={} projectId={} buildId={}",
            inspect.digitalAvatarId,
            inspect.workspaceName,
            inspect.displayName,
            inspect.exists,
            inspect.owner,
            inspect.createUserId,
            username,
            inspect.status,
            inspect.imateEnabled,
            inspect.belongsToUser,
            inspect.usable,
            inspect.reason,
            projectId,
            buildId
        )
        return inspect
    }

    private fun inspectResolvedWorkspace(
        username: String,
        workspaceName: String?,
        digitalAvatarId: String?
    ): ImateWorkspaceInspect {
        val explicitAvatar = resolveDigitalAvatar(username, digitalAvatarId)
        val listed = if (explicitAvatar == null) {
            resolveAvatarByWorkspaceName(username, workspaceName)
        } else {
            WorkspaceAvatarLookup(listed = false, avatar = null)
        }
        val avatar = explicitAvatar ?: listed.avatar
        val targetWorkspace = avatar?.workspaceName ?: workspaceName
        if (targetWorkspace.isNullOrBlank()) {
            throw ParamBlankException("Invalid workspaceName")
        }
        val inspect = lookupWorkspaceOwner(username, targetWorkspace)
        val enriched = if (avatar == null) {
            inspect
        } else {
            inspect.copy(
                digitalAvatarId = avatar.digitalAvatarId,
                agentId = avatar.agentId,
                agentName = avatar.agentName,
                displayName = inspect.displayName ?: CoffeeAIAvatarSupport.displayNameOf(avatar)
            )
        }
        return if (explicitAvatar == null && listed.listed && avatar == null) {
            CoffeeAIAvatarSupport.markMissingAvatar(enriched)
        } else {
            enriched
        }
    }

    private data class WorkspaceAvatarLookup(
        val listed: Boolean,
        val avatar: ImateAvatar?
    )

    private fun resolveAvatarByWorkspaceName(userId: String, workspaceName: String?): WorkspaceAvatarLookup {
        if (workspaceName.isNullOrBlank()) {
            return WorkspaceAvatarLookup(listed = false, avatar = null)
        }
        return try {
            val avatars = listImateAvatars(userId)
            WorkspaceAvatarLookup(
                listed = true,
                avatar = CoffeeAIAvatarSupport.matchByWorkspace(avatars, workspaceName)
            )
        } catch (e: Exception) {
            logger.warn(
                "列出数字分身失败，workspace 路径跳过分身校验：userId={} workspaceName={} message={}",
                userId,
                workspaceName,
                e.message
            )
            WorkspaceAvatarLookup(listed = false, avatar = null)
        }
    }

    /**
     * 有 digitalAvatarId 时先向 CDS 精确解析，失败直接抛错，禁止落到同桌面任意 Agent。
     * 只传 workspaceName 时走旧路径。
     */
    private fun resolveDigitalAvatar(userId: String, digitalAvatarId: String?): ImateAvatar? {
        if (digitalAvatarId.isNullOrBlank()) {
            return null
        }
        return try {
            val avatar = workspaceStartCloudClient.getCoffeeDigitalAvatar(userId, digitalAvatarId)
            logger.info(
                "CDS数字分身解析成功：userId={} digitalAvatarId={} workspaceName={} owner={} agentId={}",
                userId,
                avatar.digitalAvatarId,
                avatar.workspaceName,
                avatar.owner,
                avatar.agentId
            )
            avatar
        } catch (e: WorkspaceDispatchException) {
            logger.warn(
                "CDS数字分身解析失败：userId={} digitalAvatarId={} message={}",
                userId,
                digitalAvatarId,
                e.message
            )
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
                params = arrayOf(digitalAvatarId)
            )
        }
    }

    private fun assertAllowedBuildIdentity(projectId: String, buildId: String, username: String) {
        val vars = client.get(ServiceVarResource::class).getBuildVar(
            projectId = projectId,
            buildId = buildId,
            varName = null,
            pipelineId = null
        ).data ?: emptyMap()
        val allowed = setOfNotNull(
            vars[PIPELINE_START_USER_ID]?.takeIf { it.isNotBlank() },
            vars[PIPELINE_UPDATE_USER]?.takeIf { it.isNotBlank() },
            vars[BK_CI_AUTHORIZER]?.takeIf { it.isNotBlank() }
        )
        if (username !in allowed) {
            logger.warn(
                "拒绝代开CDS身份：username={} allowed={} projectId={} buildId={}",
                username,
                allowed,
                projectId,
                buildId
            )
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.FORBIDDEN.errorCode,
                params = arrayOf("username $username is not starter/last-modifier/authorizer of this build")
            )
        }
    }

    private fun lookupWorkspaceOwner(username: String, workspaceName: String): ImateWorkspaceInspect {
        val record = workspaceDao.fetchAnyWorkspace(dslContext, workspaceName = workspaceName)
            ?: return ImateWorkspaceInspect(
                workspaceName = workspaceName,
                exists = false,
                belongsToUser = false,
                usable = false,
                reason = "WORKSPACE_NOT_FOUND"
            )
        val owner = workspaceService.getWorkspaceMemberInfo(record.projectId, workspaceName)?.owner
            ?: record.createUserId
        val belongsToUser = owner == username
        val imateEnabled = record.coffeeAi == true
        val running = record.status == WorkspaceStatus.RUNNING
        val reason = when {
            !belongsToUser -> "OWNER_MISMATCH"
            !imateEnabled -> "IMATE_DISABLED"
            !running -> "NOT_RUNNING"
            else -> null
        }
        val envId = dispatchWorkspaceDao.getEnvIdsByWorkspaceNames(
            listOf(record.workspaceName),
            dslContext
        )[record.workspaceName]
        return ImateWorkspaceInspect(
            workspaceName = record.workspaceName,
            displayName = record.displayName.takeIf { it != "NO_CHECK" } ?: record.workspaceName,
            exists = true,
            owner = owner,
            createUserId = record.createUserId,
            status = record.status.name,
            imateEnabled = imateEnabled,
            belongsToUser = belongsToUser,
            usable = belongsToUser && imateEnabled && running,
            reason = reason,
            projectId = record.projectId.takeIf { it != "NO_CHECK" },
            ownerType = record.ownerType.name,
            workspaceId = record.workspaceId,
            envId = envId?.takeIf { it.isNotBlank() },
            ip = record.ip?.takeIf { it.isNotBlank() }
        )
    }

    fun getAiWorkspaceList(userId: String): List<WorkspaceAiInfo> {
        logger.info("查询用户开启AI功能的云桌面列表：userId={}", userId)
        // 与模块负责人口径一致：T_WORKSPACE ⋈ T_WORKSPACE_SHARED，ASSIGN_TYPE=OWNER，不是 CREATOR、不是分享人。
        val records = workspaceJoinDao.fetchWindowsWorkspacesSimple(
            dslContext = dslContext,
            owner = userId,
            coffeeAi = true,
            notStatus = listOf(WorkspaceStatus.DELETED)
        )
        if (records.isEmpty()) return emptyList()

        val envIdMap = dispatchWorkspaceDao.getEnvIdsByWorkspaceNames(
            records.map { it.workspaceName },
            dslContext
        )

        // 批量查询 zoneConfigType
        val zoneTypeMap = records.mapNotNull { it.zoneId }
            .distinct()
            .associateWith { zoneId ->
                windowsResourceZoneDao.fetchAny(dslContext, zoneId)?.type?.name
            }
        logger.info("return records workspace names: ${records.map { it.workspaceName }}")
        return records.map { record ->
            val displayName = record.displayName.takeIf { it != "NO_CHECK" } ?: record.workspaceName
            WorkspaceAiInfo(
                workspaceName = record.workspaceName,
                displayName = displayName,
                ip = record.ip ?: "",
                envId = envIdMap[record.workspaceName]?.takeIf { it.isNotBlank() } ?: "",
                projectId = record.projectId.takeIf { it != "NO_CHECK" } ?: "",
                zoneConfigType = record.zoneId?.let { zoneTypeMap[it] } ?: "",
                os = record.os,
                owner = userId,
                createUserId = record.createUserId,
                ownerType = record.ownerType.name,
                status = record.status.name,
                imateEnabled = record.coffeeAi == true
            )
        }
    }

    private fun assertWorkspaceUsable(
        inspect: ImateWorkspaceInspect,
        username: String,
        workspaceName: String
    ) {
        if (!inspect.exists) {
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
                params = arrayOf(workspaceName)
            )
        }
        if (!inspect.belongsToUser) {
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.FORBIDDEN.errorCode,
                params = arrayOf(
                    "workspace $workspaceName owner is ${inspect.owner ?: "unknown"}, not $username"
                )
            )
        }
        if (!inspect.usable) {
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.FORBIDDEN.errorCode,
                params = arrayOf(
                    "workspace $workspaceName owner=${inspect.owner} status=${inspect.status} " +
                        "imateEnabled=${inspect.imateEnabled} reason=${inspect.reason}"
                )
            )
        }
    }

    private fun normalizeExpirationMinutes(expirationMinutes: Long?): Long {
        val requested = expirationMinutes ?: TOKEN_EXPIRATION_MINUTES
        return requested.coerceIn(1, TOKEN_MAX_EXPIRATION_MINUTES)
    }

    fun enableCoffeeAI(workspaceNames: List<String>) {
        logger.info("批量开启工作空间CoffeeAI功能：workspaceNames={}", workspaceNames)
        workspaceDao.enableCoffeeAI(dslContext, workspaceNames)
    }

    fun openClawOn(userId: String, workspaceName: String?, ip: String?): WorkspaceRegistration? {
        // 暂时采用这个方法，待后续能传入ip，再根据ip获取
        val openClawWs = workspaceService.limitFetchProjectWorkspace(
            limit = PageUtil.convertPageSizeToSQLLimit(1, 10),
            queryType = QueryType.SERVICE,
            search = WorkspaceSearch(
                workspaceName = workspaceName?.let { listOf(it) },
                sips = ip?.let { listOf(it) },
                owner = listOf(userId),
                status = listOf(WorkspaceStatus.RUNNING),
                logicalArea = listOf(WindowsResourceZoneConfigType.DEVCLOUD)
            )
        ).maxByOrNull { it.lastStatusUpdateTime!! } as? WorkspaceRecordWithWindows ?: run {
            logger.warn("openClawOn: workspace not found|$userId")
            return null
        }
        // 恶心的逻辑，录屏需要校验管理员的权限，所以需要获取管理员
        val manager = client.get(ServiceTxProjectResource::class).getProjectManagers(
            openClawWs.projectId
        ).data?.first()
        enableCoffeeAI(listOf(openClawWs.workspaceName))
        workspaceRecordService.enableRecord(
            workspaceName = openClawWs.workspaceName,
            enableUser = manager
        )
        val organization = kotlin.runCatching {
            client.get(ServiceTxUserResource::class).get(userId).data?.let {
                "${it.bgName}/${it.businessLineName}/${it.deptName}/${it.centerName}/${it.groupName}"
            }
        }.onFailure { logger.warn("get user $userId info error|${it.message}") }
            .getOrElse { null }
        val envIdMap = dispatchWorkspaceDao.getEnvIdsByWorkspaceNames(listOf(openClawWs.workspaceName), dslContext)
        return WorkspaceRegistration(
            workspaceName = openClawWs.workspaceName,
            envId = envIdMap[openClawWs.workspaceName] ?: "",
            hostIp = openClawWs.ip ?: "",
            owner = userId,
            description = openClawWs.displayName,
            projectId = openClawWs.projectId,
            zoneConfigType = WindowsResourceZoneConfigType.DEVCLOUD.name,
            organization = organization ?: "",
            os = openClawWs.os
        )
    }
}
