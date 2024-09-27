package com.tencent.devops.auth.service.self

import com.tencent.devops.auth.pojo.UserOauthInfo
import com.tencent.devops.auth.pojo.enum.OauthType
import com.tencent.devops.common.client.Client
import com.tencent.devops.repository.api.ServiceGithubResource
import com.tencent.devops.repository.api.github.ServiceGithubUserResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 代码库OAUTH授权
 */
@Service
class RepoGithubOauthService @Autowired constructor(
    override val client: Client
) : AbstractRepoOauthService(
    oauthType = OauthType.GITHUB,
    client = client
) {
    override fun get(userId: String, projectId: String): UserOauthInfo? {
        val gitToken = client.get(ServiceGithubResource::class).getAccessToken(
            userId = userId
        ).data ?: return null
        var expired = false
        // github用户名和userId不一致
        val oauthUserName = try {
            client.get(ServiceGithubUserResource::class).getUser(
                token = gitToken.accessToken
            ).data
        } catch (ignored: Exception) {
            logger.warn("get user info failed", ignored)
            expired = true
            null
        }?.login ?: ""
        return UserOauthInfo(
            username = oauthUserName,
            repoCount = countOauthRepo(projectId = projectId, userId = userId),
            createTime = gitToken.createTime,
            type = oauthType,
            expired = expired
        )
    }

    companion object {
        val logger = LoggerFactory.getLogger(RepoGithubOauthService::class.java)
    }
}