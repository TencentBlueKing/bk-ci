package com.tencent.devops.remotedev.service.thirdparty

import com.tencent.devops.common.api.constant.CommonMessageCode.PARAMETER_ILLEGAL_ERROR
import com.tencent.devops.common.api.constant.CommonMessageCode.PARAMETER_SECRET_ERROR
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.util.AESUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.UUIDUtil
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
import com.tencent.devops.remotedev.service.client.StartCloudClient
import com.tencent.devops.remotedev.service.workspace.CreateControl
import jakarta.ws.rs.core.Response
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

/**
 * 第三方机器注册为云桌面相关能力
 */
@Service
class ThirdPartyDesktopService @Autowired constructor(
    private val remoteDevServiceFactory: RemoteDevServiceFactory,
    private val startCloudClient: StartCloudClient,
    private val createControl: CreateControl
) {

    /* 第三方云桌面 token 加解密密钥 */
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
        val expireTime = System.currentTimeMillis() + expireSeconds * 1000
        val info = ThirdPartyDesktopTokenInfo(
            userId = userId,
            os = osEnum.name,
            zoneId = zoneId,
            projectId = projectId,
            expireTime = expireTime,
            nonce = UUIDUtil.generate()
        )
        logger.info("genToken|$userId|$projectId|${osEnum.name}|$zoneId|$expireTime")
        val token = try {
            AESUtil.encrypt(key, JsonUtil.toJson(info, formatted = false))
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
     * 校验 token 并返回 token 中的信息，非法或过期直接抛 401
     */
    fun verifyToken(token: String?): ThirdPartyDesktopTokenInfo {
        val key = checkSecret()
        if (token.isNullOrBlank()) {
            throw unauthorized("token is empty")
        }
        val info = try {
            JsonUtil.to(AESUtil.decrypt(key, token), ThirdPartyDesktopTokenInfo::class.java)
        } catch (e: Throwable) {
            logger.warn("verifyToken|token illegal|${e.message}")
            throw unauthorized("token illegal")
        }
        if (info.expireTime < System.currentTimeMillis()) {
            logger.warn("verifyToken|token expired|${info.userId}|${info.expireTime}")
            throw unauthorized("token expired")
        }
        return info
    }

    fun registerVm(token: String?, ip: String): CreateCvmResp? {
        val info = verifyToken(token)
        if (ip.isBlank()) {
            throw illegal("ip can not be blank")
        }
        logger.info("registerVm|${info.userId}|${info.projectId}|${info.os}|${info.zoneId}|$ip")
        return remoteDevServiceFactory.loadRemoteDevService(WorkspaceMountType.BCS).registerVm(
            RegisterVmData(
                internal = true,
                ip = ip,
                os = info.os,
                machineType = DEFAULT_MACHINE_TYPE,
                zoneId = info.zoneId,
                skipInit = true
            )
        )
    }

    fun getTaskStatus(token: String?, taskId: String): WorkspaceTaskStatus? {
        val info = verifyToken(token)
        logger.info("getTaskStatus|${info.userId}|$taskId")
        return remoteDevServiceFactory.loadRemoteDevService(WorkspaceMountType.BCS).taskStatus(taskId)
    }

    fun getCdsKey(token: String?, vmId: String, envId: String): ThirdPartyDesktopCdsKeyResp {
        val info = verifyToken(token)
        logger.info("getCdsKey|${info.userId}|$vmId|$envId")
        if (vmId.isBlank() || envId.isBlank()) {
            throw illegal("vmId/envId can not be blank")
        }
        return ThirdPartyDesktopCdsKeyResp(cdsKey = startCloudClient.exchangeCdsKey(envId = envId, vmId = vmId))
    }

    fun assignWorkspace(token: String?, envId: String): Boolean {
        val info = verifyToken(token)
        if (envId.isBlank()) {
            throw illegal("envId can not be blank")
        }
        logger.info("assignWorkspace|${info.userId}|${info.projectId}|${info.os}|$envId")
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
        return true
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

    private fun illegal(msg: String) = ErrorCodeException(
        statusCode = Response.Status.BAD_REQUEST.statusCode,
        errorCode = PARAMETER_ILLEGAL_ERROR,
        defaultMessage = msg,
        params = arrayOf("param", msg)
    )

    companion object {
        private val logger = LoggerFactory.getLogger(ThirdPartyDesktopService::class.java)
        private const val DEFAULT_EXPIRE_SECONDS = 3600L
        private const val DEFAULT_MACHINE_TYPE = "XL2"
    }
}
