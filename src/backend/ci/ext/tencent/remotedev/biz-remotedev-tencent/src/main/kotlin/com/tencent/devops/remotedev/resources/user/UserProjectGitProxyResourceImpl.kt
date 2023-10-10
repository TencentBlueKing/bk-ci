package com.tencent.devops.remotedev.resources.user

import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.remotedev.api.user.UserProjectGitProxyResource
import com.tencent.devops.remotedev.pojo.gitproxy.CreateGitProxyData
import com.tencent.devops.remotedev.service.gitproxy.GitProxyService
import org.springframework.beans.factory.annotation.Autowired
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.remotedev.pojo.gitproxy.FetchRepoResp
import com.tencent.devops.remotedev.pojo.gitproxy.GitType

@RestResource
class UserProjectGitProxyResourceImpl @Autowired constructor(
    private val gitProxyService: GitProxyService
) : UserProjectGitProxyResource {
    override fun createRepo(userId: String, data: CreateGitProxyData): Result<Boolean> {
        return Result(gitProxyService.createRepo(userId, data))
    }

    override fun fetchRepo(
        userId: String,
        projectId: String,
        page: Int,
        pageSize: Int,
        gitType: GitType?
    ): Result<Page<FetchRepoResp>> {
        return Result(
            gitProxyService.fetchRepo(
                userId = userId,
                projectId = projectId,
                page = page,
                pageSize = pageSize,
                gitType = gitType
            )
        )
    }

    override fun deleteRepo(userId: String, projectId: String, repoName: String): Result<Boolean> {
        return Result(gitProxyService.deleteRepo(userId, projectId, repoName))
    }
}
