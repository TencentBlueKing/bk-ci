package com.tencent.devops.scm.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.scm.api.ServiceGithubExtResource
import com.tencent.devops.scm.services.ScmGithubExtService

@RestResource
class ServiceGithubExtResourceImpl constructor(
    private val scmGithubExtService: ScmGithubExtService
) : ServiceGithubExtResource {
    override fun webhookCommit(
        event: String,
        guid: String,
        signature: String,
        body: String
    ): Result<Boolean> {
        scmGithubExtService.webhookCommit(
            event = event,
            guid = guid,
            signature = signature,
            body = body
        )
        return Result(true)
    }
}
