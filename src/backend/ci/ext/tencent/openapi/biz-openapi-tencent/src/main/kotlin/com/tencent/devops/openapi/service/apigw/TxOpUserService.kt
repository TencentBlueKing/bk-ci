package com.tencent.devops.openapi.service.apigw

import com.tencent.devops.common.client.Client
import com.tencent.devops.openapi.service.op.OpAppUserService
import com.tencent.devops.project.api.service.service.ServiceTxUserResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

class TxOpUserService @Autowired constructor(
    val client: Client
): OpAppUserService {
    override fun checkUser(userId: String): Boolean {
        return try {
            client.get(ServiceTxUserResource::class).get(userId!!)
            true
        } catch (e: Exception) {
            logger.warn("checkUser $userId is not rtx user")
            false
        }
    }
    companion object {
        val logger = LoggerFactory.getLogger(TxOpUserService::class.java)
    }
}
