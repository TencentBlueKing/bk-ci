package com.tencent.devops.auth.service

import com.tencent.devops.auth.pojo.OauthRepository
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
    fun list(userId: String, projectId: String): List<UserOauthInfo> {
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

    fun relRepo(
        userId: String,
        projectId: String,
        oauthType: OauthType,
        page: Int,
        pageSize: Int
    ): Page<OauthRepository> {
        val relRepo = getService(oauthType).relRepo(
            userId = userId,
            projectId = projectId,
            page = page,
            pageSize = pageSize
        )
        return Page(
            page = relRepo.page,
            count = relRepo.count,
            pageSize = relRepo.pageSize,
            records = relRepo.records.map {
                OauthRepository(
                    aliasName = it.aliasName,
                    url = it.url
                )
            }
        )
    }

    fun delete(
        projectId: String,
        userId: String,
        oauthType: OauthType
    ) {
        getService(oauthType).delete(
            projectId = projectId,
            userId = userId
        )
    }

    fun reOauth(userId: String, oauthType: OauthType) {
        getService(oauthType).reOauth(userId)
    }

    private fun getService(oauthType: OauthType): OauthService {
        return when (oauthType) {
            OauthType.GIT -> repoGitOauthService
            OauthType.GITHUB -> repoGithubOauthService
        }
    }
}