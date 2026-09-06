package com.tencent.devops.environment.service.thirdpartyagent

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.AESUtil
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.SecurityUtil
import com.tencent.devops.common.api.util.ShaUtils
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.environment.constant.EnvironmentMessageCode
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.dao.thirdpartyagent.AgentInstallSessionDao
import com.tencent.devops.environment.dao.thirdpartyagent.ThirdPartyAgentDao
import com.tencent.devops.environment.model.AgentInstallSession
import com.tencent.devops.environment.model.AgentInstallSessionMode
import com.tencent.devops.environment.model.AgentInstallSessionNode
import com.tencent.devops.environment.model.AgentInstallSessionNodeStatus
import com.tencent.devops.environment.model.AgentInstallSessionStatus
import com.tencent.devops.environment.model.AgentInstallSessionTag
import com.tencent.devops.environment.permission.EnvironmentPermissionService
import com.tencent.devops.environment.pojo.NodeTag
import com.tencent.devops.environment.pojo.enums.AgentType
import com.tencent.devops.environment.pojo.enums.NodeStatus
import com.tencent.devops.environment.pojo.thirdpartyagent.AgentInstallEnvironmentInfo
import com.tencent.devops.environment.pojo.thirdpartyagent.AgentInstallEnvironmentPreview
import com.tencent.devops.environment.pojo.thirdpartyagent.AgentInstallMode
import com.tencent.devops.environment.pojo.thirdpartyagent.AgentInstallSessionCreateResponse
import com.tencent.devops.environment.pojo.thirdpartyagent.AgentInstallSessionDetail
import com.tencent.devops.environment.pojo.thirdpartyagent.AgentInstallSessionNodeInfo
import com.tencent.devops.environment.pojo.thirdpartyagent.AgentInstallSessionPreview
import com.tencent.devops.environment.pojo.thirdpartyagent.AgentInstallSessionRequest
import com.tencent.devops.environment.pojo.thirdpartyagent.AgentInstallSessionSummary
import com.tencent.devops.environment.pojo.thirdpartyagent.AgentInstallTagSnapshot
import com.tencent.devops.environment.pojo.thirdpartyagent.AgentReinstallContext
import com.tencent.devops.environment.pojo.thirdpartyagent.InstallEnvItem
import com.tencent.devops.environment.pojo.thirdpartyagent.TPAInstallType
import com.tencent.devops.environment.service.AgentUrlService
import com.tencent.devops.environment.service.DynamicEnvMatcher
import com.tencent.devops.environment.service.NodeTagService
import com.tencent.devops.environment.service.slave.SlaveGatewayService
import jakarta.ws.rs.NotFoundException
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Service
class AgentInstallSessionService(
    private val dslContext: DSLContext,
    private val sessionDao: AgentInstallSessionDao,
    private val thirdPartyAgentDao: ThirdPartyAgentDao,
    private val nodeDao: NodeDao,
    private val nodeTagService: NodeTagService,
    private val dynamicEnvMatcher: DynamicEnvMatcher,
    private val slaveGatewayService: SlaveGatewayService,
    private val environmentPermissionService: EnvironmentPermissionService,
    private val agentUrlService: AgentUrlService,
    private val redisOperation: RedisOperation
) {
    @Value("\${environment.agent-install-session.expire-days:3}")
    private var expireDays: Long = 3

    @Value("\${environment.batch-install.aes-key}")
    private var encryptionKey: String = ""

    fun preview(userId: String, projectId: String, request: AgentInstallSessionRequest): AgentInstallSessionPreview {
        return normalize(userId, projectId, request, requireAbnormal = false).preview
    }

    fun create(
        userId: String,
        projectId: String,
        request: AgentInstallSessionRequest
    ): AgentInstallSessionCreateResponse {
        val normalized = normalize(userId, projectId, request, requireAbnormal = true)
        RedisLock(
            redisOperation = redisOperation,
            lockKey = "environment:agent-install-session:create:$projectId:$userId:${normalized.fingerprint}",
            expiredTimeInSeconds = CREATE_LOCK_EXPIRE_SECONDS
        ).use { lock ->
            lock.lock()
            val now = LocalDateTime.now()
            val existing = findReusableSession(projectId, userId, normalized, now)
            return existing?.let { createResponse(it, reused = true) }
                ?: createSession(normalized, userId, projectId, now, previousSessionId = null)
        }
    }

    fun list(
        userId: String,
        projectId: String,
        page: Int?,
        pageSize: Int?
    ): Page<AgentInstallSessionDetail> {
        checkViewPermission(userId, projectId)
        val currentPage = (page ?: DEFAULT_PAGE).coerceAtLeast(1)
        val currentPageSize = (pageSize ?: DEFAULT_PAGE_SIZE).coerceIn(1, MAX_PAGE_SIZE)
        val count = sessionDao.countByProject(dslContext, projectId)
        val now = LocalDateTime.now()
        val records = sessionDao.listByProject(
            dslContext = dslContext,
            projectId = projectId,
            offset = (currentPage - 1) * currentPageSize,
            limit = currentPageSize
        ).map { toDetail(it, now) }
        return Page(currentPage, currentPageSize, count, records)
    }

    fun get(userId: String, projectId: String, sessionId: String): AgentInstallSessionDetail {
        checkViewPermission(userId, projectId)
        return toDetail(getSession(projectId, sessionId), LocalDateTime.now())
    }

    fun listNodes(userId: String, projectId: String, sessionId: String): List<AgentInstallSessionNodeInfo> {
        checkViewPermission(userId, projectId)
        getSession(projectId, sessionId)
        return sessionDao.listNodes(dslContext, sessionId).map { node ->
            AgentInstallSessionNodeInfo(
                agentId = HashUtil.encodeLongId(node.agentId),
                nodeId = node.nodeId?.let(HashUtil::encodeLongId),
                status = node.status.name,
                hostname = node.hostname,
                ip = node.ip,
                errorMessage = node.errorMessage,
                startedAt = node.startedTime,
                finishedAt = node.finishedTime,
                agentVersion = node.agentVersion
            )
        }
    }

    fun regenerate(userId: String, projectId: String, sessionId: String): AgentInstallSessionCreateResponse {
        checkViewPermission(userId, projectId)
        val source = getSession(projectId, sessionId)
        val now = LocalDateTime.now()
        if (source.status == AgentInstallSessionStatus.ACTIVE && source.expiredTime > now) {
            return createResponse(source, reused = true)
        }
        if (source.status == AgentInstallSessionStatus.REVOKED) {
            throw OperationException("Revoked install session cannot be regenerated")
        }
        if (source.mode == AgentInstallSessionMode.REINSTALL) {
            validateReinstallTarget(userId, projectId, source.targetAgentId, requireAbnormal = true)
        }
        val tags = sessionDao.listTags(dslContext, source.id).map { it.toSnapshot() }
        val normalized = NormalizedConfig(
            mode = source.mode,
            os = OS.valueOf(source.os),
            zone = source.zone,
            gateway = source.gateway,
            fileGateway = source.fileGateway,
            loginName = source.loginName,
            loginPasswordCipher = source.loginPasswordCipher,
            installType = TPAInstallType.valueOf(source.installType),
            agentType = AgentType.valueOf(source.agentType),
            parallelTaskCount = source.parallelTaskCount,
            targetAgentId = source.targetAgentId,
            targetNodeId = source.targetNodeId,
            tags = tags,
            fingerprint = source.configFingerprint,
            preview = preview(source)
        )
        RedisLock(
            redisOperation = redisOperation,
            lockKey = "environment:agent-install-session:create:$projectId:$userId:${normalized.fingerprint}",
            expiredTimeInSeconds = CREATE_LOCK_EXPIRE_SECONDS
        ).use { lock ->
            lock.lock()
            val createTime = LocalDateTime.now()
            val existing = findReusableSession(projectId, userId, normalized, createTime)
            if (existing != null && existing.id != source.id) {
                return createResponse(existing, reused = true)
            }
            sessionDao.markExpired(dslContext, source.id, createTime)
            return createSession(normalized, userId, projectId, createTime, previousSessionId = source.id)
        }
    }

    fun getReinstallContext(userId: String, projectId: String, agentHashId: String): AgentReinstallContext {
        val target = validateReinstallTarget(
            userId = userId,
            projectId = projectId,
            targetAgentId = decodeAgentId(agentHashId),
            requireAbnormal = false,
            requireEditPermission = false
        )
        if (!environmentPermissionService.checkNodePermission(
                userId = userId,
                projectId = projectId,
                nodeId = target.nodeId,
                permission = AuthPermission.VIEW
            )
        ) {
            throw PermissionForbiddenException(
                message = I18nUtil.getCodeLanMessage(EnvironmentMessageCode.ERROR_NODE_NO_VIEW_PERMISSSION)
            )
        }
        val canEdit = environmentPermissionService.checkNodePermission(
            userId = userId,
            projectId = projectId,
            nodeId = target.nodeId,
            permission = AuthPermission.EDIT
        )
        val isAbnormal = target.nodeStatus == NodeStatus.ABNORMAL.name
        val currentTags = snapshots(
            nodeTagService.fetchNodeTags(projectId, setOf(target.nodeId))[target.nodeId].orEmpty()
        )
        val tags = currentTags.filter { it.tagKeyId > 0 }
        val currentTagMap = tagMap(currentTags)
        val matched = dynamicEnvMatcher.match(projectId, currentTagMap)
        return AgentReinstallContext(
            agentId = agentHashId,
            displayName = target.displayName,
            os = target.os,
            zone = target.zone,
            installType = null,
            agentType = target.agentType,
            parallelTaskCount = target.parallelTaskCount,
            tags = tags,
            preview = AgentInstallEnvironmentPreview(associated = matched.matchedEnvironments.toApi()),
            canReinstall = canEdit && isAbnormal,
            denyReason = when {
                !canEdit -> "No permission to edit this node"
                !isAbnormal -> "Only ABNORMAL nodes can be reinstalled"
                else -> null
            }
        )
    }

    private fun normalize(
        userId: String,
        projectId: String,
        request: AgentInstallSessionRequest,
        requireAbnormal: Boolean
    ): NormalizedConfig {
        if (request.parallelTaskCount < 0) {
            invalidParam("parallelTaskCount")
        }
        val mode = AgentInstallSessionMode.valueOf(request.mode.name)
        val tags = resolveTags(projectId, request)
        val installType = request.installType ?: TPAInstallType.SERVICE
        val loginName = request.loginName?.takeIf { it.isNotBlank() }
        val loginPassword = request.loginPassword?.takeIf { it.isNotBlank() }
        return if (mode == AgentInstallSessionMode.FIRST_IMPORT) {
            checkCreatePermission(userId, projectId)
            if (request.targetAgentId != null) invalidParam("targetAgentId")
            val agentType = request.agentType ?: AgentType.BUILD
            val gateway = slaveGatewayService.getGateway(request.zone) ?: ""
            val previewResult = dynamicEnvMatcher.previewFirstImport(projectId, request.os, tagMap(tags))
            val preview = AgentInstallSessionPreview(
                mode = request.mode,
                os = request.os,
                zone = request.zone,
                loginName = loginName,
                loginPasswordConfigured = loginPassword != null,
                installType = installType,
                agentType = agentType,
                parallelTaskCount = request.parallelTaskCount,
                tags = tags,
                targetAgentId = null,
                environments = AgentInstallEnvironmentPreview(
                    willJoin = previewResult.matchedEnvironments.toApi(),
                    pending = previewResult.pendingEnvironments.toApi()
                )
            )
            normalized(
                mode = mode,
                os = request.os,
                zone = request.zone,
                gateway = gateway,
                fileGateway = slaveGatewayService.getFileGateway(request.zone),
                loginName = loginName,
                loginPassword = loginPassword,
                installType = installType,
                agentType = agentType,
                parallelTaskCount = request.parallelTaskCount,
                targetAgentId = null,
                targetNodeId = null,
                tags = tags,
                preview = preview
            )
        } else {
            val targetAgentId = request.targetAgentId?.let(::decodeAgentId) ?: invalidParam("targetAgentId")
            val target = validateReinstallTarget(userId, projectId, targetAgentId, requireAbnormal)
            if (request.os != target.os) invalidParam("os")
            if (!request.zone.isNullOrBlank() && request.zone != target.zone) invalidParam("zone")
            if (request.agentType != null && request.agentType != target.agentType) invalidParam("agentType")
            val currentTags = snapshots(
                nodeTagService.fetchNodeTags(projectId, setOf(target.nodeId))[target.nodeId].orEmpty()
            )
            val proposedTags = currentTags.filter { it.tagKeyId < 0 } + tags
            val diff = dynamicEnvMatcher.diff(projectId, tagMap(currentTags), tagMap(proposedTags))
            val preview = AgentInstallSessionPreview(
                mode = request.mode,
                os = target.os,
                zone = target.zone,
                loginName = loginName,
                loginPasswordConfigured = loginPassword != null,
                installType = installType,
                agentType = target.agentType,
                parallelTaskCount = request.parallelTaskCount,
                tags = tags,
                targetAgentId = request.targetAgentId,
                environments = AgentInstallEnvironmentPreview(
                    associated = diff.unchangedEnvironments.toApi(),
                    willJoin = diff.joiningEnvironments.toApi(),
                    willLeave = diff.leavingEnvironments.toApi()
                )
            )
            normalized(
                mode = mode,
                os = target.os,
                zone = target.zone,
                gateway = target.gateway,
                fileGateway = target.fileGateway,
                loginName = loginName,
                loginPassword = loginPassword,
                installType = installType,
                agentType = target.agentType,
                parallelTaskCount = request.parallelTaskCount,
                targetAgentId = target.agentId,
                targetNodeId = target.nodeId,
                tags = tags,
                preview = preview
            )
        }
    }

    private fun normalized(
        mode: AgentInstallSessionMode,
        os: OS,
        zone: String?,
        gateway: String,
        fileGateway: String?,
        loginName: String?,
        loginPassword: String?,
        installType: TPAInstallType,
        agentType: AgentType,
        parallelTaskCount: Int,
        targetAgentId: Long?,
        targetNodeId: Long?,
        tags: List<AgentInstallTagSnapshot>,
        preview: AgentInstallSessionPreview
    ): NormalizedConfig {
        val normalizedLoginName = loginName?.takeIf { it.isNotBlank() }
        val normalizedLoginPassword = loginPassword?.takeIf { it.isNotBlank() }
        val canonical = buildString {
            appendCanonical(mode.name)
            appendCanonical(os.name)
            appendCanonical(zone)
            appendCanonical(gateway)
            appendCanonical(fileGateway)
            appendCanonical(normalizedLoginName)
            appendCanonical(normalizedLoginPassword?.let(::passwordFingerprint))
            appendCanonical(installType.name)
            appendCanonical(agentType.name)
            appendCanonical(parallelTaskCount.toString())
            appendCanonical(targetAgentId?.toString())
            appendCanonical(targetNodeId?.toString())
            tags.sortedWith(compareBy(AgentInstallTagSnapshot::tagKeyId, AgentInstallTagSnapshot::tagValueId)).forEach {
                appendCanonical(it.tagKeyId.toString())
                appendCanonical(it.tagKeyName)
                appendCanonical(it.tagValueId.toString())
                appendCanonical(it.tagValueName)
            }
        }
        return NormalizedConfig(
            mode = mode,
            os = os,
            zone = zone,
            gateway = gateway,
            fileGateway = fileGateway,
            loginName = normalizedLoginName,
            loginPasswordCipher = normalizedLoginPassword?.let { AESUtil.encrypt(encryptionKey, it) },
            installType = installType,
            agentType = agentType,
            parallelTaskCount = parallelTaskCount,
            targetAgentId = targetAgentId,
            targetNodeId = targetNodeId,
            tags = tags,
            fingerprint = ShaUtils.sha256(canonical),
            preview = preview
        )
    }

    private fun resolveTags(projectId: String, request: AgentInstallSessionRequest): List<AgentInstallTagSnapshot> {
        if (request.tags.any { it.tagKeyId <= 0 || it.tagValueId <= 0 }) invalidParam("tags")
        val availableTags = nodeTagService.fetchTagAndNodeCount(projectId, createMod = true)
        val tagLookup = availableTags.flatMap { tag ->
            tag.tagValues.map { value ->
                (tag.tagKeyId to value.tagValueId) to AgentInstallTagSnapshot(
                    tagKeyId = tag.tagKeyId,
                    tagKeyName = tag.tagKeyName,
                    tagValueId = value.tagValueId,
                    tagValueName = value.tagValueName
                )
            }
        }.toMap()
        val requested = request.tags.distinctBy { it.tagKeyId to it.tagValueId }
        val resolved = requested.map { tagLookup[it.tagKeyId to it.tagValueId] ?: invalidParam("tags") }
        val definitions = availableTags.associateBy(NodeTag::tagKeyId)
        resolved.groupBy(AgentInstallTagSnapshot::tagKeyId).forEach { (keyId, values) ->
            if (definitions[keyId]?.tagAllowMulValue == false && values.size > 1) invalidParam("tags")
        }
        return resolved.sortedWith(compareBy(AgentInstallTagSnapshot::tagKeyId, AgentInstallTagSnapshot::tagValueId))
    }

    private fun findReusableSession(
        projectId: String,
        userId: String,
        normalized: NormalizedConfig,
        now: LocalDateTime
    ): AgentInstallSession? = sessionDao.findActiveByFingerprint(
        dslContext = dslContext,
        projectId = projectId,
        createdBy = userId,
        mode = normalized.mode,
        targetAgentId = normalized.targetAgentId,
        targetNodeId = normalized.targetNodeId,
        configFingerprint = normalized.fingerprint,
        now = now
    )

    private fun createSession(
        normalized: NormalizedConfig,
        userId: String,
        projectId: String,
        now: LocalDateTime,
        previousSessionId: String?
    ): AgentInstallSessionCreateResponse {
        val sessionId = UUID.randomUUID().toString().replace("-", "")
        val token = UUID.randomUUID().toString().replace("-", "")
        val session = AgentInstallSession(
            id = sessionId,
            projectId = projectId,
            mode = normalized.mode,
            createdBy = userId,
            os = normalized.os.name,
            zone = normalized.zone,
            gateway = normalized.gateway,
            fileGateway = normalized.fileGateway,
            loginName = normalized.loginName,
            loginPasswordCipher = normalized.loginPasswordCipher,
            installType = normalized.installType.name,
            agentType = normalized.agentType.name,
            parallelTaskCount = normalized.parallelTaskCount,
            targetAgentId = normalized.targetAgentId,
            targetNodeId = normalized.targetNodeId,
            configFingerprint = normalized.fingerprint,
            tokenHash = ShaUtils.sha256(token),
            tokenCipher = SecurityUtil.encrypt(token),
            status = AgentInstallSessionStatus.ACTIVE,
            expiredTime = now.plusDays(expireDays),
            previousSessionId = previousSessionId,
            createdTime = now,
            updatedTime = now
        )
        sessionDao.create(
            dslContext = dslContext,
            session = session,
            tags = normalized.tags.map {
                AgentInstallSessionTag(
                    sessionId = sessionId,
                    tagKeyId = it.tagKeyId,
                    tagValueId = it.tagValueId,
                    tagKeyName = it.tagKeyName,
                    tagValueName = it.tagValueName
                )
            }
        )
        return AgentInstallSessionCreateResponse(
            sessionId = session.id,
            command = agentUrlService.genAgentSessionInstallScript(normalized.os, session.gateway, token),
            expiredAt = session.expiredTime,
            reused = false,
            preview = normalized.preview,
            summary = AgentInstallSessionSummary.EMPTY
        )
    }

    private fun toDetail(session: AgentInstallSession, now: LocalDateTime): AgentInstallSessionDetail {
        val status = if (session.status == AgentInstallSessionStatus.ACTIVE && session.expiredTime <= now) {
            sessionDao.markExpired(dslContext, session.id, now)
            AgentInstallSessionStatus.EXPIRED
        } else {
            session.status
        }
        return AgentInstallSessionDetail(
            sessionId = session.id,
            projectId = session.projectId,
            createdBy = session.createdBy,
            status = status.name,
            command = command(session),
            expiredAt = session.expiredTime,
            createdAt = session.createdTime,
            previousSessionId = session.previousSessionId,
            preview = preview(session),
            summary = summarize(sessionDao.listNodes(dslContext, session.id))
        )
    }

    private fun createResponse(session: AgentInstallSession, reused: Boolean): AgentInstallSessionCreateResponse {
        return AgentInstallSessionCreateResponse(
            sessionId = session.id,
            command = command(session),
            expiredAt = session.expiredTime,
            reused = reused,
            preview = preview(session),
            summary = summarize(sessionDao.listNodes(dslContext, session.id))
        )
    }

    private fun preview(session: AgentInstallSession): AgentInstallSessionPreview {
        val tags = sessionDao.listTags(dslContext, session.id).map { it.toSnapshot() }
        val environments = if (session.mode == AgentInstallSessionMode.FIRST_IMPORT) {
            val match = dynamicEnvMatcher.previewFirstImport(session.projectId, OS.valueOf(session.os), tagMap(tags))
            AgentInstallEnvironmentPreview(willJoin = match.matchedEnvironments.toApi(), pending = match.pendingEnvironments.toApi())
        } else {
            val currentTags = session.targetNodeId?.let { nodeId ->
                snapshots(nodeTagService.fetchNodeTags(session.projectId, setOf(nodeId))[nodeId].orEmpty())
            }.orEmpty()
            val proposedTags = currentTags.filter { it.tagKeyId < 0 } + tags
            val diff = dynamicEnvMatcher.diff(session.projectId, tagMap(currentTags), tagMap(proposedTags))
            AgentInstallEnvironmentPreview(
                associated = diff.unchangedEnvironments.toApi(),
                willJoin = diff.joiningEnvironments.toApi(),
                willLeave = diff.leavingEnvironments.toApi()
            )
        }
        return AgentInstallSessionPreview(
            mode = AgentInstallMode.valueOf(session.mode.name),
            os = OS.valueOf(session.os),
            zone = session.zone,
            loginName = session.loginName,
            loginPasswordConfigured = !session.loginPasswordCipher.isNullOrBlank(),
            installType = TPAInstallType.valueOf(session.installType),
            agentType = AgentType.valueOf(session.agentType),
            parallelTaskCount = session.parallelTaskCount,
            tags = tags,
            targetAgentId = session.targetAgentId?.let(HashUtil::encodeLongId),
            environments = environments
        )
    }

    private fun command(session: AgentInstallSession): String = agentUrlService.genAgentSessionInstallScript(
        os = OS.valueOf(session.os),
        gateway = session.gateway,
        token = SecurityUtil.decrypt(session.tokenCipher)
    )

    private fun summarize(nodes: List<AgentInstallSessionNode>): AgentInstallSessionSummary {
        val counts = nodes.groupingBy(AgentInstallSessionNode::status).eachCount()
        return AgentInstallSessionSummary(
            total = nodes.size,
            pending = counts[AgentInstallSessionNodeStatus.PENDING] ?: 0,
            installing = counts[AgentInstallSessionNodeStatus.INSTALLING] ?: 0,
            importing = counts[AgentInstallSessionNodeStatus.IMPORTING] ?: 0,
            succeeded = counts[AgentInstallSessionNodeStatus.SUCCEEDED] ?: 0,
            failed = counts[AgentInstallSessionNodeStatus.FAILED] ?: 0
        )
    }

    private fun snapshots(tags: List<NodeTag>): List<AgentInstallTagSnapshot> = tags.flatMap { tag ->
        tag.tagValues.map { value ->
            AgentInstallTagSnapshot(
                tagKeyId = tag.tagKeyId,
                tagKeyName = tag.tagKeyName,
                tagValueId = value.tagValueId,
                tagValueName = value.tagValueName
            )
        }
    }.sortedWith(compareBy(AgentInstallTagSnapshot::tagKeyId, AgentInstallTagSnapshot::tagValueId))

    private fun tagMap(tags: List<AgentInstallTagSnapshot>): Map<Long, Set<Long>> =
        tags.groupBy(AgentInstallTagSnapshot::tagKeyId)
            .mapValues { (_, values) -> values.map(AgentInstallTagSnapshot::tagValueId).toSet() }

    private fun List<InstallEnvItem>.toApi(): List<AgentInstallEnvironmentInfo> = map {
        AgentInstallEnvironmentInfo(envId = it.envHashId, envName = it.name)
    }

    private fun AgentInstallSessionTag.toSnapshot() = AgentInstallTagSnapshot(
        tagKeyId = tagKeyId,
        tagKeyName = tagKeyName,
        tagValueId = tagValueId,
        tagValueName = tagValueName
    )

    private fun validateReinstallTarget(
        userId: String,
        projectId: String,
        targetAgentId: Long?,
        requireAbnormal: Boolean,
        requireEditPermission: Boolean = true
    ): ReinstallTarget {
        val agentId = targetAgentId ?: invalidParam("targetAgentId")
        val agent = thirdPartyAgentDao.getAgentByProject(dslContext, agentId, projectId)
            ?: throw NotFoundException("The agent does not exist in this project")
        val nodeId = agent.nodeId ?: throw OperationException("The agent is not associated with a node")
        val node = nodeDao.get(dslContext, projectId, nodeId)
            ?: throw NotFoundException("The node associated with the agent does not exist in this project")
        if (requireEditPermission && !environmentPermissionService.checkNodePermission(
                userId = userId,
                projectId = projectId,
                nodeId = nodeId,
                permission = AuthPermission.EDIT
            )
        ) {
            throw PermissionForbiddenException(
                message = I18nUtil.getCodeLanMessage(EnvironmentMessageCode.ERROR_NODE_NO_EDIT_PERMISSSION)
            )
        }
        if (requireAbnormal && node.nodeStatus != NodeStatus.ABNORMAL.name) {
            throw OperationException("Only ABNORMAL nodes can be reinstalled")
        }
        return ReinstallTarget(
            agentId = agentId,
            nodeId = nodeId,
            displayName = node.displayName,
            nodeStatus = node.nodeStatus,
            os = OS.valueOf(agent.os),
            zone = slaveGatewayService.getZoneName(agent.gateway),
            gateway = agent.gateway,
            fileGateway = agent.fileGateway,
            agentType = AgentType.valueOf(agent.agentType ?: AgentType.BUILD.name),
            parallelTaskCount = agent.parallelTaskCount ?: 0
        )
    }

    private fun checkCreatePermission(userId: String, projectId: String) {
        if (!environmentPermissionService.checkNodePermission(userId, projectId, AuthPermission.CREATE)) {
            throw PermissionForbiddenException(
                message = I18nUtil.getCodeLanMessage(EnvironmentMessageCode.ERROR_NODE_NO_CREATE_PERMISSSION)
            )
        }
    }

    private fun checkViewPermission(userId: String, projectId: String) = checkCreatePermission(userId, projectId)

    private fun getSession(projectId: String, sessionId: String): AgentInstallSession =
        sessionDao.getById(dslContext, projectId, sessionId) ?: throw NotFoundException("Install session does not exist")

    private fun decodeAgentId(agentId: String): Long = try {
        HashUtil.decodeIdToLong(agentId)
    } catch (_: Exception) {
        throw NotFoundException("The agent does not exist")
    }

    private fun StringBuilder.appendCanonical(value: String?) {
        val normalized = value.orEmpty()
        append(normalized.length).append(':').append(normalized).append('|')
    }

    private fun passwordFingerprint(password: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(encryptionKey.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        return mac.doFinal(password.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }

    private fun invalidParam(name: String): Nothing = throw ErrorCodeException(
        errorCode = CommonMessageCode.ERROR_INVALID_PARAM_,
        params = arrayOf(name)
    )

    private data class NormalizedConfig(
        val mode: AgentInstallSessionMode,
        val os: OS,
        val zone: String?,
        val gateway: String,
        val fileGateway: String?,
        val loginName: String?,
        val loginPasswordCipher: String?,
        val installType: TPAInstallType,
        val agentType: AgentType,
        val parallelTaskCount: Int,
        val targetAgentId: Long?,
        val targetNodeId: Long?,
        val tags: List<AgentInstallTagSnapshot>,
        val fingerprint: String,
        val preview: AgentInstallSessionPreview
    )

    private data class ReinstallTarget(
        val agentId: Long,
        val nodeId: Long,
        val displayName: String,
        val nodeStatus: String,
        val os: OS,
        val zone: String?,
        val gateway: String,
        val fileGateway: String?,
        val agentType: AgentType,
        val parallelTaskCount: Int
    )

    private companion object {
        const val CREATE_LOCK_EXPIRE_SECONDS = 10L
        const val DEFAULT_PAGE = 1
        const val DEFAULT_PAGE_SIZE = 20
        const val MAX_PAGE_SIZE = 100
    }
}
