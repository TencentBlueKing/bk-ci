package com.tencent.devops.environment.service.thirdpartyagent

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.util.AESUtil
import com.tencent.devops.common.api.util.ApiUtil
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.SecurityUtil
import com.tencent.devops.common.redis.concurrent.SimpleRateLimiter
import com.tencent.devops.environment.constant.EnvironmentMessageCode
import com.tencent.devops.environment.dao.thirdpartyagent.AgentBatchInstallTokenDao
import com.tencent.devops.environment.dao.thirdpartyagent.ThirdPartyAgentDao
import com.tencent.devops.environment.service.AgentUrlService
import com.tencent.devops.environment.service.slave.SlaveGatewayService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZoneOffset
import jakarta.ws.rs.core.Response

/**
 * 批量安装Agent相关
 */
@Service
class BatchInstallAgentService @Autowired constructor(
    private val dslContext: DSLContext,
    private val agentBatchInstallTokenDao: AgentBatchInstallTokenDao,
    private val thirdPartyAgentDao: ThirdPartyAgentDao,
    private val agentUrlService: AgentUrlService,
    private val slaveGatewayService: SlaveGatewayService,
    private val downloadAgentInstallService: DownloadAgentInstallService,
    private val simpleRateLimiter: SimpleRateLimiter
) {
    fun genInstallLink(
        projectId: String,
        userId: String,
        os: OS,
        zoneName: String?
    ): String {
        val now = LocalDateTime.now()
        val gateway = slaveGatewayService.getGateway(zoneName)
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
                token = record.token
            )
        }

        // 没有或者过期则重新生成，过期时间默认为3天后
        val tokenData = "$projectId;$userId;${now.toInstant(ZoneOffset.of("+8")).toEpochMilli()}"
        logger.debug("genInstallLink token data $tokenData")
        val token = AESUtil.encrypt(ASE_SECRET, tokenData)
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
            token = token
        )
    }

    fun genAgentInstallScript(
        token: String,
        os: OS,
        zoneName: String?
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
        val agentId = genNewAgent(
            projectId = projectId,
            userId = userId,
            os = os,
            zoneName = zoneName
        )
        val agentHashId = HashUtil.encodeLongId(agentId)

        // 生成安装脚本
        return downloadAgentInstallService.downloadInstallScript(agentHashId, true)
    }

    private fun verifyToken(token: String): Triple<String, String, String?> {
        val decodeSub = AESUtil.decrypt(ASE_SECRET, token).split(";")
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

    private fun genNewAgent(
        projectId: String,
        userId: String,
        os: OS,
        zoneName: String?
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
            fileGateway = fileGateway
        )
    }

    companion object {
        private const val ASE_SECRET = "6fQyK-&Ht49zlBwhB8TW*xAJ/JZz0ZreVcDVCSj+5bY="
        private val logger = LoggerFactory.getLogger(BatchInstallAgentService::class.java)
    }
}
