package com.tencent.devops.remotedev.service.thirdparty

import com.tencent.devops.common.api.constant.CommonMessageCode.PARAMETER_ILLEGAL_ERROR
import com.tencent.devops.common.api.constant.CommonMessageCode.PARAMETER_SECRET_ERROR
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.util.AESUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.UUIDUtil
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
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

/**
 * 一个 token 的注册记录，存 Redis，key 为 token 中的 nonce
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
 * token 格式：{AES 密文}.{HMAC-SHA256(密文)}，先验签再解密，防止密文被篡改
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

    /* 第三方云桌面 token 加解密及签名密钥 */
    @Value("\${remoteDev.thirdPartyDesktop.secret:#{null}}")
    private val secret: String? = null

    /* 第三方云桌面 token 有效期（秒），默认 1 小时 */
    @Value("\${remoteDev.thirdPartyDesktop.expireSeconds:3600}")
    private val expireSeconds: Long = DEFAULT_EXPIRE_SECONDS

    fun genToken(
        userId: String,
        projectId: String,
        os: String,
        zoneId: String
    ): ThirdPartyDesktopTokenResp {
        val key = checkSecret()
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
            expireTime = expireTime,
            nonce = UUIDUtil.generate()
        )
        logger.info("genToken|$userId|$projectId|${osEnum.name}|$zoneId|$expireTime|${info.nonce}")
        val token = try {
            val cipherText = AESUtil.encrypt(key, JsonUtil.toJson(info, formatted = false))
            "$cipherText$TOKEN_SEPARATOR${sign(key, cipherText)}"
        } catch (e: Throwable) {
            logger.error("genToken|encrypt failed", e)
            throw ErrorCodeException(
                statusCode = Response.Status.INTERNAL_SERVER_ERROR.statusCode,
                errorCode = PARAMETER_SECRET_ERROR,
                defaultMessage = "genToken failed because encrypt failed",
                params = arrayOf("encrypt", "genToken failed because encrypt failed")
            )
        }
        return ThirdPartyDesktopTokenResp(token = token, expireTime = expireTime)
    }

    /**
     * 校验 token 并返回 token 中的信息：先验 HMAC 签名，再解密，最后校验过期时间
     * 非法或过期直接抛 401
     */
    fun verifyToken(token: String?): ThirdPartyDesktopTokenInfo {
        val key = checkSecret()
        if (token.isNullOrBlank()) {
            throw unauthorized("token is empty")
        }
        val parts = token.split(TOKEN_SEPARATOR)
        if (parts.size != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            throw unauthorized("token illegal")
        }
        val (cipherText, signature) = parts
        val expected = sign(key, cipherText)
        if (!MessageDigest.isEqual(
                expected.toByteArray(Charsets.UTF_8),
                signature.toByteArray(Charsets.UTF_8)
            )
        ) {
            logger.warn("verifyToken|token signature mismatch")
            throw unauthorized("token illegal")
        }
        val info = try {
            JsonUtil.to(AESUtil.decrypt(key, cipherText), ThirdPartyDesktopTokenInfo::class.java)
        } catch (e: Throwable) {
            logger.warn("verifyToken|token illegal|${e.message}")
            throw unauthorized("token illegal")
        }
        if (info.nonce.isBlank() || info.userId.isBlank() || info.projectId.isBlank()) {
            throw unauthorized("token illegal")
        }
        if (info.expireTime < System.currentTimeMillis()) {
            logger.warn("verifyToken|token expired|${info.userId}|${info.expireTime}")
            throw unauthorized("token expired")
        }
        return info
    }

    /**
     * 一个 token 只能注册一台机器：先用 setIfAbsent 抢占 nonce，成功后记录 taskId/envId
     * BCS 调用失败会释放占位，允许同一个 token 重试
     */
    fun registerVm(token: String?, ip: String): CreateCvmResp? {
        val info = verifyToken(token)
        if (ip.isBlank()) {
            throw illegal("ip can not be blank")
        }
        val recordKey = registerKey(info.nonce)
        val ttl = remainSeconds(info)
        if (!redisOperation.setIfAbsent(recordKey, REGISTERING, ttl)) {
            logger.warn("registerVm|token already used|${info.userId}|${info.nonce}|$ip")
            throw forbidden("token has already been used to register a machine")
        }
        logger.info("registerVm|${info.userId}|${info.projectId}|${info.os}|${info.zoneId}|$ip|${info.nonce}")
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
        val info = verifyToken(token)
        if (taskId.isBlank()) {
            throw illegal("taskId can not be blank")
        }
        val record = getRecord(info)
        if (record.taskId != taskId) {
            logger.warn("getTaskStatus|taskId not belong to token|${info.userId}|$taskId|${record.taskId}")
            throw forbidden("taskId $taskId does not belong to this token")
        }
        logger.info("getTaskStatus|${info.userId}|$taskId")
        return remoteDevServiceFactory.loadRemoteDevService(WorkspaceMountType.BCS).taskStatus(taskId)
    }

    /**
     * envId 必须是本 token 注册出来的；vmId 由 CDS 校验其归属的 cdsId 与 envId 一致
     */
    fun getCdsKey(token: String?, vmId: String, envId: String): ThirdPartyDesktopCdsKeyResp {
        val info = verifyToken(token)
        if (vmId.isBlank() || envId.isBlank()) {
            throw illegal("vmId/envId can not be blank")
        }
        checkEnvBelong(info, envId, "getCdsKey")
        logger.info("getCdsKey|${info.userId}|$vmId|$envId")
        return ThirdPartyDesktopCdsKeyResp(cdsKey = startCloudClient.exchangeCdsKey(envId = envId, vmId = vmId))
    }

    /**
     * envId 必须是本 token 注册出来的，且一个 token 只能分配一次（避免重复增加项目配额）
     */
    fun assignWorkspace(token: String?, envId: String): Boolean {
        val info = verifyToken(token)
        if (envId.isBlank()) {
            throw illegal("envId can not be blank")
        }
        checkEnvBelong(info, envId, "assignWorkspace")
        val assignKey = assignKey(info.nonce)
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

    private fun checkEnvBelong(info: ThirdPartyDesktopTokenInfo, envId: String, action: String) {
        val record = getRecord(info)
        if (record.envId.isNullOrBlank() || record.envId != envId) {
            logger.warn("$action|envId not belong to token|${info.userId}|$envId|${record.envId}")
            throw forbidden("env $envId does not belong to this token")
        }
    }

    private fun getRecord(info: ThirdPartyDesktopTokenInfo): ThirdPartyDesktopRegisterRecord {
        val value = redisOperation.get(registerKey(info.nonce))
        if (value.isNullOrBlank()) {
            throw forbidden("no machine registered by this token")
        }
        if (value == REGISTERING) {
            throw forbidden("machine is registering, please retry later")
        }
        return try {
            JsonUtil.to(value, ThirdPartyDesktopRegisterRecord::class.java)
        } catch (e: Throwable) {
            logger.warn("getRecord|parse failed|${info.nonce}|${e.message}")
            throw forbidden("no machine registered by this token")
        }
    }

    /* Redis 记录与 token 同生命周期，至少保留 1 分钟 */
    private fun remainSeconds(info: ThirdPartyDesktopTokenInfo): Long {
        return maxOf((info.expireTime - System.currentTimeMillis()) / 1000 + 1, MIN_RECORD_SECONDS)
    }

    private fun registerKey(nonce: String) = "$REDIS_KEY_PREFIX:register:$nonce"

    private fun assignKey(nonce: String) = "$REDIS_KEY_PREFIX:assign:$nonce"

    /**
     * HMAC-SHA256 签名，签名密钥由 secret 派生，避免与加密直接共用同一把密钥
     */
    private fun sign(secret: String, content: String): String {
        val macKey = hmac(secret.toByteArray(Charsets.UTF_8), MAC_KEY_INFO.toByteArray(Charsets.UTF_8))
        val signature = hmac(macKey, content.toByteArray(Charsets.UTF_8))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(signature)
    }

    private fun hmac(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance(HMAC_SHA256)
        mac.init(SecretKeySpec(key, HMAC_SHA256))
        return mac.doFinal(data)
    }

    private fun checkSecret(): String {
        if (secret.isNullOrBlank()) {
            logger.error("[remoteDev.thirdPartyDesktop.secret] is not configured")
            throw ErrorCodeException(
                statusCode = Response.Status.INTERNAL_SERVER_ERROR.statusCode,
                errorCode = PARAMETER_SECRET_ERROR,
                defaultMessage = "[remoteDev.thirdPartyDesktop.secret] is not configured",
                params = arrayOf("config", "[remoteDev.thirdPartyDesktop.secret] is not configured")
            )
        }
        return secret
    }

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
        private const val TOKEN_SEPARATOR = "."
        private const val HMAC_SHA256 = "HmacSHA256"
        private const val MAC_KEY_INFO = "remotedev:third_party_desktop:token:mac"
        private const val REDIS_KEY_PREFIX = "remotedev:third_party_desktop"
        private const val REGISTERING = "REGISTERING"
    }
}
