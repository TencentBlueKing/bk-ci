package com.tencent.devops.remotedev.resources.op

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.remotedev.api.op.OpCodeProxyResource
import com.tencent.devops.remotedev.pojo.gitproxy.CallbackLinktgitData
import com.tencent.devops.remotedev.service.gitproxy.GitProxyTGitService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpCodeProxyResourceImpl @Autowired constructor(
    private val gitProxyTGitService: GitProxyTGitService
) : OpCodeProxyResource {
    override fun tgitlink(data: CallbackLinktgitData): Result<Map<String, Boolean>> {
        return Result(
            gitProxyTGitService.linkTGit(
                userId = data.userId,
                projectId = data.projectId,
                urls = data.urls
            )
        )
    }
}
