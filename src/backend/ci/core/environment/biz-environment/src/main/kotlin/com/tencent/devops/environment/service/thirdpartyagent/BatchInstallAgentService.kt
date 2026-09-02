package com.tencent.devops.environment.service.thirdpartyagent

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.util.AESUtil
import com.tencent.devops.common.api.util.ApiUtil
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.SecurityUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.redis.concurrent.SimpleRateLimiter
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.environment.constant.EnvironmentMessageCode
import com.tencent.devops.environment.dao.thirdpartyagent.AgentBatchInstallTokenDao
import com.tencent.devops.environment.dao.thirdpartyagent.ThirdPartyAgentDao
import com.tencent.devops.environment.model.AgentProps
import com.tencent.devops.environment.pojo.enums.AgentType
import com.tencent.devops.environment.pojo.thirdpartyagent.ReInstallResp
import com.tencent.devops.environment.pojo.thirdpartyagent.RegistryResp
import com.tencent.devops.environment.pojo.thirdpartyagent.TPAInstallType
import com.tencent.devops.environment.pojo.thirdpartyagent.create.AgentPropsSource
import com.tencent.devops.environment.service.AgentUrlService
import com.tencent.devops.environment.service.CreateEnvService
import com.tencent.devops.environment.service.slave.SlaveGatewayService
import jakarta.ws.rs.NotFoundException
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZoneOffset
import jakarta.ws.rs.core.Response
import java.util.Base64

/**
 * 批量安装Agent相关
 */
@Service
class BatchInstallAgentService @Autowired constructor(
    private val dslContext: DSLContext,
    private val commonConfig: CommonConfig,
    private val agentBatchInstallTokenDao: AgentBatchInstallTokenDao,
    private val thirdPartyAgentDao: ThirdPartyAgentDao,
    private val agentUrlService: AgentUrlService,
    private val slaveGatewayService: SlaveGatewayService,
    private val downloadAgentInstallService: DownloadAgentInstallService,
    private val simpleRateLimiter: SimpleRateLimiter,
    private val createEnvService: CreateEnvService
) {
    @Value("\${environment.batch-install.aes-key}")
    private val batchInstallAesKey = ""

    fun genReInstallLink(
        projectId: String,
        userId: String,
        os: OS,
        reInstallId: String
    ): ReInstallResp {
        val record = thirdPartyAgentDao.getAgentByProject(dslContext, HashUtil.decodeIdToLong(reInstallId), projectId)
            ?: throw NotFoundException("The agent is not exist")
        val zoneName = slaveGatewayService.getZoneName(record.gateway)
        return ReInstallResp(
            zoneName = zoneName,
            script = genInstallLink(
                projectId = projectId,
                userId = userId,
                os = os,
                zoneName = null,
                loginName = null,
                loginPassword = null,
                installType = null,
                reInstallId = reInstallId,
                agentType = null,
                iGateway = record.gateway
            )
        )
    }

    fun genInstallLink(
        projectId: String,
        userId: String,
        os: OS,
        zoneName: String?,
        loginName: String?,
        loginPassword: String?,
        installType: TPAInstallType?,
        reInstallId: String?,
        agentType: AgentType?,
        iGateway: String? = null
    ): String {
        val now = LocalDateTime.now()
        val gateway = iGateway ?: if (reInstallId.isNullOrBlank()) {
            slaveGatewayService.getGateway(zoneName)
        } else {
            thirdPartyAgentDao.getAgentByProject(dslContext, HashUtil.decodeIdToLong(reInstallId), projectId)?.gateway
        }
        // 先确定下是否已经生成过了，以及有没有过期
        val record = agentBatchInstallTokenDao.getToken(
            dslContext = dslContext,
            projectId = projectId,
            userId = userId
        )
        if (record != null && record.expiredTime > now) {
            return agentUrlService.genAgentBatchInstallScript(
                os = os,
                zoneName = zoneName,
                gateway = gateway,
                token = record.token,
                loginName = loginName,
                loginPassword = if (loginPassword.isNullOrBlank()) {
                    null
                } else {
                    AESUtil.encrypt(batchInstallAesKey, loginPassword)
                },
                installType = installType,
                reInstallId = reInstallId,
                agentType = agentType
            )
        }

        // 没有或者过期则重新生成，过期时间默认为3天后
        val tokenData = "$projectId;$userId;${now.toInstant(ZoneOffset.of("+8")).toEpochMilli()}"
        val token = AESUtil.encrypt(batchInstallAesKey, tokenData)
        val expireTime = now.plusDays(3)
        agentBatchInstallTokenDao.createOrUpdateToken(
            dslContext = dslContext,
            projectId = projectId,
            userId = userId,
            token = token,
            createTime = now,
            expireTime = expireTime
        )

        return agentUrlService.genAgentBatchInstallScript(
            os = os,
            zoneName = zoneName,
            gateway = gateway,
            token = token,
            loginName = loginName,
            loginPassword = if (loginPassword.isNullOrBlank()) {
                null
            } else {
                AESUtil.encrypt(batchInstallAesKey, loginPassword)
            },
            installType = installType,
            reInstallId = reInstallId,
            agentType = agentType
        )
    }

    fun genAgentInstallScript(
        token: String,
        os: OS,
        zoneName: String?,
        loginName: String?,
        loginPassword: String?,
        installType: TPAInstallType?,
        reInstallId: String?,
        agentType: AgentType?
    ): Response {
        // 先校验是否可以创建
        val (projectId, userId, errorMsg) = verifyToken(token)
        if (errorMsg != null) {
            throw ErrorCodeException(
                errorCode = EnvironmentMessageCode.ERROR_NODE_NO_CREATE_PERMISSSION,
                defaultMessage = errorMsg
            )
        }

        // 增加下载限制
        val lockKey = "lock:tpa:batch:rate:$token"
        val acquire = simpleRateLimiter.acquire(ImportService.BU_SIZE, lockKey = lockKey)
        if (!acquire) {
            throw OperationException("Frequency $lockKey limit: ${ImportService.BU_SIZE}")
        }

        // 直接创建新agent
        val agentHashId = if (reInstallId.isNullOrBlank()) {
            val agentId = genNewAgent(
                projectId = projectId,
                userId = userId,
                os = os,
                zoneName = zoneName,
                agentType = agentType,
                createWorkspaceName = null,
                agentProps = null
            )
            HashUtil.encodeLongId(agentId)
        } else {
            reInstallId
        }

        val decodePassword = if (loginPassword.isNullOrBlank()) {
            null
        } else {
            AESUtil.decrypt(batchInstallAesKey, loginPassword)
        }

        // 生成安装脚本
        return downloadAgentInstallService.downloadInstallScript(
            agentHashId,
            loginName,
            decodePassword,
            installType
        )
    }

    private fun verifyToken(token: String): Triple<String, String, String?> {
        val decodeSub = AESUtil.decrypt(batchInstallAesKey, token).split(";")
        if (decodeSub.size < 3) {
            return Triple("", "", "token verify error")
        }

        val record = agentBatchInstallTokenDao.getToken(dslContext, decodeSub[0], decodeSub[1])
            ?: return Triple("", "", "token's project and user not find")

        if (record.token != token || record.expiredTime <= LocalDateTime.now()) {
            return Triple("", "", "token is expired")
        }

        return Triple(decodeSub[0], decodeSub[1], null)
    }

    fun genNewAgent(
        projectId: String,
        userId: String,
        os: OS,
        zoneName: String?,
        agentType: AgentType?,
        createWorkspaceName: String?,
        agentProps: AgentProps?
    ): Long {
        val gateway = slaveGatewayService.getGateway(zoneName)
        val fileGateway = slaveGatewayService.getFileGateway(zoneName)
        val secretKey = ApiUtil.randomSecretKey()
        return thirdPartyAgentDao.add(
            dslContext = dslContext,
            userId = userId,
            projectId = projectId,
            os = os,
            secretKey = SecurityUtil.encrypt(secretKey),
            gateway = gateway,
            fileGateway = fileGateway,
            agentType = agentType,
            createWorkspaceName = createWorkspaceName,
            agentProps = agentProps
        )
    }

    fun genCreateAgentId(
        userId: String,
        projectId: String,
        workspaceName: String,
        os: OS
    ): String {
        val agentId = genNewAgent(
            projectId = projectId,
            userId = userId,
            os = os,
            zoneName = createEnvService.getWorkspaceZoneName(projectId, workspaceName),
            agentType = AgentType.CREATE,
            createWorkspaceName = workspaceName,
            agentProps = AgentProps.emptyBySource(AgentPropsSource.REMOTEDEV)
        )
        return HashUtil.encodeLongId(agentId)
    }

    fun registry(
        token: String,
        userId: String,
        deviceId: String?,
    ): RegistryResp {
        val (projectId, agentHashId, errMsg) = try {
            verifyRegistryToken(token, deviceId, userId)
        } catch (e: Exception) {
            throw ErrorCodeException(
                errorCode = EnvironmentMessageCode.ERROR_NODE_NO_CREATE_PERMISSSION,
                defaultMessage = e.message
            )
        }
        if (errMsg != null) {
            logger.warn("registry $userId|$deviceId token check error $errMsg")
            throw ErrorCodeException(
                errorCode = EnvironmentMessageCode.ERROR_NODE_NO_CREATE_PERMISSSION,
                defaultMessage = errMsg
            )
        }
        val record = if (deviceId.isNullOrBlank()) {
            thirdPartyAgentDao.getAgentByProject(
                dslContext = dslContext,
                id = HashUtil.decodeIdToLong(agentHashId),
                projectId = projectId
            )
        } else {
            thirdPartyAgentDao.getAgentByWorkspaceIdGlobal(
                dslContext = dslContext,
                workspaceId = deviceId,
                projectId = projectId
            )
        }
        if (record == null) {
            logger.error("addCreateNode no found agent $projectId|$agentHashId|$deviceId|$userId")
            throw ErrorCodeException(
                errorCode = EnvironmentMessageCode.ERROR_NODE_NOT_EXISTS,
                params = arrayOf(deviceId ?: "", agentHashId)
            )
        }
        // 标注是 SDK
        thirdPartyAgentDao.updateAgentProps(
            dslContext = dslContext,
            projectId = projectId,
            agentId = record.id,
            props = AgentProps.parseAgentProps(record.agentProps)?.copy(sdk = true) ?: AgentProps.emptyBySdk(true)
        )
        return RegistryResp(
            gateway = record.gateway ?: "",
            fileGateway = record.fileGateway ?: "",
            projectId = record.projectId,
            agentId = HashUtil.encodeLongId(record.id),
            secretKey = SecurityUtil.decrypt(record.secretKey),
            parallelTaskCount = record.parallelTaskCount ?: 4,
            dockerParallelTaskCount = record.dockerParallelTaskCount ?: 4,
            language = commonConfig.devopsDefaultLocaleLanguage,
        )
    }

    /**
     * 校验注册token
     * "projectId;userId;time"
     * "projectId;deviceId;userId;time"
     * @return <projectId, agentHashId, errMsg>
     */
    private fun verifyRegistryToken(token: String, deviceId: String?, userId: String): Triple<String, String, String?> {
        val realToken = Base64.getUrlDecoder().decode(token)
        val decodeSub = AESUtil.decrypt(batchInstallAesKey, realToken).toString(charset("UTF-8")).split(";")
        if (!deviceId.isNullOrBlank()) {
            if (decodeSub.size < 4) {
                return Triple("", "", "token verify error")
            }

            if (decodeSub[1] != deviceId || decodeSub[2] != userId) {
                return Triple("", "", "token's deviceId or user not find")
            }

            if (decodeSub[3].toLong() <= LocalDateTime.now().timestampmilli()) {
                return Triple("", "", "token is expired")
            }

            return Triple(decodeSub[0], "", null)
        } else {
            if (decodeSub.size < 4) {
                return Triple("", "", "token verify error")
            }

            if (decodeSub[2] != userId) {
                return Triple("", "", "token's user not find")
            }

            if (decodeSub[3].toLong() <= LocalDateTime.now().timestampmilli()) {
                return Triple("", "", "token is expired")
            }

            return Triple(decodeSub[0], decodeSub[1], null)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BatchInstallAgentService::class.java)
    }
}
