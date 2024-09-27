package com.tencent.devops.auth.service.self

import com.tencent.devops.auth.pojo.UserOauthInfo
import com.tencent.devops.auth.pojo.enum.OauthType
import com.tencent.devops.common.client.Client
import com.tencent.devops.repository.api.ServiceOauthResource
import com.tencent.devops.repository.api.scm.ServiceGitResource
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 代码库OAUTH授权
 */
@Service
class RepoGitOauthService @Autowired constructor(
    override val client: Client
) : AbstractRepoOauthService(
    oauthType = OauthType.GIT,
    client = client
) {
    override fun get(userId: String, projectId: String): UserOauthInfo? {
        val gitToken = client.get(ServiceOauthResource::class).gitGet(userId).data ?: return null
        // 授权过期
        var expired = false
        // 存量数据中可能存在A用户授权到B用户的情况
        val oauthUserName = try {
            client.get(ServiceGitResource::class).getUserInfoByToken(
                token = gitToken.accessToken,
                tokenType = TokenTypeEnum.OAUTH
            ).data
        } catch (ignored: Exception) {
            logger.warn("get user info failed", ignored)
            expired = true
            null
        }?.username ?: ""
        return UserOauthInfo(
            username = oauthUserName,
            repoCount = countOauthRepo(projectId = projectId, userId = userId),
            createTime = gitToken.createTime,
            type = oauthType,
            expired = expired
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RepoGitOauthService::class.java)
    }
}