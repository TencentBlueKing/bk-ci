package com.tencent.devops.repository.pojo.github

import com.tencent.devops.repository.pojo.oauth.GithubTokenType

/**
 * GitHub OAuth 运行时配置快照
 *
 * 通过 GithubOAuthConfigProvider 构造, 遵循"整体切换"策略:
 * 平台管理(DB)中 clientId/clientSecret/callbackUrl 都配齐时, 三者全部使用 DB 值;
 * 否则整体回退到 application.yml 中的 GitConfig
 */
data class GithubOAuthConfig(
    val githubClientId: String,
    val githubClientSecret: String,
    val githubCallbackUrl: String,
    val oauthAppClientId: String,
    val oauthAppClientSecret: String,
    val githubRedirectUrl: String,
    val githubAppUrl: String
) {
    fun clientIdOf(tokenType: GithubTokenType): String = when (tokenType) {
        GithubTokenType.GITHUB_APP -> githubClientId
        GithubTokenType.OAUTH_APP -> oauthAppClientId
    }

    fun clientSecretOf(tokenType: GithubTokenType): String = when (tokenType) {
        GithubTokenType.GITHUB_APP -> githubClientSecret
        GithubTokenType.OAUTH_APP -> oauthAppClientSecret
    }
}
