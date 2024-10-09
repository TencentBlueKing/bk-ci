package com.tencent.devops.remotedev.service.gitproxy

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.DHKeyPair
import com.tencent.devops.common.api.util.DHUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.pojo.gitproxy.TGitCredType
import com.tencent.devops.remotedev.service.gitproxy.OffshoreTGitApiClient.Companion.LOG_UPDATE_TGIT_ACL_TAG
import com.tencent.devops.repository.api.ServiceOauthResource
import com.tencent.devops.repository.pojo.oauth.GitToken
import com.tencent.devops.ticket.api.ServiceCredentialResource
import com.tencent.devops.ticket.pojo.enums.CredentialType
import org.slf4j.LoggerFactory
import java.util.Base64

/**
 * 把获取 Token 的行为全部封装在这里，非线程安全。对于client只是引用没有性能损耗，单独类更清晰些
 * @param save 是否缓存 token
 * @param bThrowE 是否抛出异常
 * @param bLogE 是否在异常时打印日志
 */
class TokenBox(
    private val client: Client,
    private val save: Boolean,
    private val bThrowE: Boolean? = null,
    private val bLogE: Boolean? = null
) {
    private var oauthUserTokens: MutableMap<String, TGitToken>? = null
    private var credIdTokens: MutableMap<String, TGitToken>? = null
    private var dhKeyPair: DHKeyPair? = null

    init {
        if (save) {
            oauthUserTokens = mutableMapOf()
            credIdTokens = mutableMapOf()
        }
    }

    fun get(
        projectId: String,
        credType: TGitCredType,
        cred: String,
        throwE: Boolean? = null,
        log: Boolean? = null
    ): TGitToken? {
        if (save) {
            return getTokenAndSave(
                projectId = projectId,
                credType = credType,
                cred = cred,
                throwE = throwE ?: bThrowE ?: false,
                log = log ?: bLogE ?: true
            )
        }
        return getToken(
            projectId = projectId,
            credType = credType,
            cred = cred,
            throwE = throwE ?: bThrowE ?: true,
            log = log ?: bLogE ?: false
        )
    }

    private fun getToken(
        projectId: String,
        credType: TGitCredType,
        cred: String,
        throwE: Boolean,
        log: Boolean
    ): TGitToken? {
        when (credType) {
            TGitCredType.OAUTH_USER -> {
                val token = requestOauthToken(cred)?.accessToken
                if (token != null) {
                    return TGitToken(token, false)
                }
                if (log) {
                    logger.error("$LOG_UPDATE_TGIT_ACL_TAG|getToken|$projectId|$credType|$cred is null")
                }
                if (throwE) {
                    throw ErrorCodeException(
                        errorCode = ErrorCodeEnum.NO_TGIT_OAUTH_ERROR.errorCode,
                        errorType = ErrorCodeEnum.NO_TGIT_OAUTH_ERROR.errorType,
                        params = arrayOf(cred, "TGit")
                    )
                }
                return null
            }

            TGitCredType.CRED_ACCESS_TOKEN_ID -> {
                val token = requestCredIdToken(projectId, cred)
                if (token != null) {
                    return TGitToken(token, true)
                }
                if (log) {
                    logger.error("$LOG_UPDATE_TGIT_ACL_TAG|getToken|$projectId|$credType|$cred is null")
                }
                if (throwE) {
                    throw ErrorCodeException(
                        errorCode = ErrorCodeEnum.NO_CRED_ID_ERROR.errorCode,
                        errorType = ErrorCodeEnum.NO_CRED_ID_ERROR.errorType,
                        params = arrayOf(projectId, cred)
                    )
                }
            }
        }
        return null
    }

    private fun getTokenAndSave(
        projectId: String,
        credType: TGitCredType,
        cred: String,
        throwE: Boolean,
        log: Boolean
    ): TGitToken? {
        return when (credType) {
            TGitCredType.OAUTH_USER -> {
                oauthUserTokens?.get(cred) ?: run {
                    val token = getToken(
                        projectId = projectId,
                        credType = credType,
                        cred = cred,
                        throwE = throwE,
                        log = log
                    ) ?: return null
                    oauthUserTokens?.set(cred, token)
                    token
                }
            }

            TGitCredType.CRED_ACCESS_TOKEN_ID -> {
                credIdTokens?.get(cred) ?: run {
                    val token = getToken(
                        projectId = projectId,
                        credType = credType,
                        cred = cred,
                        throwE = throwE,
                        log = log
                    ) ?: return null
                    credIdTokens?.set(cred, token)
                    token
                }
            }
        }
    }

    private fun requestOauthToken(userId: String): GitToken? {
        return client.get(ServiceOauthResource::class).tGitGet(userId).data
    }

    private fun requestCredIdToken(projectId: String, credId: String): String? {
        val pair = if (dhKeyPair == null) {
            dhKeyPair = DHUtil.initKey()
            dhKeyPair
        } else {
            dhKeyPair
        }!!
        val encoder = Base64.getEncoder()
        val decoder = Base64.getDecoder()
        val credRes = client.get(ServiceCredentialResource::class).get(
            projectId = projectId,
            credentialId = credId,
            publicKey = encoder.encodeToString(pair.publicKey)
        )
        if (credRes.isNotOk()) {
            logger.error("$LOG_UPDATE_TGIT_ACL_TAG|requestCredIdToken|$projectId|$credId get ticket fail", credRes)
            return null
        }
        val cred = credRes.data ?: return null
        if (cred.credentialType != CredentialType.ACCESSTOKEN) {
            logger.warn("$LOG_UPDATE_TGIT_ACL_TAG|requestCredIdToken|$projectId|$credId cred type not access_token")
            return null
        }
        return String(DHUtil.decrypt(decoder.decode(cred.v1), decoder.decode(cred.publicKey), pair.privateKey))
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TokenBox::class.java)
    }
}