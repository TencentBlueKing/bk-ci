package com.tencent.devops.remotedev.service.thirdparty

import com.tencent.devops.common.api.constant.CommonMessageCode.PARAMETER_ILLEGAL_ERROR
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.ShaUtils
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.remotedev.dao.WorkspaceDao
import com.tencent.devops.remotedev.dao.WorkspaceJoinDao
import com.tencent.devops.remotedev.dispatch.kubernetes.service.factory.RemoteDevServiceFactory
import com.tencent.devops.remotedev.pojo.WorkspaceMountType
import com.tencent.devops.remotedev.pojo.WorkspaceOwnerType
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import com.tencent.devops.remotedev.pojo.expert.WorkspaceTaskStatus
import com.tencent.devops.remotedev.pojo.op.OpProjectWorkspaceAssignData
import com.tencent.devops.remotedev.pojo.remotedev.CreateCvmResp
import com.tencent.devops.remotedev.pojo.remotedev.RegisterVmData
import com.tencent.devops.remotedev.pojo.thirdparty.ThirdPartyDesktopBinding
import com.tencent.devops.remotedev.pojo.thirdparty.ThirdPartyDesktopCdsKeyResp
import com.tencent.devops.remotedev.pojo.thirdparty.ThirdPartyDesktopTokenInfo
import com.tencent.devops.remotedev.pojo.thirdparty.ThirdPartyDesktopTokenResp
import com.tencent.devops.remotedev.service.PermissionService
import com.tencent.devops.remotedev.service.client.StartCloudClient
import com.tencent.devops.remotedev.service.workspace.CreateControl
import jakarta.ws.rs.core.Response
import java.security.SecureRandom
import java.util.Base64
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

/**
 * 一个 token 的注册记录，存 Redis
 * 后续 taskStatus / cdsKey / assign 接口只允许操作这里记录的资源
 */
data class ThirdPartyDesktopRegisterRecord(
    val ip: String,
    /* 重装 token 没有注册任务，为 null */
    val taskId: String?,
    val envId: String?,
    /* 用于重复调用 register 时原样返回 */
    val taskUid: String? = null
)

/**
 * 第三方机器注册为云桌面相关能力
 *
 * token 为 32 位随机串（24 字节 SecureRandom，base64url），本身不携带任何信息
 * userId/os/zoneId/projectId 存在 Redis 中，key 使用 token 的 sha256，Redis 中不落 token 明文
 * 一个 token 只能注册一台机器，后续接口的入参必须属于该 token 的注册记录
 *
 * 重装场景：根据已有 cdsId 签发 token，签发时直接写入注册记录（envId = cdsId），不能注册新机器；
 * 机器未分配时允许分配，已分配则拒绝（避免重复加项目配额）
 *
 * 注册失败重试：BCS 调用失败直接释放占位；BCS 任务失败(failed/abort)后，同一个 token 可以再注册一次
 */
@Service
class ThirdPartyDesktopService @Autowired constructor(
    private val remoteDevServiceFactory: RemoteDevServiceFactory,
    private val startCloudClient: StartCloudClient,
    private val createControl: CreateControl,
    private val redisOperation: RedisOperation,
    private val permissionService: PermissionService,
    private val dslContext: DSLContext,
    private val workspaceJoinDao: WorkspaceJoinDao,
    private val workspaceDao: WorkspaceDao
) {

    /* 第三方云桌面 token 有效期（秒），默认 1 小时 */
    @Value("\${remoteDev.thirdPartyDesktop.expireSeconds:3600}")
    private val expireSeconds: Long = DEFAULT_EXPIRE_SECONDS

    /* cdsId 与注册人的绑定有效期（天），用于未分配机器的重装，默认 30 天 */
    @Value("\${remoteDev.thirdPartyDesktop.bindingExpireDays:30}")
    private val bindingExpireDays: Long = DEFAULT_BINDING_EXPIRE_DAYS

    private val secureRandom = SecureRandom()

    /**
     * 校验通过的 token：id 为 token 的 sha256，用于拼接各个 Redis key
     */
    private data class VerifiedToken(
        val id: String,
        val info: ThirdPartyDesktopTokenInfo
    )

    fun genToken(
        userId: String,
        projectId: String,
        os: String,
        zoneId: String
    ): ThirdPartyDesktopTokenResp {
        if (userId.isBlank() || projectId.isBlank() || zoneId.isBlank()) {
            throw illegal("userId/projectId/zoneId can not be blank")
        }
        val osEnum = OS.parse(os.uppercase()) ?: throw illegal("os $os is not supported")
        // token 后续可以直接交付机器、增加项目配额，签发前必须确认是项目成员
        if (!permissionService.checkUserVisitPermission(userId = userId, projectCode = projectId)) {
            logger.warn("genToken|$userId is not member of project $projectId")
            throw forbidden("user $userId is not member of project $projectId")
        }
        val expireTime = System.currentTimeMillis() + expireSeconds * 1000
        val info = ThirdPartyDesktopTokenInfo(
            userId = userId,
            os = osEnum.name,
            zoneId = zoneId,
            projectId = projectId,
            expireTime = expireTime
        )
        val (token, tokenId) = issueToken(info)
        logger.info("genToken|$userId|$projectId|${osEnum.name}|$zoneId|$expireTime|${tokenId.take(LOG_ID_LENGTH)}")
        return ThirdPartyDesktopTokenResp(token = token, expireTime = expireTime)
    }

    /**
     * 重装场景：根据已注册的 cdsId 签发 token
     * 已分配：按工作空间记录校验，用户需是拥有者或项目管理员
     * 未分配：还没有工作空间记录，按注册时写入的 cdsId 绑定校验，用户需是注册人或项目管理员
     * 签发时直接写入注册记录 envId = cdsId，后续 cdskey / assign 接口复用原有归属校验
     */
    fun genReinstallToken(
        userId: String,
        cdsId: String
    ): ThirdPartyDesktopTokenResp {
        if (userId.isBlank() || cdsId.isBlank()) {
            throw illegal("userId/cdsId can not be blank")
        }
        // T_WORKSPACE_WINDOWS.HOST_IP 存的就是 cgsId(即 cdsId)
        val ws = workspaceJoinDao.fetchWindowsWorkspacesSimple(
            dslContext = dslContext,
            ip = cdsId,
            notStatus = listOf(WorkspaceStatus.DELETED, WorkspaceStatus.DELIVERING_FAILED)
        ).maxByOrNull { it.createTime }
        val expireTime = System.currentTimeMillis() + expireSeconds * 1000
        val (info, ip) = if (ws != null) {
            if (!permissionService.hasManagerOrOwnerPermission(
                    userId = userId,
                    projectId = ws.projectId,
                    workspaceName = ws.workspaceName,
                    ownerType = ws.ownerType
                )
            ) {
                logger.warn("genReinstallToken|no permission|$userId|$cdsId|${ws.workspaceName}|${ws.projectId}")
                throw forbidden("user $userId is not owner of $cdsId or manager of project ${ws.projectId}")
            }
            ThirdPartyDesktopTokenInfo(
                userId = userId,
                os = ws.os?.name ?: "",
                zoneId = ws.zoneId?.ifBlank { null } ?: cdsId.substringBefore("."),
                projectId = ws.projectId,
                expireTime = expireTime,
                reinstall = true
            ) to (ws.ip ?: "")
        } else {
            val binding = getBinding(cdsId)
            if (binding == null) {
                logger.warn("genReinstallToken|workspace and binding not found|$userId|$cdsId")
                throw notFound("no machine found by cdsId $cdsId")
            }
            if (binding.userId != userId && !permissionService.hasUserManager(userId, binding.projectId)) {
                logger.warn("genReinstallToken|no permission|$userId|$cdsId|${binding.userId}|${binding.projectId}")
                throw forbidden("user $userId is not register of $cdsId or manager of project ${binding.projectId}")
            }
            ThirdPartyDesktopTokenInfo(
                userId = userId,
                os = binding.os,
                zoneId = binding.zoneId,
                projectId = binding.projectId,
                expireTime = expireTime,
                reinstall = true,
                // 未分配的机器后续分配时，拥有者仍是注册人
                assignOwner = binding.userId
            ) to binding.ip
        }
        val (token, tokenId) = issueToken(info)
        redisOperation.set(
            key = registerKey(tokenId),
            value = JsonUtil.toJson(
                ThirdPartyDesktopRegisterRecord(ip = ip, taskId = null, envId = cdsId),
                formatted = false
            ),
            expiredInSecond = expireSeconds
        )
        logger.info(
            "genReinstallToken|$userId|${info.projectId}|${ws?.workspaceName}|$cdsId|$expireTime|" +
                tokenId.take(LOG_ID_LENGTH)
        )
        return ThirdPartyDesktopTokenResp(token = token, expireTime = expireTime)
    }

    /**
     * 生成随机 token 并写入 Redis，返回 token 明文和 tokenId(sha256)
     */
    private fun issueToken(info: ThirdPartyDesktopTokenInfo): Pair<String, String> {
        val token = randomToken()
        val tokenId = ShaUtils.sha256(token)
        // 碰撞概率可忽略，用 setIfAbsent 兜底，保证不会覆盖已有 token
        if (!redisOperation.setIfAbsent(tokenKey(tokenId), JsonUtil.toJson(info, formatted = false), expireSeconds)) {
            logger.error("issueToken|token conflict|${info.userId}|${info.projectId}")
            throw ErrorCodeException(
                statusCode = Response.Status.INTERNAL_SERVER_ERROR.statusCode,
                errorCode = PARAMETER_ILLEGAL_ERROR,
                defaultMessage = "genToken failed, please retry",
                params = arrayOf("token", "genToken failed, please retry")
            )
        }
        return token to tokenId
    }

    /**
     * 校验 token：格式合法且 Redis 中存在未过期的记录，否则抛 401
     */
    private fun verifyToken(token: String?): VerifiedToken {
        if (token.isNullOrBlank()) {
            throw unauthorized("token is empty")
        }
        if (!TOKEN_REGEX.matches(token)) {
            throw unauthorized("token illegal")
        }
        val tokenId = ShaUtils.sha256(token)
        val value = redisOperation.get(tokenKey(tokenId))
        if (value.isNullOrBlank()) {
            logger.warn("verifyToken|token not found or expired|${tokenId.take(LOG_ID_LENGTH)}")
            throw unauthorized("token illegal or expired")
        }
        val info = try {
            JsonUtil.to(value, ThirdPartyDesktopTokenInfo::class.java)
        } catch (e: Throwable) {
            logger.warn("verifyToken|parse failed|${tokenId.take(LOG_ID_LENGTH)}|${e.message}")
            throw unauthorized("token illegal")
        }
        if (info.expireTime < System.currentTimeMillis()) {
            logger.warn("verifyToken|token expired|${info.userId}|${info.expireTime}")
            throw unauthorized("token expired")
        }
        return VerifiedToken(id = tokenId, info = info)
    }

    /**
     * 一个 token 只能注册一台机器：先用 setIfAbsent 抢占，成功后记录 taskId/envId
     * 已注册（有 taskId/envId）时不查任务状态，直接返回已有结果，不再调用 BCS；
     * 仅作用于第三方机器注册，不影响其他机器类型
     * BCS 调用报错或返回空会释放占位，允许同一个 token 重试；BCS 任务执行失败需重新签发 token 再注册
     */
    fun registerVm(token: String?, ip: String): CreateCvmResp? {
        val verified = verifyToken(token)
        val info = verified.info
        if (info.reinstall) {
            logger.warn("registerVm|reinstall token can not register|${info.userId}|$ip")
            throw forbidden("reinstall token can not register a new machine")
        }
        if (ip.isBlank()) {
            throw illegal("ip can not be blank")
        }
        val recordKey = registerKey(verified.id)
        val ttl = remainSeconds(info)
        if (!redisOperation.setIfAbsent(recordKey, REGISTERING, ttl)) {
            return existedRegister(verified, ip)
        }
        logger.info("registerVm|${info.userId}|${info.projectId}|${info.os}|${info.zoneId}|$ip")
        val resp = try {
            remoteDevServiceFactory.loadRemoteDevService(WorkspaceMountType.BCS).registerVm(
                RegisterVmData(
                    internal = true,
                    ip = ip,
                    os = info.os,
                    machineType = DEFAULT_MACHINE_TYPE,
                    zoneId = info.zoneId,
                    skipInit = true
                )
            )
        } catch (e: Throwable) {
            redisOperation.delete(recordKey)
            throw e
        }
        if (resp == null) {
            redisOperation.delete(recordKey)
            logger.warn("registerVm|bcs return null|${info.userId}|$ip")
            return null
        }
        if (resp.environmentUid.isNullOrBlank()) {
            logger.warn("registerVm|bcs return empty environmentUid|${info.userId}|$ip|${resp.taskID}")
        } else {
            // 长期记录 cdsId 归属，未分配就重装时用于签发重装 token
            redisOperation.set(
                key = bindingKey(resp.environmentUid!!),
                value = JsonUtil.toJson(
                    ThirdPartyDesktopBinding(
                        userId = info.userId,
                        projectId = info.projectId,
                        os = info.os,
                        zoneId = info.zoneId,
                        ip = ip
                    ),
                    formatted = false
                ),
                expiredInSecond = bindingExpireDays * SECONDS_PER_DAY
            )
        }
        redisOperation.set(
            key = recordKey,
            value = JsonUtil.toJson(
                ThirdPartyDesktopRegisterRecord(
                    ip = ip,
                    taskId = resp.taskID,
                    envId = resp.environmentUid,
                    taskUid = resp.taskUid
                ),
                formatted = false
            ),
            expiredInSecond = ttl
        )
        return resp
    }

    /**
     * 已有注册记录：不查任务状态，有 taskId 或 envId 就直接返回已有结果
     * 注册中（getRecord 抛 403 稍后重试）或记录里两者都为空时拒绝
     */
    private fun existedRegister(verified: VerifiedToken, ip: String): CreateCvmResp {
        val info = verified.info
        val record = getRecord(verified)
        val envId = record.envId?.ifBlank { null }
        val taskId = record.taskId?.ifBlank { null }
        if (envId == null && taskId == null) {
            throw forbidden("token has already been used to register a machine")
        }
        logger.info(
            "registerVm|already registered, return existed|${info.userId}|$envId|$taskId|" +
                "recordIp=${record.ip}|reqIp=$ip"
        )
        return CreateCvmResp(environmentUid = envId, taskID = taskId ?: "", taskUid = record.taskUid ?: "")
    }

    fun getTaskStatus(token: String?, taskId: String): WorkspaceTaskStatus? {
        val verified = verifyToken(token)
        if (taskId.isBlank()) {
            throw illegal("taskId can not be blank")
        }
        val record = getRecord(verified)
        if (record.taskId != taskId) {
            logger.warn("getTaskStatus|taskId not belong to token|${verified.info.userId}|$taskId|${record.taskId}")
            throw forbidden("taskId $taskId does not belong to this token")
        }
        logger.info("getTaskStatus|${verified.info.userId}|$taskId")
        return remoteDevServiceFactory.loadRemoteDevService(WorkspaceMountType.BCS).taskStatus(taskId)
    }

    /**
     * envId 必须是本 token 注册出来的；CDS apply 会把 vmId 与 envId(cdsId) 绑定，
     * 已绑定到其他 cdsId 的 vmId 会被 CDS 拒绝，同一组 vmId + envId 重复调用返回同一个 key
     */
    fun getCdsKey(token: String?, vmId: String, envId: String): ThirdPartyDesktopCdsKeyResp {
        val verified = verifyToken(token)
        if (vmId.isBlank() || envId.isBlank()) {
            throw illegal("vmId/envId can not be blank")
        }
        checkEnvBelong(verified, envId, "getCdsKey")
        logger.info("getCdsKey|${verified.info.userId}|$vmId|$envId")
        return ThirdPartyDesktopCdsKeyResp(cdsKey = startCloudClient.applyCdsKey(envId = envId, vmId = vmId))
    }

    /**
     * envId 必须是本 token 注册（或重装 token 绑定）的，且 cds 未分配
     * 一个 token 只能分配一次（避免重复增加项目配额）
     */
    fun assignWorkspace(token: String?, envId: String): Boolean {
        val verified = verifyToken(token)
        val info = verified.info
        if (envId.isBlank()) {
            throw illegal("envId can not be blank")
        }
        checkEnvBelong(verified, envId, "assignWorkspace")
        // createControl.assignWorkspace 会先给项目加配额再跳过已分配的 cgsId，必须在这里提前拦截
        if (workspaceDao.getAvailableCgsWorkspace(dslContext = dslContext, cgsId = envId) > 0) {
            logger.warn("assignWorkspace|cds already assigned|${info.userId}|$envId|reinstall=${info.reinstall}")
            throw forbidden("env $envId has already been assigned")
        }
        val assignKey = assignKey(verified.id)
        if (!redisOperation.setIfAbsent(assignKey, envId, remainSeconds(info))) {
            logger.warn("assignWorkspace|already assigned|${info.userId}|$envId")
            throw forbidden("env $envId has already been assigned by this token")
        }
        logger.info("assignWorkspace|${info.userId}|${info.projectId}|${info.os}|$envId")
        try {
            createControl.assignWorkspace(
                userId = info.userId,
                data = OpProjectWorkspaceAssignData(
                    projectId = info.projectId,
                    owner = null,
                    cgsIds = listOf(envId),
                    ips = null,
                    repoId = null,
                    localDriver = null,
                    type = WorkspaceOwnerType.PROJECT,
                    os = OS.parse(info.os)
                ),
                assignOwner = info.assignOwner ?: info.userId
            )
        } catch (e: Throwable) {
            // 分配失败释放占位，允许重试
            redisOperation.delete(assignKey)
            throw e
        }
        return true
    }

    private fun checkEnvBelong(verified: VerifiedToken, envId: String, action: String) {
        val record = getRecord(verified)
        if (record.envId.isNullOrBlank() || record.envId != envId) {
            logger.warn("$action|envId not belong to token|${verified.info.userId}|$envId|${record.envId}")
            throw forbidden("env $envId does not belong to this token")
        }
    }

    private fun getRecord(verified: VerifiedToken): ThirdPartyDesktopRegisterRecord {
        val value = redisOperation.get(registerKey(verified.id))
        if (value.isNullOrBlank()) {
            throw forbidden("no machine registered by this token")
        }
        if (value == REGISTERING) {
            throw forbidden("machine is registering, please retry later")
        }
        return try {
            JsonUtil.to(value, ThirdPartyDesktopRegisterRecord::class.java)
        } catch (e: Throwable) {
            logger.warn("getRecord|parse failed|${verified.info.userId}|${e.message}")
            throw forbidden("no machine registered by this token")
        }
    }

    private fun randomToken(): String {
        val bytes = ByteArray(TOKEN_BYTES)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    /* 注册/分配记录与 token 同生命周期，至少保留 1 分钟 */
    private fun remainSeconds(info: ThirdPartyDesktopTokenInfo): Long {
        return maxOf((info.expireTime - System.currentTimeMillis()) / 1000 + 1, MIN_RECORD_SECONDS)
    }

    private fun getBinding(cdsId: String): ThirdPartyDesktopBinding? {
        val value = redisOperation.get(bindingKey(cdsId))?.ifBlank { null } ?: return null
        return try {
            JsonUtil.to(value, ThirdPartyDesktopBinding::class.java)
        } catch (e: Throwable) {
            logger.warn("getBinding|parse failed|$cdsId|${e.message}")
            null
        }
    }

    private fun tokenKey(tokenId: String) = "$REDIS_KEY_PREFIX:token:$tokenId"

    private fun registerKey(tokenId: String) = "$REDIS_KEY_PREFIX:register:$tokenId"

    private fun assignKey(tokenId: String) = "$REDIS_KEY_PREFIX:assign:$tokenId"

    private fun bindingKey(cdsId: String) = "$REDIS_KEY_PREFIX:binding:$cdsId"

    private fun unauthorized(msg: String) = ErrorCodeException(
        statusCode = Response.Status.UNAUTHORIZED.statusCode,
        errorCode = PARAMETER_ILLEGAL_ERROR,
        defaultMessage = msg,
        params = arrayOf("token", msg)
    )

    private fun forbidden(msg: String) = ErrorCodeException(
        statusCode = Response.Status.FORBIDDEN.statusCode,
        errorCode = PARAMETER_ILLEGAL_ERROR,
        defaultMessage = msg,
        params = arrayOf("permission", msg)
    )

    private fun notFound(msg: String) = ErrorCodeException(
        statusCode = Response.Status.NOT_FOUND.statusCode,
        errorCode = PARAMETER_ILLEGAL_ERROR,
        defaultMessage = msg,
        params = arrayOf("cdsId", msg)
    )

    private fun illegal(msg: String) = ErrorCodeException(
        statusCode = Response.Status.BAD_REQUEST.statusCode,
        errorCode = PARAMETER_ILLEGAL_ERROR,
        defaultMessage = msg,
        params = arrayOf("param", msg)
    )

    companion object {
        private val logger = LoggerFactory.getLogger(ThirdPartyDesktopService::class.java)
        private const val DEFAULT_EXPIRE_SECONDS = 3600L
        private const val DEFAULT_BINDING_EXPIRE_DAYS = 30L
        private const val SECONDS_PER_DAY = 24 * 3600L
        private const val MIN_RECORD_SECONDS = 60L
        private const val DEFAULT_MACHINE_TYPE = "XL2"
        private const val REDIS_KEY_PREFIX = "remotedev:third_party_desktop"
        private const val REGISTERING = "REGISTERING"

        /* 24 字节随机数，base64url 无填充后固定 32 位 */
        private const val TOKEN_BYTES = 24
        private val TOKEN_REGEX = Regex("^[A-Za-z0-9_-]{32}$")
        private const val LOG_ID_LENGTH = 8
    }
}
