package com.tencent.devops.repository.service

import com.tencent.devops.repository.pojo.OauthResetUrl
import com.tencent.devops.repository.pojo.UserOauthRepositoryInfo
import com.tencent.devops.common.api.enums.ScmCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.repository.constant.RepositoryMessageCode
import com.tencent.devops.repository.pojo.RepoOauthRefVo
import com.tencent.devops.repository.pojo.enums.RedirectUrlTypeEnum
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.repository.service.github.GithubOAuthService
import com.tencent.devops.repository.service.oauth2.Oauth2TokenStoreManager
import com.tencent.devops.repository.service.scm.IGitOauthService
import com.tencent.devops.repository.service.scm.IGitService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * OAUTH授权代码库服务类
 */
@Service
class OauthRepositoryService @Autowired constructor(
    val oauth2TokenStoreManager: Oauth2TokenStoreManager,
    val gitService: IGitService,
    val gitOauthService: IGitOauthService,
    val githubOAuthService: GithubOAuthService,
    val repositoryService: RepositoryService
) {
    fun list(userId: String): List<UserOauthRepositoryInfo> {
        val list = mutableListOf<UserOauthRepositoryInfo>()
        listOf(
            ScmCode.TGIT,
            ScmCode.GITHUB
        ).forEach { scmCode ->
            val userOauthInfo = oauth2TokenStoreManager.get(
                userId = userId,
                scmCode = scmCode.name
            )?.let {
                UserOauthRepositoryInfo(
                    username = it.userId ?: "",
                    repoCount = repositoryService.countOauthRepo(
                        userId = userId,
                        scmType = scmCode.convertScmType()
                    ),
                    type = scmCode,
                    createTime = it.createTime,
                    expired = it.expiresIn?.let { expiresIn ->
                        (it.createTime ?: 0L) + expiresIn * 1000 > System.currentTimeMillis()
                    } ?: false,
                    authorized = true
                )
            } ?: UserOauthRepositoryInfo(
                username = userId,
                repoCount = 0L,
                type = scmCode,
                expired = true,
                authorized = false
            )
            list.add(userOauthInfo)
        }
        return list
    }

    fun relSource(
        userId: String,
        scmCode: ScmCode,
        page: Int,
        pageSize: Int
    ): Page<RepoOauthRefVo> {
        val limit = PageUtil.convertPageSizeToSQLLimit(page, pageSize)
        val result = repositoryService.listOauthRepo(
            userId = userId,
            scmType = scmCode.convertScmType(),
            limit = limit.limit,
            offset = limit.offset
        )
        return Page(page, pageSize, result.count, result.records)
    }

    fun delete(
        userId: String,
        scmCode: ScmCode
    ) {
        // 检查是否还有关联代码库
        if (countOauthRepo(userId, scmCode) > 0) {
            throw ErrorCodeException(
                errorCode = RepositoryMessageCode.OAUTH_INFO_OCCUPIED_CANNOT_DELETE
            )
        }
        oauth2TokenStoreManager.delete(userId = userId, scmCode = scmCode.name)
    }

    fun countOauthRepo(userId: String, scmCode: ScmCode) = repositoryService.countOauthRepo(
        userId = userId,
        scmType = scmCode.convertScmType()
    )

    fun reset(
        userId: String,
        scmCode: ScmCode,
        redirectUrl: String
    ): OauthResetUrl {
        val url = when (scmCode) {
            ScmCode.TGIT -> {
                gitOauthService.getOauthUrl(
                    userId = userId,
                    redirectUrl = redirectUrl
                )
            }

            ScmCode.GITHUB -> {
                githubOAuthService.getGithubOauth(
                    userId = userId,
                    projectId = "",
                    redirectUrlTypeEnum = RedirectUrlTypeEnum.SPEC,
                    specRedirectUrl = redirectUrl,
                    repoHashId = null
                ).redirectUrl
            }
        }
        return OauthResetUrl(url)
    }

    /**
     * 获取git server端真实用户名
     */
    private fun getRealUsername(
        scmCode: ScmCode,
        accessToken: String
    ): Pair<Boolean, String?> {
        var expired = false
        val username = try {
            when (scmCode) {
                ScmCode.TGIT -> {
                    gitService.getUserInfoByToken(
                        token = accessToken,
                        tokenType = TokenTypeEnum.OAUTH
                    ).name
                }

                ScmCode.GITHUB -> {
                    githubOAuthService.getUser(accessToken).login
                }
            }
        } catch (ignored: Exception) {
            logger.warn("get [$scmCode] user info failed", ignored)
            expired = true
            null
        }
        return expired to username
    }

    companion object {
        val logger = LoggerFactory.getLogger(OauthRepositoryService::class.java)
    }
}
