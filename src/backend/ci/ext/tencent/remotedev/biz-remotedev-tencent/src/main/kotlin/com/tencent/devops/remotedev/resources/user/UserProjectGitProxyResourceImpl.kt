package com.tencent.devops.remotedev.resources.user

import com.tencent.devops.common.web.RestResource
import com.tencent.devops.remotedev.api.user.UserProjectGitProxyResource
import com.tencent.devops.remotedev.pojo.gitproxy.CreateGitProxyData
import com.tencent.devops.remotedev.service.gitproxy.GitProxyService
import org.springframework.beans.factory.annotation.Autowired
import com.tencent.devops.common.api.pojo.Result

@RestResource
class UserProjectGitProxyResourceImpl @Autowired constructor(
    private val gitProxyService: GitProxyService
) : UserProjectGitProxyResource {
    override fun createRepo(userId: String, data: CreateGitProxyData): Result<Boolean> {
        return Result(gitProxyService.createRepo(userId, data))
    }

    override fun fetchRepo(userId: String, projectId: String, page: Int, pageSize: Int): Result<Map<String, String>> {
        return Result(gitProxyService.fetchRepo(
            userId = userId,
            projectId = projectId,
            page = page,
            pageSize = pageSize
        ))
    }

    override fun deleteRepo(userId: String, projectId: String, repoName: String): Result<Boolean> {
        return Result(gitProxyService.deleteRepo(userId, projectId, repoName))
    }
}
