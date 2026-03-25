package com.tencent.devops.auth.resources

import com.tencent.devops.auth.api.ServiceBkTokenResource
import com.tencent.devops.auth.pojo.BkAccessTokenInfo
import com.tencent.devops.auth.service.BkOAuthTokenService
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource

@RestResource
class ServiceBkTokenResourceImpl(
    private val bkOAuthTokenService: BkOAuthTokenService
) : ServiceBkTokenResource {

    override fun getAccessToken(
        userId: String,
        bkTicket: String
    ): Result<BkAccessTokenInfo> {
        return Result(
            bkOAuthTokenService.getAccessToken(
                userId = userId,
                bkTicket = bkTicket
            )
        )
    }
}
