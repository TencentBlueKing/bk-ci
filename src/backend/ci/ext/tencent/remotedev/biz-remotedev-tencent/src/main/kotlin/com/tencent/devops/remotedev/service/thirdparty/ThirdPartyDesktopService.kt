package com.tencent.devops.remotedev.service.thirdparty

import com.tencent.devops.common.api.constant.CommonMessageCode.PARAMETER_ILLEGAL_ERROR
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.ShaUtils
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.remotedev.dispatch.kubernetes.service.factory.RemoteDevServiceFactory
import com.tencent.devops.remotedev.pojo.WorkspaceMountType
import com.tencent.devops.remotedev.pojo.WorkspaceOwnerType
import com.tencent.devops.remotedev.pojo.expert.WorkspaceTaskStatus
import com.tencent.devops.remotedev.pojo.op.OpProjectWorkspaceAssignData
import com.tencent.devops.remotedev.pojo.remotedev.CreateCvmResp
import com.tencent.devops.remotedev.pojo.remotedev.RegisterVmData
import com.tencent.devops.remotedev.pojo.thirdparty.ThirdPartyDesktopCdsKeyResp
import com.tencent.devops.remotedev.pojo.thirdparty.ThirdPartyDesktopTokenInfo
import com.tencent.devops.remotedev.pojo.thirdparty.ThirdPartyDesktopTokenResp
import com.tencent.devops.remotedev.service.PermissionService
import com.tencent.devops.remotedev.service.client.StartCloudClient
import com.tencent.devops.remotedev.service.workspace.CreateControl
import jakarta.ws.rs.core.Response
import java.security.SecureRandom
import java.util.Base64
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
    val taskId: String,
    val envId: String?
)

/**
 * 第三方机器注册为云桌面相关能力
 *
 * token 为 32 位随机串（24 字节 SecureRandom，base64url），本身不携带任何信息
 * userId/os/zoneId/projectId 存在 Redis 中，key 使用 token 的 sha256，Redis 中不落 token 明文
 * 一个 token 只能注册一台机器，后续接口的入参必须属于该 token 的注册记录
 */
@Service
class ThirdPartyDesktopService @Autowired constructor(
    private val remoteDevServiceFactory: RemoteDevServiceFactory,
    private val startCloudClient: StartCloudClient,
    private val createControl: CreateControl,
    private val redisOperation: RedisOperation,
    private val permissionService: PermissionService
) {

    /* 第三方云桌面 token 有效期（秒），默认 1 小时 */
    @Value("\${remoteDev.thirdPartyDesktop.expireSeconds:3600}")
    private val expireSeconds: Long = DEFAULT_EXPIRE_SECONDS

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
        val token = randomToken()
        val tokenId = ShaUtils.sha256(token)
        // 碰撞概率可忽略，用 setIfAbsent 兜底，保证不会覆盖已有 token
        if (!redisOperation.setIfAbsent(tokenKey(tokenId), JsonUtil.toJson(info, formatted = false), expireSeconds)) {
            logger.error("genToken|token conflict|$userId|$projectId")
            throw ErrorCodeException(
                statusCode = Response.Status.INTERNAL_SERVER_ERROR.statusCode,
                errorCode = PARAMETER_ILLEGAL_ERROR,
                defaultMessage = "genToken failed, please retry",
                params = arrayOf("token", "genToken failed, please retry")
            )
        }
        logger.info("genToken|$userId|$projectId|${osEnum.name}|$zoneId|$expireTime|${tokenId.take(LOG_ID_LENGTH)}")
        return ThirdPartyDesktopTokenResp(token = token, expireTime = expireTime)
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
     * BCS 调用失败会释放占位，允许同一个 token 重试
     */
    fun registerVm(token: String?, ip: String): CreateCvmResp? {
        val verified = verifyToken(token)
        val info = verified.info
        if (ip.isBlank()) {
            throw illegal("ip can not be blank")
        }
        val recordKey = registerKey(verified.id)
        val ttl = remainSeconds(info)
        if (!redisOperation.setIfAbsent(recordKey, REGISTERING, ttl)) {
            logger.warn("registerVm|token already used|${info.userId}|$ip")
            throw forbidden("token has already been used to register a machine")
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
        }
        redisOperation.set(
            key = recordKey,
            value = JsonUtil.toJson(
                ThirdPartyDesktopRegisterRecord(ip = ip, taskId = resp.taskID, envId = resp.environmentUid),
                formatted = false
            ),
            expiredInSecond = ttl
        )
        return resp
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
     * envId 必须是本 token 注册出来的，且一个 token 只能分配一次（避免重复增加项目配额）
     */
    fun assignWorkspace(token: String?, envId: String): Boolean {
        val verified = verifyToken(token)
        val info = verified.info
        if (envId.isBlank()) {
            throw illegal("envId can not be blank")
        }
        checkEnvBelong(verified, envId, "assignWorkspace")
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
                assignOwner = info.userId
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

    private fun tokenKey(tokenId: String) = "$REDIS_KEY_PREFIX:token:$tokenId"

    private fun registerKey(tokenId: String) = "$REDIS_KEY_PREFIX:register:$tokenId"

    private fun assignKey(tokenId: String) = "$REDIS_KEY_PREFIX:assign:$tokenId"

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

    private fun illegal(msg: String) = ErrorCodeException(
        statusCode = Response.Status.BAD_REQUEST.statusCode,
        errorCode = PARAMETER_ILLEGAL_ERROR,
        defaultMessage = msg,
        params = arrayOf("param", msg)
    )

    companion object {
        private val logger = LoggerFactory.getLogger(ThirdPartyDesktopService::class.java)
        private const val DEFAULT_EXPIRE_SECONDS = 3600L
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
