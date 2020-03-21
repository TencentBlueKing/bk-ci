package com.tencent.devops.openapi.resources.v2

import com.tencent.devops.artifactory.api.ServiceFilePushResource
import com.tencent.devops.artifactory.pojo.dto.PushFileDTO
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.v2.ApigwFileResourceV2
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwFileResourceV2Impl @Autowired constructor(
    private val client: Client
) : ApigwFileResourceV2 {
    override fun pushFile(userId: String, pushInfo: PushFileDTO): Result<Boolean?> {
        logger.info("pushFile userId: $userId, pushInfo:$pushInfo")
        return client.get(ServiceFilePushResource::class).pushFile(userId, pushInfo)
    }
    companion object {
        private val logger = LoggerFactory.getLogger(ApigwFileResourceV2Impl::class.java)
    }
}