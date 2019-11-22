package com.tencent.devops.gitci.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.gitci.api.ExternalScmResource
import com.tencent.devops.gitci.service.GitCIRequestService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ExternalScmResourceImpl @Autowired constructor(private val gitCIRequestService: GitCIRequestService) : ExternalScmResource {

    override fun webHookCodeGitCommit(token: String, event: String) =
            Result(gitCIRequestService.externalCodeGitBuild(token, event))
}
