package com.tencent.devops.process.api.user

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.external.api.ServiceGithubResource
import com.tencent.devops.process.pojo.github.GithubAppUrl
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserGithubResourceImpl @Autowired constructor(
    val client: Client
) : UserGithubResource {
    override fun getGithubAppUrl(): Result<GithubAppUrl> {
        val url = client.get(ServiceGithubResource::class).getGithubAppUrl().data!!
        return Result(GithubAppUrl(url))
    }
}