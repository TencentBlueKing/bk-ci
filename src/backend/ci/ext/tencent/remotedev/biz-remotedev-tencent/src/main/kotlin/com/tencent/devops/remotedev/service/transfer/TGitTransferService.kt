package com.tencent.devops.remotedev.service.transfer

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.remotedev.service.GitTransferService
import com.tencent.devops.repository.api.ServiceOauthResource
import com.tencent.devops.repository.pojo.AuthorizeResult
import com.tencent.devops.repository.pojo.enums.RedirectUrlTypeEnum
import org.springframework.beans.factory.annotation.Autowired

class TGitTransferService @Autowired constructor(
    private val client: Client,
) : GitTransferService {
    override fun isOAuth(
        userId: String,
        redirectUrlType: RedirectUrlTypeEnum?,
        redirectUrl: String?,
        gitProjectId: Long,
        refreshToken: Boolean?
    ): Result<AuthorizeResult> {
        return client.get(ServiceOauthResource::class).isOAuth(
            userId = userId,
            redirectUrlType = redirectUrlType,
            redirectUrl = redirectUrl,
            gitProjectId = gitProjectId,
            refreshToken = false
        )
    }
}