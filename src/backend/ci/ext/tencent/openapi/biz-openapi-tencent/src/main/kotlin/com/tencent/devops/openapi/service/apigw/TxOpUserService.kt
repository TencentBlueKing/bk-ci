package com.tencent.devops.openapi.service.apigw

import com.google.common.cache.CacheBuilder
import com.tencent.devops.common.client.Client
import com.tencent.devops.openapi.service.op.OpAppUserService
import com.tencent.devops.project.api.service.service.ServiceTxUserResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service("opAppUserService")
class TxOpUserService @Autowired constructor(
    val client: Client
) : OpAppUserService {

    private val errorUserId = CacheBuilder.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(2, TimeUnit.MINUTES)
        .build<String, Boolean>()

    override fun checkUser(userId: String): Boolean {
        if (errorUserId.getIfPresent(userId) != null &&
            errorUserId.getIfPresent(userId) == true) {
            return false
        }
        return try {
            client.get(ServiceTxUserResource::class).get(userId)
            true
        } catch (e: Exception) {
            logger.warn("checkUser $userId is not rtx user")
            errorUserId.put(userId, true)
            false
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(TxOpUserService::class.java)
    }
}
