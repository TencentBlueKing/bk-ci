package com.tencent.devops.openapi.resources.apigw

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.ApigwRemoteDevResource
import com.tencent.devops.project.api.service.service.ServiceTxProjectResource
import com.tencent.devops.remotedev.api.service.ServiceRemoteDevResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwRemoteDevResourceImpl @Autowired constructor(private val client: Client)
    : ApigwRemoteDevResource {
    override fun updateClientVersion(
        appCode: String?,
        apigwType: String?,
        userId: String,
        env: String,
        version: String
    ): Result<Boolean> {
        logger.info("Get  projects info by group ,userId:$userId,env:$env,version:$version")
        return client.get(ServiceRemoteDevResource::class).updateClientVersion(
            userId = userId,
            env = env,
            version = version
        )
    }
    companion object {
        private val logger = LoggerFactory.getLogger(ApigwProjectResourceImpl::class.java)
    }
}
