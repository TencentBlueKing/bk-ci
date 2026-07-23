package com.tencent.devops.repository.service.github

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.repository.pojo.enums.ScmConfigStatus
import com.tencent.devops.repository.pojo.github.GithubOAuthConfig
import com.tencent.devops.repository.service.RepositoryScmConfigService
import com.tencent.devops.scm.config.GitConfig
import com.tencent.devops.scm.spring.properties.Oauth2ClientProperties
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * GitHub OAuth 配置提供者
 *
 * 遵循"整体切换"策略从平台管理(DB)与 application.yml(GitConfig) 中解析出运行时配置:
 *  1. 若 DB 中 scmCode=GITHUB 记录的 oauth2ClientProperties 三个核心字段
 *     (clientId/clientSecret/redirectUri) 全部非空, 则使用 DB 值;
 *  2. 否则整体回退到 GitConfig
 *
 * 每次调用 getConfig() 会查询一次 DB, 以保证平台管理修改后能及时生效
 */
@Service
class GithubOAuthConfigProvider @Autowired constructor(
    private val gitConfig: GitConfig,
    private val repositoryScmConfigService: RepositoryScmConfigService
) {

    fun getConfig(): GithubOAuthConfig {
        val oauth2Props = loadOauth2PropsFromDb()
        return if (isValidDbConfig(oauth2Props)) {
            buildFromDb(oauth2Props!!)
        } else {
            buildFromGitConfig()
        }
    }

    private fun isValidDbConfig(props: Oauth2ClientProperties?): Boolean {
        return props != null &&
                !props.clientId.isNullOrBlank() &&
                !props.clientSecret.isNullOrBlank()
    }

    private fun buildFromDb(props: Oauth2ClientProperties): GithubOAuthConfig {
        return GithubOAuthConfig(
            githubClientId = props.clientId!!,
            githubClientSecret = props.clientSecret!!,
            githubCallbackUrl = gitConfig.githubCallbackUrl,
            oauthAppClientId = gitConfig.oauthAppClientId,
            oauthAppClientSecret = gitConfig.oauthAppClientSecret,
            githubRedirectUrl = gitConfig.githubRedirectUrl,
            githubAppUrl = gitConfig.githubAppUrl
        )
    }

    private fun buildFromGitConfig(): GithubOAuthConfig {
        return GithubOAuthConfig(
            githubClientId = gitConfig.githubClientId,
            githubClientSecret = gitConfig.githubClientSecret,
            githubCallbackUrl = gitConfig.githubCallbackUrl,
            oauthAppClientId = gitConfig.oauthAppClientId,
            oauthAppClientSecret = gitConfig.oauthAppClientSecret,
            githubRedirectUrl = gitConfig.githubRedirectUrl,
            githubAppUrl = gitConfig.githubAppUrl
        )
    }

    private fun loadOauth2PropsFromDb(): Oauth2ClientProperties? {
        return runCatching {
            repositoryScmConfigService.getOrNull(ScmType.GITHUB.name)
                ?.takeIf { it.status == ScmConfigStatus.SUCCESS && it.oauth2Enabled }
                ?.providerProps
                ?.oauth2ClientProperties
        }.onFailure {
            logger.warn(
                "Failed to load GitHub OAuth config from DB, fallback to GitConfig",
                it
            )
        }.getOrNull()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GithubOAuthConfigProvider::class.java)
    }
}
