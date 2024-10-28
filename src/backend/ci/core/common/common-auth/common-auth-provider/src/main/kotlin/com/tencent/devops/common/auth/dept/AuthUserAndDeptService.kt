package com.tencent.devops.common.auth.dept

import com.google.common.cache.CacheBuilder
import com.tencent.devops.auth.api.service.ServiceDeptResource
import com.tencent.devops.common.auth.api.AuthUserAndDeptApi
import com.tencent.devops.common.client.Client
import java.util.concurrent.TimeUnit

class AuthUserAndDeptService(
    private val client: Client
) : AuthUserAndDeptApi {
    private val user2DepartedStatus = CacheBuilder.newBuilder()
        .maximumSize(30000)
        .expireAfterWrite(24, TimeUnit.HOURS)
        .build<String/*userId*/, Boolean/*isDeparted*/>()

    override fun checkUserDeparted(name: String): Boolean {
        val isCachePresent = user2DepartedStatus.getIfPresent(name) != null
        if (!isCachePresent) {
            val isUserDeparted = client.get(ServiceDeptResource::class).checkUserDeparted(name).data ?: true
            user2DepartedStatus.put(name, isUserDeparted)
        }
        return user2DepartedStatus.getIfPresent(name) ?: true
    }
}
