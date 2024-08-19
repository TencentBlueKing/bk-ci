package com.tencent.devops.remotedev.resources.op

import com.tencent.bk.audit.annotations.AuditEntry
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.remotedev.api.op.OpCodeProxyResource
import com.tencent.devops.remotedev.pojo.gitproxy.CallbackLinktgitData
import com.tencent.devops.remotedev.pojo.gitproxy.UpdateTgitAclIpData
import com.tencent.devops.remotedev.pojo.gitproxy.UpdateTgitAclUserData
import com.tencent.devops.remotedev.service.gitproxy.GitProxyTGitService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpCodeProxyResourceImpl @Autowired constructor(
    private val gitProxyTGitService: GitProxyTGitService
) : OpCodeProxyResource {

    @AuditEntry(actionId = ActionId.TGIT_LINK_CREATE)
    override fun tgitlink(data: CallbackLinktgitData): Result<Map<Long, Boolean>> {
        return Result(
            gitProxyTGitService.linkTGit(
                userId = data.userId,
                projectId = data.projectId,
                // repoId;url\nrepoId;url
                repoIds = data.repoIds.split("\n").filter { it.isNotBlank() }
                    .associate { it.split(";").first().trim().toLong() to it.split(";").last() }
            )
        )
    }

    override fun updateTgitAclIp(
        data: UpdateTgitAclIpData
    ) {
        gitProxyTGitService.addOrRemoveAclIp(
            projectId = data.projectId,
            ips = data.ips,
            remove = data.remove,
            tgitId = data.tgitId
        )
    }

    override fun updateTgitAclUser(
        data: UpdateTgitAclUserData
    ) {
        gitProxyTGitService.refreshProjectTGitSpecUser(
            projectId = data.projectId,
            tgitId = data.tgitId
        )
    }
}
