package com.tencent.devops.openapi.resources.v2

import com.tencent.devops.artifactory.api.ServiceFilePushResource
import com.tencent.devops.artifactory.pojo.dto.PushFileDTO
import com.tencent.devops.artifactory.pojo.vo.PushResultVO
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

    override fun pushFile(
        appCode: String?,
        apigwType: String?,
        userId: String,
        pushInfo: PushFileDTO
    ): Result<Long?> {
        logger.info("pushFile userId: $userId, pushInfo:$pushInfo")
        return client.get(ServiceFilePushResource::class).pushFile(userId, pushInfo)
    }

    override fun checkPushStatus(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        jobInstanceId: Long
    ): Result<PushResultVO?> {
        logger.info("checkPushStatus userId: $userId, projectId:$projectId, jobInstanceId:$jobInstanceId")
        return client.get(ServiceFilePushResource::class).checkPushStatus(userId, projectId, jobInstanceId)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ApigwFileResourceV2Impl::class.java)
    }
}