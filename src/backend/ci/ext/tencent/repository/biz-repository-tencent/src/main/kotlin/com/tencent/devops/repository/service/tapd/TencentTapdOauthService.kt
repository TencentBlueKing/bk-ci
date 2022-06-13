package com.tencent.devops.repository.service.tapd

import com.tencent.devops.common.client.Client
import com.tencent.devops.repository.tapd.service.ITapdOauthService
import com.tencent.devops.scm.api.ServiceTapdResource
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Service
@Primary
class TencentTapdOauthService(
    private val client: Client
) : ITapdOauthService {

    override fun appInstallUrl(userId: String): String {
        return client.getScm(ServiceTapdResource::class).appInstallUrl(userId = userId).data!!
    }

    override fun callbackUrl(code: String, state: String, resource: String): String {
        return client.getScm(ServiceTapdResource::class).callbackUrl(
            code = code,
            state = state,
            resource = resource
        ).data!!
    }
}
