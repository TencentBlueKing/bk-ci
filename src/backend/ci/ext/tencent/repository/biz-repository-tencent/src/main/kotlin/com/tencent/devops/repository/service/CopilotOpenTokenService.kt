package com.tencent.devops.repository.service

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.security.util.BkCryptoUtil
import com.tencent.devops.repository.config.CopilotConfig
import com.tencent.devops.repository.constant.RepositoryMessageCode
import com.tencent.devops.repository.dao.RepositoryScmTokenDao
import com.tencent.devops.repository.pojo.CodeGitCopilotOauthResponse
import com.tencent.devops.repository.pojo.OauthInfo
import com.tencent.devops.repository.pojo.enums.TokenAppTypeEnum
import com.tencent.devops.repository.pojo.oauth.RepositoryScmToken
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.URL
import java.time.Instant
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

@Service
class CopilotOpenTokenService @Autowired constructor(
    val copilotConfig: CopilotConfig,
    val dslContext: DSLContext,
    val repositoryScmTokenDao: RepositoryScmTokenDao
) {

    @Value("\${aes.git:#{null}}")
    private val aesKey: String = ""

    fun getAccessToken(userId: String, refresh: Boolean): String {
        // 获取copilot token
        val copilotToken = if (refresh) {
            null
        } else {
            repositoryScmTokenDao.getToken(
                dslContext = dslContext,
                scmCode = ScmType.CODE_GIT.name,
                appType = TokenAppTypeEnum.COPILOT_OPEN_TOKEN.name,
                userId = userId
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
        repositoryScmTokenDao.saveAccessToken(
            dslContext = dslContext,
            scmToken = RepositoryScmToken(
                userId = userId,
                accessToken = BkCryptoUtil.encryptSm4ButAes(aesKey, oauthInfo.accessToken),
                scmCode = "TGIT",
                appType = TokenAppTypeEnum.COPILOT_OPEN_TOKEN.name
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
        val response = OkhttpUtils.doHttp(
            Request.Builder()
                .post(OkhttpUtils.joinParams(body).toRequestBody(MEDIA_TYPE_FORM_URLENCODED))
                .url(URL("${copilotConfig.apiUrl}/auth/open_app_user_token"))
                .header("Content-Type", APPLICATION_FORM_URLENCODED)
                .build()
        )
        if (!response.isSuccessful) {
            logger.warn("fail to get copilot access token|${response.code}|${response.body?.string()}")
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
        private const val APPLICATION_FORM_URLENCODED = "application/x-www-form-urlencoded"
        private val MEDIA_TYPE_FORM_URLENCODED = APPLICATION_FORM_URLENCODED.toMediaTypeOrNull()
    }
}