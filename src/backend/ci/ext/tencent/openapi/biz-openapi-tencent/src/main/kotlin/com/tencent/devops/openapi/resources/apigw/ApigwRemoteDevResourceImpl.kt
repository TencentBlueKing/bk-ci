package com.tencent.devops.openapi.resources.apigw

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.ApigwRemoteDevResource
import com.tencent.devops.remotedev.api.service.ServiceRemoteDevResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwRemoteDevResourceImpl @Autowired constructor(private val client: Client)
    : ApigwRemoteDevResource {

    companion object {
        private val logger = LoggerFactory.getLogger(ApigwProjectResourceImpl::class.java)
    }
    override fun validateUserTicket(appCode: String?, apigwType: String?, userId: String, isOffshore: Boolean, ticket: String): Result<Boolean> {
        logger.info("Get  projects info by group ,userId:$userId,isOffshore:$isOffshore,ticket:$ticket")
        return client.get(ServiceRemoteDevResource::class).validateUserTicket(
            userId = userId,
            isOffshore = isOffshore,
            ticket = ticket
        )
    }
}
