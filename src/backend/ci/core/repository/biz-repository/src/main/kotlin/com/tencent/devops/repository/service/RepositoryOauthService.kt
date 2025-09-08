package com.tencent.devops.repository.service

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.repository.constant.RepositoryMessageCode
import com.tencent.devops.repository.pojo.Oauth2State
import com.tencent.devops.repository.pojo.RepoCondition
import com.tencent.devops.repository.pojo.RepoOauthRefVo
import com.tencent.devops.repository.pojo.enums.RedirectUrlTypeEnum
import com.tencent.devops.repository.pojo.RepositoryScmConfigVo
import com.tencent.devops.repository.pojo.enums.RepoAuthType
import com.tencent.devops.repository.pojo.enums.ScmConfigStatus
import com.tencent.devops.repository.pojo.oauth.Oauth2Url
import com.tencent.devops.repository.pojo.oauth.OauthTokenInfo
import com.tencent.devops.repository.pojo.oauth.OauthTokenVo
import com.tencent.devops.repository.pojo.oauth.OauthUserVo
import com.tencent.devops.repository.service.code.CodeRepositoryManager
import com.tencent.devops.repository.service.github.GithubOAuthService
import com.tencent.devops.repository.service.hub.ScmTokenApiService
import com.tencent.devops.repository.service.hub.ScmUserApiService
import com.tencent.devops.repository.service.oauth2.Oauth2TokenStoreManager
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder
import java.util.Base64

/**
 * OAUTH授权代码库服务类
 */
@Service
class RepositoryOauthService @Autowired constructor(
    private val oauth2TokenStoreManager: Oauth2TokenStoreManager,
    private val codeRepositoryManager: CodeRepositoryManager,
    private val scmConfigService: RepositoryScmConfigService,
    private val scmTokenApiService: ScmTokenApiService,
    private val scmUserApiService: ScmUserApiService,
    val githubOAuthService: GithubOAuthService
) {
    fun list(
        userId: String,
        page: Int?,
        pageSize: Int?
    ): List<OauthTokenVo> {
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: PageUtil.DEFAULT_PAGE_SIZE
        val limit = PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull)
        val scmConfigList = scmConfigService.listConfigVo(
            userId = userId,
            status = ScmConfigStatus.SUCCESS,
            oauth2Enabled = true,
            limit = limit.limit,
            offset = limit.offset
        ).records.filter {
            // 新的代码源开启OAUTH授权后，才需展示OAUTH信息
            if (it.scmType == ScmType.SCM_SVN || it.scmType == ScmType.SCM_GIT) {
                it.oauth2Enabled == true
            } else {
                true
            }
        }
        val list = mutableListOf<OauthTokenVo>()
        scmConfigList.forEach { scmConfig ->
            val tokenList = oauth2TokenStoreManager.list(
                userId = userId,
                scmCode = scmConfig.scmCode
            )
            if (tokenList.isEmpty()) {
                list.add(convertEmptyOauthVo(userId = userId, scmConfig = scmConfig))
            } else {
                tokenList.forEach {
                    list.add(convertOauthVo(scmConfig = scmConfig, oauthInfo = it))
                }
            }
        }
        return list
    }

    /**
     * 使用oauth授权的代码库列表
     */
    fun listRepoOauthRef(
        userId: String,
        scmCode: String,
        page: Int?,
        pageSize: Int?
    ): Page<RepoOauthRefVo> {
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: PageUtil.DEFAULT_PAGE_SIZE
        val limit = PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull)
        val repoCondition = RepoCondition(
            authType = RepoAuthType.OAUTH,
            oauthUserId = userId,
            scmCode = scmCode
        )
        val count = codeRepositoryManager.countByCondition(
            scmCode = scmCode,
            repoCondition = repoCondition
        )
        val records = codeRepositoryManager.listByCondition(
            scmCode = scmCode,
            repoCondition = repoCondition,
            limit = limit.limit,
            offset = limit.offset
        )?.map {
            RepoOauthRefVo(
                aliasName = it.aliasName,
                url = it.url,
                projectId = it.projectId!!,
                hashId = it.repoHashId!!
            )
        } ?: emptyList()
        return Page(pageNotNull, pageSizeNotNull, count, records)
    }

    fun delete(
        userId: String,
        scmCode: String,
        oauthUserId: String
    ) {
        val repoCondition = RepoCondition(
            authType = RepoAuthType.OAUTH,
            oauthUserId = oauthUserId,
            scmCode = scmCode
        )
        val count = codeRepositoryManager.countByCondition(
            scmCode = scmCode,
            repoCondition = repoCondition
        )
        // 检查是否还有关联代码库
        if (count > 0) {
            throw ErrorCodeException(
                errorCode = RepositoryMessageCode.OAUTH_INFO_OCCUPIED_CANNOT_DELETE
            )
        }
        // 删除指定用户名的授权信息
        oauth2TokenStoreManager.delete(username = oauthUserId, scmCode = scmCode, userId = userId)
    }

    fun oauthUrl(
        userId: String,
        scmCode: String,
        redirectUrl: String,
        oauthUserId: String
    ): Oauth2Url {
        val url = when (scmCode) {
            // GITHUB 相关sdk尚未完善，完善后移除这部分代码
            ScmType.GITHUB.name -> {
                githubOAuthService.getGithubOauth(
                    userId = oauthUserId,
                    projectId = "",
                    redirectUrlTypeEnum = RedirectUrlTypeEnum.SPEC,
                    specRedirectUrl = redirectUrl,
                    repoHashId = null,
                    operator = userId
                ).redirectUrl
            }

            else -> {
                val state = encodeOauthState(userId = userId, redirectUrl = redirectUrl, username = oauthUserId)
                scmTokenApiService.authorizationUrl(scmCode = scmCode, state = state)
            }
        }
        return Oauth2Url(url)
    }

    fun oauthCallback(
        scmCode: String,
        code: String,
        state: String
    ): String {
        val oauth2State = decodeOauthState(state)
        val oauth2AccessToken = scmTokenApiService.callback(scmCode = scmCode, code = code)
        val user = scmUserApiService.getUser(scmCode = scmCode, accessToken = oauth2AccessToken.accessToken)
        // 重置的授权用户与实际用户不一致
        if (user.username != oauth2State.oauthUserId) {
            logger.warn(
                "oauth authorization mismatch: actual user [${user.username}] does not match " +
                        "target user [${oauth2State.oauthUserId}], potential security risk"
            )
        }
        val oauthTokenInfo = with(oauth2AccessToken) {
            OauthTokenInfo(
                accessToken = accessToken,
                tokenType = tokenType,
                expiresIn = expiresIn,
                refreshToken = refreshToken,
                userId = user.username,
                operator = oauth2State.userId
            )
        }
        oauth2TokenStoreManager.store(scmCode = scmCode, oauthTokenInfo = oauthTokenInfo)
        return UriComponentsBuilder.fromUriString(oauth2State.redirectUrl)
                .replaceQueryParam("userId", user.username)
                .build()
                .toUriString()
    }

    fun oauthUserList(
        userId: String,
        scmCode: String
    ): List<OauthUserVo> {
        return oauth2TokenStoreManager.list(userId = userId, scmCode = scmCode)
                .sortedWith(
                    compareByDescending<OauthTokenInfo> { it.userId == userId }
                            .thenBy { it.userId }
                )
                .map {
                    OauthUserVo(
                        username = it.userId,
                        operator = it.operator ?: it.userId
                    )
                }
    }

    private fun encodeOauthState(
        userId: String,
        redirectUrl: String,
        username: String
    ): String {
        val oauth2State = Oauth2State(userId = userId, redirectUrl = redirectUrl, oauthUserId = username)
        return Base64.getEncoder().encodeToString(JsonUtil.toJson(oauth2State, false).toByteArray())
    }

    private fun decodeOauthState(state: String): Oauth2State {
        return JsonUtil.to(String(Base64.getDecoder().decode(state)), Oauth2State::class.java)
    }

    private fun convertOauthVo(scmConfig: RepositoryScmConfigVo, oauthInfo: OauthTokenInfo): OauthTokenVo {
        with(oauthInfo) {
            val scmCode = scmConfig.scmCode
            val repoCondition = RepoCondition(
                type = scmConfig.scmType,
                authType = RepoAuthType.OAUTH,
                oauthUserId = userId,
                scmCode = scmCode
            )
            val count = codeRepositoryManager.countByCondition(
                scmCode = scmCode,
                repoCondition = repoCondition
            )
            return OauthTokenVo(
                username = userId,
                repoCount = count,
                scmCode = scmCode,
                scmType = scmConfig.scmType,
                name = generateI18nConfigName(scmConfig.scmCode).ifBlank { scmConfig.name },
                operator = operator ?: userId,
                createTime = createTime,
                expired = expiresIn?.let { expiresIn ->
                    (updateTime ?: 0L) + expiresIn * 1000 <= System.currentTimeMillis()
                } ?: false,
                authorized = true
            )
        }
    }

    private fun convertEmptyOauthVo(userId: String, scmConfig: RepositoryScmConfigVo): OauthTokenVo {
        with(scmConfig) {
            return OauthTokenVo(
                username = userId,
                repoCount = 0L,
                scmCode = scmCode,
                expired = true,
                authorized = false,
                scmType = scmType,
                name = generateI18nConfigName(scmCode).ifBlank { name },
                operator = userId
            )
        }
    }

    private fun generateI18nConfigName(scmCode: String): String {
        return I18nUtil.getCodeLanMessage(scmCode)
    }

    companion object {
        val logger = LoggerFactory.getLogger(RepositoryOauthService::class.java)
    }
}
