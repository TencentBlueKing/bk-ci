package com.tencent.devops.repository.sdk.github.request

import com.tencent.devops.repository.sdk.common.enums.HttpMethod
import com.tencent.devops.repository.sdk.github.GithubRequest
import com.tencent.devops.repository.sdk.github.response.GetUserResponse

class GetUserRequest : GithubRequest<GetUserResponse>() {
    override fun getHttpMethod() = HttpMethod.GET

    override fun getApiPath() = "/user"
}
