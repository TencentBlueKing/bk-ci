package com.tencent.devops.openapi.service.apigw

import com.google.common.cache.CacheBuilder
import com.tencent.devops.auth.api.service.ServiceDeptResource
import com.tencent.devops.common.client.Client
import com.tencent.devops.openapi.service.op.OpAppUserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service("opAppUserService")
class TxOpUserService @Autowired constructor(
    val client: Client
) : OpAppUserService {

    private val userCache = CacheBuilder.newBuilder()
        .maximumSize(5000)
        .expireAfterWrite(2, TimeUnit.MINUTES)
        .build<String, Boolean>()

    override fun checkUser(userId: String): Boolean {
        return userCache.getIfPresent(userId) ?: let {
            val result =
                client.get(ServiceDeptResource::class).getUserInfo(userId = "admin", name = userId).data != null
            userCache.put(userId, result)
            return result
        }
    }
}
