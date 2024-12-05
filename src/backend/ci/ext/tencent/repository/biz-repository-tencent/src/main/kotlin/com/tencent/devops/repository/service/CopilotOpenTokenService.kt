package com.tencent.devops.repository.service

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.AESUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.security.util.BkCryptoUtil
import com.tencent.devops.repository.config.CopilotConfig
import com.tencent.devops.repository.constant.RepositoryMessageCode
import com.tencent.devops.repository.dao.GitTokenDao
import com.tencent.devops.repository.pojo.CodeGitCopilotOauthResponse
import com.tencent.devops.repository.pojo.OauthInfo
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.repository.pojo.oauth.GitToken
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDateTime
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

@Service
class CopilotOpenTokenService @Autowired constructor(
    val copilotConfig: CopilotConfig,
    val gitTokenDao: GitTokenDao,
    val dslContext: DSLContext
) {

    @Value("\${aes.git:#{null}}")
    private val aesKey: String = ""

    fun getAccessToken(userId: String, refresh: Boolean): String {
        // 获取copilot token
        val copilotToken = if (refresh) {
            null
        } else {
            gitTokenDao.getAccessToken(
                dslContext = dslContext,
                userId = userId,
                tokenType = TokenTypeEnum.COPILOT_TOKEN.name
            )
        }
        return if (copilotToken == null) {
            // 不存在copilot token, 开始授权
            logger.warn("copilot token is not exist")
            val oauthInfo = getCopilotAccessToken(userId)
            saveCopilotToken(userId, oauthInfo)
            oauthInfo.accessToken
        } else {
            BkCryptoUtil.decryptSm4OrAes(aesKey, copilotToken.accessToken)
        }
    }

    /**
     * 保存copilot token
     */
    private fun saveCopilotToken(userId: String, oauthInfo: OauthInfo) {
        gitTokenDao.saveAccessToken(
            dslContext = dslContext,
            userId = userId,
            token = GitToken(
                accessToken = AESUtil.decrypt(aesKey, oauthInfo.accessToken),
                tokenType = TokenTypeEnum.COPILOT_TOKEN.name,
                createTime = LocalDateTime.now().timestampmilli()
            )
        )
    }

    @Throws(Exception::class)
    private fun getCopilotAccessToken(userId: String): OauthInfo {
        val body = mutableMapOf(
            "app_id" to copilotConfig.appId,
            "username" to userId,
            "timestamp" to "${Instant.now().toEpochMilli()}",
            "nonce" to UUID.randomUUID().toString()
        )

        val verify = generateVerify(JsonUtil.toJson(body, false))
        val encryptedBytes = Base64.getDecoder().decode(verify)
        val encryptedBase64Str = Base64.getEncoder().encodeToString(encryptedBytes)
        body["verify"] = encryptedBase64Str
        val response = OkhttpUtils.doPost(
            url = copilotConfig.authUrl, jsonParam = JsonUtil.toJson(body, false), headers =
            mapOf("Content-Type" to "application/x-www-form-urlencoded")
        )
        if (!response.isSuccessful) {
            logger.warn("fail to get copilot access token|${response.code}|${response.body}")
            throw ErrorCodeException(
                errorCode = RepositoryMessageCode.FAIL_TO_GET_OPEN_COPILOT_TOKEN,
                params = arrayOf(response.body?.string() ?: response.code.toString())
            )
        }
        val responseContent = response.body!!.string()
        return JsonUtil.to(responseContent, object : TypeReference<CodeGitCopilotOauthResponse>() {}).data
    }

    @Throws(Exception::class)
    private fun generateVerify(data: String): String {
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        val keySpec = SecretKeySpec(copilotConfig.appSecret.toByteArray(Charsets.UTF_8), "AES")
        cipher.init(Cipher.ENCRYPT_MODE, keySpec)
        val encrypted: ByteArray = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(encrypted)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CopilotOpenTokenService::class.java)
    }
}