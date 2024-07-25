package com.tencent.devops.remotedev.resources.user

import com.tencent.bk.audit.annotations.AuditEntry
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.remotedev.api.user.UserProjectGitProxyResource
import com.tencent.devops.remotedev.pojo.gitproxy.CreateTGitProjectInfo
import com.tencent.devops.remotedev.pojo.gitproxy.LinktgitData
import com.tencent.devops.remotedev.pojo.gitproxy.TGitNamespace
import com.tencent.devops.remotedev.pojo.gitproxy.TGitRepoData
import com.tencent.devops.remotedev.service.gitproxy.GitProxyTGitService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserProjectGitProxyResourceImpl @Autowired constructor(
    private val gitProxyTGitService: GitProxyTGitService
) : UserProjectGitProxyResource {

    @AuditEntry(actionId = ActionId.TGIT_LINK_CREATE)
    override fun linktgit(
        userId: String,
        projectId: String,
        data: LinktgitData
    ): Result<Map<String, Boolean>> {
        return Result(gitProxyTGitService.checkUserPermission(userId, projectId, data.codeUrls))
    }

    @AuditEntry(actionId = ActionId.TGIT_LINK_LIST)
    override fun tgitList(userId: String, projectId: String): Result<List<TGitRepoData>> {
        return Result(gitProxyTGitService.tgitLinkList(projectId))
    }

    @AuditEntry(actionId = ActionId.TGIT_LINK_DELETE)
    override fun deleteTgitRepo(userId: String, projectId: String, repoId: Long, url: String): Result<Boolean> {
        return Result(gitProxyTGitService.deleteTgitLink(userId, projectId, repoId, url))
    }

    override fun getTGitNamespaces(
        userId: String,
        page: Int,
        pageSize: Int,
        svnProject: Boolean
    ): Result<List<TGitNamespace>> {
        return Result(gitProxyTGitService.getTGitNamespaces(userId, page, pageSize, svnProject))
    }

    @AuditEntry(actionId = ActionId.TGIT_LINK_CREATE)
    override fun createProject(userId: String, data: CreateTGitProjectInfo): Result<Boolean> {
        return Result(gitProxyTGitService.createProjectAndLinkTGit(userId, data))
    }
}
