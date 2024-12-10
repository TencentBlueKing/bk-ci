package com.tencent.devops.auth.service

import com.tencent.devops.auth.pojo.OauthRelResource
import com.tencent.devops.auth.pojo.OauthResetUrl
import com.tencent.devops.auth.pojo.UserOauthInfo
import com.tencent.devops.auth.pojo.enum.OauthType
import com.tencent.devops.auth.service.self.OauthService
import com.tencent.devops.auth.service.self.RepoGitOauthService
import com.tencent.devops.auth.service.self.RepoGithubOauthService
import com.tencent.devops.common.api.pojo.Page
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class UserOauthService @Autowired constructor(
    val repoGithubOauthService: RepoGithubOauthService,
    val repoGitOauthService: RepoGitOauthService
) {
    fun list(userId: String, projectId: String?): List<UserOauthInfo> {
        val list = mutableListOf<UserOauthInfo>()
        listOf(
            repoGitOauthService,
            repoGithubOauthService
        ).forEach {
            val userOauthInfo = it.get(userId, projectId)
            if (userOauthInfo != null) {
                list.add(userOauthInfo)
            }
        }
        return list
    }

    fun relSource(
        userId: String,
        projectId: String?,
        oauthType: OauthType,
        page: Int,
        pageSize: Int
    ): Page<OauthRelResource> {
        return getService(oauthType).relSource(
            userId = userId,
            projectId = projectId,
            page = page,
            pageSize = pageSize
        )
    }

    fun delete(
        projectId: String?,
        userId: String,
        oauthType: OauthType
    ) {
        getService(oauthType).delete(
            projectId = projectId,
            userId = userId
        )
    }

    fun reOauth(
        userId: String,
        oauthType: OauthType,
        redirectUrl: String
    ): OauthResetUrl {
        return getService(oauthType).reOauth(
            userId = userId,
            redirectUrl = redirectUrl
        )
    }

    private fun getService(oauthType: OauthType): OauthService {
        return when (oauthType) {
            OauthType.GIT -> repoGitOauthService
            OauthType.GITHUB -> repoGithubOauthService
        }
    }
}
