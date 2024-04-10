package com.tencent.devops.remotedev.resources.user

import com.tencent.bk.audit.annotations.AuditEntry
import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.remotedev.api.user.UserProjectGitProxyResource
import com.tencent.devops.remotedev.pojo.gitproxy.CreateGitProxyData
import com.tencent.devops.remotedev.pojo.gitproxy.FetchRepoResp
import com.tencent.devops.remotedev.pojo.gitproxy.LinktgitData
import com.tencent.devops.remotedev.pojo.gitproxy.TGitRepoData
import com.tencent.devops.remotedev.service.gitproxy.GitProxyService
import com.tencent.devops.remotedev.service.gitproxy.GitProxyTGitService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserProjectGitProxyResourceImpl @Autowired constructor(
    private val gitProxyService: GitProxyService,
    private val gitProxyTGitService: GitProxyTGitService
) : UserProjectGitProxyResource {
    @AuditEntry(actionId = ActionId.CODE_PROXY_CREATE)
    override fun createRepo(userId: String, data: CreateGitProxyData): Result<Boolean> {
        return Result(gitProxyService.createRepo(userId, data))
    }

    override fun fetchRepo(
        userId: String,
        projectId: String,
        page: Int,
        pageSize: Int,
        gitType: ScmType?
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

    @AuditEntry(actionId = ActionId.CODE_PROXY_DELETE)
    override fun deleteRepo(userId: String, projectId: String, repoName: String): Result<Boolean> {
        return Result(gitProxyService.deleteRepo(userId, projectId, repoName))
    }

    override fun linktgit(
        userId: String,
        projectId: String,
        data: LinktgitData
    ): Result<Map<String, Boolean>> {
        return Result(gitProxyTGitService.checkUserPermission(userId, projectId, data.codeUrls))
    }

    override fun tgitList(userId: String, projectId: String): Result<List<TGitRepoData>> {
        return Result(gitProxyTGitService.tgitLinkList(projectId))
    }

    override fun deleteTgitRepo(userId: String, projectId: String, repoId: Long, url: String): Result<Boolean> {
        return Result(gitProxyTGitService.deleteTgitLink(userId, projectId, repoId, url))
    }
}
