package com.tencent.devops.repository.service

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.PageUtil
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
import com.tencent.devops.repository.service.code.CodeRepositoryManager
import com.tencent.devops.repository.service.github.GithubOAuthService
import com.tencent.devops.repository.service.hub.ScmTokenApiService
import com.tencent.devops.repository.service.hub.ScmUserApiService
import com.tencent.devops.repository.service.oauth2.Oauth2TokenStoreManager
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
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
        ).records
        return scmConfigList.map { scmConfig ->
            oauth2TokenStoreManager.get(
                userId = userId,
                scmCode = scmConfig.scmCode
            )?.let {
                convertOauthVo(scmConfig = scmConfig, oauthInfo = it)
            } ?: convertEmptyOauthVo(userId = userId, scmConfig = scmConfig)
        }
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
        scmCode: String
    ) {
        val repoCondition = RepoCondition(
            authType = RepoAuthType.OAUTH,
            oauthUserId = userId,
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
        oauth2TokenStoreManager.delete(userId = userId, scmCode = scmCode)
    }

    fun oauthUrl(
        userId: String,
        scmCode: String,
        redirectUrl: String
    ): Oauth2Url {
        val url = when (scmCode) {
            // GITHUB 相关sdk尚未完善，完善后移除这部分代码
            ScmType.GITHUB.name -> {
                githubOAuthService.getGithubOauth(
                    userId = userId,
                    projectId = "",
                    redirectUrlTypeEnum = RedirectUrlTypeEnum.SPEC,
                    specRedirectUrl = redirectUrl,
                    repoHashId = null
                ).redirectUrl
            }

            else -> {
                val state = encodeOauthState(userId = userId, redirectUrl = redirectUrl)
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
        return oauth2State.redirectUrl
    }

    private fun encodeOauthState(
        userId: String,
        redirectUrl: String
    ): String {
        val oauth2State = Oauth2State(userId = userId, redirectUrl = redirectUrl)
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
                name = scmConfig.name,
                operator = operator ?: userId,
                createTime = createTime,
                expired = expiresIn?.let { expiresIn ->
                    (createTime ?: 0L) + expiresIn * 1000 <= System.currentTimeMillis()
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
                name = name,
                operator = userId
            )
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(RepositoryOauthService::class.java)
    }
}
