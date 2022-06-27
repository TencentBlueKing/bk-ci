package com.tencent.devops.common.sdk.github.request

import com.tencent.devops.common.sdk.enums.HttpMethod
import com.tencent.devops.common.sdk.github.GithubRequest
import com.tencent.devops.common.sdk.github.response.GetUserResponse

class GetUserRequest : GithubRequest<GetUserResponse>() {
    override fun getHttpMethod() = HttpMethod.GET

    override fun getApiPath() = "/user"
}
