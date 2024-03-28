package com.tencent.devops.repository.sdk.github.request

import com.tencent.devops.repository.sdk.common.enums.HttpMethod
import com.tencent.devops.repository.sdk.github.GithubRequest
import com.tencent.devops.repository.sdk.github.response.GetUserEmailResponse

class GetUserEmailRequest : GithubRequest<List<GetUserEmailResponse>>() {
    override fun getHttpMethod() = HttpMethod.GET

    override fun getApiPath() = "/user/emails"
}
